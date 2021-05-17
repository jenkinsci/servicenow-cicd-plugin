package io.jenkins.plugins.servicenow.instancescan;

import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;
import io.jenkins.plugins.servicenow.api.model.Result;

import java.io.IOException;
import java.net.URISyntaxException;

public interface ScanAction {

    boolean isApplicable(ScanType scanType);

    Result execute(ServiceNowAPIClient apiClient, String... parameters) throws IOException, URISyntaxException;
}
