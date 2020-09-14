package io.jenkins.plugins.servicenow;

public final class Constants {

    private Constants() {}

    /**
     * Interval in milliseconds between next progress check (ServiceNow API call).
     */
    public static final int PROGRESS_CHECK_INTERVAL = 5000;
}
