package io.jenkins.plugins.servicenow.api;

public interface RequestParameters {

    String API_VERSION = "api_version";
    String SCOPE = "scope";
    String APP_SCOPE = "app_scope";
    String SYSTEM_ID = "sys_id";
    String APP_SYSTEM_ID = "app_sys_id";
    String BRANCH_NAME = "branch_name";

    String TEST_SUITE_NAME = "test_suite_name";
    String TEST_SUITE_SYS_ID = "test_suite_sys_id";
    String OS_NAME = "os_name";
    String OS_VERSION = "os_version";
    String BROWSER_NAME = "browser_name";
    String BROWSER_VERSION = "browser_version";

    String APP_VERSION = "version";
    String DEV_NOTES = "dev_notes";

    // instance scan parameters
    String TARGET_TABLE = "target_table";
    String TARGET_SYS_ID = "target_sys_id";
}
