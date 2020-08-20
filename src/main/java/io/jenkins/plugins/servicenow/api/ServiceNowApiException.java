package io.jenkins.plugins.servicenow.api;

public class ServiceNowApiException extends RuntimeException {

    private String detail;

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public ServiceNowApiException(String message, String detail) {
        super(message);
        this.detail = detail;
    }
}
