package io.jenkins.plugins.servicenow.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Links extends JsonResponseObject {
    @JsonProperty
    private LinkObject progress;

    @JsonProperty
    private LinkObject results;

    @JsonProperty
    private LinkObject source;

    @JsonProperty
    private LinkObject rollback;

    public LinkObject getProgress() {
        return progress;
    }

    public void setProgress(LinkObject progress) {
        this.progress = progress;
    }

    public LinkObject getResults() {
        return results;
    }

    public void setResults(LinkObject results) {
        this.results = results;
    }

    public LinkObject getSource() {
        return source;
    }

    public void setSource(LinkObject source) {
        this.source = source;
    }

    public LinkObject getRollback() {
        return rollback;
    }

    public void setRollback(LinkObject rollback) {
        this.rollback = rollback;
    }
}
