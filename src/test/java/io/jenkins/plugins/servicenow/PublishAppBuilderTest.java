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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublishAppBuilderTest extends BaseAPICallResultTest {

    private PublishAppBuilder publishAppBuilder;

    @Mock
    private AbstractBuild runMock;
    @Mock
    private Launcher launcherMock;
    @Mock
    private TaskListener taskListenerMock;
    @Mock
    private RunFactory<ServiceNowAPIClient> clientFactoryMock;
    @Mock
    private ServiceNowAPIClient restClientMock;
    @Mock
    private ParametersAction parametersActionMock;

    @Before
    public void setUp() throws Exception {
        this.publishAppBuilder = new PublishAppBuilder(TestData.credentials);
        this.publishAppBuilder.setClientFactory(clientFactoryMock);
        given(this.runMock.getEnvironment(any())).willReturn(new EnvVars());
        given(this.clientFactoryMock.create(eq(runMock), eq(TestData.url), eq(TestData.credentials)))
                .willReturn(restClientMock);
        given(taskListenerMock.getLogger()).willReturn(System.out);
        given(runMock.getAction(eq(ParametersAction.class))).willReturn(parametersActionMock);
    }

    @Test
    public void performWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        publishAppBuilder.setUrl(TestData.url);
        publishAppBuilder.setCredentialsId(TestData.credentials);
        publishAppBuilder.setAppVersion(TestData.applicationVersion);
        publishAppBuilder.setApiVersion(PublishAppBuilderTest.TestData.apiVersion);
        publishAppBuilder.setAppScope(TestData.scope);
        publishAppBuilder.setAppSysId((TestData.sysId));
        publishAppBuilder.setDevNotes(TestData.devNotes);
        publishAppBuilder.setObtainVersionAutomatically(false);

        given(this.restClientMock.publishApp(eq(TestData.scope), eq(TestData.sysId), eq(TestData.applicationVersion), eq(TestData.devNotes))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        publishAppBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(publishAppBuilder.getRestClient(), is(restClientMock));

        ArgumentCaptor<String> scopeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sysIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> appVersionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> devNotesCaptor = ArgumentCaptor.forClass(String.class);
        verify(restClientMock, times(1)).publishApp(scopeCaptor.capture(), sysIdCaptor.capture(), appVersionCaptor.capture(), devNotesCaptor.capture());
        assertThat(scopeCaptor.getValue(), is(TestData.scope));
        assertThat(sysIdCaptor.getValue(), is(TestData.sysId));
        assertThat(appVersionCaptor.getValue(), is(TestData.applicationVersion));
        assertThat(devNotesCaptor.getValue(), is(TestData.devNotes));

        verify(restClientMock, times(1)).checkProgress();
    }

    @Test(expected = AbortException.class)
    public void performWithBuildFailed() throws IOException, InterruptedException, URISyntaxException {
        // given
        publishAppBuilder.setUrl(TestData.url);
        publishAppBuilder.setCredentialsId(TestData.credentials);
        publishAppBuilder.setAppVersion(TestData.applicationVersion);
        publishAppBuilder.setApiVersion(PublishAppBuilderTest.TestData.apiVersion);
        publishAppBuilder.setAppScope(TestData.scope);
        publishAppBuilder.setAppSysId((TestData.sysId));
        publishAppBuilder.setDevNotes(TestData.devNotes);
        publishAppBuilder.setObtainVersionAutomatically(false);
        given(this.restClientMock.publishApp(eq(TestData.scope), eq(TestData.sysId), eq(TestData.applicationVersion), eq(TestData.devNotes))).willReturn(getFailedResult("error"));

        // when
        publishAppBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    @Test
    public void performWithNullableFlagObtainVersionFromSC() throws IOException, InterruptedException, URISyntaxException {
        // given
        publishAppBuilder.setUrl(TestData.url);
        publishAppBuilder.setCredentialsId(TestData.credentials);
        publishAppBuilder.setAppVersion(TestData.applicationVersion);
        publishAppBuilder.setApiVersion(PublishAppBuilderTest.TestData.apiVersion);
        publishAppBuilder.setAppScope(TestData.scope);
        publishAppBuilder.setAppSysId((TestData.sysId));
        publishAppBuilder.setDevNotes(TestData.devNotes);
        publishAppBuilder.setObtainVersionAutomatically(null);
        given(this.restClientMock.publishApp(eq(TestData.scope), eq(TestData.sysId), eq(TestData.applicationVersion), eq(TestData.devNotes))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        publishAppBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(publishAppBuilder.getRestClient(), is(restClientMock));
        verify(restClientMock, times(1)).publishApp(eq(TestData.scope), eq(TestData.sysId), eq(TestData.applicationVersion), eq(TestData.devNotes));
        verify(restClientMock, times(1)).checkProgress();
    }

    private interface TestData {
        String url = "https://test.service-now.com";
        String apiVersion = "1.0";
        String credentials = "1234";
        String sysId = "123456789";
        String scope = "testScope";
        String applicationVersion = "1.0.1";
        String devNotes = "test developers note";
    }

}