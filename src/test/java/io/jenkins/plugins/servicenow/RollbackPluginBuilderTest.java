package io.jenkins.plugins.servicenow;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class )
public class RollbackPluginBuilderTest {

    private RollbackPluginBuilder rollbackPluginBuilder;

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

    @Before
    public void setUp() throws Exception {
        this.rollbackPluginBuilder = new RollbackPluginBuilder(TestData.credentials);
        this.rollbackPluginBuilder.setClientFactory(clientFactoryMock);
        given(this.runMock.getEnvironment(any())).willReturn(new EnvVars());
        given(this.clientFactoryMock.create(eq(runMock), eq(TestData.url), eq(TestData.credentials)))
                .willReturn(restClientMock);
        given(taskListenerMock.getLogger()).willReturn(System.out);
        given(runMock.getAction(eq(ParametersAction.class))).willReturn(parametersActionMock);
    }

    @Test
    public void performWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        rollbackPluginBuilder.setUrl(TestData.url);
        rollbackPluginBuilder.setPluginId(TestData.pluginId);
        rollbackPluginBuilder.setApiVersion(TestData.apiVersion);

        given(this.restClientMock.rollbackPlugin(eq(TestData.pluginId))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        rollbackPluginBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(rollbackPluginBuilder.getRestClient(), is(restClientMock));

        ArgumentCaptor<String> pluginIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClientMock, times(1)).rollbackPlugin(pluginIdCaptor.capture());
        String plugin = pluginIdCaptor.getValue();
        assertThat(plugin, is(TestData.pluginId));

        verify(restClientMock, times(1)).checkProgress();
    }

    @Test(expected = AbortException.class)
    public void performWithBuildFailed() throws IOException, InterruptedException, URISyntaxException {
        // given
        rollbackPluginBuilder.setUrl(TestData.url);
        rollbackPluginBuilder.setPluginId(TestData.pluginId);
        rollbackPluginBuilder.setApiVersion(TestData.apiVersion);
        given(this.restClientMock.rollbackPlugin(eq(TestData.pluginId))).willReturn(getFailedResult("Plugin is not Active on the instance, unable to rollback"));

        // when
        rollbackPluginBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    private Result getPendingResult() {
        final Result result = new Result();
        result.setStatus(ActionStatus.PENDING.getStatus());
        return result;
    }

    private Result getSuccessfulResult(int percentComplete, String statusMessage) {
        final Result result = new Result();
        result.setStatus(ActionStatus.SUCCESSFUL.getStatus());
        result.setPercentComplete(percentComplete);
        if(StringUtils.isNotBlank(statusMessage)) {
            result.setStatusMessage(statusMessage);
        }
        return result;
    }

    private Result getFailedResult(String errorMessage) {
        final Result result = new Result();
        result.setStatus(ActionStatus.FAILED.getStatus());
        if(StringUtils.isNotBlank(errorMessage)) {
            result.setStatusMessage(errorMessage);
        }
        return result;
    }

    private interface TestData {
        String url = "https://test.service-now.com";
        String apiVersion = "1.0";
        String credentials = "1234";
        String pluginId = "testPlugin";
    }
}