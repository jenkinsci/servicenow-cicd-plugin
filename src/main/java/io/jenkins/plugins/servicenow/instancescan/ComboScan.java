package io.jenkins.plugins.servicenow.instancescan;

import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;

import java.io.IOException;
import java.net.URISyntaxException;

public class ComboScan implements ScanAction {
    @Override
    public boolean isApplicable(ScanType scanType) {
        return ScanType.scanWithCombo.equals(scanType);
    }

    /**
     * Send a request to execute a scan with combo.
     * @param apiClient
     * @param parameters Combo scan arguments: 1 argument: combo system id
     * @return Result or link to a progress of the scan
     * @throws IOException
     * @throws URISyntaxException
     */
    @Override
    public Result execute(ServiceNowAPIClient apiClient, String[] parameters) throws IOException, URISyntaxException {
        final String comboSysId = parameters.length > 0 ? parameters[0] : null;
        return apiClient.executeScanWithCombo(comboSysId);
    }
}
