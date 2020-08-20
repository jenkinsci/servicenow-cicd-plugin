package io.jenkins.plugins.servicenow;

import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ApplyChangesBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    String credentials = "3453-2365-4563-6567";
    final String username = "user";
    final String password = "secret";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        RestClientFactory restClientFactory = new RestClientFactory();
        ApplyChangesBuilder builder = new ApplyChangesBuilder(credentials);
        builder.setClientFactory(restClientFactory);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(builder, project.getBuildersList().get(0));
    }

}