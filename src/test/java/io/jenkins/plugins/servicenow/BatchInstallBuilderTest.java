package io.jenkins.plugins.servicenow;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
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
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BatchInstallBuilderTest extends BaseAPICallResultTest {

    private Path resourceDirectory = Paths.get("src","test","resources");

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
    public void performWithoutManifestFileWithSuccess() throws IOException, InterruptedException, URISyntaxException {
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

    @Test
    public void performWithBatchFileWithSuccess() throws IOException, URISyntaxException, InterruptedException {
        // given
        batchInstallBuilder.setUrl(TestData.url);
        batchInstallBuilder.setCredentialsId(TestData.credentials);
        batchInstallBuilder.setApiVersion(BatchInstallBuilderTest.TestData.apiVersion);

        FilePath workspacePath = createWorkspace(null);
        final String manifestFile = "sn_batch_manifest.json";
        batchInstallBuilder.setUseFile(true);
        batchInstallBuilder.setFile(manifestFile);

        environment.put(ServiceNowParameterDefinition.PARAMETER_NAME,
                "{'name': '" + ServiceNowParameterDefinition.PARAMETER_NAME + "'}"); // empty parameter snParam

        Result expectedResult = getPendingResult();
        expectedResult.getLinks().getResults().setUrl(TestData.resultsUrl);
        LinkObject rollback = new LinkObject();
        rollback.setId(TestData.rollbackId);
        expectedResult.getLinks().setRollback(rollback);
        given(this.restClientMock.batchInstall(anyString())).willReturn(expectedResult);
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        batchInstallBuilder.perform(runMock, workspacePath, launcherMock, taskListenerMock);

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

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClientMock, times(1))
                .batchInstall(payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertThat(payload, not(blankOrNullString()));
        JSONObject jsonPayload = JSONObject.fromObject(payload);
        assertThat(jsonPayload, notNullValue());
        assertThat(jsonPayload.getString("name"), is(TestData.batchName));
        assertThat(jsonPayload.getJSONArray("packages").size(), is(3));
        assertThat(jsonPayload.has("notes"), is(false));
        verify(restClientMock, times(1)).checkProgress();

    }

    @Test
    public void performWithEmptyBatchFileFieldWithSuccess() throws IOException, URISyntaxException, InterruptedException {
        // given
        batchInstallBuilder.setUrl(TestData.url);
        batchInstallBuilder.setCredentialsId(TestData.credentials);
        batchInstallBuilder.setApiVersion(BatchInstallBuilderTest.TestData.apiVersion);

        FilePath workspacePath = createWorkspace(null);
        batchInstallBuilder.setUseFile(true);
        batchInstallBuilder.setFile(null); // should be used default file name that exists in this test

        environment.put(ServiceNowParameterDefinition.PARAMETER_NAME,
                "{'name': '" + ServiceNowParameterDefinition.PARAMETER_NAME + "'}"); // empty parameter snParam

        Result expectedResult = getPendingResult();
        expectedResult.getLinks().getResults().setUrl(TestData.resultsUrl);
        LinkObject rollback = new LinkObject();
        rollback.setId(TestData.rollbackId);
        expectedResult.getLinks().setRollback(rollback);
        given(this.restClientMock.batchInstall(anyString())).willReturn(expectedResult);
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        batchInstallBuilder.perform(runMock, workspacePath, launcherMock, taskListenerMock);

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

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClientMock, times(1))
                .batchInstall(payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertThat(payload, not(blankOrNullString()));
        JSONObject jsonPayload = JSONObject.fromObject(payload);
        assertThat(jsonPayload, notNullValue());
        assertThat(jsonPayload.getString("name"), is(TestData.batchName));
        assertThat(jsonPayload.getJSONArray("packages").size(), is(1));
        assertThat(jsonPayload.has("notes"), is(false));
        verify(restClientMock, times(1)).checkProgress();

    }

    @Test
    public void performWithNotExistingBatchFileWithBuildFailed() throws IOException, URISyntaxException, InterruptedException {
        // given
        batchInstallBuilder.setUrl(TestData.url);
        batchInstallBuilder.setCredentialsId(TestData.credentials);
        batchInstallBuilder.setApiVersion(BatchInstallBuilderTest.TestData.apiVersion);

        batchInstallBuilder.setUseFile(true);
        FilePath workspacePath = createWorkspace(null);
        final String manifestFile = "notExisting.json";
        batchInstallBuilder.setFile(manifestFile);

        environment.put(ServiceNowParameterDefinition.PARAMETER_NAME,
                "{'name': '" + ServiceNowParameterDefinition.PARAMETER_NAME + "'}"); // empty parameter snParam

        given(this.restClientMock.batchInstall(eq(""))).willReturn(getFailedResult("payload empty"));

        // when
        try {
            batchInstallBuilder.perform(runMock, workspacePath, launcherMock, taskListenerMock);
        } catch(AbortException ex) {
            // expected
        }

        // then
        assertThat(batchInstallBuilder.getRestClient(), is(restClientMock));
        verify(parametersActionMock, never()).createUpdated(anyList());
        verify(restClientMock, times(1))
                .batchInstall(eq(""));
        verify(restClientMock, never()).checkProgress();
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

    private FilePath createWorkspace(String workspacePath) {
        if(StringUtils.isBlank(workspacePath)) {
            return new FilePath(this.resourceDirectory.toFile());
        } else {
            return new FilePath(Paths.get(workspacePath).toFile());
        }
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