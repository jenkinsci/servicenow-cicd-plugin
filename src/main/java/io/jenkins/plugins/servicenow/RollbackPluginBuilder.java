package io.jenkins.plugins.servicenow;

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
import io.jenkins.plugins.servicenow.utils.Validator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.UnknownHostException;

/**
 * Build step rolls back the specified plugin to the previous installed version.
 * See API documentation: https://developer.servicenow.com/dev.do#!/reference/api/orlando/rest/cicd-api#cicd-POST-plugin-rollbackdita?navFilter=sn_cicd
 */
public class RollbackPluginBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(RollbackPluginBuilder.class);

    private String pluginId;

    @DataBoundConstructor
    public RollbackPluginBuilder(final String credentialsId) {
        super(credentialsId);
    }

    public String getPluginId() {
        return pluginId;
    }

    @DataBoundSetter
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    protected boolean perform(Run<?, ?> run, @NonNull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Roll back the plugin " + this.pluginId);

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().rollbackPlugin(this.getPluginId());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'rollback plugin' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        } catch(UnknownHostException ex) {
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
                            taskListener.getLogger().println("\nPlugin roll-back DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nPlugin roll-back DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                } else { //SUCCESS
                    if(serviceNowResult.getPercentComplete() == 100) {
                        if(StringUtils.isNotBlank(serviceNowResult.getStatusMessage())) {
                            taskListener.getLogger().println("Plugin rollback DONE but with message: " + serviceNowResult.getStatusMessage());
                        }
                        result = true;
                    } else {
                        taskListener.getLogger().println("Plugin rollback DONE but not completed! Details: " + serviceNowResult.toString());
                        result = false;
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Rolling back the plugin request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when rolling back of the plugin was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Rollback plugin action failed. Check logs!");
        }

        return result;
    }

    @Symbol("snRollbackPlugin")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if(StringUtils.isBlank(value) || !Validator.validateInstanceUrl(value)) {
                return FormValidation.error(Messages.ServiceNowBuilder_DescriptorImpl_errors_wrongUrl());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckPluginId(@QueryParameter String value) {
            if(StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.ActivatePluginBuilder_DescriptorImpl_errors_emptyPluginId());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String value) {
            if(StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.ActivatePluginBuilder_DescriptorImpl_errors_emptyCredentials());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }


        @Override
        public String getDisplayName() {
            return Messages.RollbackPluginBuilder_DescriptorImpl_DisplayName();
        }

    }
}
