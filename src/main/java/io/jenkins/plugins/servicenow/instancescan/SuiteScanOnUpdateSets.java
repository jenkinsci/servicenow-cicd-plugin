package io.jenkins.plugins.servicenow.instancescan;

import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;

import java.io.IOException;
import java.net.URISyntaxException;

public class SuiteScanOnUpdateSets implements ScanAction {
    @Override
    public boolean isApplicable(ScanType scanType) {
        return ScanType.scanWithSuiteOnUpdateSets.equals(scanType);
    }

    /**
     * Sends a request to execute scan with suite on update sets.
     * @param apiClient
     * @param parameters Suite scan arguments: 1 argument: suite system id
     * @return Result or link to a progress of the scan
     * @throws IOException
     * @throws URISyntaxException
     */
    @Override
    public Result execute(ServiceNowAPIClient apiClient, String[] parameters) throws IOException, URISyntaxException {
        final String suiteSysId = parameters.length > 0 ? parameters[0] : null;
        final String requestBody = parameters.length > 1 ? parameters[1] : null;
        return apiClient.executeScanWithSuiteOnUpdateSet(suiteSysId, requestBody);
    }
}
