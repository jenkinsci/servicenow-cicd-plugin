package io.jenkins.plugins.servicenow;

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
import io.jenkins.plugins.servicenow.api.ResponseUnboundParameters;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstallAppBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(InstallAppBuilder.class);

    private String appScope;
    private String appSysId;
    private String appVersion;
    private String rollbackAppVersion;

    /**
     * duplicated variable for <code>appVersion</code>, because in <code>appVersion</code> must stay original value
     */
    private String appVersionToInstall;

    @DataBoundConstructor
    public InstallAppBuilder(final String credentialsId) {
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

    @Override
    protected boolean perform(@Nonnull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Install the specified application (version: " + Optional.ofNullable(this.appVersionToInstall).orElse("the latest") + ")");
        if(StringUtils.isBlank(this.appVersionToInstall)) {
            taskListener.getLogger().println("WARNING: Parameter '" + BuildParameters.publishedAppVersion + "' is empty.\n" +
                    "Probably the build will fail! Following reason can be:\n" +
                    "1) the step 'publish application' was not launched before,\n" +
                    "2) Jenkins instance was not started with following option:\n" +
                    "\t-Dhudson.model.ParametersAction.safeParameters="+BuildParameters.publishedAppVersion+","+BuildParameters.rollbackAppVersion+" or\n" +
                    "\t-Dhudson.model.ParametersAction.keepUndefinedParameters=true\n" +
                    "3) lack of additional String Parameter defined for the build with the name " + BuildParameters.publishedAppVersion);
        }


        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().installApp(this.getAppScope(), this.getAppSysId(), this.appVersionToInstall);
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'install application' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        }  catch (UnknownHostException ex) {
            taskListener.getLogger().println("Check connection: " + ex.getMessage());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex.getMessage());
        }

        if(serviceNowResult != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Response from 'install app' call: " + serviceNowResult.toString());
            }

            if(!ActionStatus.FAILED.getStatus().equals(serviceNowResult.getStatus())) {
                if(!ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                    this.rollbackAppVersion = (String)getValue(serviceNowResult, ResponseUnboundParameters.rollbackAppVersion);
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
                            taskListener.getLogger().println("\nApplication installed with rollback version " + this.rollbackAppVersion);
                            taskListener.getLogger().println("Installation DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nInstallation DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Install app request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when installation of the application was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Install app action failed. Check logs!");
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
            this.appVersionToInstall = environment.get(BuildParameters.publishedAppVersion);
        } else {
            this.appVersionToInstall = appVersion;
        }
    }

    @Override
    protected List<ParameterValue> setupParametersAfterBuildStep() {
        List<ParameterValue> parameters = new ArrayList<>();
        if(StringUtils.isNotBlank(this.rollbackAppVersion)) {
            parameters.add(new StringParameterValue(BuildParameters.rollbackAppVersion, this.rollbackAppVersion));
            LOG.info("Store following rollback version in case of tests failure: " + this.rollbackAppVersion);
        }
        return parameters;
    }

    @Symbol("snInstallApp")
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
            return Messages.InstallAppBuilder_DescriptorImpl_DisplayName();
        }

    }
}
