package io.jenkins.plugins.servicenow.instancescan;

import java.util.ArrayList;
import java.util.List;

public final class ScanParameters {

    private List<String> params;

    private ScanParameters() {
        params = new ArrayList<>();
    }

    public static ScanParameters params() {
        return new ScanParameters();
    }

    public ScanParameters add(String parameter) {
        params.add(parameter);
        return this;
    }

    public String[] build() {
        return params.toArray(new String[0]);
    }
}
