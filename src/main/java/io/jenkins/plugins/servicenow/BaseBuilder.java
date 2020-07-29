package io.jenkins.plugins.servicenow;

import hudson.EnvVars;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public abstract class BaseBuilder extends Builder implements SimpleBuildStep {

    protected String url;
    protected String credentialsId;
    protected String apiVersion;

    public BaseBuilder(String credentialsId) {
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

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    @DataBoundSetter
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    protected void setupBuilderParameters(EnvVars environment) throws IOException, InterruptedException {
        if(StringUtils.isBlank(this.url)) {
            this.url = environment.get(BuildParameters.instanceUrl);
        }
        if(StringUtils.isBlank(this.credentialsId)) {
            this.credentialsId = environment.get(BuildParameters.credentials);
        }

        if(StringUtils.isBlank(this.apiVersion)) {
            this.apiVersion = environment.get(BuildParameters.apiVersion);
        }

        this.setupAdditionalBuilderParameters(environment);
    }

    protected void setupAdditionalBuilderParameters(EnvVars environment) {
        // must be override in a subclass if required
    }
}
