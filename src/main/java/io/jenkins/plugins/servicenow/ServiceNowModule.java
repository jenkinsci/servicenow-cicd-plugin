package io.jenkins.plugins.servicenow;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class ServiceNowModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(RunFactory.class).to(RestClientFactory.class).in(Singleton.class);
    }
}
