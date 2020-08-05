package io.jenkins.plugins.servicenow;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Builder;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.util.StopWatch;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

public abstract class ProgressBuilder extends Builder implements SimpleBuildStep {

    /**
     * Interval in milliseconds between next progress check (ServiceNow API call).
     */
    private static final int CHECK_PROGRESS_INTERVAL = 5000;

    private String url;
    private String credentialsId;
    private String apiVersion;

    public ProgressBuilder(final String credentialsId) {
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

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher,
            @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        setupBuilderParameters(run.getEnvironment(taskListener));

        final StandardUsernamePasswordCredentials usernamePasswordCredentials =
                CredentialsProvider.findCredentialById(this.credentialsId, StandardUsernamePasswordCredentials.class, run, new DomainRequirement());
        final Integer progressCheckInterval = Integer.parseInt(run.getEnvironment((taskListener)).get(BuildParameters.progressCheckInterval, String.valueOf(CHECK_PROGRESS_INTERVAL)));

        boolean success = perform(taskListener, usernamePasswordCredentials.getUsername(), usernamePasswordCredentials.getPassword().getPlainText(), progressCheckInterval);

        stopWatch.stop();
        Long durationInMillis = stopWatch.getTotalTimeMillis();
        long millis = durationInMillis % 1000;
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        String time = String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
        taskListener.getLogger().println(String.format("Elapsed Time: %s ([%f] seconds)", time, stopWatch.getTotalTimeSeconds()));

        //((AbstractBuild)run).getBuildVariables();
        List<ParameterValue> buildVariablesForNextSteps = this.setupParametersAfterBuildStep();
        ParametersAction newAction = run.getAction(ParametersAction.class).createUpdated(buildVariablesForNextSteps);
        run.addOrReplaceAction(newAction);

        if(!success) {
            throw new AbortException("Build Failed");
        }
    }

    protected abstract boolean perform(@Nonnull final TaskListener taskListener, final String username, final String password, final Integer progressCheckInterval);

    protected void setupBuilderParameters(EnvVars environment) {
        if(StringUtils.isBlank(this.url)) {
            this.url = environment.get(BuildParameters.instanceUrl);
        }
        if(StringUtils.isBlank(this.credentialsId)) {
            this.credentialsId = environment.get(BuildParameters.credentials);
        }

        if(StringUtils.isBlank(this.apiVersion)) {
            this.apiVersion = environment.get(BuildParameters.apiVersion);
        }
    }

    protected List<ParameterValue> setupParametersAfterBuildStep() {
        // nothing to do here
        return Collections.emptyList();
    }

    protected Result checkProgress(ServiceNowAPIClient restClient, PrintStream logger, int progressCheckInterval) throws InterruptedException {
        Result result = null;
        do {
            logger.print(".");
            result = restClient.checkProgress();
            if(result != null) {
                final int progress = result.getPercentComplete();
                if(progress != 100) {
                    Thread.sleep(progressCheckInterval);
                }
            }
        } while(result != null &&
                !ActionStatus.FAILED.getStatus().equals(result.getStatus()) &&
                !ActionStatus.SUCCESSFUL.getStatus().equals(result.getStatus()));

        return result;
    }

    protected String buildErrorDetailFromFailedResponse(Result serviceNowResult) {
        StringBuilder errorDetail = new StringBuilder();
        if(StringUtils.isNotBlank(serviceNowResult.getStatusMessage())) {
            errorDetail.append(serviceNowResult.getStatusMessage()).append(". ");
        }
        if (StringUtils.isNotBlank(serviceNowResult.getStatusDetail())) {
            errorDetail.append("(").append(serviceNowResult.getStatusDetail()).append(") ");
        }
        if(StringUtils.isNotBlank(serviceNowResult.getError())) {
            errorDetail.append(serviceNowResult.getError()).append(".");
        }
        return errorDetail.toString();
    }

    /**
     * Get value from additional response attribute that was strictly not implemented by current structure of the response object.
     * @param result Result returned by ServiceNow API and taken from the response object.
     * @param name Name of the attribute that should occur in the response
     * @return Value of the attribute
     */
    protected Object getValue(final Result result, final String name) {
        if(result.getUnboundAttributes() != null &&
                result.getUnboundAttributes().size() > 0 &&
                result.getUnboundAttributes().containsKey(name)) {
            return result.getUnboundAttributes().get(name);
        }
        return StringUtils.EMPTY;
    }
}
