package io.jenkins.plugins.servicenow.utils;

/**
 * Validator class to validate content of fields used by different components of the plugin.
 */
public class Validator {

    /**
     * Validates url of an instance. The url should start from `http(s)://`.
     * @param instanceUrl Url of an instance.
     * @return true - if successfully validated.
     */
    public static boolean validateInstanceUrl(String instanceUrl) {
        final String regex = "^https?://.+";
        if(instanceUrl != null && !instanceUrl.matches(regex)) {
            return false;
        }

        return true;
    }
}
