package io.jenkins.plugins.servicenow;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.parameter.ServiceNowParameterDefinition;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class )
public class BatchRollbackBuilderTest extends BaseAPICallResultTest {

    private BatchRollbackBuilder batchRollbackBuilder;

    @Mock
    private AbstractBuild runMock;
    @Mock
    private Launcher launcherMock;
    @Mock
    private TaskListener taskListenerMock;
    @Mock
    private RestClientFactory clientFactoryMock;
    @Mock
    private ServiceNowAPIClient restClientMock;
    @Mock
    private ParametersAction parametersActionMock;
    private EnvVars environment = new EnvVars();

    private ByteArrayOutputStream consoleLogs;
    private PrintStream console;

    @Before
    public void setUp() throws Exception {
        this.batchRollbackBuilder = new BatchRollbackBuilder(TestData.credentials);
        this.batchRollbackBuilder.setClientFactory(clientFactoryMock);
        given(this.runMock.getEnvironment(any())).willReturn(environment);
        given(this.clientFactoryMock.create(eq(runMock), eq(TestData.url), eq(TestData.credentials)))
                .willReturn(restClientMock);
        consoleLogs = new ByteArrayOutputStream();
        console = new PrintStream(consoleLogs);
        given(taskListenerMock.getLogger()).willReturn(console);
        given(runMock.getAction(eq(ParametersAction.class))).willReturn(parametersActionMock);
    }

    @After
    public void tearDown() throws IOException {
        System.out.println(consoleLogs.toString());
        console.close();
        consoleLogs.close();
    }

    @Test
    public void performWithSuccess_providedRollbackId() throws IOException, InterruptedException, URISyntaxException {
        // given
        batchRollbackBuilder.setUrl(TestData.url);
        batchRollbackBuilder.setApiVersion(TestData.apiVersion);
        batchRollbackBuilder.setRollbackId(TestData.rollbackId);

        given(this.restClientMock.batchRollback(eq(TestData.rollbackId))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        batchRollbackBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(batchRollbackBuilder.getRestClient(), is(restClientMock));

        verify(restClientMock, times(1)).batchRollback(eq(TestData.rollbackId));

        verify(restClientMock, times(1)).checkProgress();
    }

    @Test
    public void performWithSuccess_rollbackIdFromSNParams() throws IOException, InterruptedException, URISyntaxException {
        // given
        batchRollbackBuilder.setUrl(TestData.url);
        batchRollbackBuilder.setApiVersion(TestData.apiVersion);
        batchRollbackBuilder.setRollbackId(null); // value for this variable should be taken from SN Parameters

        environment.put(ServiceNowParameterDefinition.PARAMETER_NAME,
                "{'name': '" + ServiceNowParameterDefinition.PARAMETER_NAME + "'," +
                        "'batchRollbackId': '" + TestData.rollbackId + "'}"); // SN Parameters with batch rollback id

        given(this.restClientMock.batchRollback(eq(TestData.rollbackId))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        batchRollbackBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(batchRollbackBuilder.getRestClient(), is(restClientMock));

        verify(restClientMock, times(1)).batchRollback(eq(TestData.rollbackId));

        verify(restClientMock, times(1)).checkProgress();
    }

    @Test(expected = AbortException.class)
    public void performWithBuildFailed_APIResultWithError() throws IOException, InterruptedException, URISyntaxException {
        // given
        batchRollbackBuilder.setUrl(TestData.url);
        batchRollbackBuilder.setApiVersion(TestData.apiVersion);
        batchRollbackBuilder.setRollbackId(TestData.rollbackId);
        given(this.restClientMock.batchRollback(eq(TestData.rollbackId)))
                .willReturn(getFailedResult("Rollback not possible"));

        // when
        batchRollbackBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    @Test//(expected = AbortException.class)
    public void performWithBuildFailed_NotExistingBatchRollbackId() throws IOException, InterruptedException, URISyntaxException {
        // given
        batchRollbackBuilder.setUrl(TestData.url);
        batchRollbackBuilder.setApiVersion(TestData.apiVersion);
        batchRollbackBuilder.setRollbackId(null);
        // 2 lines below commented due to org.mockito.exceptions.misusing.UnnecessaryStubbingException
        // given(this.restClientMock.batchRollback(eq(TestData.rollbackId))).willReturn(getPendingResult());
        // given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        try {
            batchRollbackBuilder.perform(runMock, null, launcherMock, taskListenerMock);
        } catch(AbortException ex) {
            // expected
        }

        // then
        assertThat(getLogs(), Matchers.containsString("action failed"));
        ArgumentCaptor<String> rollbackIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.restClientMock, times(1)).batchRollback(rollbackIdCaptor.capture());
        assertThat(rollbackIdCaptor.getValue(), Matchers.blankOrNullString());
    }

    private String getLogs() {
        return consoleLogs.toString();
    }

    private interface TestData {
        String url = "https://test.service-now.com";
        String apiVersion = "1.0";
        String credentials = "1234";
        String rollbackId = "qwerty";
    }
}