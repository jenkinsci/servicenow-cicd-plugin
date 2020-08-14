package io.jenkins.plugins.servicenow;

import com.google.inject.Guice;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.application.ApplicationVersion;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PublishAppBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(PublishAppBuilder.class);

    private String appScope;
    private String appSysId;
    private String appVersion;
    private String devNotes;
    private Boolean obtainVersionFromSC = true;

    private String calculatedAppVersion;

    private ApplicationVersion applicationVersion;

    @DataBoundConstructor
    public PublishAppBuilder(final String credentialsId) {
        super(credentialsId);
    }

    public String getAppScope() {
        return appScope;
    }

    @DataBoundSetter
    public void setAppScope(String appScope) {
        this.appScope = appScope;
    }

    public String getAppSysId() {
        return appSysId;
    }

    @DataBoundSetter
    public void setAppSysId(String appSysId) {
        this.appSysId = appSysId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    @DataBoundSetter
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getDevNotes() {
        return devNotes;
    }

    @DataBoundSetter
    public void setDevNotes(String devNotes) {
        this.devNotes = devNotes;
    }

    public Boolean getObtainVersionFromSC() {
        return obtainVersionFromSC;
    }

    @DataBoundSetter
    public void setObtainVersionFromSC(Boolean obtainVersionFromSC) {
        this.obtainVersionFromSC = obtainVersionFromSC;
    }

    @Inject
    public void setApplicationVersion(ApplicationVersion applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @Override
    protected boolean perform(@Nonnull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Publish the specified application (version: " + this.calculatedAppVersion + ")");

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().publishApp(this.getAppScope(), this.getAppSysId(), this.calculatedAppVersion, this.getDevNotes());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'publish application' was called: '%s' [details: '%s'].\n", ex.getMessage(), ex.getDetail());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex);
        }

        if(serviceNowResult != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Response from 'publish app' call: " + serviceNowResult.toString());
            }

            if(!ActionStatus.FAILED.getStatus().equals(serviceNowResult.getStatus())) {
                if(!ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                    taskListener.getLogger().format("Checking progress");
                    try {
                        serviceNowResult = checkProgress(taskListener.getLogger(), progressCheckInterval);
                    } catch(InterruptedException e) {
                        serviceNowResult = null;
                        e.printStackTrace();
                        e.printStackTrace(taskListener.getLogger());
                    }
                    if(serviceNowResult != null) {
                        if(ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                            taskListener.getLogger().println("\nPublishing DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nPublishing DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Publish app request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when publishing the application was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Publish app action failed. Check logs!");
        }

        return result;
    }

    @Override
    protected void setupBuilderParameters(EnvVars environment) {
        super.setupBuilderParameters(environment);
        if(StringUtils.isBlank(this.appScope)) {
            this.appScope = environment.get(BuildParameters.appScope);
        }
        if(StringUtils.isBlank(this.appSysId)) {
            this.appSysId = environment.get(BuildParameters.appSysId);
        }
        if(StringUtils.isBlank(this.appVersion)) {
            this.calculatedAppVersion = "1.0." + environment.get("BUILD_NUMBER");
        } else if(this.appVersion.split("\\.").length == 2) {
            this.calculatedAppVersion = this.appVersion + "." + environment.get("BUILD_NUMBER");
        } else {
            this.calculatedAppVersion = this.appVersion;
        }
        if(this.obtainVersionFromSC) {
            this.calculatedAppVersion = Optional.ofNullable(getNextVersionFromSc(environment.get("WORKSPACE")))
                    .orElseGet(() -> {
                        LOG.warn("Application version couldn't be found in the workspace for the build '" + environment.get("JOB_NAME") +
                                "' #" + environment.get("BUILD_NUMBER"));
                        return this.calculatedAppVersion;
                    });
        }
    }

    private String getNextVersionFromSc(String workspace) {
        if(this.applicationVersion == null) {
            Guice.createInjector(new ServiceNowModule()).injectMembers(this);
        }
        final String currentVersion = this.applicationVersion.getVersion(workspace, this.appSysId, this.appScope);
        if(StringUtils.isNotBlank(currentVersion)) {
            String[] versionNumbers = currentVersion.split("\\.");
            if(versionNumbers.length > 1) {
                versionNumbers[versionNumbers.length - 1] = String.valueOf(Integer.parseInt(versionNumbers[2]) + 1);
                return Arrays.stream(versionNumbers).collect(Collectors.joining("."));
            }
        }
        return null;
    }

    @Override
    protected List<ParameterValue> setupParametersAfterBuildStep() {
        List<ParameterValue> parameters = new ArrayList<>();
        if(StringUtils.isNotBlank(this.calculatedAppVersion)) {
            parameters.add(new StringParameterValue(BuildParameters.publishedAppVersion, this.calculatedAppVersion));
            LOG.info("Store following published version to be installed: " + this.calculatedAppVersion);
        }
        return parameters;
    }

@Symbol("publishApp")
@Extension
public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    public FormValidation doCheckName(@QueryParameter String url)
            throws IOException, ServletException {

        final String regex = "^https?://.+";
        if(url.matches(regex)) {
            return FormValidation.error(Messages.ServiceNowBuilder_DescriptorImpl_errors_wrongUrl());
        }
        return FormValidation.ok();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }


    @Override
    public String getDisplayName() {
        return Messages.PublishAppBuilder_DescriptorImpl_DisplayName();
    }

}
}
