package io.jenkins.plugins.servicenow.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Links extends JsonResponseObject {
    @JsonProperty
    private LinkObject progress;

    @JsonProperty
    private LinkObject source;

    public LinkObject getProgress() {
        return progress;
    }

    public void setProgress(LinkObject progress) {
        this.progress = progress;
    }

    public LinkObject getSource() {
        return source;
    }

    public void setSource(LinkObject source) {
        this.source = source;
    }
}
