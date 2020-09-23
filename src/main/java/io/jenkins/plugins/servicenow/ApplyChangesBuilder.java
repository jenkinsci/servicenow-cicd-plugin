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
import java.net.UnknownHostException;

/**
 * Build step responsible for applying changes from a remote source control to a specified local application.
 * See: <a href="https://developer.servicenow.com/dev.do#!/reference/api/orlando/rest/cicd-api#cicd-POST-sc-apply_changes?navFilter=sn_cicd">
 *     API documentation</a>.
 */
public class ApplyChangesBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(ApplyChangesBuilder.class);

    private String appScope;
    private String appSysId;
    private String branchName;

    @DataBoundConstructor
    public ApplyChangesBuilder(String credentialsId) {
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

    public String getBranchName() {
        return branchName;
    }

    @DataBoundSetter
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    @Override
    protected void setupBuilderParameters(EnvVars environment) {
        super.setupBuilderParameters(environment);

        // parameters used in version <= 0.91
        if(StringUtils.isBlank(this.appScope)) {
            this.appScope = environment.get(BuildParameters.appScope);
        }
        if(StringUtils.isBlank(this.appSysId)) {
            this.appSysId = environment.get(BuildParameters.appSysId);
        }
        if(StringUtils.isBlank(this.branchName)) {
            this.branchName = environment.get(BuildParameters.branchName);
        }

        // new ServiceNow Parameter for version > 0.91
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
        }
    }

    @Override
    protected boolean perform(Run<?, ?> run, @Nonnull TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Apply changes");

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().applyChanges(this.getAppScope(), this.getAppSysId(), this.getBranchName());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'apply changes' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        }  catch (UnknownHostException ex) {
            taskListener.getLogger().println("Check connection: " + ex.getMessage());
        } catch(Exception ex) {
            LOG.error(ex.getMessage(), ex);
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
                    if(serviceNowResult != null && ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                        //taskListener.getLogger().println(serviceNowResult.toString());
                        taskListener.getLogger().println("\nChanges applied.");
                        result = true;
                    } else {
                        String message = serviceNowResult != null ? serviceNowResult.getStatusMessage() : "[no message]";
                        taskListener.getLogger().println("\nAction DONE but failed: " + message);
                        result = false;
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Apply changes request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when publishing the application was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Apply changes action failed. Check logs!");
        }

        return result;
    }

    @Symbol("snApplyChanges")
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

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }


        @Override
        public String getDisplayName() {
            return Messages.ApplyChangesBuilder_DescriptorImpl_DisplayName();
        }

    }
}
