package io.jenkins.plugins.servicenow.parameter;

import junit.framework.TestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceNowParameterValueTest extends TestCase {

    public void testGetCredentialsForPublishedApp() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getCredentialsForPublishedApp();

        // then
        assertThat(result).isEqualTo(TestData.credentialsForInstalledApp);
    }

    public void testGetInstanceForPublishedAppUrl() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getInstanceForPublishedAppUrl();

        // then
        assertThat(result).isEqualTo(TestData.instanceForPublishedAppUrl);
    }

    public void testGetCredentialsForInstalledApp() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getCredentialsForInstalledApp();

        // then
        assertThat(result).isEqualTo(TestData.credentialsForInstalledApp);
    }

    public void testGetInstanceForInstalledAppUrl() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getInstanceForInstalledAppUrl();

        // then
        assertThat(result).isEqualTo(TestData.instanceForInstalledAppUrl);
    }

    public void testGetSysId() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getSysId();

        // then
        assertThat(result).isEqualTo(TestData.systemId);
    }

    public void testGetAppScope() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getAppScope();

        // then
        assertThat(result).isEqualTo(TestData.applicationScope);
    }

    public void testGetPublishedAppVersion() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getPublishedAppVersion();

        // then
        assertThat(result).isEqualTo(TestData.publishedAppVersion);
    }

    public void testGetRollbackAppVersion() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        String result = parameterValue.getRollbackAppVersion();

        // then
        assertThat(result).isEqualTo(TestData.rollbackAppVersion);
    }

    public void testGetProgressCheckInterval() {
        // given
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", TestData.getJson());

        // when
        Integer result = parameterValue.getProgressCheckInterval();

        // then
        assertThat(result).isEqualTo(TestData.progressCheckInterval);
    }

    public void testGetEmptyProgressCheckInterval() {
        // given
        String replace = "\"progressCheckInterval\":\"" + TestData.progressCheckInterval;
        String by = "\"progressCheckInterval\":\"";
        String json = TestData.getJson().replace(replace, by);
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", json);

        // when
        Integer result = parameterValue.getProgressCheckInterval();

        // then
        assertThat(result).isNull();
    }

    public void testNotExistingProgressCheckInterval() {
        // given
        String replace = "\"progressCheckInterval\":\"" + TestData.progressCheckInterval + "\"";
        String by = "";
        String json = TestData.getJson().replace(replace, by);
        ServiceNowParameterValue parameterValue = new ServiceNowParameterValue("test", json);

        // when
        Integer result = parameterValue.getProgressCheckInterval();

        // then
        assertThat(result).isNull();
    }

    private interface TestData {
        String description = "description";
        String credentialsForInstalledApp = "88dbbe69-0e00-4dd5-838b-2fbd8dfedeb4";
        String publishingCredentials = credentialsForInstalledApp;
        String instanceForPublishedAppUrl = "https://publish-instance.service-now.com";
        String instanceForInstalledAppUrl = "https://install-instance.service-now.com";
        String applicationScope = "x_sofse_cicdjenkin";
        String publishedAppVersion = "1028.0.4";
        String rollbackAppVersion = "1028.0.3";
        String systemId = "123erwqe";
        Integer progressCheckInterval = 100;

        static String getJson() {
            return "{\"name\":\"snParam\"," +
                    "\"description\":\"" + description + "\"," +
                    "\"credentialsForPublishedApp\":\"" + publishingCredentials + "\"," +
                    "\"instanceForPublishedAppUrl\":\"" + instanceForPublishedAppUrl + "\"," +
                    "\"credentialsForInstalledApp\":\"" + credentialsForInstalledApp + "\"," +
                    "\"instanceForInstalledAppUrl\":\"" + instanceForInstalledAppUrl + "\"," +
                    "\"sysId\":\"" + systemId + "\"," +
                    "\"appScope\":\"" + applicationScope + "\"," +
                    "\"publishedAppVersion\":\"" + publishedAppVersion + "\"," +
                    "\"rollbackAppVersion\":\"" + rollbackAppVersion + "\"," +
                    "\"progressCheckInterval\":\"" + progressCheckInterval + "\"}";
        }
    }
}