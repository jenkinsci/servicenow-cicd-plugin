package io.jenkins.plugins.servicenow;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.LinkObject;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterValue;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BatchInstallBuilderTest extends BaseAPICallResultTest {

    private BatchInstallBuilder batchInstallBuilder;

    @Mock
    private AbstractBuild runMock;
    @Mock
    private Launcher launcherMock;
    @Mock
    private TaskListener taskListenerMock;
    @Mock
    private ParametersAction parametersActionMock;
    private EnvVars environment = new EnvVars();

    @Mock
    private RunFactory<ServiceNowAPIClient> clientFactoryMock;
    @Mock
    private ServiceNowAPIClient restClientMock;

    @Before
    public void setUp() throws Exception {
        this.batchInstallBuilder = new BatchInstallBuilder(TestData.credentials);
        this.batchInstallBuilder.setClientFactory(clientFactoryMock);
        given(this.runMock.getEnvironment(any())).willReturn(environment);
        given(this.clientFactoryMock.create(eq(runMock), eq(TestData.url), eq(TestData.credentials)))
                .willReturn(restClientMock);
        given(taskListenerMock.getLogger()).willReturn(System.out);
        given(runMock.getAction(eq(ParametersAction.class))).willReturn(parametersActionMock);
    }

    @Test
    public void performWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        batchInstallBuilder.setUrl(TestData.url);
        batchInstallBuilder.setCredentialsId(TestData.credentials);
        batchInstallBuilder.setApiVersion(BatchInstallBuilderTest.TestData.apiVersion);
        batchInstallBuilder.setBatchName(TestData.batchName);
        batchInstallBuilder.setPackages((TestData.packages));
        batchInstallBuilder.setNotes(TestData.notes);

        environment.put(ServiceNowParameterDefinition.PARAMETER_NAME,
                "{'name': '" + ServiceNowParameterDefinition.PARAMETER_NAME + "'}"); // empty parameter snParam

        Result expectedResult = getPendingResult();
        expectedResult.getLinks().getResults().setUrl(TestData.resultsUrl);
        LinkObject rollback = new LinkObject();
        rollback.setId(TestData.rollbackId);
        expectedResult.getLinks().setRollback(rollback);
        given(this.restClientMock.batchInstall(eq(TestData.batchName), eq(TestData.packages), eq(TestData.notes)))
                .willReturn(expectedResult);
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        batchInstallBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(batchInstallBuilder.getRestClient(), is(restClientMock));

        ArgumentCaptor<List<ParameterValue>> paramsCaptor = ArgumentCaptor.forClass(List.class);
        verify(parametersActionMock, times(1)).createUpdated(paramsCaptor.capture());
        List<ParameterValue> params = paramsCaptor.getValue();
        assertThat(params, Matchers.hasSize(1));
        ServiceNowParameterValue snParam = (ServiceNowParameterValue) params.stream()
                .filter(p -> p instanceof ServiceNowParameterValue).findFirst().orElse(null);
        // check if batchRollbackId is inside ServiceNowParameters
        assertThat(snParam.getBatchRollbackId(), is(TestData.rollbackId));

        verify(restClientMock, times(1))
                .batchInstall(eq(TestData.batchName), eq(TestData.packages), eq(TestData.notes));
        verify(restClientMock, times(1)).checkProgress();
    }

    @Test(expected = AbortException.class)
    public void performWithBuildFailed_APIResultWithError() throws IOException, InterruptedException, URISyntaxException {
        // given
        batchInstallBuilder.setUrl(TestData.url);
        batchInstallBuilder.setCredentialsId(TestData.credentials);
        batchInstallBuilder.setApiVersion(BatchInstallBuilderTest.TestData.apiVersion);
        batchInstallBuilder.setBatchName(TestData.batchName);
        batchInstallBuilder.setPackages((TestData.packages));
        batchInstallBuilder.setNotes(TestData.notes);
        given(this.restClientMock.batchInstall(eq(TestData.batchName), eq(TestData.packages), eq(TestData.notes)))
                .willReturn(getFailedResult("error"));

        // when
        batchInstallBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    private interface TestData {
        String url = "https://test.service-now.com";
        String apiVersion = "1.0";
        String credentials = "1234";
        String batchName = "Test batch name";
        String notes = "Short test notes.";
        String packages = "[{\n" +
                "     \"id\": \"syd_id_abcefghi\",\n" +
                "     \"type\": \"application\",\n" +
                "     \"load_demo_data\": false,\n" +
                "     \"requested_version\": \"1.0.2\",\n" +
                "     \"notes\": \"User specific text to describe this application install\"\n" +
                "   }]";
        String resultsUrl = "https://now-instance/api/sn_cicd/app/batch/results/results-id";
        String rollbackId = "rollback-id";
    }

}