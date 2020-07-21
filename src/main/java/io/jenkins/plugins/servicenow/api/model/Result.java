package io.jenkins.plugins.servicenow.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Result extends JsonResponseObject {

    @JsonProperty
    private Links links;

    @JsonProperty
    private String status;

    @JsonProperty("status_label")
    private String statusLabel;

    @JsonProperty("status_message")
    private String statusMessage;

    @JsonProperty("status_detail")
    private String statusDetail;

    @JsonProperty
    private String error;

    @JsonProperty("percent_complete")
    private Integer percentComplete;

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(Integer percentComplete) {
        this.percentComplete = percentComplete;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n\t'links':");
        if(links != null) {
            final LinkObject progress = links.getProgress();
            if(progress != null) {
                sb.append("\n\t\t'progress':");
                toString(sb, progress);
            }
            final LinkObject source = links.getSource();
            if(progress != null) {
                sb.append("\n\t\t'source':");
                toString(sb, progress);
            }
        }
        sb.append("\n\t'status': ").append(status);
        sb.append("\n\t'statusLabel': ").append(statusLabel);
        sb.append("\n\t'statusMessage': ").append(statusMessage);
        sb.append("\n\t'statusDetails': ").append(statusDetail);
        sb.append("\n\t'error': ").append(error);
        sb.append("\n\t'percentComplete': ").append(percentComplete);
        return sb.toString();
    }

    private void toString(final StringBuffer sb, final LinkObject linkObject) {
        sb.append("\n\t\t\t'id': ").append(linkObject.getId());
        sb.append("\n\t\t\t'url': ").append(linkObject.getUrl());
    }
}
