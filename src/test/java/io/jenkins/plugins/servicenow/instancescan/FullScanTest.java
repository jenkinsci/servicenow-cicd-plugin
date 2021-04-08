package io.jenkins.plugins.servicenow.instancescan;

import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class FullScanTest {

    @Mock
    private ServiceNowAPIClient apiClient;

    private FullScan testedFullScan = new FullScan();

    @Test
    public void shouldBeApplicableForFullScan() {
        // given
        ScanType scanType = ScanType.fullScan;

        // when
        boolean result = testedFullScan.isApplicable(scanType);

        // then
        assertTrue(result);
    }

    @Test
    public void shouldNotBeApplicableForOtherScanTypes() {
        boolean result;
        for(ScanType scanType : ScanType.values()) {
            if(scanType == ScanType.fullScan) {
                continue;
            }
            // when
            result = testedFullScan.isApplicable(scanType);

            // then
            assertFalse("Failed for scan type=" + scanType, result);
        }
    }

    @Test
    public void shouldSendRequestWithDummyParameters() throws IOException, URISyntaxException {
        // given
        String param1 = "test";
        String[] params = new String[]{param1};
        Result expectedResult = new Result();
        given(apiClient.executeFullScan()).willReturn(expectedResult);

        // when
        Result result = testedFullScan.execute(apiClient, params);

        // then
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldSendRequest() throws IOException, URISyntaxException {
        // given
        String[] params = new String[0];
        Result expectedResult = new Result();
        given(apiClient.executeFullScan()).willReturn(expectedResult);

        // when
        Result result = testedFullScan.execute(apiClient, params);

        // then
        assertThat(result, is(expectedResult));
    }
}