package io.jenkins.plugins.servicenow;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.instancescan.ScanAction;
import io.jenkins.plugins.servicenow.instancescan.ScanParameters;
import io.jenkins.plugins.servicenow.instancescan.ScanType;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import io.jenkins.plugins.servicenow.utils.Validator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Build step responsible for instance scan as well as getting progress and results of runs.
 * See API documentation: https://developer.servicenow.com/dev.do#!/reference/api/quebec/....
 */
public class InstanceScanBuilder extends ProgressBuilder {

    static final Logger LOG = LogManager.getLogger(InstanceScanBuilder.class);

    private String scanType;
    private String targetTable;
    private String targetRecordSysId;
    private String comboSysId;
    private String suiteSysId;
    private String requestBody;
    private Set<ScanAction> scanExecutions;

    @DataBoundConstructor
    public InstanceScanBuilder(String credentialsId) {
        super(credentialsId);
    }

    public String getScanType() {
        return scanType;
    }

    @DataBoundSetter
    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getTargetTable() {
        return targetTable;
    }

    @DataBoundSetter
    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getTargetRecordSysId() {
        return targetRecordSysId;
    }

    @DataBoundSetter
    public void setTargetRecordSysId(String targetRecordSysId) {
        this.targetRecordSysId = targetRecordSysId;
    }

    public String getComboSysId() {
        return comboSysId;
    }

    @DataBoundSetter
    public void setComboSysId(String comboSysId) {
        this.comboSysId = comboSysId;
    }

    public String getSuiteSysId() {
        return suiteSysId;
    }

    @DataBoundSetter
    public void setSuiteSysId(String suiteSysId) {
        this.suiteSysId = suiteSysId;
    }

    public String getRequestBody() {
        return requestBody;
    }

    @DataBoundSetter
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    @Inject
    public void setScanExecutions(Set<ScanAction> scanExecutions) {
        this.scanExecutions = scanExecutions;
    }

    @Override
    protected void setupBuilderParameters(EnvVars environment) {
        super.setupBuilderParameters(environment);

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
        }
    }

    @Override
    protected boolean perform(Run<?, ?> run, @Nonnull TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Instance scan");
        taskListener.getLogger().println("Scan type: " + this.scanType);
        taskListener.getLogger().println(" param[target table]: " + this.targetTable);
        taskListener.getLogger().println(" param[target record]: " + this.targetRecordSysId);
        taskListener.getLogger().println(" param[combo sys id]: " + this.comboSysId);
        taskListener.getLogger().println(" param[suite sys id]: " + this.suiteSysId);
        taskListener.getLogger().println(" param[request body]: " + this.requestBody);

        Result serviceNowResult = null;

        try {
            checkInputRequirements();

            Optional<ScanAction> scanExec = scanExecutions.stream()
                    .filter(exec -> exec.isApplicable(ScanType.valueOf(this.scanType)))
                    .findFirst();
            if(scanExec.isPresent()) {
                serviceNowResult = executeScan(scanExec.get());
            }
        } catch(
                ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'instance scan' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        } catch(
                UnknownHostException ex) {
            taskListener.getLogger().println("Check connection: " + ex.getMessage());
        } catch(
                Exception ex) {
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
                        taskListener.getLogger().println("\nInstance scan executed.");
                        result = true;
                    } else {
                        String message = serviceNowResult != null ? serviceNowResult.getStatusMessage() : "[no message]";
                        taskListener.getLogger().println("\nAction DONE but failed: " + message);
                        result = false;
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Instance scan request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when instance scan was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Instance scan action failed. Check logs!");
        }

        return result;
    }

    private void checkInputRequirements() {
        String errorMessage = StringUtils.EMPTY;
        if(CollectionUtils.isEmpty(scanExecutions)) {
            errorMessage += "No scan executors were found in the system!";
        }

        try {
            ScanType.valueOf(this.scanType);
        } catch(Exception ex) {
            errorMessage += System.lineSeparator() +
                    "Invalid value of scan type '" + this.scanType + "'!";
        }

        if(StringUtils.isNotBlank(errorMessage)) {
            errorMessage = "Check requirements before scanning: " + System.lineSeparator() +
                    errorMessage;
            throw new IllegalStateException(errorMessage);
        }
    }

    private Result executeScan(ScanAction scanAction) throws IOException, URISyntaxException {
        String[] params;
        switch(ScanType.valueOf(this.scanType)) {
            case pointScan:
                params = ScanParameters.params()
                        .add(this.targetTable)
                        .add(targetRecordSysId)
                        .build();
                break;
            case scanWithCombo:
                params = ScanParameters.params()
                        .add(comboSysId)
                        .build();
                break;
            case scanWithSuiteOnScopedApp:
            case scanWithSuiteOnUpdateSets:
                ScanParameters sc = ScanParameters.params()
                        .add(suiteSysId);
                if(StringUtils.isNotBlank(this.requestBody)) {
                    sc.add(this.requestBody);
                }
                params = sc.build();
                break;
            default:
                params = ScanParameters.params().build();
        }
        return scanAction.execute(getRestClient(), params);
    }

    @Symbol("snInstanceScan")
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

        public String getTranslatedScanType(String scanType) {
            return scanType;
        }

        public List<String> getScanTypes() {
            return Arrays.stream(ScanType.values())
                    .map(type -> type.toString())
                    .collect(Collectors.toList());
        }

        public ListBoxModel doFillScanTypeItems() {
            ListBoxModel items = new ListBoxModel();

            for(ScanType stype : ScanType.values()) {
                String scanType = stype.toString();
                items.add(CustomMessages.translate(scanType), scanType);
            }

            return items;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.InstanceScanBuilder_DescriptorImpl_DisplayName();
        }

    }

    public static class CustomMessages {

        private final static ResourceBundleHolder holder = ResourceBundleHolder.get(io.jenkins.plugins.servicenow.Messages.class);

        public static String translate(String key) {
            String message = key;
            try {
                message = holder.format("InstanceScanBuilder.ScanType." + key);
            } catch(MissingResourceException ex) {
                InstanceScanBuilder.LOG.error("No translation!", ex);
            }

            return message;
        }
    }

}
