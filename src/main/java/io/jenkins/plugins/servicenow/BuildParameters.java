package io.jenkins.plugins.servicenow;

public interface BuildParameters {

    String instanceUrl = "instanceUrl";
    String apiVersion = "apiVersion";
    String credentials = "credentials";
    String appSysId = "appSysId";
    String branchName = "branchName";
    String appScope = "appScope";

    String resultId = "result_id";

    String progressCheckInterval = "progressCheckInterval";
}
