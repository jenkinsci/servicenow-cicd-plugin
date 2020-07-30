package io.jenkins.plugins.servicenow;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

public class PublishAppBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(PublishAppBuilder.class);

    private String appScope;
    private String appSysId;
    private String appVersion;
    private String devNotes;

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

    @Override
    protected boolean perform(TaskListener taskListener, String username, String password, Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("START: ServiceNow - Publish the specified application (version: " + this.getAppVersion() + ")");

        ServiceNowAPIClient restClient = new ServiceNowAPIClient(this.getUrl(), username, password);

        Result serviceNowResult = null;
        try {
            serviceNowResult = restClient.publishApp(this.getAppScope(), this.getAppSysId(), this.getAppVersion(), this.getDevNotes());
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
                    taskListener.getLogger().format("\nChecking progress");
                    try {
                        serviceNowResult = checkProgress(restClient, taskListener.getLogger(), progressCheckInterval);
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
                LOG.error("Publish app action replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when publishing the application was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Run test suite action failed. Check logs!");
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
