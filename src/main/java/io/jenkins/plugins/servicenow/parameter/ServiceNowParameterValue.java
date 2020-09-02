package io.jenkins.plugins.servicenow.parameter;

import hudson.model.StringParameterValue;

public class ServiceNowParameterValue extends StringParameterValue {

    private static final long serialVersionUID = 1327354888410861686L;

    public ServiceNowParameterValue(String name, String value) {
        super(name, value);
    }
}
