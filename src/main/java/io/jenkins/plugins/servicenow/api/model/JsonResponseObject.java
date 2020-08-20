package io.jenkins.plugins.servicenow.api.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class JsonResponseObject implements JsonData {
    private Map<String, Object> unboundAttributes = new HashMap<>();

    @JsonAnySetter
    public void addUnboundAttribute(String key, Object value) {
        unboundAttributes.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getUnboundAttributes() {
        return unboundAttributes;
    }
}
