package io.jenkins.plugins.servicenow.parameter;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import io.jenkins.plugins.servicenow.Messages;
import io.jenkins.plugins.servicenow.utils.Validator;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

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
    }

    private String credentialsForPublishedApp;
    private String instanceForPublishedAppUrl;
    private String credentialsForInstalledApp;
    private String instanceForInstalledAppUrl;
    private String sysId;
    private String appScope;
    private String publishedAppVersion;
    private String rollbackAppVersion;

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
            String sysId, String appScope, String publishedAppVersion, String rollbackAppVersion) {
        super(PARAMETER_NAME, description);
        this.credentialsForPublishedApp = credentialsForPublishedApp;
        this.instanceForPublishedAppUrl = instanceForPublishedAppUrl;
        this.credentialsForInstalledApp = credentialsForInstalledApp;
        this.instanceForInstalledAppUrl = instanceForInstalledAppUrl;
        this.sysId = sysId;
        this.appScope = appScope;
        this.publishedAppVersion = publishedAppVersion;
        this.rollbackAppVersion = rollbackAppVersion;
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
                "{\"publishedAppVersion\":\"\", \"rollbackAppVersion\":\"\"}"); // create parameter with fields that are used between build steps
        return snParameterValue;
    }

    public static ServiceNowParameterDefinition createFrom(final String value) {
        JSONObject o = JSONObject.fromObject(value);
        return new ServiceNowParameterDefinition(
                (String) o.get(PARAMS_NAMES.description),
                (String) o.get(PARAMS_NAMES.credentialsForPublishedApp),
                (String) o.get(PARAMS_NAMES.instanceForPublishedAppUrl),
                (String) o.get(PARAMS_NAMES.credentialsForInstalledApp),
                (String) o.get(PARAMS_NAMES.instanceForInstalledAppUrl),
                (String) o.get(PARAMS_NAMES.sysId),
                (String) o.get(PARAMS_NAMES.appScope),
                (String) o.get(PARAMS_NAMES.publishedAppVersion),
                (String) o.get(PARAMS_NAMES.rollbackAppVersion)
        );
    }

    @Extension
    @Symbol(PARAMETER_NAME)
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ServiceNowParameterDefinition_DescriptorImpl_DisplayName();
        }

        public FormValidation doCheckInstanceForPublishedAppUrl(@QueryParameter String value)
                throws IOException, ServletException {

            if(StringUtils.isNotBlank(value)) {
                if(!Validator.validateInstanceUrl(value)) {
                    return FormValidation.error(Messages.ServiceNowParameterDefinition_DescriptorImpl_errors_wrongInstanceForPublishedAppUrl());
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckInstanceForInstalledAppUrl(@QueryParameter String value)
                throws IOException, ServletException {

            if(StringUtils.isNotBlank(value)) {
                if(!Validator.validateInstanceUrl(value)) {
                    return FormValidation.error(Messages.ServiceNowParameterDefinition_DescriptorImpl_errors_wrongInstanceForInstalledAppUrl());
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