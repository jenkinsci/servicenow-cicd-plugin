package io.jenkins.plugins.servicenow;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Run;
import hudson.util.Secret;
import io.jenkins.plugins.servicenow.api.ServiceNowAPIClient;

public class RestClientFactory implements RunFactory<ServiceNowAPIClient> {

    @Override
    public ServiceNowAPIClient create(Run run, String... parameters) {
        if(parameters == null || parameters.length != 2) {
            throw new IllegalArgumentException("Factory requires 2 parameters for api url and credentials identifier!");
        }
        return create(run, parameters[0], parameters[1]);
    }

    private ServiceNowAPIClient create(Run<?, ?> run, String apiUrl, String credentialsId) {
        final StandardUsernamePasswordCredentials usernamePasswordCredentials =
                CredentialsProvider.findCredentialById(credentialsId, StandardUsernamePasswordCredentials.class, run, new DomainRequirement());
        ServiceNowAPIClient serviceNowAPIClient = null;
        if(usernamePasswordCredentials != null) {
            final Secret password = usernamePasswordCredentials.getPassword();
            serviceNowAPIClient = new ServiceNowAPIClient(apiUrl, usernamePasswordCredentials.getUsername(), password);
        }
        return serviceNowAPIClient;
    }
}
