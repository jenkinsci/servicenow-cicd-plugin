package io.jenkins.plugins.servicenow;

import hudson.model.FreeStyleProject;
import io.jenkins.plugins.sample.HelloWorldBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class ApplyChangesBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String username = "user";
    final String password = "secret";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new ApplyChangesBuilder(username, password));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new ApplyChangesBuilder(username, password), project.getBuildersList().get(0));
    }

}