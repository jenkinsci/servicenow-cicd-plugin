package io.jenkins.plugins.servicenow.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LinkObject extends JsonResponseObject {

    @JsonProperty
    private String id;

    @JsonProperty
    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String toString() {
        return "'id': " + id + ", 'url': " + url;
    }
}
