package io.jenkins.plugins.servicenow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.util.Secret;
import io.jenkins.plugins.servicenow.api.model.Error;
import io.jenkins.plugins.servicenow.api.model.Response;
import io.jenkins.plugins.servicenow.api.model.Result;
import io.jenkins.plugins.servicenow.api.model.TableResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static hudson.Util.removeTrailingSlash;

public class ServiceNowAPIClient {

    private static final Logger LOG = LogManager.getLogger(ServiceNowAPIClient.class);

    private String getCICDApiUrl() {
        return removeTrailingSlash(this.apiUrl) + "/api/sn_cicd/";
    }

    private String getTableApiUrl() {
        return removeTrailingSlash(this.apiUrl) + "/api/now/table/";
    }

    private static final String BATCH_INSTALL_ENDPOINT = "app/batch/install";

    private final String apiUrl;
    private final String username;
    private final Secret password;

    private String lastActionProgressUrl;

    public String getLastActionProgressUrl() {
        return lastActionProgressUrl;
    }

    public AcceptResponseType acceptResponseType = AcceptResponseType.JSON;

    private String getAcceptResponseType() {
        switch(acceptResponseType) {
            case JSON:
                return MediaType.JSON;
            case XML:
                return MediaType.XML;
        }
        return MediaType.JSON;
    }

    /**
     * @param url      URL of the ServiceNow API
     * @param username User name used to authorize requests
     * @param password User password used for request authorization
     */
    public ServiceNowAPIClient(final String url, final String username, final Secret password) {
        if(StringUtils.isBlank(url) || !isURL(url)) {
            throw new IllegalArgumentException("Wrong 'url' parameter. Should not be empty and should be valid url string starting from the phrase: 'http(s)://'");
        }
        this.apiUrl = url;
        this.username = username;
        this.password = password;
    }

    public Result applyChanges(
            final String applicationScope,
            final String systemId,
            final String branchName) throws IOException, URISyntaxException {
        final String endpoint = "sc/apply_changes";
        LOG.debug("ServiceNow API call > applyChanges");

        List<NameValuePair> params = new ArrayList<>();
        addParameter(params, RequestParameters.APP_SCOPE, applicationScope);
        addParameter(params, RequestParameters.APP_SYSTEM_ID, systemId);
        addParameter(params, RequestParameters.BRANCH_NAME, branchName);

        return sendRequest(endpoint, params, null);
    }

    public Result runTestSuite(
            final String testSuiteName,
            final String testSuiteSysId,
            final String osName,
            final String osVersion,
            final String browserName,
            final String browserVersion) throws IOException, URISyntaxException {
        final String endpoint = "testsuite/run";
        LOG.debug("ServiceNow API call > runTestSuite");

        List<NameValuePair> params = new ArrayList<>();
        addParameter(params, RequestParameters.TEST_SUITE_NAME, testSuiteName);
        addParameter(params, RequestParameters.TEST_SUITE_SYS_ID, testSuiteSysId);
        addParameter(params, RequestParameters.OS_NAME, osName);
        addParameter(params, RequestParameters.OS_VERSION, osVersion);
        addParameter(params, RequestParameters.BROWSER_NAME, browserName);
        addParameter(params, RequestParameters.BROWSER_VERSION, browserVersion);

        return sendRequest(endpoint, params, null);
    }

    public Result checkProgress() {
        if(StringUtils.isBlank(this.lastActionProgressUrl)) {
            throw new IllegalStateException("Did you forget to call action? Action request must be called first to have active link to the progress!");
        }
        final String endpoint = this.lastActionProgressUrl;

        return sendRequest(endpoint, null);
    }

    public Result getTestSuiteResults(String resultsId) {
        String endpoint = "testsuite/results/";
        LOG.debug("ServiceNow API call > runTestSuite");

        if(StringUtils.isBlank(resultsId)) {
            throw new ServiceNowApiException("Missing parameter 'results_id", "Parameter 'results_id is require when following API end-point is called " + endpoint);
        } else {
            endpoint += resultsId;
        }

        return sendRequest(endpoint, null);
    }

    public Result publishApp(final String applicationScope, final String applicationSysId, final String applicationVersion, final String devNotes) throws IOException, URISyntaxException {
        final String endpoint = "app_repo/publish";
        LOG.debug("ServiceNow API call > publishApp");

        List<NameValuePair> params = new ArrayList<>();
        addParameter(params, RequestParameters.SCOPE, applicationScope);
        addParameter(params, RequestParameters.SYSTEM_ID, applicationSysId);
        addParameter(params, RequestParameters.APP_VERSION, applicationVersion);
        addParameter(params, RequestParameters.DEV_NOTES, devNotes);

        return sendRequest(endpoint, params, null);
    }

