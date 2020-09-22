package io.jenkins.plugins.servicenow.parameter;

import hudson.model.ParameterValue;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ServiceNowParameterDefinitionTest extends TestCase {

    @Test
    public void testCreateValue_fromStaplerRequest() {
        // given
        ServiceNowParameterDefinition instance = new ServiceNowParameterDefinition(TestData.description);
        StaplerRequest request = mock(StaplerRequest.class);

        // when
        ParameterValue result = instance.createValue(request);

        // then
        assertThat(result).isInstanceOf(ServiceNowParameterValue.class);
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue()).isInstanceOf(String.class);

        JSONObject resultValue = JSONObject.fromObject(result.getValue());
        assertThat(resultValue.containsKey(ServiceNowParameterDefinition.PARAMS_NAMES.publishedAppVersion)).isTrue();
        assertThat(resultValue.getString(ServiceNowParameterDefinition.PARAMS_NAMES.publishedAppVersion)).isBlank();
        assertThat(resultValue.containsKey(ServiceNowParameterDefinition.PARAMS_NAMES.rollbackAppVersion)).isTrue();
        assertThat(resultValue.getString(ServiceNowParameterDefinition.PARAMS_NAMES.rollbackAppVersion)).isBlank();
    }

    @Test
    public void testCreateValue_withData() {
        // given
        ServiceNowParameterDefinition instance = new ServiceNowParameterDefinition(null, TestData.description);
        StaplerRequest request = mock(StaplerRequest.class);

        // when
        ParameterValue result = instance.createValue(request, JSONObject.fromObject(TestData.getJson()));

        // then
        assertThat(result).isInstanceOf(ServiceNowParameterValue.class);
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue()).isInstanceOf(String.class);

        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.description, TestData.description);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.credentialsForPublishedApp, TestData.publishingCredentials);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.credentialsForInstalledApp, TestData.credentialsForInstalledApp);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.instanceForPublishedAppUrl, TestData.instanceForPublishedAppUrl);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.instanceForInstalledAppUrl, TestData.instanceForInstalledAppUrl);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.appScope, TestData.applicationScope);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.publishedAppVersion, TestData.publishedAppVersion);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.rollbackAppVersion, TestData.rollbackAppVersion);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.sysId, TestData.systemId);
        checkParameter(result, ServiceNowParameterDefinition.PARAMS_NAMES.progressCheckInterval, TestData.progressCheckInterval);
    }

    @Test
    public void testCreateFrom() {
        // when
        ServiceNowParameterDefinition parameterDefinition = ServiceNowParameterDefinition.createFrom(TestData.getJson());

        // then
        assertThat(parameterDefinition.getAppScope()).isEqualTo(TestData.applicationScope);
        assertThat(parameterDefinition.getCredentialsForInstalledApp()).isEqualTo(TestData.credentialsForInstalledApp);
        assertThat(parameterDefinition.getCredentialsForPublishedApp()).isEqualTo(TestData.publishingCredentials);
        assertThat(parameterDefinition.getInstanceForInstalledAppUrl()).isEqualTo(TestData.instanceForInstalledAppUrl);
        assertThat(parameterDefinition.getInstanceForPublishedAppUrl()).isEqualTo(TestData.instanceForPublishedAppUrl);
        assertThat(parameterDefinition.getPublishedAppVersion()).isEqualTo(TestData.publishedAppVersion);
        assertThat(parameterDefinition.getRollbackAppVersion()).isEqualTo(TestData.rollbackAppVersion);
        assertThat(parameterDefinition.getSysId()).isEqualTo(TestData.systemId);
        assertThat(parameterDefinition.getProgressCheckInterval()).isEqualTo(TestData.progressCheckInterval);
        assertThat(parameterDefinition.getDescription()).isEqualTo(TestData.description);
    }

    @Test
    public void testCreateFrom_noDescription() {
        // when
        String phraseToBeRemoved = "\"description\":\"" + TestData.description + "\",";
        ServiceNowParameterDefinition parameterDefinition = ServiceNowParameterDefinition.createFrom(TestData.getJson().replace(phraseToBeRemoved, ""));

        // then
        assertThat(parameterDefinition.getAppScope()).isEqualTo(TestData.applicationScope);
        assertThat(parameterDefinition.getCredentialsForInstalledApp()).isEqualTo(TestData.credentialsForInstalledApp);
        assertThat(parameterDefinition.getCredentialsForPublishedApp()).isEqualTo(TestData.publishingCredentials);
        assertThat(parameterDefinition.getInstanceForInstalledAppUrl()).isEqualTo(TestData.instanceForInstalledAppUrl);
        assertThat(parameterDefinition.getInstanceForPublishedAppUrl()).isEqualTo(TestData.instanceForPublishedAppUrl);
        assertThat(parameterDefinition.getPublishedAppVersion()).isEqualTo(TestData.publishedAppVersion);
        assertThat(parameterDefinition.getRollbackAppVersion()).isEqualTo(TestData.rollbackAppVersion);
        assertThat(parameterDefinition.getSysId()).isEqualTo(TestData.systemId);
        assertThat(parameterDefinition.getProgressCheckInterval()).isEqualTo(TestData.progressCheckInterval);
        assertThat(parameterDefinition.getDescription()).isBlank();
    }

    private void checkParameter(ParameterValue parametersValue, String paramName, String value) {
        JSONObject parameters = JSONObject.fromObject(parametersValue.getValue());
        assertThat(parameters.containsKey(paramName)).isTrue();
        assertThat(parameters.getString(paramName)).isEqualTo(value);
    }

    private void checkParameter(ParameterValue parametersValue, String paramName, Integer value) {
        JSONObject parameters = JSONObject.fromObject(parametersValue.getValue());
        assertThat(parameters.containsKey(paramName)).isTrue();
        assertThat(parameters.getInt(paramName)).isEqualTo(value);
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