package io.jenkins.plugins.servicenow;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.jenkins.plugins.servicenow.application.ApplicationVersion;
import io.jenkins.plugins.servicenow.application.WorkspaceApplicationVersion;
import io.jenkins.plugins.servicenow.instancescan.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ServiceNowModule extends AbstractModule {

    private static final Logger LOG = LogManager.getLogger(ServiceNowModule.class);

    @Override
    protected void configure() {
        bind(RunFactory.class).to(RestClientFactory.class).in(Singleton.class);
        bind(ApplicationVersion.class).to(WorkspaceApplicationVersion.class).in(Singleton.class);

        // inject available Scan Actions
        final Multibinder<ScanAction> scanExecutors =
                Multibinder.newSetBinder(binder(), ScanAction.class);

        scanExecutors.addBinding().to(FullScan.class);
        scanExecutors.addBinding().to(PointScan.class);
        scanExecutors.addBinding().to(ComboScan.class);
        scanExecutors.addBinding().to(SuiteScanOnScopedApps.class);
        scanExecutors.addBinding().to(SuiteScanOnUpdateSets.class);

    }
}
