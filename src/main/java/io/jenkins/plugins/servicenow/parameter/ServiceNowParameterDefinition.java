package io.jenkins.plugins.servicenow.parameter;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.Constants;
import io.jenkins.plugins.servicenow.Messages;
import io.jenkins.plugins.servicenow.utils.Validator;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ServiceNowParameterDefinition extends ParameterDefinition implements Comparable<ServiceNowParameterDefinition> {

    private static final long serialVersionUID = -3495528189247664707L;

    public static final String PARAMETER_NAME = "snParam";

    public interface PARAMS_NAMES {
        String description = "description";
        String credentialsForPublishedApp = "credentialsForPublishedApp";
        String instanceForPublishedAppUrl = "instanceForPublishedAppUrl";
        String credentialsForInstalledApp = "credentialsForInstalledApp";
        String instanceForInstalledAppUrl = "instanceForInstalledAppUrl";
        String sysId = "sysId";
        String appScope = "appScope";
        String publishedAppVersion = "publishedAppVersion";
        String rollbackAppVersion = "rollbackAppVersion";
        String progressCheckInterval = "progressCheckInterval";
        String batchRollbackId = "batchRollbackId";
    }

    private String credentialsForPublishedApp;
    private String instanceForPublishedAppUrl;
    private String credentialsForInstalledApp;
    private String instanceForInstalledAppUrl;
    private String sysId;
    private String appScope;
    private String publishedAppVersion;
    private String rollbackAppVersion;
    private String batchRollbackId;
    private Integer progressCheckInterval;

    public String getCredentialsForPublishedApp() {
        return credentialsForPublishedApp;
    }

    public String getInstanceForPublishedAppUrl() {
        return instanceForPublishedAppUrl;
    }

    public String getCredentialsForInstalledApp() {
        return credentialsForInstalledApp;
    }

    public String getInstanceForInstalledAppUrl() {
        return instanceForInstalledAppUrl;
    }

    public String getSysId() {
        return sysId;
    }

    public String getAppScope() {
        return appScope;
    }

    public String getPublishedAppVersion() {
        return publishedAppVersion;
    }

    public String getRollbackAppVersion() {
        return rollbackAppVersion;
    }

    public String getBatchRollbackId() {
        return batchRollbackId;
    }

    public Integer getProgressCheckInterval() {
        return progressCheckInterval;
    }

    // Override the standard constructor
    public ServiceNowParameterDefinition(String name) {
        super(PARAMETER_NAME);
    }

    // Override the standard constructor
    public ServiceNowParameterDefinition(String name, String description) {
        super(PARAMETER_NAME, description);
    }

    @DataBoundConstructor
    public ServiceNowParameterDefinition(String description, String credentialsForPublishedApp,
            String instanceForPublishedAppUrl, String credentialsForInstalledApp, String instanceForInstalledAppUrl,
            String sysId, String appScope, String publishedAppVersion, String rollbackAppVersion,
            String batchRollbackId, Integer progressCheckInterval) {
        super(PARAMETER_NAME, description);
        this.credentialsForPublishedApp = credentialsForPublishedApp;
        this.instanceForPublishedAppUrl = instanceForPublishedAppUrl;
        this.credentialsForInstalledApp = credentialsForInstalledApp;
        this.instanceForInstalledAppUrl = instanceForInstalledAppUrl;
        this.sysId = sysId;
        this.appScope = appScope;
        this.publishedAppVersion = publishedAppVersion;
        this.rollbackAppVersion = rollbackAppVersion;
        this.batchRollbackId = batchRollbackId;
        if(progressCheckInterval != null) {
            this.progressCheckInterval = progressCheckInterval;
        } else {
            this.progressCheckInterval = Constants.PROGRESS_CHECK_INTERVAL;
        }
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
        ServiceNowParameterValue snParameterValue = new ServiceNowParameterValue(jsonObject.getString("name"),
                jsonObject.toString());
        return snParameterValue;
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest) {
        ServiceNowParameterValue snParameterValue = new ServiceNowParameterValue(PARAMETER_NAME,
                "{\"name\":\""+PARAMETER_NAME+"\"," +
                "\"" + PARAMS_NAMES.description + "\":\"" + getSafeValue(getDescription()) + "\"," +
                "\"" + PARAMS_NAMES.credentialsForPublishedApp + "\":\"" + getSafeValue(credentialsForPublishedApp) + "\"," +
                "\"" + PARAMS_NAMES.instanceForPublishedAppUrl + "\":\"" + getSafeValue(instanceForPublishedAppUrl) + "\"," +
                "\"" + PARAMS_NAMES.credentialsForInstalledApp + "\":\"" + getSafeValue(credentialsForInstalledApp) + "\"," +
                "\"" + PARAMS_NAMES.instanceForInstalledAppUrl + "\":\"" + getSafeValue(instanceForInstalledAppUrl) + "\"," +
                "\"" + PARAMS_NAMES.sysId + "\":\"" + getSafeValue(sysId) + "\"," +
                "\"" + PARAMS_NAMES.appScope + "\":\"" + getSafeValue(appScope) + "\"," +
                "\"" + PARAMS_NAMES.publishedAppVersion + "\":\"" + getSafeValue(publishedAppVersion) + "\"," +
                "\"" + PARAMS_NAMES.rollbackAppVersion + "\":\"" + getSafeValue(rollbackAppVersion) + "\"," +
                "\"" + PARAMS_NAMES.batchRollbackId + "\":\"" + getSafeValue(batchRollbackId) + "\"," +
                "\"" + PARAMS_NAMES.progressCheckInterval + "\":\"" + getSafeValue(progressCheckInterval) + "\"}"); // create parameter with fields that are used between build steps
        return snParameterValue;
    }

    public static ServiceNowParameterDefinition createFrom(final String value) {
        JSONObject o = JSONObject.fromObject(value);
        return new ServiceNowParameterDefinition(
                getSafeValue((String)o.get(PARAMS_NAMES.description)),
                getSafeValue((String)o.get(PARAMS_NAMES.credentialsForPublishedApp)),
                getSafeValue((String)o.get(PARAMS_NAMES.instanceForPublishedAppUrl)),
                getSafeValue((String)o.get(PARAMS_NAMES.credentialsForInstalledApp)),
                getSafeValue((String)o.get(PARAMS_NAMES.instanceForInstalledAppUrl)),
                getSafeValue((String)o.get(PARAMS_NAMES.sysId)),
                getSafeValue((String)o.get(PARAMS_NAMES.appScope)),
                getSafeValue((String)o.get(PARAMS_NAMES.publishedAppVersion)),
                getSafeValue((String)o.get(PARAMS_NAMES.rollbackAppVersion)),
                getSafeValue((String)o.get(PARAMS_NAMES.batchRollbackId)),
                o.has(PARAMS_NAMES.progressCheckInterval) ? o.getInt(PARAMS_NAMES.progressCheckInterval) : null
        );
    }

    private static String getSafeValue(final String value) {
        return value != null ? value : StringUtils.EMPTY;
    }

    private static String getSafeValue(final Integer value) {
        return value != null ? value.toString() : StringUtils.EMPTY;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol(PARAMETER_NAME)
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ServiceNowParameterDefinition_DescriptorImpl_DisplayName();
        }

        public FormValidation doCheckInstanceForPublishedAppUrl(@QueryParameter String value) {

            if(StringUtils.isNotBlank(value)) {
                if(!Validator.validateInstanceUrl(value)) {
                    return FormValidation.error(Messages.ServiceNowParameterDefinition_DescriptorImpl_errors_wrongInstanceForPublishedAppUrl());
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckInstanceForInstalledAppUrl(@QueryParameter String value) {

            if(StringUtils.isNotBlank(value)) {
                if(!Validator.validateInstanceUrl(value)) {
                    return FormValidation.error(Messages.ServiceNowParameterDefinition_DescriptorImpl_errors_wrongInstanceForInstalledAppUrl());
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckProgressCheckInterval(@QueryParameter Integer value) {

            if(value != null) {
                if(value < 100) {
                    return FormValidation.error(Messages.ServiceNowParameterDefinition_DescriptorImpl_errors_progressCheckIntervalTooSmall());
                }
            }
            return FormValidation.ok();
        }
    }

    @Override
    public int compareTo(ServiceNowParameterDefinition o) {
        return this.equals(o) ? 0 : -1;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}