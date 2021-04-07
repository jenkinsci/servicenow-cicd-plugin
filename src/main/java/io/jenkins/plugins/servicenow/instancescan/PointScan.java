package io.jenkins.plugins.servicenow.instancescan;

import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;

import java.io.IOException;
import java.net.URISyntaxException;

public class PointScan implements ScanAction {
    @Override
    public boolean isApplicable(ScanType scanType) {
        return ScanType.pointScan.equals(scanType);
    }

    /**
     * Sends a request to execute point scan for specified table and record in parameters.
     * @param apiClient
     * @param parameters Point scan arguments: 1 argument: target table, 2 argument: target record (sys_id)
     * @return Result or link to a progress of the scan
     * @throws IOException
     * @throws URISyntaxException
     */
    @Override
    public Result execute(ServiceNowAPIClient apiClient, String[] parameters) throws IOException, URISyntaxException {
        final String targetTable = parameters.length > 0 ? parameters[0] : null;
        final String targetRecordId = parameters.length > 1 ? parameters[1] : null;
        return apiClient.executePointScan(targetTable, targetRecordId);
    }
}
