package io.jenkins.plugins.servicenow;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
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
public class ActivatePluginBuilderTest extends BaseAPICallResultTest {

    private ActivatePluginBuilder activatePluginBuilder;

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
        this.activatePluginBuilder = new ActivatePluginBuilder(TestData.credentials);
        this.activatePluginBuilder.setClientFactory(clientFactoryMock);
        given(this.runMock.getEnvironment(any())).willReturn(new EnvVars());
        given(this.clientFactoryMock.create(eq(runMock), eq(TestData.url), eq(TestData.credentials)))
                .willReturn(restClientMock);
        given(taskListenerMock.getLogger()).willReturn(System.out);
        given(runMock.getAction(eq(ParametersAction.class))).willReturn(parametersActionMock);
    }

    @Test
    public void performWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        activatePluginBuilder.setUrl(TestData.url);
        activatePluginBuilder.setPluginId(TestData.pluginId);
        activatePluginBuilder.setApiVersion(TestData.apiVersion);

        given(this.restClientMock.activatePlugin(eq(TestData.pluginId))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        activatePluginBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(activatePluginBuilder.getRestClient(), is(restClientMock));

        ArgumentCaptor<String> pluginIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClientMock, times(1)).activatePlugin(pluginIdCaptor.capture());
        String plugin = pluginIdCaptor.getValue();
        assertThat(plugin, is(TestData.pluginId));

        verify(restClientMock, times(1)).checkProgress();
    }

    @Test
    public void performWithPluginAlreadyActivated() throws IOException, InterruptedException, URISyntaxException {
        // given
        activatePluginBuilder.setUrl(TestData.url);
        activatePluginBuilder.setPluginId(TestData.pluginId);
        activatePluginBuilder.setApiVersion(TestData.apiVersion);
        String resultMessage = "Plugin already activated";
        given(this.restClientMock.activatePlugin(eq(TestData.pluginId))).willReturn(getSuccessfulResult(100, resultMessage));

        // when
        activatePluginBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(activatePluginBuilder.getRestClient(), is(restClientMock));

        ArgumentCaptor<String> pluginIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClientMock, times(1)).activatePlugin(pluginIdCaptor.capture());
        String plugin = pluginIdCaptor.getValue();
        assertThat(plugin, is(TestData.pluginId));

        verify(restClientMock, never()).checkProgress();
    }

    @Test(expected = AbortException.class)
    public void performWithBuildFailed() throws IOException, InterruptedException, URISyntaxException {
        // given
        activatePluginBuilder.setUrl(TestData.url);
        activatePluginBuilder.setPluginId(TestData.pluginId);
        activatePluginBuilder.setApiVersion(TestData.apiVersion);
        given(this.restClientMock.activatePlugin(eq(TestData.pluginId))).willReturn(getFailedResult("error"));

        // when
        activatePluginBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    private interface TestData {
        String url = "https://test.service-now.com";
        String apiVersion = "1.0";
        String credentials = "1234";
        String pluginId = "testPlugin";
    }
}