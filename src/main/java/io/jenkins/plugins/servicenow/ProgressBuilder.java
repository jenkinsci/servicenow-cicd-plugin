package io.jenkins.plugins.servicenow;

import com.google.inject.Guice;
import com.google.inject.Inject;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.util.StopWatch;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 * Base class of other build step classes checking a progress of the CI/CD function associated with a passed-in progress ID.
 * See API documentation: https://developer.servicenow.com/dev.do#!/reference/api/orlando/rest/cicd-api#cicd-GET-progress?navFilter=sn_cicd
 */
public abstract class ProgressBuilder extends Builder implements SimpleBuildStep {

    private String url;
    private String credentialsId;
    private String apiVersion;

    private JSONObject globalSNParams;

    protected FilePath workspace;

    private RunFactory clientFactory;
    /**
     * Rest client initialized every time a build is performed and used by subclasses of the builder.
     * There is no need to serialize the field.
     */
    private transient ServiceNowAPIClient restClient;

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

    public String getCredentialsId() {
        return this.credentialsId;
    }

    @DataBoundSetter
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public JSONObject getGlobalSNParams() {
        return globalSNParams;
    }

    public RunFactory getClientFactory() {
        return clientFactory;
    }

    @Inject
    public void setClientFactory(RunFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ServiceNowAPIClient getRestClient() {
        return this.restClient;
    }

    public void setRestClient(ServiceNowAPIClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath filePath, @NonNull Launcher launcher,
            @NonNull TaskListener taskListener) throws InterruptedException, IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        this.workspace = filePath;

        setupBuilderParameters(run.getEnvironment(taskListener));

        if(this.clientFactory == null) {
            Guice.createInjector(new ServiceNowModule()).injectMembers(this);
        }

        this.restClient = (ServiceNowAPIClient) this.clientFactory.create(run, url, credentialsId);
        final Integer progressCheckInterval = retrieveProgressCheckIntervalParameter(run.getEnvironment((taskListener)));

        boolean success = perform(run, taskListener, progressCheckInterval);

        stopWatch.stop();
        Long durationInMillis = stopWatch.getTotalTimeMillis();
        long millis = durationInMillis % 1000;
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

        String time = String.format("%02d:%02d:%02d.%d", hour, minute, second, millis);
        taskListener.getLogger().println(String.format("Elapsed Time: %s ([%f] seconds)", time, stopWatch.getTotalTimeSeconds()));


        List<ParameterValue> buildVariablesForNextSteps = this.setupParametersAfterBuildStep();
        if(run.getAction(ParametersAction.class) != null && CollectionUtils.isNotEmpty(buildVariablesForNextSteps)) {
            ParametersAction newAction = run.getAction(ParametersAction.class).createUpdated(buildVariablesForNextSteps);
            run.addOrReplaceAction(newAction);
        }

        if(!success) {
            throw new AbortException("Build Failed");
        }
    }

    private int retrieveProgressCheckIntervalParameter(EnvVars environment) {
        Integer parameter = null;
        try {
            parameter = getGlobalSNParams() != null && getGlobalSNParams().getOrDefault(ServiceNowParameterDefinition.PARAMS_NAMES.progressCheckInterval, null) != null ?
                    getGlobalSNParams().getInt(ServiceNowParameterDefinition.PARAMS_NAMES.progressCheckInterval) :
                    Integer.parseInt(environment.get(BuildParameters.progressCheckInterval));
        } catch(NumberFormatException ex) {
        }

        return parameter == null ? Constants.PROGRESS_CHECK_INTERVAL : parameter.intValue();
    }

    protected abstract boolean perform(Run<?, ?> run, @NonNull final TaskListener taskListener, final Integer progressCheckInterval);

    protected void setupBuilderParameters(EnvVars environment) {
        final String globalSNParams = environment.get(ServiceNowParameterDefinition.PARAMETER_NAME);
        if(StringUtils.isNotBlank(globalSNParams)) {
            this.globalSNParams = JSONObject.fromObject(globalSNParams);
        }
        // there are older parameters below valid for the plugin version <= 0.91 (they stay for compatibility purpose)
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

    protected Result checkProgress(PrintStream logger, int progressCheckInterval) throws InterruptedException {
        if(restClient == null) {
            throw new IllegalStateException("Service Now REST client was not initialized!");
        }
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
        if(StringUtils.isNotBlank(serviceNowResult.getStatusDetail())) {
            errorDetail.append("(").append(serviceNowResult.getStatusDetail()).append(") ");
        }
        if(StringUtils.isNotBlank(serviceNowResult.getError())) {
            errorDetail.append(serviceNowResult.getError()).append(".");
        }
        return errorDetail.toString();
    }

    /**
     * Get value from additional response attribute that was strictly not implemented by current structure of the response object.
     *
     * @param result Result returned by ServiceNow API and taken from the response object.
     * @param name   Name of the attribute that should occur in the response
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
