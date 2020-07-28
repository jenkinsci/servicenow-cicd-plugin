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
import hudson.util.ListBoxModel;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient.AcceptResponseType;
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

public class RunTestSuiteBuilder extends Builder implements SimpleBuildStep {

    /**
     * Interval in milliseconds between next progress check (ServiceNow API call).
     */
    private static final int CHECK_PROGRESS_INTERVAL = 5000;

    private String url;
    private String credentialsId;
    private String apiVersion;
    private String browserName;
    private String browserVersion;
    private String osName;
    private String osVersion;
    private String testSuiteName;
    private String testSuiteSysId;
    private String responseBodyFormat;

    @DataBoundConstructor
    public RunTestSuiteBuilder(String credentialsId) {
        super();
        this.credentialsId = credentialsId;
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    @DataBoundSetter
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getBrowserName() {
        return browserName;
    }

    @DataBoundSetter
    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    @DataBoundSetter
    public void setBrowserVersion(String browserVersion) {
        this.browserVersion = browserVersion;
    }

    public String getOsName() {
        return osName;
    }

    @DataBoundSetter
    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    @DataBoundSetter
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getTestSuiteName() {
        return testSuiteName;
    }
    @DataBoundSetter
    public void setTestSuiteName(String testSuiteName) {
        this.testSuiteName = testSuiteName;
    }

    public String getTestSuiteSysId() {
        return testSuiteSysId;
    }

    @DataBoundSetter
    public void setTestSuiteSysId(String testSuiteSysId) {
        this.testSuiteSysId = testSuiteSysId;
    }

    public String getResponseBodyFormat() {
        return responseBodyFormat;
    }

    @DataBoundSetter
    public void setResponseBodyFormat(String responseBodyFormat) {
        this.responseBodyFormat = responseBodyFormat;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        setupBuilderParameters(run.getEnvironment(taskListener));

        final StandardUsernamePasswordCredentials usernamePasswordCredentials =
                CredentialsProvider.findCredentialById(this.getCredentialsId(), StandardUsernamePasswordCredentials.class, run, new DomainRequirement());
        final int progressCheckInterval = Integer.parseInt(run.getEnvironment((taskListener)).get(Parameters.progressCheckInterval, String.valueOf(CHECK_PROGRESS_INTERVAL)));

        boolean success = performRunTestSuite(taskListener, usernamePasswordCredentials.getUsername(), usernamePasswordCredentials.getPassword().getPlainText(), progressCheckInterval);

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
            this.url = environment.get(Parameters.instanceUrl);
        }
        if(StringUtils.isBlank(this.credentialsId)) {
            this.credentialsId = environment.get(Parameters.credentials);
        }

        if(StringUtils.isBlank(this.apiVersion)) {
            this.apiVersion = environment.get(Parameters.apiVersion);
        }
    }

    private boolean performRunTestSuite(@Nonnull TaskListener taskListener, final String username, final String password, int progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().format("Call API: %s\nusing:\n - username: %s\n - password: %s\n", this.url, username, password.replaceAll(".", "*"));

        ServiceNowAPIClient restClient = new ServiceNowAPIClient(this.url, username, password);
        restClient.acceptResponseType = AcceptResponseType.valueOf(this.responseBodyFormat);

        Result serviceNowResult = null;
        try {
            serviceNowResult = restClient.runTestSuite(this.getTestSuiteName(),
                    this.getTestSuiteSysId(),
                    this.getOsName(),
                    this.getOsVersion(),
                    this.getBrowserName(),
                    this.getBrowserVersion());
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

    @Symbol("runTestSuite")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter("url") String url)
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
            return Messages.RunTestSuiteBuilder_DescriptorImpl_DisplayName();
        }

        public ListBoxModel doFillResponseBodyFormatItems() {
            ListBoxModel items = new ListBoxModel();
            for (AcceptResponseType responseType : AcceptResponseType.values()) {
                items.add(responseType.name(), responseType.name());
            }
            return items;
        }

    }
}
