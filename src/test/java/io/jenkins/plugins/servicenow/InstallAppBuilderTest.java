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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InstallAppBuilderTest extends BaseAPICallResultTest {

    private InstallAppBuilder installAppBuilder;

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
        this.installAppBuilder = new InstallAppBuilder(TestData.credentials);
        this.installAppBuilder.setClientFactory(clientFactoryMock);
        given(this.runMock.getEnvironment(any())).willReturn(new EnvVars());
        given(this.clientFactoryMock.create(eq(runMock), eq(TestData.url), eq(TestData.credentials)))
                .willReturn(restClientMock);
        given(taskListenerMock.getLogger()).willReturn(System.out);
        given(runMock.getAction(eq(ParametersAction.class))).willReturn(parametersActionMock);
    }

    @Test
    public void performWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        installAppBuilder.setUrl(TestData.url);
        installAppBuilder.setCredentialsId(TestData.credentials);
        installAppBuilder.setAppVersion(TestData.applicationVersion);
        installAppBuilder.setApiVersion(InstallAppBuilderTest.TestData.apiVersion);
        installAppBuilder.setAppScope(TestData.scope);
        installAppBuilder.setAppSysId((TestData.sysId));
        installAppBuilder.setBaseAppAutoUpgrade(TestData.updateBaseVersion);
        installAppBuilder.setBaseAppVersion(TestData.baseAppVersion);

        given(this.restClientMock.installApp(eq(TestData.scope), eq(TestData.sysId), eq(TestData.applicationVersion),
                eq(TestData.baseAppVersion), eq(TestData.updateBaseVersion))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        installAppBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(installAppBuilder.getRestClient(), is(restClientMock));

        ArgumentCaptor<String> scopeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sysIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> appVersionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> baseAppVersionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> updateBaseAppVersionCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(restClientMock, times(1)).installApp(scopeCaptor.capture(), sysIdCaptor.capture(),
                appVersionCaptor.capture(), baseAppVersionCaptor.capture(), updateBaseAppVersionCaptor.capture());
        assertThat(scopeCaptor.getValue(), is(TestData.scope));
        assertThat(sysIdCaptor.getValue(), is(TestData.sysId));
        assertThat(appVersionCaptor.getValue(), is(TestData.applicationVersion));
        assertThat(baseAppVersionCaptor.getValue(), is(TestData.baseAppVersion));
        assertThat(updateBaseAppVersionCaptor.getValue(), is(true));

        verify(restClientMock, times(1)).checkProgress();
    }

    @Test(expected = AbortException.class)
    public void performWithBuildFailed() throws IOException, InterruptedException, URISyntaxException {
        // given
        installAppBuilder.setUrl(TestData.url);
        installAppBuilder.setCredentialsId(TestData.credentials);
        installAppBuilder.setAppVersion(TestData.applicationVersion);
        installAppBuilder.setApiVersion(TestData.apiVersion);
        installAppBuilder.setAppScope(TestData.scope);
        installAppBuilder.setAppSysId((TestData.sysId));
        installAppBuilder.setBaseAppAutoUpgrade(TestData.updateBaseVersion);
        installAppBuilder.setBaseAppVersion(TestData.baseAppVersion);
        given(this.restClientMock.installApp(
                eq(TestData.scope), eq(TestData.sysId), eq(TestData.applicationVersion),
                eq(TestData.baseAppVersion), eq(TestData.updateBaseVersion)))
                .willReturn(getFailedResult("error"));

        // when
        installAppBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    private interface TestData {
        String url = "https://test.service-now.com";
        String apiVersion = "1.0";
        String credentials = "1234";
        String sysId = "123456789";
        String scope = "testScope";
        String applicationVersion = "1.0.1";
        String baseAppVersion = "2.0.0";
        Boolean updateBaseVersion = true;
    }

}