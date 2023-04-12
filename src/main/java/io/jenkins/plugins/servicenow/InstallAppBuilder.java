package io.jenkins.plugins.servicenow;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ResponseUnboundParameters;
import io.jenkins.plugins.servicenow.api.ServiceNowApiException;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition.PARAMS_NAMES;
import io.jenkins.plugins.servicenow.utils.Validator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Build step used for installation of an application.
 * See API documentation: https://developer.servicenow.com/dev.do#!/reference/api/orlando/rest/cicd-api#cicd-POST-app_repo-install?navFilter=sn_cicd
 */
public class InstallAppBuilder extends ProgressBuilder {

    private static final Logger LOG = LogManager.getLogger(InstallAppBuilder.class);

    private String appScope;
    private String appSysId;
    private String appVersion;
    private String rollbackAppVersion;
    private String baseAppVersion;
    private Boolean baseAppAutoUpgrade;

    /**
     * duplicated variable for <code>appVersion</code>, because in <code>appVersion</code> must stay original value
     */
    private String appVersionToInstall;

    @DataBoundConstructor
    public InstallAppBuilder(final String credentialsId) {
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

    public Boolean getBaseAppAutoUpgrade() {
        return baseAppAutoUpgrade;
    }

    @DataBoundSetter
    public void setBaseAppAutoUpgrade(Boolean baseAppAutoUpgrade) {
        this.baseAppAutoUpgrade = baseAppAutoUpgrade;
    }

    public String getBaseAppVersion() {
        return baseAppVersion;
    }

    @DataBoundSetter
    public void setBaseAppVersion(String baseAppVersion) {
        this.baseAppVersion = baseAppVersion;
    }

    @Override
    protected boolean perform(Run<?, ?> run, @NonNull final TaskListener taskListener, final Integer progressCheckInterval) {
        boolean result = false;

        taskListener.getLogger().println("\nSTART: ServiceNow - Install the specified application (version: " + Optional.ofNullable(this.appVersionToInstall).orElse("the latest") + ")");
        if(StringUtils.isBlank(this.appVersionToInstall) && getGlobalSNParams() == null) {
            taskListener.getLogger().println("WARNING: Parameter '" + BuildParameters.publishedAppVersion + "' is empty.\n" +
                    "Probably the build will fail! Following reason can be:\n" +
                    "1) the step 'publish application' was not launched before,\n" +
                    "2) Jenkins instance was not started with following option:\n" +
                    "\t-Dhudson.model.ParametersAction.safeParameters="+BuildParameters.publishedAppVersion+","+BuildParameters.rollbackAppVersion+" or\n" +
                    "\t-Dhudson.model.ParametersAction.keepUndefinedParameters=true\n" +
                    "3) lack of additional String Parameter defined for the build with the name " + BuildParameters.publishedAppVersion);
        }


        Result serviceNowResult = null;
        try {
            serviceNowResult = getRestClient().installApp(
                    this.getAppScope(),
                    this.getAppSysId(),
                    this.appVersionToInstall,
                    this.baseAppVersion,
                    this.baseAppAutoUpgrade);
        } catch(ServiceNowApiException ex) {
            taskListener.getLogger().format("Error occurred when API with the action 'install application' was called: '%s' [details: '%s'].%n", ex.getMessage(), ex.getDetail());
        }  catch (UnknownHostException ex) {
            taskListener.getLogger().println("Check connection: " + ex.getMessage());
        } catch(Exception ex) {
            taskListener.getLogger().println(ex.getMessage());
        }

        if(serviceNowResult != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Response from 'install app' call: " + serviceNowResult.toString());
            }

            if(!ActionStatus.FAILED.getStatus().equals(serviceNowResult.getStatus())) {
                if(!ActionStatus.SUCCESSFUL.getStatus().equals(serviceNowResult.getStatus())) {
                    this.rollbackAppVersion = (String)getValue(serviceNowResult, ResponseUnboundParameters.rollbackAppVersion);
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
                            taskListener.getLogger().println("\nApplication installed with rollback version " + this.rollbackAppVersion);
                            taskListener.getLogger().println("Installation DONE.");
                            result = true;
                        } else {
                            taskListener.getLogger().println("\nInstallation DONE but failed: " + serviceNowResult.getStatusMessage());
                            result = false;
                        }
                    }
                }
            } else { // serve result with the status FAILED
                LOG.error("Install app request replied with failure: " + serviceNowResult);
                String errorDetail = this.buildErrorDetailFromFailedResponse(serviceNowResult);
                taskListener.getLogger().println("Error occurred when installation of the application was requested: " + errorDetail);
            }
        } else {
            taskListener.getLogger().println("Install app action failed. Check logs!");
        }

        return result;
    }

    @Override
    protected void setupBuilderParameters(EnvVars environment) {
        super.setupBuilderParameters(environment);

        // parameters used in version <= 0.91
        if(StringUtils.isBlank(this.appScope)) {
            this.appScope = environment.get(BuildParameters.appScope);
        }
        if(StringUtils.isBlank(this.appSysId)) {
            this.appSysId = environment.get(BuildParameters.appSysId);
        }
        if(StringUtils.isBlank(this.appVersion)) {
            this.appVersionToInstall = environment.get(BuildParameters.publishedAppVersion);
        } else {
            this.appVersionToInstall = appVersion;
        }

        // new ServiceNow Parameter for version > 0.91
        if(getGlobalSNParams() != null) {
            final String url = getGlobalSNParams().getString(PARAMS_NAMES.instanceForInstalledAppUrl);
            if(StringUtils.isBlank(this.getUrl()) && StringUtils.isNotBlank(url)) {
                this.setUrl(url);
            }
            final String credentialsId = getGlobalSNParams().getString(PARAMS_NAMES.credentialsForInstalledApp);
            if(StringUtils.isBlank(this.getCredentialsId()) && StringUtils.isNotBlank(credentialsId)) {
                this.setCredentialsId(credentialsId);
            }
            final String scope = getGlobalSNParams().getString(PARAMS_NAMES.appScope);
            if(StringUtils.isBlank(this.appScope) && StringUtils.isNotBlank(scope)) {
                this.appScope = scope;
            }
            final String sysId = getGlobalSNParams().getString(PARAMS_NAMES.sysId);
            if(StringUtils.isBlank(this.appSysId) && StringUtils.isNotBlank(sysId)) {
                this.appSysId = sysId;
            }
            final String appVersion = getGlobalSNParams().getString(PARAMS_NAMES.publishedAppVersion);
            if(StringUtils.isBlank(this.appVersion) && StringUtils.isNotBlank(appVersion)) {
                this.appVersionToInstall = appVersion;
            }
        }
    }

    @Override
    protected List<ParameterValue> setupParametersAfterBuildStep() {
        List<ParameterValue> parameters = new ArrayList<>();
        if(StringUtils.isNotBlank(this.rollbackAppVersion)) {
            // valid for version <= 0.92
            parameters.add(new StringParameterValue(BuildParameters.rollbackAppVersion, this.rollbackAppVersion));
            // valid from version > 0.92
            if(getGlobalSNParams() != null) {
                getGlobalSNParams().replace(PARAMS_NAMES.rollbackAppVersion, this.rollbackAppVersion);
                parameters.add(ServiceNowParameterDefinition.createFrom(getGlobalSNParams().toString()).createValue(null, getGlobalSNParams()));
            }
            LOG.info("Store following rollback version in case of tests failure: " + this.rollbackAppVersion);
        }

        return parameters;
    }

    @Symbol("snInstallApp")
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


        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.InstallAppBuilder_DescriptorImpl_DisplayName();
        }

    }
}
