package io.jenkins.plugins.servicenow;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.jenkins.plugins.servicenow.application.ApplicationVersion;
import io.jenkins.plugins.servicenow.application.WorkspaceApplicationVersion;

public class ServiceNowModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(RunFactory.class).to(RestClientFactory.class).in(Singleton.class);
        bind(ApplicationVersion.class).to(WorkspaceApplicationVersion.class).in(Singleton.class);
    }
}
