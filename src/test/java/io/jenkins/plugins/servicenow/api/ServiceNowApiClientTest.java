package io.jenkins.plugins.servicenow.api;

import hudson.util.Secret;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.verify.VerificationTimes;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ServiceNowApiClientTest {
    private static final Logger LOG = LogManager.getLogger(ServiceNowAPIClientIntegrationTest.class);

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);

    private static MockServerClient mockServer;
    private int PORT;

    private ServiceNowAPIClient serviceNowAPIClient;

    private static String HOST_MOCKED = "localhost";
    private static String USER_MOCKED = "test";
    private static Secret PASSWORD_MOCK;
    private static String PROGRESS_ID = "1234";

    @Before
    public void setUp() throws Exception {
        PORT = mockServerRule.getPort();
        final String url = "http://" + HOST_MOCKED + ":" + PORT;
        LOG.info("Mock server started at port: " + PORT);

        serviceNowAPIClient = new ServiceNowAPIClient(url, USER_MOCKED, PASSWORD_MOCK);
    }

    @Test
    public void applyChanges() throws IOException, URISyntaxException {
        // given
        String systemId = "testSystemId";
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/sn_cicd/sc/apply_changes")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody("{\n" +
                                        "    \"result\": {\n" +
                                        "        \"links\": {\n" +
                                        "            \"progress\": {\n" +
                                        "                \"id\": \"" + PROGRESS_ID + "\",\n" +
                                        "                \"url\": \"http://" + HOST_MOCKED + ":" + PORT + "/api/sn_cicd/progress/" + PROGRESS_ID + "\"\n" +
                                        "            }\n" +
                                        "        },\n" +
                                        "        \"status\": \"0\",\n" +
                                        "        \"status_label\": \"Pending\",\n" +
                                        "        \"status_message\": \"\",\n" +
                                        "        \"status_detail\": \"\",\n" +
                                        "        \"error\": \"\",\n" +
                                        "        \"percent_complete\": 0\n" +
                                        "    }\n" +
                                        "}")
                );

        // when
        Result result = serviceNowAPIClient.applyChanges(null, systemId, null);

        // then
        mockServer.verify(
                request("/api/sn_cicd/sc/apply_changes"), VerificationTimes.exactly(1)
        );
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("0");
        assertThat(result.getLinks().getProgress().getId()).isEqualTo(PROGRESS_ID);
        assertThat(result.getLinks().getProgress().getUrl()).contains(HOST_MOCKED, Integer.toString(PORT), PROGRESS_ID);
        assertThat(serviceNowAPIClient.getLastActionProgressUrl()).endsWith(PROGRESS_ID);
    }

    @Test
    public void applyChanges_notAuthorized() throws IOException {
        // given
        String systemId = "testSystemId";
        String errorMessage = "User Not Authenticated";
        String errorDetail = "Required to provide Auth information";
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/sn_cicd/sc/apply_changes")
        )
                .respond(
                        response()
                                .withStatusCode(401)
                                .withBody("{\"error\":{\"message\":\""+errorMessage+"\",\"detail\":\""+errorDetail+"\"},\"status\":\"failure\"}")
                );

        // when
        Result result = null;
        Exception expectedException = null;
        try {
            result = serviceNowAPIClient.applyChanges(null, systemId, null);
        } catch (Exception ex) {
            expectedException = ex;
        }

        // then
        mockServer.verify(
                request("/api/sn_cicd/sc/apply_changes"), VerificationTimes.exactly(1)
        );
        assertThat(result).isNull();
        assertThat(expectedException).isNotNull();
        assertThat(expectedException).isInstanceOf(ServiceNowApiException.class);
        assertThat(expectedException.getMessage()).isEqualTo(errorMessage);
        assertThat(((ServiceNowApiException)expectedException).getDetail()).isEqualTo(errorDetail);
    }

    @Test
    public void applyChanges_invalidAppSysId() throws IOException, URISyntaxException {
        // given
        String systemId = "testSystemId";
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/sn_cicd/sc/apply_changes")
        )
                .respond(
                        response()
                                .withStatusCode(404)
                                .withBody("{\"result\":{\"status\":\"3\",\"status_label\":\"Failed\",\"status_message\":\"\",\"status_detail\":\"\",\"error\":\"Invalid app sys id: "+systemId+"\"}}")
                );

        // when
        Result result = serviceNowAPIClient.applyChanges(null, systemId, null);

        // then
        mockServer.verify(
                request("/api/sn_cicd/sc/apply_changes"), VerificationTimes.exactly(1)
        );
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("3");
        assertThat(result.getLinks()).isNull();
        assertThat(result.getError()).contains(systemId);
    }

    @Test
    public void checkProgress() throws IOException, URISyntaxException {
        // given
        String commitId = "1590365066990cfe14b2f974cfd9db33b4c49be6";
        String PROGRESS_ID2 = "5678";
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/sn_cicd/sc/apply_changes")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody("{\n" +
                                        "    \"result\": {\n" +
                                        "        \"links\": {\n" +
                                        "            \"progress\": {\n" +
                                        "                \"id\": \"" + PROGRESS_ID + "\",\n" +
                                        "                \"url\": \"http://" + HOST_MOCKED + ":" + PORT + "/api/sn_cicd/progress/" + PROGRESS_ID + "\"\n" +
                                        "            }\n" +
                                        "        },\n" +
                                        "        \"status\": \"0\",\n" +
                                        "        \"status_label\": \"Pending\",\n" +
                                        "        \"status_message\": \"\",\n" +
                                        "        \"status_detail\": \"\",\n" +
                                        "        \"error\": \"\",\n" +
                                        "        \"percent_complete\": 0\n" +
                                        "    }\n" +
                                        "}")
                );

        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/api/sn_cicd/progress/" + PROGRESS_ID)
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody("{\n" +
                                        "    \"result\": {\n" +
                                        "        \"links\": {\n" +
                                        "            \"progress\": {\n" +
                                        "                \"id\": \"" + PROGRESS_ID2 + "\",\n" +
                                        "                \"url\": \"http://" + HOST_MOCKED + ":" + PORT + "/api/sn_cicd/progress/" + PROGRESS_ID2 + "\"\n" +
                                        "            }\n" +
                                        "        },\n" +
                                        "        \"status\": \"2\",\n" +
                                        "        \"status_label\": \"Successful\",\n" +
                                        "        \"status_message\": \"This operation succeeded\",\n" +
                                        "        \"status_detail\": \"Successfully applied commit "+commitId+" from source control\",\n" +
                                        "        \"error\": \"\",\n" +
                                        "        \"percent_complete\": 100\n" +
                                        "    }\n" +
                                        "}")
                );

        // when
        Result result = serviceNowAPIClient.applyChanges(null, "1234", null);
        Result progressResult = serviceNowAPIClient.checkProgress();

        // then
        mockServer.verify(
                request("/api/sn_cicd/sc/apply_changes"), VerificationTimes.exactly(1)
        );
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("0");
        assertThat(result.getLinks().getProgress().getId()).isEqualTo(PROGRESS_ID);
        assertThat(result.getLinks().getProgress().getUrl()).contains(HOST_MOCKED, Integer.toString(PORT), PROGRESS_ID);

        mockServer.verify(
                request("/api/sn_cicd/progress/" + PROGRESS_ID), VerificationTimes.exactly(1)
        );
        assertThat(progressResult).isNotNull();
        assertThat(progressResult.getStatus()).isEqualTo("2");
        assertThat(progressResult.getLinks().getProgress().getId()).isEqualTo(PROGRESS_ID2);
        assertThat(progressResult.getLinks().getProgress().getUrl()).contains(HOST_MOCKED, Integer.toString(PORT), PROGRESS_ID2);
        assertThat(progressResult.getStatusDetail()).contains(commitId);
        assertThat(progressResult.getError()).isBlank();
        assertThat(progressResult.getPercentComplete()).isEqualTo(100);
        assertThat(serviceNowAPIClient.getLastActionProgressUrl()).endsWith(PROGRESS_ID2);
    }
}
