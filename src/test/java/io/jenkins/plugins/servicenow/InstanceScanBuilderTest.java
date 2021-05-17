package io.jenkins.plugins.servicenow;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.instancescan.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InstanceScanBuilderTest extends BaseAPICallResultTest {

    private InstanceScanBuilder instanceScanBuilder;

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

    // Scan Actions
    private FullScan actionFullStack = new FullScan();
    private PointScan actionPointScan = new PointScan();
    private ComboScan actionComboScan = new ComboScan();
    private SuiteScanOnScopedApps actionSuiteScanOnScopedApps = new SuiteScanOnScopedApps();
    private SuiteScanOnUpdateSets actionSuiteScanOnUpdateSets = new SuiteScanOnUpdateSets();

    @Before
    public void setUp() throws Exception {
        this.instanceScanBuilder = new InstanceScanBuilder(TestData.credentials);
        this.instanceScanBuilder.setClientFactory(clientFactoryMock);
        this.instanceScanBuilder.setScanExecutions(Sets.newSet(actionFullStack, actionPointScan,
                actionComboScan, actionSuiteScanOnScopedApps, actionSuiteScanOnUpdateSets));

        given(this.runMock.getEnvironment(any())).willReturn(new EnvVars());
        given(this.clientFactoryMock.create(eq(runMock), eq(TestData.url), eq(TestData.credentials)))
                .willReturn(restClientMock);
        given(taskListenerMock.getLogger()).willReturn(System.out);
        given(runMock.getAction(eq(ParametersAction.class))).willReturn(parametersActionMock);
    }

    @Test
    public void performFullScanWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType(ScanType.fullScan.name());

        given(this.restClientMock.executeFullScan()).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(instanceScanBuilder.getRestClient(), is(restClientMock));
        verify(restClientMock, times(1)).executeFullScan();
        verify(restClientMock, times(1)).checkProgress();
    }

    @Test(expected = AbortException.class)
    public void performFullScanWithBuildFailed_ApiCallResponseWithError() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType(ScanType.fullScan.name());
        given(this.restClientMock.executeFullScan()).willReturn(getFailedResult("error"));

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    @Test(expected = AbortException.class)
    public void performFullScanWithBuildFailed_ScanActionsNotInjected() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType(ScanType.fullScan.name());
        instanceScanBuilder.setScanExecutions(null);

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    @Test(expected = AbortException.class)
    public void performFullScanWithBuildFailed_InvalidScanTypeValue() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType("surprise");

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        // expect an exception
    }

    @Test
    public void performPointScanWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType(ScanType.pointScan.name());
        instanceScanBuilder.setTargetTable(TestData.targetTable);
        instanceScanBuilder.setTargetRecordSysId(TestData.targetRecordId);

        given(this.restClientMock.executePointScan(eq(TestData.targetTable), eq(TestData.targetRecordId))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(instanceScanBuilder.getRestClient(), is(restClientMock));
        verify(restClientMock, times(1)).executePointScan(eq(TestData.targetTable), eq(TestData.targetRecordId));
        verify(restClientMock, times(1)).checkProgress();
    }

    @Test
    public void performComboScanWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType(ScanType.scanWithCombo.name());
        instanceScanBuilder.setComboSysId(TestData.comboSysId);

        given(this.restClientMock.executeScanWithCombo(eq(TestData.comboSysId))).willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(instanceScanBuilder.getRestClient(), is(restClientMock));
        verify(restClientMock, times(1)).executeScanWithCombo(eq(TestData.comboSysId));
        verify(restClientMock, times(1)).checkProgress();
    }

    @Test
    public void performSuiteScanOnScopedAppsWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType(ScanType.scanWithSuiteOnScopedApps.name());
        instanceScanBuilder.setSuiteSysId(TestData.suiteSysId);
        instanceScanBuilder.setRequestBody(TestData.requestBodyOnScopedApps);

        given(this.restClientMock
                .executeScanWithSuiteOnScopedApps(eq(TestData.suiteSysId), eq(TestData.requestBodyOnScopedApps)))
                .willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(instanceScanBuilder.getRestClient(), is(restClientMock));
        verify(restClientMock, times(1)).executeScanWithSuiteOnScopedApps(eq(TestData.suiteSysId), eq(TestData.requestBodyOnScopedApps));
        verify(restClientMock, times(1)).checkProgress();
    }

    @Test
    public void performSuiteScanOnUpdateSetsWithSuccess() throws IOException, InterruptedException, URISyntaxException {
        // given
        instanceScanBuilder.setUrl(TestData.url);
        instanceScanBuilder.setScanType(ScanType.scanWithSuiteOnUpdateSets.name());
        instanceScanBuilder.setSuiteSysId(TestData.suiteSysId);
        instanceScanBuilder.setRequestBody(TestData.requestBodyOnUpdateSets);

        given(this.restClientMock
                .executeScanWithSuiteOnUpdateSet(eq(TestData.suiteSysId), eq(TestData.requestBodyOnUpdateSets)))
                .willReturn(getPendingResult());
        given(this.restClientMock.checkProgress()).willReturn(getSuccessfulResult(100,null));

        // when
        instanceScanBuilder.perform(runMock, null, launcherMock, taskListenerMock);

        // then
        assertThat(instanceScanBuilder.getRestClient(), is(restClientMock));
        verify(restClientMock, times(1)).executeScanWithSuiteOnUpdateSet(eq(TestData.suiteSysId), eq(TestData.requestBodyOnUpdateSets));
        verify(restClientMock, times(1)).checkProgress();
    }

    @Test
    public void shouldFillScanTypeItems() {
        // given
        InstanceScanBuilder.DescriptorImpl descriptor = new InstanceScanBuilder.DescriptorImpl();

        // when
        ListBoxModel result = descriptor.doFillScanTypeItems();

        // then
        assertThat(result, notNullValue());
        assertThat(result, hasSize(ScanType.values().length));
        final List<String> resultOptionValues = result.stream()
                .map(option -> option.value)
                .collect(Collectors.toList());
        for(ScanType scanType : ScanType.values()) {
            assertThat(resultOptionValues, hasItem(scanType.name()));
        }
    }

    private interface TestData {
        String url = "https://test.service-now.com";
        String credentials = "1234";
        String targetTable = "table";
        String targetRecordId = "recordId";
        String comboSysId = "23kj4";
        String suiteSysId = "456kjl";
        String requestBodyOnScopedApps = "{app_scope_sys_ids: ['123', '234']}";
        String requestBodyOnUpdateSets = "{update_set_sys_ids: ['345', '456']}";
    }

}