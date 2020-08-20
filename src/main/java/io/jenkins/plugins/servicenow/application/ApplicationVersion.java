package io.jenkins.plugins.servicenow.application;

public interface ApplicationVersion {

    /**
     * Get version of the application defined by sysId or scopeId.
     * @param target Target place where the application version can be taken from (path to app project in source control,
     *               url of a repository, or other)
     * @param sysId Sys ID of the application.  You can locate this value in the Sys ID field in the Custom Application [sys_app] table.
     * @param scope The scope name of the application, such as x_aah_custom_app. You can locate this value in the scope field in the Custom Application [sys_app] table.
     * @return Version of the application.
     */
    String getVersion(String target, final String sysId, final String scope);
}
