package io.jenkins.plugins.servicenow.parameter;

import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;

public class ServiceNowParameterValue extends StringParameterValue {

    private static final long serialVersionUID = 1327354888410861686L;

    public ServiceNowParameterValue(String name, String value) {
        super(name, value);
    }

    public String getCredentialsForPublishedApp() {
        return (String)JSONObject.fromObject(this.value).get("credentialsForPublishedApp");
    }
    public String getInstanceForPublishedAppUrl() {
        return (String)JSONObject.fromObject(this.value).get("instanceForPublishedAppUrl");
    }
    public String getCredentialsForInstalledApp() {
        return (String)JSONObject.fromObject(this.value).get("credentialsForInstalledApp");
    }
    public String getInstanceForInstalledAppUrl() {
        return (String)JSONObject.fromObject(this.value).get("instanceForInstalledAppUrl");
    }
    public String getSysId() {
        return (String)JSONObject.fromObject(this.value).get("sysId");
    }
    public String getAppScope() {
        return (String)JSONObject.fromObject(this.value).get("appScope");
    }
    public String getPublishedAppVersion() {
        return (String)JSONObject.fromObject(this.value).get("publishedAppVersion");
    }
    public String getRollbackAppVersion() {
        return (String)JSONObject.fromObject(this.value).get("rollbackAppVersion");
    }
}
