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
public class ComboScanTest extends TestCase {

    @Mock
    private ServiceNowAPIClient apiClient;

    private ComboScan testedComboScan = new ComboScan();

    @Test
    public void shouldBeApplicableForScanWithCombo() {
        // given
        ScanType scanType = ScanType.scanWithCombo;

        // when
        boolean result = testedComboScan.isApplicable(scanType);

        // then
        assertTrue(result);
    }

    @Test
    public void shouldNotBeApplicableForOtherScanTypes() {
        boolean result;
        for(ScanType scanType : ScanType.values()) {
            if(scanType == ScanType.scanWithCombo) {
                continue;
            }
            // when
            result = testedComboScan.isApplicable(scanType);

            // then
            assertFalse("Failed for scan type=" + scanType, result);
        }
    }

    @Test
    public void shouldSendRequest() throws IOException, URISyntaxException {
        // given
        String param1 = "test";
        String[] params = new String[]{param1};
        Result expectedResult = new Result();
        given(apiClient.executeScanWithCombo(eq(param1))).willReturn(expectedResult);

        // when
        Result result = testedComboScan.execute(apiClient, params);

        // then
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldSendRequestWithEmptyParameter() throws IOException, URISyntaxException {
        // given
        String[] params = new String[0];
        Result expectedResult = new Result();
        given(apiClient.executeScanWithCombo(isNull())).willReturn(expectedResult);

        // when
        Result result = testedComboScan.execute(apiClient, params);

        // then
        assertThat(result, is(expectedResult));
    }
}