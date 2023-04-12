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

import edu.umd.cs.findbugs.annotations.NonNull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Build step initiates a rollback of a specified application to a specified version.
 * See API documentation: https://developer.servicenow.com/dev.do#!/reference/api/orlando/rest/cicd-api#cicd-POST-app_repo-rollback?navFilter=sn_cicd
 */
public class BatchRollbackBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(BatchRollbackBuilder.class);

    private String rollbackId;

    @DataBoundConstructor
    public BatchRollbackBuilder(final String credentialsId) {
        super(credentialsId);
    }

    public String getRollbackId() {
        return rollbackId;
    }

    @DataBoundSetter
    public void setRollbackId(String rollbackId) {
        this.rollbackId = rollbackId;
    }

    @Override
    protected boolean perform(Run<?, ?> run, @NonNull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Batch Rollback (batch id: " + this.rollbackId + ")");
        if(StringUtils.isBlank(this.rollbackId) && getGlobalSNParams() == null) {
            taskListener.getLogger().println("WARNING: ServiceNow parameter '" + ServiceNowParameterDefinition.PARAMS_NAMES.batchRollbackId + "' is empty.\n" +
                    "Probably the build will fail! One of the reasons can be:\n" +
                    "1) the step 'batch install' was not launched before,\n" +
                    "2) lack of ServiceNow Parameters defined for the build,\n" +
                    "3) lack of the plugin 'parameterized-trigger' to let trigger new builds and send parameters for new build.");
        }

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().batchRollback(this.rollbackId);
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'batch rollback' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
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
                            taskListener.getLogger().println("\nBatch rollback DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nBatch rollback DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                } else { //SUCCESS
                    if(serviceNowResult.getPercentComplete() == 100) {
                        if(StringUtils.isNotBlank(serviceNowResult.getStatusMessage())) {
                            taskListener.getLogger().println("Batch rollback DONE but with message: " + serviceNowResult.getStatusMessage());
                        }
                        result = true;
                    } else {
                        taskListener.getLogger().println("Batch rollback DONE but not completed! Details: " + serviceNowResult.toString());
                        result = false;
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Batch Rollback request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when rollback of the batch was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Batch Rollback action failed. Check logs!");
        }

        return result;
    }

    @Override
    protected void setupBuilderParameters(EnvVars environment) {
        super.setupBuilderParameters(environment);

        if(getGlobalSNParams() != null) {
            final String url = (String)getGlobalSNParams().getOrDefault(ServiceNowParameterDefinition.PARAMS_NAMES.instanceForInstalledAppUrl, null);
            if(StringUtils.isBlank(this.getUrl()) && StringUtils.isNotBlank(url)) {
                this.setUrl(url);
            }
            final String credentialsId = (String)getGlobalSNParams().getOrDefault(ServiceNowParameterDefinition.PARAMS_NAMES.credentialsForInstalledApp, null);
            if(StringUtils.isBlank(this.getCredentialsId()) && StringUtils.isNotBlank(credentialsId)) {
                this.setCredentialsId(credentialsId);
            }
            final String batchRollbackId = getGlobalSNParams().has(ServiceNowParameterDefinition.PARAMS_NAMES.batchRollbackId) ?
                    getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.batchRollbackId) : null;
            if(StringUtils.isBlank(this.rollbackId) && StringUtils.isNotBlank(batchRollbackId)) {
                this.rollbackId = batchRollbackId;
            }
        }
    }

    @Symbol("snBatchRollback")
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
            return Messages.BatchRollbackBuilder_DescriptorImpl_DisplayName();
        }

    }
}