    public Result installApp(final String applicationScope,
            final String applicationSysId,
            final String applicationVersion) throws IOException, URISyntaxException {
        return this.installApp(applicationScope, applicationSysId, applicationVersion, null, null);
    }

    public Result installApp(final String applicationScope,
            final String applicationSysId,
            final String applicationVersion,
            final String baseAppVersion,
            final Boolean autoUpgradeBaseApp) throws IOException, URISyntaxException {
        final String endpoint = "app_repo/install";
        LOG.debug("ServiceNow API call > installApp");

        List<NameValuePair> params = new ArrayList<>();
        addParameter(params, RequestParameters.SCOPE, applicationScope);
        addParameter(params, RequestParameters.SYSTEM_ID, applicationSysId);
        addParameter(params, RequestParameters.APP_VERSION, applicationVersion);
        if(StringUtils.isNotBlank(baseAppVersion)) {
            addParameter(params, RequestParameters.APP_BASE_VERSION, baseAppVersion);
        }
        if(autoUpgradeBaseApp != null) {
            addParameter(params, RequestParameters.APP_AUTO_UPGRADE_BASE, autoUpgradeBaseApp.toString());
        }

        return sendRequest(endpoint, params, null);
    }

    public Result rollbackApp(final String applicationScope, final String applicationSysId, final String rollbackVersion) throws IOException, URISyntaxException {
        final String endpoint = "app_repo/rollback";
        LOG.debug("ServiceNow API call > rollbackApp");

        List<NameValuePair> params = new ArrayList<>();
        addParameter(params, RequestParameters.SCOPE, applicationScope);
        addParameter(params, RequestParameters.SYSTEM_ID, applicationSysId);
        addParameter(params, RequestParameters.APP_VERSION, rollbackVersion);

        return sendRequest(endpoint, params, null);
    }

    public Result activatePlugin(String pluginId) throws IOException, URISyntaxException {
        final String endpoint = "plugin/" + pluginId + "/activate";
        LOG.debug("ServiceNow API call > activatePlugin");

        return sendRequest(endpoint, null, null);
    }

    public Result rollbackPlugin(String pluginId) throws IOException, URISyntaxException {
        final String endpoint = "plugin/" + pluginId + "/rollback";
        LOG.debug("ServiceNow API call > rollbackPlugin");

        return sendRequest(endpoint, null, null);
    }

    public String getCurrentAppVersion(final String applicationScope, final String systemId) {
        return getAppVersion(false, applicationScope, systemId);
    }

    public String getCurrentAppCustomizationVersion(final String applicationScope, final String systemId) {
        return getAppVersion(true, applicationScope, systemId);
    }

    private String getAppVersion(boolean customized, final String applicationScope, final String systemId) {
        String endpoint = getTableApiUrl() + (customized ? "sys_app_customization" : "sys_app");
        if(StringUtils.isNotBlank(systemId)) {
            endpoint += "/" + systemId + "?sysparm_fields=version";
            final Result result = sendRequest(endpoint, null);
            if(result != null && result.getUnboundAttributes().containsKey("version")) {
                return (String) result.getUnboundAttributes().getOrDefault("version", StringUtils.EMPTY);
            }
        } else if(StringUtils.isNotBlank(applicationScope)) {
            endpoint += "?sysparm_fields=scope,version";
            final TableResponse response = this.getTable(endpoint, null);
            if(response != null && response.getUnboundAttributes().containsKey("result")) {
                return (String) ((List) response.getUnboundAttributes().get("result")).stream()
                        .filter(record ->
                                record instanceof HashMap &&
                                        ((HashMap) record).containsKey("scope") &&
                                        applicationScope.equals(((HashMap) record).get("scope")))
                        .map(record -> ((HashMap) record).get("version"))
                        .findFirst()
                        .orElse(StringUtils.EMPTY);
            }
        } else {
            throw new IllegalArgumentException("One of arguments (system id or application scope) must be valid!");
        }
        return StringUtils.EMPTY;
    }

    public Result executeFullScan() throws IOException, URISyntaxException {
        final String endpoint = "instance_scan/full_scan";
        LOG.debug("ServiceNow API call > execute full scan");

        return sendRequest(endpoint, null, null);
    }

