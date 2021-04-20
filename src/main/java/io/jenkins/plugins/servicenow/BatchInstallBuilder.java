package io.jenkins.plugins.servicenow;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import io.jenkins.plugins.servicenow.utils.Validator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Build step responsible for publishing the specified application and all of its artifacts to the application repository.
 * See API documentation: https://developer.servicenow.com/dev.do#!/reference/api/orlando/rest/cicd-api#cicd-POST-app_repo-publish?navFilter=sn_cicd
 */
public class BatchInstallBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(BatchInstallBuilder.class);

    private static final String DEFAULT_MANIFEST_FILE = "now_batch_manifest.json";

    private String batchName;
    private String packages;
    private String notes;
    private String file;
    private Boolean useFile = Boolean.FALSE;

    private String rollbackId;

    @DataBoundConstructor
    public BatchInstallBuilder(final String credentialsId) {
        super(credentialsId);
    }

    public String getBatchName() {
        return batchName;
    }

    @DataBoundSetter
    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getPackages() {
        if(StringUtils.isBlank(packages)) {
            packages = "[]"; // empty JSON array
        }
        return packages;
    }

    @DataBoundSetter
    public void setPackages(String packages) {
        this.packages = packages;
    }

    public String getNotes() {
        return notes;
    }

    @DataBoundSetter
    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getFile() {
        return file;
    }

    @DataBoundSetter
    public void setFile(String file) {
        this.file = file;
    }

    public Boolean getUseFile() {
        return useFile;
    }

    @DataBoundSetter
    public void setUseFile(Boolean useFile) {
        this.useFile = useFile;
    }

    @Override
    protected boolean perform(Run<?, ?> run, @Nonnull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        if(this.useFile && StringUtils.isBlank(this.file)) {
            this.setFile(DEFAULT_MANIFEST_FILE);
        }

        taskListener.getLogger().println("\nSTART: ServiceNow - Batch Install (packages installation)");
        taskListener.getLogger().println(" param[useFile]: " + this.useFile);
        taskListener.getLogger().println(" param[file]: " + this.file);
        taskListener.getLogger().println(" param[batchName]: " + this.batchName);
        taskListener.getLogger().println(" param[notes]: " + this.notes);
        taskListener.getLogger().println(" param[packages]: " + this.packages);

        Result serviceNowResult = null;
        try {
            serviceNowResult = executeBatchInstall(run, taskListener);
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'batch install' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex.getMessage());
            LOG.error("Unexpected error occurred", ex);
        }

        if(serviceNowResult != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Response from 'batch install' call: " + serviceNowResult.toString());
            }

            if(!ActionStatus.FAILED.getStatus().equals(serviceNowResult.getStatus())) {
                if(!ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                    this.rollbackId = getRollbackBatchVersion(serviceNowResult);
                    final String resultsUrl = getResultsUrl(serviceNowResult);
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
                            taskListener.getLogger().println("\nPackages installation DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nPackages installation DONE but failed: " + serviceNowResult.getStatusMessage());
                            taskListener.getLogger().println("Check following link for details: " + resultsUrl);
                            result = false;
                        }
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("'Batch install' request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when 'batch install' was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("'Batch install' action failed. Check logs!");
        }

        return result;
    }

    private Result executeBatchInstall(Run<?, ?> run, TaskListener taskListener) throws URISyntaxException, InterruptedException, IOException {
        if(this.useFile) {
            String payload = getJsonManifestFromFile(run, taskListener);
            return getRestClient().batchInstall(payload);
        } else {
            return getRestClient().batchInstall(batchName, packages, notes);
        }
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "many null checks not recognized")
    private String getJsonManifestFromFile(Run<?, ?> run, TaskListener taskListener) throws IOException, InterruptedException {
        if(StringUtils.isBlank(this.file)) {
            throw new IllegalArgumentException("Batch file was not defined!");
        }

        EnvVars environment = run.getEnvironment(taskListener);
        if(StringUtils.isBlank(environment.get("WORKSPACE"))) {
            taskListener.getLogger().println(
                    "Environment variable 'WORKSPACE' is not visible inside the build step! Please initialize it first!");
        }

        Path filePath = Paths.get(environment.get("WORKSPACE"), this.file);
        String payload = "";
        try {
            payload = Files.lines(filePath).collect(Collectors.joining(" "));
        } catch(IOException ex) {
            String dirPath = "";
            if(filePath != null && filePath.getParent() != null && filePath.getParent().toAbsolutePath() != null) {
                dirPath = filePath.getParent().toAbsolutePath().toString();
            }
            taskListener.getLogger().println("Batch file '" + this.file + "' was not found in " + dirPath);
            LOG.error("Batch file was not found for the build " + environment.get("JOB_NAME") + "#" + environment.get("BUILD_NUMBER") + "!", ex);
        }
        return payload;
    }

    private String getResultsUrl(Result serviceNowResult) {
        String url = "[url not available]";
        if(serviceNowResult != null &&
                serviceNowResult.getLinks() != null &&
                serviceNowResult.getLinks().getResults() != null &&
                StringUtils.isNotBlank(serviceNowResult.getLinks().getResults().getUrl())) {
            url = serviceNowResult.getLinks().getResults().getUrl();
        }
        return url;
    }

    private String getRollbackBatchVersion(Result serviceNowResult) {
        return serviceNowResult.getLinks() != null ?
                serviceNowResult.getLinks().getRollback() != null ?
                        serviceNowResult.getLinks().getRollback().getId() : null
                : null;
    }

    @Override
    protected List<ParameterValue> setupParametersAfterBuildStep() {
        List<ParameterValue> parameters = new ArrayList<>();
        if(StringUtils.isNotBlank(this.rollbackId)) {
            LOG.info("Store following batch rollback id: " + this.rollbackId);
            if(getGlobalSNParams() != null) {
                getGlobalSNParams().put(ServiceNowParameterDefinition.PARAMS_NAMES.batchRollbackId, this.rollbackId);
                parameters.add(ServiceNowParameterDefinition.createFrom(getGlobalSNParams().toString()).createValue(null, getGlobalSNParams()));
            } else {
                LOG.warn("Batch rollback step will not succeed without defining ServiceNow Parameters for batch rollback id: " + this.rollbackId);
            }
        }
        return parameters;
    }

    @Symbol("snBatchInstall")
    @Extension
    public static final class DescriptorImpl extends SNDescriptor {

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
            return Messages.BatchInstallBuilder_DescriptorImpl_DisplayName();
        }

        public String getDefaultManifestFile() {
            return DEFAULT_MANIFEST_FILE;
        }

    }
}
