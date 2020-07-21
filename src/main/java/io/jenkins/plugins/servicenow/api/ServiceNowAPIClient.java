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

    /**
     *
     * @param url URL of the ServiceNow API
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

    //public static boolean

    public Result applyChanges(
            final String applicationScope,
            final String systemId,
            final String branchName) throws IOException {
        final String endpoint = "sc/apply_changes";
        LOG.info("ServiceNow API call > applyChanges");

        List<NameValuePair> params = new ArrayList<>();
        if(StringUtils.isNotBlank(applicationScope)) {
            params.add(new BasicNameValuePair(RequestParameters.APP_SCOPE, applicationScope));
        }
        if(StringUtils.isNotBlank(systemId)) {
            params.add(new BasicNameValuePair(RequestParameters.SYSTEM_ID, systemId));
        }
        if(StringUtils.isNotBlank(branchName)) {
            params.add(new BasicNameValuePair(RequestParameters.BRANCH_NAME, branchName));
        }

        Response response = this.post(endpoint, params, null);

        final Result result = response != null ? response.getResult() : null;
        if(result != null) {
            if(ActionStatus.FAILED.getStatus().equals(result.getStatus())) {
                LOG.warn("Response with failed result came for the request 'applyChanges': " + response.toString());
            } else if(result.getLinks().getProgress() != null) {
                this.lastActionProgressUrl = result.getLinks().getProgress().getUrl();
            }
        }

        return result;
    }

    public Result checkProgress() {
        if(StringUtils.isBlank(this.lastActionProgressUrl)) {
            throw new IllegalStateException("Did you forget to call action? Action request must be called first to have active link to the progress!");
        }
        final String endpoint = this.lastActionProgressUrl;
        final Response response = get(endpoint, null);

        final Result result = response != null ? response.getResult() : null;
        if(result != null) {
            if(ActionStatus.FAILED.getStatus().equals(result.getStatus())) {
                LOG.warn("Response with failed result came for the request 'checkProgress': " + response.toString());
            } else if(result.getLinks().getProgress() != null) {
                this.lastActionProgressUrl = result.getLinks().getProgress().getUrl();
            }
        }

        return result;
    }

    private Response get(final String endpointPath, final List<NameValuePair> parameters) {
        try(CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(getCredentials()).build()) {

            HttpGet request = new HttpGet();
            HttpResponse response = callRequest(client, request, endpointPath, parameters, null); //client.execute(request);

            final int responseStatusCode = response.getStatusLine().getStatusCode();
            if(responseStatusCode < 200 || responseStatusCode > 202) {
                LOG.error("GET request [" + request.getURI().toString() + "] call with error status: " + responseStatusCode);
            } else {
                this.lastActionProgressUrl = StringUtils.EMPTY;
            }

            String responseJSON = EntityUtils.toString(response.getEntity());
            LOG.info(responseJSON);
            Response result = new ObjectMapper().readValue(responseJSON, Response.class);
            if(result.getError() != null) {
                Error error = result.getError();
                LOG.error(error);
                throw new ServiceNowApiException(error.getMessage(), error.getDetail());
            }

            return result;
        } catch(URISyntaxException ex) {
            LOG.error("Wrong URL: " + ex.getMessage());
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private Response post(final String endpointPath, final List<NameValuePair> parameters, final JsonData jsonBody) {
        this.lastActionProgressUrl = StringUtils.EMPTY;
        try(CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(getCredentials()).build()) {

            HttpPost request = new HttpPost();
            HttpResponse response = callRequest(client, request, endpointPath, parameters, jsonBody); //client.execute(request);

            final int responseStatusCode = response.getStatusLine().getStatusCode();
            if(responseStatusCode < 200 || responseStatusCode > 202) {
                LOG.error("POST request [" + request.getURI().toString() + "] call with error status: " + responseStatusCode);
            }

            String responseJSON = EntityUtils.toString(response.getEntity());
            LOG.info(responseJSON);
            Response result = new ObjectMapper().readValue(responseJSON, Response.class);
            if(result.getError() != null) {
                Error error = result.getError();
                LOG.error(error);
                throw new ServiceNowApiException(error.getMessage(), error.getDetail());
            }

            return result;
        } catch(URISyntaxException ex) {
            LOG.error("Wrong URL: " + ex.getMessage());
        } catch(IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private HttpResponse callRequest(final CloseableHttpClient client, final HttpRequestBase request, final String endpointPath, final List<NameValuePair> parameters, final JsonData jsonBody) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(isURL(endpointPath) ? endpointPath : this.getCICDApiUrl() + endpointPath);
        if(parameters != null) {
            uriBuilder.setParameters(parameters);
        }

        request.setURI(uriBuilder.build());
        request.setHeader(HttpHeaders.USER_AGENT, "Jenkins plugin client");
        request.setHeader(HttpHeaders.ACCEPT, "application/json");
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        if(jsonBody != null && request instanceof HttpPost) {
            final HttpEntity requestBody = new StringEntity(new ObjectMapper().writeValueAsString(jsonBody));
            ((HttpPost)request).setEntity(requestBody);
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
}
