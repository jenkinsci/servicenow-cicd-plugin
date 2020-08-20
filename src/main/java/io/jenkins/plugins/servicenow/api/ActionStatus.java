package io.jenkins.plugins.servicenow.api;

public enum ActionStatus {
    PENDING("0"),
    RUNNING("1"),
    SUCCESSFUL("2"),
    FAILED("3"),
    CANCELED("4");

    private String status;

    ActionStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
