package io.jenkins.plugins.servicenow.instancescan;

import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class SuiteScanOnScopedAppsTest extends TestCase {

    @Mock
    private ServiceNowAPIClient apiClient;

    private SuiteScanOnScopedApps testedSuiteScan = new SuiteScanOnScopedApps();

    @Test
    public void shouldBeApplicableForSuiteScan() {
        // given
        ScanType scanType = ScanType.scanWithSuiteOnScopedApps;

        // when
        boolean result = testedSuiteScan.isApplicable(scanType);

        // then
        assertTrue(result);
    }

    @Test
    public void shouldNotBeApplicableForOtherScanTypes() {
        boolean result;
        for(ScanType scanType : ScanType.values()) {
            if(scanType == ScanType.scanWithSuiteOnScopedApps) {
                continue;
            }
            // when
            result = testedSuiteScan.isApplicable(scanType);

            // then
            assertFalse("Failed for scan type=" + scanType, result);
        }
    }

    @Test
    public void shouldSendRequest() throws IOException, URISyntaxException {
        // given
        String param1 = "suiteId";
        String param2 = "requestBody";
        String[] params = new String[]{param1, param2};
        Result expectedResult = new Result();
        given(apiClient.executeScanWithSuiteOnScopedApps(eq(param1), eq(param2))).willReturn(expectedResult);

        // when
        Result result = testedSuiteScan.execute(apiClient, params);

        // then
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldSendRequestWithEmptyParameters() throws IOException, URISyntaxException {
        // given
        String[] params = new String[2];
        Result expectedResult = new Result();
        given(apiClient.executeScanWithSuiteOnScopedApps(isNull(), isNull())).willReturn(expectedResult);

        // when
        Result result = testedSuiteScan.execute(apiClient, params);

        // then
        assertThat(result, is(expectedResult));
    }
}