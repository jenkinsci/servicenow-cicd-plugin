package io.jenkins.plugins.servicenow.application;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkspaceApplicationVersionTest {

    private Path resourceDirectory = Paths.get("src","test","resources");

    private WorkspaceApplicationVersion applicationVersion = new WorkspaceApplicationVersion();

    @Test
    public void shouldGetVersionFromWorkspace_BySysId() {
        // given
        final String workspaceDir = "source-control-1";
        final String workspace = resourceDirectory.toFile().getAbsolutePath() + "/" + workspaceDir;
        final String sysId = "90eb12afdb021010b40a9eb5db9619aa";

        // when
        final String result = applicationVersion.getVersion(workspace, sysId, null);

        // then
        assertThat(result).isEqualTo("1.0.27");
    }

    @Test
    public void shouldGetVersionFromWorkspace_ByScope() {
        // given
        final String workspaceDir = "source-control-1";
        final String workspace = resourceDirectory.toFile().getAbsolutePath() + "/" + workspaceDir;
        final String sysId = "90eb12afdb021010b40a9eb5db9619aa";

        // when
        final String result = applicationVersion.getVersion(workspace, sysId, null);

        // then
        assertThat(result).isEqualTo("1.0.27");
    }

    @Test
    public void shouldGetEmptyVersion_noApplicationData() {
        // given
        final String workspaceDir = "source-control-2";
        final String workspace = resourceDirectory.toFile().getAbsolutePath() + "/" + workspaceDir;
        final String scope = "x_sofse_cicdjenkin";

        // when
        final String result = applicationVersion.getVersion(workspace, null, scope);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldGetEmptyVersion_withoutRepository() {
        // given
        final String workspaceDir = "source-control-3";
        final String workspace = resourceDirectory.toFile().getAbsolutePath() + "/" + workspaceDir;
        final String scope = "x_sofse_cicdjenkin";

        // when
        final String result = applicationVersion.getVersion(workspace, null, scope);

        // then
        assertThat(result).isEmpty();
    }

}