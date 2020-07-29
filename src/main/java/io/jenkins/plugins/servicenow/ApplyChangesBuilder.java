package io.jenkins.plugins.servicenow;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.*;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.util.StopWatch;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;

public class ApplyChangesBuilder extends Builder implements SimpleBuildStep {

    /**
     * Interval in milliseconds between next progress check (ServiceNow API call).
     */
    private static final int CHECK_PROGRESS_INTERVAL = 5000;

    private String url;
    private String credentialsId;
    private String apiVersion;
    private String appScope;
    private String appSysId;
    private String branchName;

    @DataBoundConstructor
    public ApplyChangesBuilder(String credentialsId) {
        super();
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    @DataBoundSetter
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
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
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher,
            @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        setupBuilderParameters(run.getEnvironment(taskListener));

        final StandardUsernamePasswordCredentials usernamePasswordCredentials =
                CredentialsProvider.findCredentialById(this.getCredentialsId(), StandardUsernamePasswordCredentials.class, run, new DomainRequirement());
        final int progressCheckInterval = Integer.parseInt(run.getEnvironment((taskListener)).get(BuildParameters.progressCheckInterval, String.valueOf(CHECK_PROGRESS_INTERVAL)));

        boolean success = performApplyChanges(taskListener, usernamePasswordCredentials.getUsername(), usernamePasswordCredentials.getPassword().getPlainText(), progressCheckInterval);

        stopWatch.stop();
        Long durationInMillis = stopWatch.getTotalTimeMillis();
        long millis = durationInMillis % 1000;
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        String time = String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
        taskListener.getLogger().println(String.format("Elapsed Time: %s ([%f] seconds)", time, stopWatch.getTotalTimeSeconds()));

        if(!success) {
            throw new AbortException("Build Failed");
        }
    }

    private void setupBuilderParameters(EnvVars environment) throws IOException, InterruptedException {
        if(StringUtils.isBlank(this.url)) {
            this.url = environment.get(BuildParameters.instanceUrl);
        }
        if(StringUtils.isBlank(this.credentialsId)) {
            this.credentialsId = environment.get(BuildParameters.credentials);
        }

        if(StringUtils.isBlank(this.apiVersion)) {
            this.apiVersion = environment.get(BuildParameters.apiVersion);
        }
        if(StringUtils.isBlank(this.appScope)) {
            this.appScope = environment.get(BuildParameters.appScope);
        }
        if(StringUtils.isBlank(this.appSysId)) {
            this.appSysId = environment.get(BuildParameters.appSysId);
        }
        if(StringUtils.isBlank(this.branchName)) {
            this.branchName = environment.get(BuildParameters.branchName);
        }
    }

    private boolean performApplyChanges(@Nonnull TaskListener taskListener, final String username, final String password, int progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().format("Call API: %s\nusing:\n - username: %s\n - password: %s\n", this.url, username, password.replaceAll(".", "*"));

        ServiceNowAPIClient restClient = new ServiceNowAPIClient(this.url, username, password);

        Result serviceNowResult = null;
        try {
            serviceNowResult = restClient.applyChanges(this.getAppScope(), this.getAppSysId(), this.getBranchName());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'apply changes' was called: '%s' [details: '%s'].\n", ex.getMessage(), ex.getDetail());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex);
        }

        if(serviceNowResult != null) {
            taskListener.getLogger().println(serviceNowResult.toString());

            if(!ActionStatus.FAILED.getStatus().equals(serviceNowResult.getStatus())) {
                if(!ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                    taskListener.getLogger().println("Checking progress...");
                    try {
                        serviceNowResult = checkProgress(restClient, taskListener.getLogger(), progressCheckInterval);
                    } catch(InterruptedException e) {
                        serviceNowResult = null;
                        e.printStackTrace();
                        e.printStackTrace(taskListener.getLogger());
                    }
                    if(serviceNowResult != null && ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                        taskListener.getLogger().println(serviceNowResult.toString());
                        taskListener.getLogger().println("Changes applied.");
                        result = true;
                    }
                }
            }
        } else {
            taskListener.getLogger().println("Apply changes action failed. Check logs!");
        }

        return result;
    }

    private Result checkProgress(ServiceNowAPIClient restClient, PrintStream logger, int progressCheckInterval) throws InterruptedException {
        Result result = null;
        logger.println("");
        do {
            result = restClient.checkProgress();
            if(result != null) {
                final int progress = result.getPercentComplete();
                logger.print("\rProgress: " + progress + "%");
                if(progress != 100) {
                    Thread.sleep(progressCheckInterval);
                }
            }
        } while(result != null &&
                !ActionStatus.FAILED.getStatus().equals(result.getStatus()) &&
                !ActionStatus.SUCCESSFUL.getStatus().equals(result.getStatus()));

        return result;
    }

    @Symbol("applyChanges")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter String url)
                throws IOException, ServletException {

            final String regex = "^https?://.+";
            if(url.matches(regex)) {
                return FormValidation.error(Messages.ApplyChangesBuilder_DescriptorImpl_errors_wrongUrl());
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
