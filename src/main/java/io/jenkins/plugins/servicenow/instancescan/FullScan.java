package io.jenkins.plugins.servicenow.instancescan;

import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;

import java.io.IOException;
import java.net.URISyntaxException;

public class FullScan implements ScanAction {
    @Override
    public boolean isApplicable(ScanType scanType) {
        return ScanType.fullScan.equals(scanType);
    }

    /**
     * Execute full instance scan.
     * @param apiClient ServiceNow API client
     * @param parameters Not required here
     * @return Result or link to a progress of the instance scan.
     * @throws IOException
     * @throws URISyntaxException
     */
    @Override
    public Result execute(ServiceNowAPIClient apiClient, String[] parameters) throws IOException, URISyntaxException {
        return apiClient.executeFullScan();
    }
}
