package io.jenkins.plugins.servicenow;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
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
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.UnknownHostException;

public class RollbackAppBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(RollbackAppBuilder.class);

    private String appScope;
    private String appSysId;
    private String rollbackAppVersion;

    @DataBoundConstructor
    public RollbackAppBuilder(final String credentialsId) {
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

    public String getRollbackAppVersion() {
        return rollbackAppVersion;
    }

    @DataBoundSetter
    public void setRollbackAppVersion(String rollbackAppVersion) {
        this.rollbackAppVersion = rollbackAppVersion;
    }

    @Override
    protected boolean perform(Run<?, ?> run, @Nonnull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Roll back the specified application (downgrade version: " + this.rollbackAppVersion + ")");
        if(StringUtils.isBlank(this.rollbackAppVersion) && getGlobalSNParams() == null) {
            taskListener.getLogger().println("WARNING: Parameter '" + BuildParameters.rollbackAppVersion + "' is empty.\n" +
                    "Probably the build will fail! Following reason can be:\n" +
                    "1) the step 'install application' was not launched before,\n" +
                    "2) Jenkins instance was not started with following option:\n" +
                    "\t-Dhudson.model.ParametersAction.safeParameters=" + BuildParameters.publishedAppVersion + "," + BuildParameters.rollbackAppVersion + " or\n" +
                    "\t-Dhudson.model.ParametersAction.keepUndefinedParameters=true\n" +
                    "3) lack of additional String Parameter defined for the build with the name " + BuildParameters.rollbackAppVersion + ",\n" +
                    "4) lack of the plugin parameterized-trigger to let trigger new builds and send parameters for new build.");
        }

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().rollbackApp(this.getAppScope(), this.getAppSysId(), this.getRollbackAppVersion());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'rollback application' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        }  catch (UnknownHostException ex) {
            taskListener.getLogger().println("Check connection: " + ex.getMessage());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex.getMessage());
        }

        if(serviceNowResult != null) {

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
                            taskListener.getLogger().println("\nApplication rollback DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nApplication rollback DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                } else { //SUCCESS
                    if(serviceNowResult.getPercentComplete() == 100) {
                        if(StringUtils.isNotBlank(serviceNowResult.getStatusMessage())) {
                            taskListener.getLogger().println("Application rollback DONE but with message: " + serviceNowResult.getStatusMessage());
                        }
                        result = true;
                    } else {
                        taskListener.getLogger().println("Application rollback DONE but not completed! Details: " + serviceNowResult.toString());
                        result = false;
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Rollback app request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when rollback of the application was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Rollback app action failed. Check logs!");
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
        if(StringUtils.isBlank(this.rollbackAppVersion)) {
            this.rollbackAppVersion = environment.get(BuildParameters.rollbackAppVersion);
        }

        // valid for version > 0.92
        if(getGlobalSNParams() != null) {
            final String url = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.instanceForInstalledAppUrl);
            if(StringUtils.isBlank(this.getUrl()) && StringUtils.isNotBlank(url)) {
                this.setUrl(url);
            }
            final String credentialsId = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.credentialsForInstalledApp);
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
            final String appVersion = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.rollbackAppVersion);
            if(StringUtils.isBlank(this.rollbackAppVersion) && StringUtils.isNotBlank(appVersion)) {
                this.rollbackAppVersion = appVersion;
            }
        }
    }

    @Symbol("snRollbackApp")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckUrl(@QueryParameter String value)
                throws IOException, ServletException {

            if(StringUtils.isNotBlank(value)) {
                if(!Validator.validateInstanceUrl(value)) {
                    return FormValidation.error(Messages.ServiceNowBuilder_DescriptorImpl_errors_wrongUrl());
                }
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }


        @Override
        public String getDisplayName() {
            return Messages.RollbackAppBuilder_DescriptorImpl_DisplayName();
        }

    }
}
