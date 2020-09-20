package io.jenkins.plugins.servicenow.api;

import hudson.remoting.Base64;
import hudson.util.Secret;
import io.jenkins.plugins.servicenow.Constants;
import io.jenkins.plugins.servicenow.PublishAppBuilder;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceNowAPIClientIntegrationTest {

    private static String HOST_SN = "https://cicdjenkinsappauthor.service-now.com";
    private static String SYSTEM_ID = "90eb12afdb021010b40a9eb5db9619aa";
    private static String APP_SCOPE = "x_sofse_cicdjenkin";
    private static String USER_SN = System.getenv("USERNAME");
    private static String PASSWORD_SN = System.getenv("PASSWORD");

    private ServiceNowAPIClient serviceNowAPIClient;

    @Test
    public void testGetCurrentAppVersion_bySysId() {
        // given
        final String systemId = SYSTEM_ID;
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);

        // when
        final String result = serviceNowAPIClient.getCurrentAppVersion(null, systemId);

        // then
        assertThat(result).isNotBlank();
    }

    @Test
    public void testGetCurrentAppVersion_byScope() {
        // given
        final String scope = APP_SCOPE;
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);

        // when
        final String result = serviceNowAPIClient.getCurrentAppVersion(scope, null);

        // then
        assertThat(result).isNotBlank();
    }

    @Test
    public void testApplyChanges() throws IOException, URISyntaxException {
        // given
        final String systemId = SYSTEM_ID;
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);

        // when
        Result result = serviceNowAPIClient.applyChanges(null, systemId, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getError()).isBlank();
        assertThat(result.getStatus()).isEqualTo("0");
        assertThat(result.getLinks().getProgress()).isNotNull();
        assertThat(result.getLinks().getProgress().getUrl()).contains(HOST_SN);
        assertThat(serviceNowAPIClient.getLastActionProgressUrl()).isEqualTo(result.getLinks().getProgress().getUrl());
    }

    @Test
    public void testCheckProgress() throws IOException, URISyntaxException, InterruptedException {
        // given
        final String systemId = SYSTEM_ID;
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);
        Result result = serviceNowAPIClient.applyChanges(null, systemId, null);
        assertThat(serviceNowAPIClient.getLastActionProgressUrl()).isEqualTo(result.getLinks().getProgress().getUrl());
        Result progressResult = null;

        // when
        do {
            if(progressResult != null) {
                Thread.sleep(Constants.PROGRESS_CHECK_INTERVAL);
            }
            progressResult = serviceNowAPIClient.checkProgress();
            assertThat(progressResult).isNotNull();
        } while(progressResult.getStatus() == "1");

        // then
        assertThat(progressResult).isNotNull();
        assertThat(progressResult.getError()).isBlank();
        assertThat(progressResult.getStatus()).isEqualTo("2");
        assertThat(progressResult.getPercentComplete()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testPublishApplication() throws IOException, URISyntaxException {
        // given
        final String systemId = SYSTEM_ID;
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);
        String applicationVersion = getNextApplicationVersionToBePublished(serviceNowAPIClient, systemId);
        validateApplicationVersion(applicationVersion);

        // when
        Result result = serviceNowAPIClient.publishApp(null, systemId, applicationVersion, "integration test of servicenow-cicd jenkins plugin");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getError()).isBlank();
        assertThat(result.getStatus()).isEqualTo("0");
        assertThat(result.getLinks().getProgress()).isNotNull();
        assertThat(result.getLinks().getProgress().getUrl()).contains(HOST_SN);
        assertThat(serviceNowAPIClient.getLastActionProgressUrl()).isEqualTo(result.getLinks().getProgress().getUrl());
    }

    @Test
    @Ignore("Result of application installation may vary depending on previous requests.")
    public void testInstallApplication() throws IOException, URISyntaxException {
        // given
        final String systemId = SYSTEM_ID;
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);

        // when
        Result result = serviceNowAPIClient.installApp(null, systemId, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getError()).isBlank();
        assertThat(result.getStatus()).isEqualTo("0");
        assertThat(result.getLinks().getProgress()).isNotNull();
        assertThat(result.getLinks().getProgress().getUrl()).contains(HOST_SN);
        assertThat(serviceNowAPIClient.getLastActionProgressUrl()).isEqualTo(result.getLinks().getProgress().getUrl());
    }

    @Test
    @Ignore("Impossible to test it without full CI/CD workflow. The request will be tested in another integration test.")
    public void testRollbackApplication() {}

    @Test
    public void testActivatePlugin() throws IOException, URISyntaxException {
        // given
        String pluginName = "com.servicenow_now_calendar";
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);

        // when
        Result result = serviceNowAPIClient.activatePlugin(pluginName);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getError()).isBlank();
        assertThat(result.getStatus()).isNotEqualTo("3");
        assertThat(result.getStatus()).isNotEqualTo("4");
        if(ActionStatus.PENDING.getStatus().equals(result.getStatus()) || ActionStatus.RUNNING.getStatus().equals(result.getStatus())) {
            assertThat(result.getLinks().getProgress()).isNotNull();
            assertThat(result.getLinks().getProgress().getUrl()).contains(HOST_SN);
            assertThat(serviceNowAPIClient.getLastActionProgressUrl()).isEqualTo(result.getLinks().getProgress().getUrl());
        }
    }

    @Test
    @Ignore("Flawed API endpoint")
    public void testRollbackPlugin() throws IOException, URISyntaxException {
        // given
        String pluginName = "com.servicenow_now_calendar";
        Secret password = Secret.fromString(getPW());
        serviceNowAPIClient = new ServiceNowAPIClient(HOST_SN, USER_SN, password);

        // when
        Result result = serviceNowAPIClient.rollbackPlugin(pluginName);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getError()).isBlank();
        assertThat(result.getStatus()).isNotEqualTo("3");
        assertThat(result.getStatus()).isNotEqualTo("4");
        if(ActionStatus.PENDING.getStatus().equals(result.getStatus()) || ActionStatus.RUNNING.getStatus().equals(result.getStatus())) {
            assertThat(result.getLinks().getProgress()).isNotNull();
            assertThat(result.getLinks().getProgress().getUrl()).contains(HOST_SN);
            assertThat(serviceNowAPIClient.getLastActionProgressUrl()).isEqualTo(result.getLinks().getProgress().getUrl());
        }
    }

    private String getNextApplicationVersionToBePublished(final ServiceNowAPIClient serviceNowAPIClient, String appSystemId) {
        final String currentVersion = serviceNowAPIClient.getCurrentAppVersion(null, appSystemId);
        validateApplicationVersion(currentVersion);
        return PublishAppBuilder.getNextAppVersion(currentVersion);
    }

    private void validateApplicationVersion(final String applicationVersion) {
        assertThat(applicationVersion).isNotBlank();
        assertThat(applicationVersion.split("\\.")).hasSize(3);
    }

    private String getPW() {
        return new String(Base64.decode(PASSWORD_SN));
    }
}