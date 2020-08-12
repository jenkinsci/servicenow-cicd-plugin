package io.jenkins.plugins.servicenow;

import hudson.model.Run;

public interface RunFactory<T> {

    T create(Run run, String... parameters );
}
