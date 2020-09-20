package io.jenkins.plugins.servicenow;

import com.google.inject.Guice;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.application.ApplicationVersion;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import io.jenkins.plugins.servicenow.utils.Validator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
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
    private Boolean obtainVersionAutomatically = false;

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

    public Boolean getObtainVersionAutomatically() {
        return obtainVersionAutomatically;
    }

    @DataBoundSetter
    public void setObtainVersionAutomatically(Boolean obtainVersionAutomatically) {
        this.obtainVersionAutomatically = obtainVersionAutomatically;
    }

    @Inject
    public void setApplicationVersion(ApplicationVersion applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @Override
    protected boolean perform(Run<?, ?> run, @Nonnull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Publish the specified application");

        Result serviceNowResult = null;
        try {
            calculateNextAppVersion(run.getEnvironment(taskListener));
            taskListener.getLogger().println("> new published application version: " + this.calculatedAppVersion);
            serviceNowResult = getRestClient().publishApp(this.getAppScope(), this.getAppSysId(), this.calculatedAppVersion, this.getDevNotes());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'publish application' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
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

        // valid for version <= 0.92
        if(StringUtils.isBlank(this.appScope)) {
            this.appScope = environment.get(BuildParameters.appScope);
        }
        if(StringUtils.isBlank(this.appSysId)) {
            this.appSysId = environment.get(BuildParameters.appSysId);
        }

        // valid for version > 0.92
        if(getGlobalSNParams() != null) {
            final String url = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.instanceForPublishedAppUrl);
            if(StringUtils.isBlank(this.getUrl()) && StringUtils.isNotBlank(url)) {
                this.setUrl(url);
            }
            final String credentialsId = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.credentialsForPublishedApp);
            if(StringUtils.isBlank(this.getCredentialsId()) && StringUtils.isNotBlank(credentialsId)) {
                this.setCredentialsId(credentialsId);
            }
            final String scope = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.appScope);
            if(StringUtils.isBlank(this.appScope) && StringUtils.isNotBlank(scope)) {
                this.appScope = scope;
            }
            final String sysId = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.sysId);
            if(StringUtils.isBlank(this.appSysId) && StringUtils.isNotBlank(sysId)) {
                this.appSysId = sysId;
            }
            final String appVersion = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.publishedAppVersion);
            if(StringUtils.isBlank(this.appVersion) && StringUtils.isNotBlank(appVersion)) {
                this.calculatedAppVersion = appVersion;
            }
        }
    }

    private void calculateNextAppVersion(EnvVars environment) {
        if(StringUtils.isBlank(this.appVersion)) {
            if(StringUtils.isBlank(this.calculatedAppVersion)) {
                this.calculatedAppVersion = "1.0." + environment.get("BUILD_NUMBER");
            }
        } else if(this.appVersion.split("\\.").length == 2) {
            this.calculatedAppVersion = this.appVersion + "." + environment.get("BUILD_NUMBER");
        } else {
            this.calculatedAppVersion = this.appVersion;
        }
        if(Boolean.TRUE.equals(this.obtainVersionAutomatically)) {
            final String newPublishedVersion = getNextVersionFromAPI();
            if(StringUtils.isBlank(newPublishedVersion)) {
                LOG.warn("Application version couldn't be retrieved from API for the build '" + environment.get("JOB_NAME") +
                        "' #" + environment.get("BUILD_NUMBER"));
                this.calculatedAppVersion = Optional.ofNullable(getNextVersionFromSc(environment.get("WORKSPACE")))
                        .orElseGet(() -> {
                            LOG.warn("Application version couldn't be found in the workspace for the build '" + environment.get("JOB_NAME") +
                                    "' #" + environment.get("BUILD_NUMBER"));
                            return this.calculatedAppVersion;
                        });
            } else {
                this.calculatedAppVersion = newPublishedVersion;
            }
        }
    }

    private String getNextVersionFromSc(String workspace) {
        if(StringUtils.isBlank(workspace)) {
            return null;
        }
        if(this.applicationVersion == null) {
            Guice.createInjector(new ServiceNowModule()).injectMembers(this);
        }
        final String currentVersion = this.applicationVersion.getVersion(workspace, this.appSysId, this.appScope);
        return getNextAppVersion(currentVersion);
    }

    private String getNextVersionFromAPI() {
        if(getRestClient() != null) {
            final String currentVersion = getRestClient().getCurrentAppVersion(this.getAppScope(), this.getAppSysId());
            return getNextAppVersion(currentVersion);
        }
        return null;
    }

    /**
     * Returns next application version based on current version given in the argument.
     * @param currentVersion Current version of the application
     * @return Next valid application version.
     */
    public static String getNextAppVersion(String currentVersion) {
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
            // valid for version <= 0.92
            parameters.add(new StringParameterValue(BuildParameters.publishedAppVersion, this.calculatedAppVersion));
            // valid from version > 0.92
            if(getGlobalSNParams() != null) {
                getGlobalSNParams().replace(ServiceNowParameterDefinition.PARAMS_NAMES.publishedAppVersion, this.calculatedAppVersion);
                parameters.add(ServiceNowParameterDefinition.createFrom(getGlobalSNParams().toString()).createValue(null, getGlobalSNParams()));
            }
            LOG.info("Store following published version to be installed: " + this.calculatedAppVersion);
        }
        return parameters;
    }

    @Symbol("snPublishApp")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if(StringUtils.isNotBlank(value)) {
                if(!Validator.validateInstanceUrl(value)) {
                    return FormValidation.error(Messages.ServiceNowBuilder_DescriptorImpl_errors_wrongUrl());
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckObtainVersionAutomatically(@QueryParameter Boolean value, @QueryParameter("appVersion") String appVersion) {
            if(value && StringUtils.isNotBlank(appVersion)) {
                return FormValidation.warning(Messages.PublishAppBuilder_DescriptorImpl_warnings_obtainVersionAutomatically());
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
