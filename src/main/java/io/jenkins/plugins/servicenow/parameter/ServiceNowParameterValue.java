package io.jenkins.plugins.servicenow.parameter;

import hudson.model.StringParameterValue;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

public class ServiceNowParameterValue extends StringParameterValue {

    private static final long serialVersionUID = 1327354888410861686L;

    public ServiceNowParameterValue(String name, String value) {
        super(name, value);
    }

    public String getCredentialsForPublishedApp() {
        return getSafeValue("credentialsForPublishedApp");
    }

    public String getInstanceForPublishedAppUrl() {
        return getSafeValue("instanceForPublishedAppUrl");
    }

    public String getCredentialsForInstalledApp() {
        return getSafeValue("credentialsForInstalledApp");
    }

    public String getInstanceForInstalledAppUrl() {
        return getSafeValue("instanceForInstalledAppUrl");
    }

    public String getSysId() {
        return getSafeValue("sysId");
    }

    public String getAppScope() {
        return getSafeValue("appScope");
    }

    public String getPublishedAppVersion() {
        return getSafeValue("publishedAppVersion");
    }

    public String getRollbackAppVersion() {
        return getSafeValue("rollbackAppVersion");
    }

    public Integer getProgressCheckInterval() {
        try {
            return Integer.parseInt(JSONObject.fromObject(this.value).getString("progressCheckInterval"));
        } catch(NumberFormatException | JSONException ex) {
        }
        return null;
    }

    public String getBatchRollbackId() {
        return getSafeValue("batchRollbackId");
    }

    public String getDescription() {
        return getSafeValue("description");
    }

    private String getSafeValue(String parameterName) {
        return JSONObject.fromObject(this.value).has(parameterName) ?
                JSONObject.fromObject(this.value).getString(parameterName) : StringUtils.EMPTY;
    }
}
