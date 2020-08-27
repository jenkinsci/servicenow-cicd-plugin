package io.jenkins.plugins.servicenow;

import hudson.model.FreeStyleProject;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ApplyChangesBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    String credentials = "3453-2365-4563-6567";

    @Test
    public void testConfigRoundtrip() throws Exception {
        // given
        FreeStyleProject project = jenkins.createFreeStyleProject();
        RestClientFactory restClientFactory = new RestClientFactory();
        ApplyChangesBuilder builder = new ApplyChangesBuilder(credentials);
        builder.setClientFactory(restClientFactory);
        builder.setAppSysId(TestData.appSysId);
        builder.setAppScope(TestData.appScope);
        builder.setBranchName(TestData.branchName);
        builder.setApiVersion(TestData.apiVersion);
        builder.setUrl(TestData.url);
        project.getBuildersList().add(builder);

        // when
        project = jenkins.configRoundtrip(project);

        // then
        ApplyChangesBuilder expected = new ApplyChangesBuilder(credentials);
        expected.setAppSysId(TestData.appSysId);
        expected.setAppScope(TestData.appScope);
        expected.setBranchName(TestData.branchName);
        expected.setApiVersion(TestData.apiVersion);
        expected.setUrl(TestData.url);
        jenkins.assertEqualDataBoundBeans(builder, project.getBuildersList().get(0));
    }


    private static class TestData {
        public static String appSysId = "test001";
        public static String appScope = "testScope";
        public static String branchName = "testBranch";
        public static String apiVersion = "v1";
        public static String url = "https://test";
    }

}