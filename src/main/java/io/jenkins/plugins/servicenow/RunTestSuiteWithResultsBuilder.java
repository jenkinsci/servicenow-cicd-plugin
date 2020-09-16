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
import io.jenkins.plugins.servicenow.api.ResponseUnboundParameters;
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

import javax.annotation.Nonnull;
import java.text.MessageFormat;

public class RunTestSuiteWithResultsBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(RunTestSuiteWithResultsBuilder.class);

    private String browserName;
    private String browserVersion;
    private String osName;
    private String osVersion;
    private String testSuiteName;
    private String testSuiteSysId;
    private String responseBodyFormat;
    private Boolean withResults;

    @DataBoundConstructor
    public RunTestSuiteWithResultsBuilder(String credentialsId) {
        super(credentialsId);
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

    public Boolean getWithResults() {
        return withResults;
    }

    @DataBoundSetter
    public void setWithResults(Boolean withResults) {
        this.withResults = withResults;
    }

    @Override
    protected boolean perform(Run<?, ?> run, @Nonnull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().format("%nSTART: ServiceNow - Run test suite '%s' [%s]", this.getTestSuiteName(), this.getTestSuiteSysId());

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().runTestSuite(this.getTestSuiteName(),
                    this.getTestSuiteSysId(),
                    this.getOsName(),
                    this.getOsVersion(),
                    this.getBrowserName(),
                    this.getBrowserVersion());
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'run test suite' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex);
        }

        if(serviceNowResult != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Response from 'run test suite' call: " + serviceNowResult.toString());
            }

            if(!ActionStatus.FAILED.getStatus().equals(serviceNowResult.getStatus())) {
                if(!ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                    taskListener.getLogger().format("%nChecking progress");
                    try {
                        serviceNowResult = checkProgress(taskListener.getLogger(), progressCheckInterval);
                    } catch(InterruptedException e) {
                        serviceNowResult = null;
                        e.printStackTrace();
                        e.printStackTrace(taskListener.getLogger());
                    }
                    if(serviceNowResult != null) {
                        if(ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                            taskListener.getLogger().println("\nTest suite DONE.");
                            result = true;

                            result &= generateTestResult(taskListener, serviceNowResult);
                        } else {
                            taskListener.getLogger().println("\nTest suite DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                }
            }
        } else {
            taskListener.getLogger().println("Run test suite action failed. Check logs!");
        }

        return result;
    }

    @Override
    protected void setupBuilderParameters(EnvVars environment) {
        super.setupBuilderParameters(environment);

        if(getGlobalSNParams() != null) {
            final String url = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.instanceForInstalledAppUrl);
            if(StringUtils.isBlank(this.getUrl()) && StringUtils.isNotBlank(url)) {
                this.setUrl(url);
            }
            final String credentialsId = getGlobalSNParams().getString(ServiceNowParameterDefinition.PARAMS_NAMES.credentialsForInstalledApp);
            if(StringUtils.isBlank(this.getCredentialsId()) && StringUtils.isNotBlank(credentialsId)) {
                this.setCredentialsId(credentialsId);
            }
        }
    }

    private boolean generateTestResult(@Nonnull TaskListener taskListener, final Result serviceNowResult) {
        if(Boolean.TRUE.equals(this.withResults)) {
            final String testSuiteResultsId = serviceNowResult.getLinks().getResults() != null ?
                    serviceNowResult.getLinks().getResults().getId() : StringUtils.EMPTY;
            return performTestSuiteResults(taskListener, testSuiteResultsId);
        }
        return true;
    }

    private boolean performTestSuiteResults(final TaskListener taskListener, final String resultsId) {
        boolean result = false;

        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().getTestSuiteResults(resultsId);
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API 'GET /sn_cicd/testsuite/results/{result_id}' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex);
        }

        if(serviceNowResult != null) {
            taskListener.getLogger().println("TEST SUITE results");
            taskListener.getLogger().println(formatTestResults(serviceNowResult));

            if(ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                result = true;
            }
        } else {
            taskListener.getLogger().println("Test suite result action failed. Check logs!");
        }

        return result;
    }

    private String formatTestResults(Result serviceNowResult) {
        return MessageFormat.format(
                "\tTest suite name:\t{0}%n" +
                        "\tStatus:\t\t{1}%n" +
                        "\tDuration:\t{2}%n" +
                        "\tSuccessfully rolledup tests:\t{3}%n" +
                        "\tFailed rolledup tests:\t{4}%n" +
                        "\tSkipped rolledup tests:\t{5}%n" +
                        "\tRolledup tests with error:\t{6}%n" +
                        "\tLink to the result: {7}",
                getValue(serviceNowResult, ResponseUnboundParameters.TestResults.name),
                getValue(serviceNowResult, ResponseUnboundParameters.TestResults.status),
                getValue(serviceNowResult, ResponseUnboundParameters.TestResults.duration),
                getValue(serviceNowResult, ResponseUnboundParameters.TestResults.rolledupTestSuccessCount),
                getValue(serviceNowResult, ResponseUnboundParameters.TestResults.rolledupTestFailureCount),
                getValue(serviceNowResult, ResponseUnboundParameters.TestResults.rolledupTestSkipCount),
                getValue(serviceNowResult, ResponseUnboundParameters.TestResults.rolledupTestErrorCount),
                serviceNowResult.getLinks().getResults().getUrl()
        );
    }

    @Symbol("snRunTestSuite")
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

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.RunTestSuiteWithResultsBuilder_DescriptorImpl_DisplayName();
        }

    }
}
