package io.jenkins.plugins.servicenow;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.apache.commons.lang3.RandomStringUtils;
import org.kohsuke.stapler.bind.JavaScriptMethod;

public abstract class SNDescriptor extends BuildStepDescriptor<Builder> {

    @JavaScriptMethod
    public synchronized String generateBuilderId() {
        return RandomStringUtils.random(5, true, false);
    }
}
