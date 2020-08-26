package io.jenkins.plugins.servicenow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jenkins.plugins.servicenow.api.model.Error;
import io.jenkins.plugins.servicenow.api.model.JsonData;
import io.jenkins.plugins.servicenow.api.model.Response;
import io.jenkins.plugins.servicenow.api.model.Result;
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
import java.util.ArrayList;
import java.util.List;

import static hudson.Util.removeTrailingSlash;

public class ServiceNowAPIClient {

    private static final Logger LOG = LogManager.getLogger(ServiceNowAPIClient.class);

    private String getCICDApiUrl() {
        return removeTrailingSlash(this.apiUrl) + "/api/sn_cicd/";
    }

    private final String apiUrl;
    private final String username;
    private final String password;

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
    public ServiceNowAPIClient(final String url, final String username, final String password) {
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


    public Result installApp(final String applicationScope, final String applicationSysId, final String applicationVersion) throws IOException, URISyntaxException {
        final String endpoint = "app_repo/install";
        LOG.debug("ServiceNow API call > installApp");

        List<NameValuePair> params = new ArrayList<>();
        addParameter(params, RequestParameters.SCOPE, applicationScope);
        addParameter(params, RequestParameters.SYSTEM_ID, applicationSysId);
        addParameter(params, RequestParameters.APP_VERSION, applicationVersion);

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
        final String endpoint = "plugin/"+pluginId+"/activate";
        LOG.debug("ServiceNow API call > activatePlugin");

        return sendRequest(endpoint, null, null);
    }

    public Result rollbackPlugin(String pluginId) throws IOException, URISyntaxException {
        final String endpoint = "plugin/"+pluginId+"/rollback";
        LOG.debug("ServiceNow API call > rollbackPlugin");

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
    private Result sendRequest(String endpoint, List<NameValuePair> params, JsonData jsonBody) throws IOException, URISyntaxException {
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
            } else if(result.getLinks().getProgress() != null) {
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

    private Response post(final String endpointPath, final List<NameValuePair> parameters, final JsonData jsonBody) throws URISyntaxException, IOException {
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

    private HttpResponse sendRequest(final CloseableHttpClient client, final HttpRequestBase request, final String endpointPath, final List<NameValuePair> parameters, final JsonData jsonBody) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(isURL(endpointPath) ? endpointPath : this.getCICDApiUrl() + endpointPath);
        if(parameters != null) {
            uriBuilder.setParameters(parameters);
        }

        request.setURI(uriBuilder.build());
        request.setHeader(HttpHeaders.USER_AGENT, "sncicd_extint_jenkins");
        request.setHeader(HttpHeaders.ACCEPT, getAcceptResponseType());
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON);

        if(jsonBody != null && request instanceof HttpPost) {
            final HttpEntity requestBody = new StringEntity(new ObjectMapper().writeValueAsString(jsonBody));
            ((HttpPost) request).setEntity(requestBody);
        }

        return client.execute(request);
    }

    private CredentialsProvider getCredentials() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(this.username, this.password)
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
