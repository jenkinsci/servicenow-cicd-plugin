package io.jenkins.plugins.servicenow;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.Label;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

@Ignore("Set up API credentials by creating environment variables USERNAME and PASSWORD and then run integration tests.")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SNStepsIntegrationTest {

    private static Logger LOG = LogManager.getLogger(SNStepsIntegrationTest.class);

    private static String HOST_PUBLISHING = "https://cicdjenkinsappauthor.service-now.com";
    private static String HOST_INSTALLATION = "https://cicdjenkinsappclient.service-now.com";
    private static String SYSTEM_ID = "90eb12afdb021010b40a9eb5db9619aa";
    private static String APP_SCOPE = "x_sofse_cicdjenkin";
    private static String CREDENTIALS_ID = "1234";
    private static String USER_SN = System.getenv("USERNAME");
    private static String PASSWORD_SN = System.getenv("PASSWORD");

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setUp() throws IOException {
        final StandardUsernamePasswordCredentials credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                CREDENTIALS_ID,
                "test",
                USER_SN,
                getPW());
        final CredentialsStore store = CredentialsProvider.lookupStores(jenkins.jenkins).iterator().next();
        store.addCredentials(Domain.global(), credentials);
    }

    @Test
    public void testA_PublishAppWithEmptyCredentials() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "testJob");
        String script = "node {\n" +
                " snPublishApp()\n" +
                "}";
        String credentialsId = "";

        job.setDefinition(new CpsFlowDefinition(script, true));
        ServiceNowParameterDefinition snParams = new ServiceNowParameterDefinition("",
                credentialsId,
                HOST_PUBLISHING,
                credentialsId,
                HOST_INSTALLATION,
                SYSTEM_ID,
                APP_SCOPE,
                "",
                "",
                "",
                null);

        job.addProperty(new ParametersDefinitionProperty(snParams));
        ParametersAction action = new ParametersAction(snParams.createValue(null));
        final WorkflowRun build = jenkins.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0, action));
        //LOG.info(jenkins.getLog(build));
        jenkins.assertLogContains("snPublishApp", build);
    }

    @Test
    public void testB_PublishAppWithCredentials() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "testJob");
        String script = "node {\n" +
                " snPublishApp obtainVersionAutomatically: true\n" +
                "}";
        String credentialsId = CREDENTIALS_ID;

        job.setDefinition(new CpsFlowDefinition(script, true));
        ServiceNowParameterDefinition snParams = new ServiceNowParameterDefinition("",
                credentialsId,
                HOST_PUBLISHING,
                credentialsId,
                HOST_INSTALLATION,
                SYSTEM_ID,
                "",
                "",
                "",
                "",
                null);

        job.addProperty(new ParametersDefinitionProperty(snParams));
        ParametersAction action = new ParametersAction(snParams.createValue(null));
        WorkflowRun build = job.getParameterizedJobMixIn().scheduleBuild2(0,action).get();//job.scheduleBuild2(0).get();
        //LOG.info(jenkins.getLog(build));
        jenkins.assertLogContains("START: ServiceNow - Publish the specified application", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void testC_InstallTestRollbackAppWithPlugin() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "testJob");
        String script = "node {\n" +
                " snActivatePlugin url: '" + HOST_INSTALLATION + "', credentialsId: '" + CREDENTIALS_ID + "', pluginId: 'com.servicenow_now_calendar'\n" +
                " snInstallApp()\n" +
                " snRunTestSuite browserName: 'Firefox', osName: 'Windows', osVersion: '10', testSuiteName: 'My CHG:Change Management', withResults: true\n" +
                " snRollbackApp()\n" +
                " snRollbackPlugin url: '" + HOST_INSTALLATION + "', credentialsId: '" + CREDENTIALS_ID + "', pluginId: 'com.servicenow_now_calendar'\n" +
                "}";
        String credentialsId = CREDENTIALS_ID;


        job.setDefinition(new CpsFlowDefinition(script, true));
        ServiceNowParameterDefinition snParams = new ServiceNowParameterDefinition("",
                credentialsId,
                HOST_PUBLISHING,
                credentialsId,
                HOST_INSTALLATION,
                SYSTEM_ID,
                "",
                "",
                "",
                "",
                null);

        job.addProperty(new ParametersDefinitionProperty(snParams));
        ParametersAction action = new ParametersAction(snParams.createValue(null));
        final WorkflowRun build = job.getParameterizedJobMixIn().scheduleBuild2(0,action).get();
        LOG.info(jenkins.getLog(build));
        jenkins.assertLogContains("START: ServiceNow - Activate the plugin com.servicenow_now_calendar", build);
        jenkins.assertLogContains("START: ServiceNow - Install the specified application", build);
        jenkins.assertLogContains("START: ServiceNow - Run test suite", build);
        jenkins.assertLogContains("START: ServiceNow - Roll back the specified application", build);
        jenkins.assertLogContains("START: ServiceNow - Roll back the plugin com.servicenow_now_calendar", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    private String getPW() {
        return PASSWORD_SN;
    }

}