    /**
     * Execute Point Scan (with progress flow).
     * @param targetTable Target table to be scanned.
     * @param targetSysId Target record to be scanned.
     * @return Results with a link to follow the progress of the scan.
     */
    public Result executePointScan(final String targetTable, final String targetSysId) throws IOException, URISyntaxException {
        final String endpoint = "instance_scan/point_scan";
        LOG.debug("ServiceNow API call > execute point scan");

        List<NameValuePair> params = new ArrayList<>();
        addParameter(params, RequestParameters.TARGET_TABLE, targetTable);
        addParameter(params, RequestParameters.TARGET_SYS_ID, targetSysId);

        return sendRequest(endpoint, params, null);
    }

    public Result executeScanWithCombo(final String comboSysId) throws IOException, URISyntaxException {
        if(StringUtils.isBlank(comboSysId)) {
            throw new IllegalArgumentException("Scan with combo cannot be executed without the parameter combo_sys_id!");
        }
        final String endpoint = "instance_scan/suite_scan/combo/" + comboSysId;
        LOG.debug("ServiceNow API call > execute scan with combo");

        return sendRequest(endpoint, null, null);
    }

    public Result executeScanWithSuiteOnScopedApps(final String suiteSysId, final String requestBody) throws IOException, URISyntaxException {
        if(StringUtils.isBlank(suiteSysId)) {
            throw new IllegalArgumentException("Scan with suite on scoped apps cannot be executed without the parameter suiteSysId!");
        }
        final String endpoint = "instance_scan/suite_scan/" + suiteSysId + "/scoped_apps";
        LOG.debug("ServiceNow API call > execute scan with suite on scoped apps");

        return sendRequest(endpoint, null, requestBody);
    }

    public Result executeScanWithSuiteOnUpdateSet(final String suiteSysId, String requestBody) throws IOException, URISyntaxException {
        if(StringUtils.isBlank(suiteSysId)) {
            throw new IllegalArgumentException("Scan with suite on scoped apps cannot be executed without the parameter suiteSysId!");
        }
        final String endpoint = "instance_scan/suite_scan/" + suiteSysId + "/update_sets";
        LOG.debug("ServiceNow API call > execute scan with suite on update set");

        return sendRequest(endpoint, null, requestBody);
    }

    public Result batchInstall(String payload) throws IOException, URISyntaxException {
        final String endpoint = BATCH_INSTALL_ENDPOINT;
        LOG.debug("ServiceNow API call > batch install");

        LOG.debug("Batch install payload: " + payload);

        return sendRequest(endpoint, null, payload);
    }

    public Result batchInstall(final String batchName, final String packages, final String notes) throws IOException, URISyntaxException {
        final String endpoint = BATCH_INSTALL_ENDPOINT;
        LOG.debug("ServiceNow API call > batch install");

        String requestBody = "{" +
                MessageFormat.format(
                        "\"name\": \"{0}\", \"packages\": {1}, \"notes\": \"{2}\"",
                        batchName, packages, notes) +
                "}";

        LOG.debug("Batch install payload: " + requestBody);

        return sendRequest(endpoint, null, requestBody);
    }

    public Result batchRollback(final String rollbackId) throws IOException, URISyntaxException {
        if(StringUtils.isBlank(rollbackId)) {
            throw new IllegalArgumentException("Rollback id must not be empty or blank!");
        }
        final String endpoint = "app/batch/rollback/" + rollbackId;
        LOG.debug("ServiceNow API call > batch rollback [id=" + rollbackId + "]");

        return sendRequest(endpoint, null, null);
    }

    /**
     * Send POST request using following parameters.
     *
     * @param endpoint End-point path
     * @param params   Request parameters
     * @param jsonBody Body of the request (as JSON object)
     * @return Result of the response or null if there was thrown an exception.
     */
    private Result sendRequest(String endpoint, List<NameValuePair> params, String jsonBody) throws IOException, URISyntaxException {
        Response response = this.post(endpoint, params, jsonBody);

        return getResult(endpoint, response);
    }

    /**
     * Send GET request.
     *
     * @param endpoint End-point path
     * @param params   Request parameters
     * @return Result of the response or null if there was thrown an exception.
     */
    private Result sendRequest(String endpoint, List<NameValuePair> params) {
        Response response = this.get(endpoint, params);

        return getResult(endpoint, response);
    }

    private void addParameter(final List<NameValuePair> toParamsList, final String paramName, final String paramValue) {
        if(StringUtils.isNotBlank(paramValue)) {
            toParamsList.add(new BasicNameValuePair(paramName, paramValue));
        }
    }

