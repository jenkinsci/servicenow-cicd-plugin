package io.jenkins.plugins.servicenow;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
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

public class ActivatePluginBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(ActivatePluginBuilder.class);

    private String pluginId;

    @DataBoundConstructor
    public ActivatePluginBuilder(final String credentialsId) {
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
    protected boolean perform(@Nonnull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Activate the plugin " + this.pluginId);

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().activatePlugin(this.getPluginId());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'activate plugin' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        } catch (UnknownHostException ex) {
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
                            taskListener.getLogger().println("\nPlugin activation DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nPlugin activation DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                } else { // SUCCESS
                    if(serviceNowResult.getPercentComplete() == 100) {
                        if(StringUtils.isNotBlank(serviceNowResult.getStatusMessage())) {
                            taskListener.getLogger().println("Plugin activation DONE but with message: " + serviceNowResult.getStatusMessage());
                        }
                        result = true;
                    } else {
                        taskListener.getLogger().println("Plugin activation DONE but not completed! Details: " + serviceNowResult.toString());
                        result = false;
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Activate plugin request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when activation of the plugin was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Activate plugin action failed. Check logs!");
        }

        return result;
    }

    @Symbol("activatePlugin")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter("url") String url, @QueryParameter("pluginId") String pluginId)
                throws IOException, ServletException {

            final String regex = "^https?://.+";
            if(url.matches(regex)) {
                return FormValidation.error(Messages.ServiceNowBuilder_DescriptorImpl_errors_wrongUrl());
            }
            if(StringUtils.isBlank(pluginId)) {
                return FormValidation.error(Messages.ActivatePluginBuilder_DescriptorImpl_errors_emptyPluginId());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }


        @Override
        public String getDisplayName() {
            return Messages.ActivatePluginBuilder_DescriptorImpl_DisplayName();
        }

    }
}
