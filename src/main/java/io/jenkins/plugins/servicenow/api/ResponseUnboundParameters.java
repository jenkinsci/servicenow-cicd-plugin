package io.jenkins.plugins.servicenow.api;

public interface ResponseUnboundParameters {

    interface TestResults {
        String name = "test_suite_name";
        String status = "test_suite_status";
        String duration = "test_suite_duration";
        String rolledupTestSuccessCount = "rolledup_test_success_count";
        String rolledupTestFailureCount = "rolledup_test_failure_count";
        String rolledupTestErrorCount = "rolledup_test_error_count";
        String rolledupTestSkipCount = "rolledup_test_skip_count";
    }
}