    private Result getResult(String endpoint, Response response) {
        final Result result = response != null ? response.getResult() : null;
        if(result != null) {
            if(ActionStatus.FAILED.getStatus().equals(result.getStatus())) {
                LOG.warn("Response with failed result came for the request '" + endpoint + "': " + response.toString());
            } else if(result.getLinks() != null && result.getLinks().getProgress() != null) {
                this.lastActionProgressUrl = result.getLinks().getProgress().getUrl();
            }
        }

        return result;
    }

    private Response get(final String endpointPath, final List<NameValuePair> parameters) {
        try(CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(getCredentials()).build()) {

            HttpGet request = new HttpGet();
            HttpResponse response = sendRequest(client, request, endpointPath, parameters, null); //client.execute(request);

            final int responseStatusCode = response.getStatusLine().getStatusCode();
            if(responseStatusCode < 200 || responseStatusCode > 202) {
                LOG.error("GET request [" + request.getURI().toString() + "] call with error status: " + responseStatusCode);
            } else {
                this.lastActionProgressUrl = StringUtils.EMPTY;
            }

            return getResponse(response);
        } catch(URISyntaxException ex) {
            LOG.error("Wrong URL: " + ex.getMessage());
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private TableResponse getTable(final String endpointPath, final List<NameValuePair> parameters) {
        try(CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(getCredentials()).build()) {

            HttpGet request = new HttpGet();
            HttpResponse response = sendRequest(client, request, endpointPath, parameters, null); //client.execute(request);

            final int responseStatusCode = response.getStatusLine().getStatusCode();
            if(responseStatusCode < 200 || responseStatusCode > 202) {
                LOG.error("GET request [" + request.getURI().toString() + "] call with error status: " + responseStatusCode);
            } else {
                this.lastActionProgressUrl = StringUtils.EMPTY;
            }

            String responseJSON = EntityUtils.toString(response.getEntity());
            TableResponse result = new ObjectMapper().readValue(responseJSON, TableResponse.class);
            return result;
        } catch(URISyntaxException ex) {
            LOG.error("Wrong URL: " + ex.getMessage());
        } catch(IOException ex) {
            LOG.error("GET request [" + endpointPath + "] replied with error!", ex);
        }
        return null;
    }

    private Response post(final String endpointPath, final List<NameValuePair> parameters, final String jsonBody) throws URISyntaxException, IOException {
        this.lastActionProgressUrl = StringUtils.EMPTY;
        try(CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(getCredentials()).build()) {

            HttpPost request = new HttpPost();
            HttpResponse response = sendRequest(client, request, endpointPath, parameters, jsonBody);

            final int responseStatusCode = response.getStatusLine().getStatusCode();
            if(responseStatusCode < 200 || responseStatusCode > 202) {
                LOG.error("POST request [" + request.getURI().toString() + "] call with error status: " + responseStatusCode);
            }

            return getResponse(response);
        } catch(URISyntaxException ex) {
            LOG.error("Wrong URL: " + ex.getMessage());
            throw ex;
        } catch(IOException ex) {
            LOG.error(ex);
            throw ex;
        }
    }

    private Response getResponse(HttpResponse response) throws IOException {
        String responseJSON = EntityUtils.toString(response.getEntity());
        LOG.debug(responseJSON);
        Response result = new ObjectMapper().readValue(responseJSON, Response.class);
        if(result.getError() != null) {
            Error error = result.getError();
            LOG.error(error);
            throw new ServiceNowApiException(error.getMessage(), error.getDetail());
        }

        return result;
    }

    private HttpResponse sendRequest(final CloseableHttpClient client, final HttpRequestBase request, final String endpointPath, final List<NameValuePair> parameters, final String jsonBody) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(isURL(endpointPath) ? endpointPath : this.getCICDApiUrl() + endpointPath);
        if(parameters != null) {
            uriBuilder.setParameters(parameters);
        }

        request.setURI(uriBuilder.build());
        request.setHeader(HttpHeaders.USER_AGENT, "sncicd_extint_jenkins");
        request.setHeader(HttpHeaders.ACCEPT, getAcceptResponseType());
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON);

        if(jsonBody != null && request instanceof HttpPost) {
            final HttpEntity requestBody = new StringEntity(jsonBody);
            ((HttpPost) request).setEntity(requestBody);
        }

        return client.execute(request);
    }

    private CredentialsProvider getCredentials() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(this.username, this.password != null ? this.password.getPlainText() : StringUtils.EMPTY)
        );
        return provider;
    }

    private boolean isURL(String value) {
        final String regex = "^https?://.+";
        return value.matches(regex);
    }

    public enum AcceptResponseType {
        JSON,
        XML
    }

    private interface MediaType {
        String JSON = "application/json";
        String XML = "application/xml";
    }
}
