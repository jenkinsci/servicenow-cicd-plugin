package io.jenkins.plugins.servicenow.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Error {

    @JsonProperty
    private String message;

    @JsonProperty
    private String detail;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" ['message': '").append(message).append("'");
        sb.append(", 'detail': '").append(detail).append("']");
        return sb.toString();
    }
}
