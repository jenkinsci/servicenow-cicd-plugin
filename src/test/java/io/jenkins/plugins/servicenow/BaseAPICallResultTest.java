package io.jenkins.plugins.servicenow;

import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.apache.commons.lang.StringUtils;

public abstract class BaseAPICallResultTest {

    protected Result getPendingResult() {
        final Result result = new Result();
        result.setStatus(ActionStatus.PENDING.getStatus());
        return result;
    }

    protected Result getSuccessfulResult(int percentComplete, String statusMessage) {
        final Result result = new Result();
        result.setStatus(ActionStatus.SUCCESSFUL.getStatus());
        result.setPercentComplete(percentComplete);
        if(StringUtils.isNotBlank(statusMessage)) {
            result.setStatusMessage(statusMessage);
        }
        return result;
    }

    protected Result getFailedResult(String errorMessage) {
        final Result result = new Result();
        result.setStatus(ActionStatus.FAILED.getStatus());
        if(StringUtils.isNotBlank(errorMessage)) {
            result.setStatusMessage(errorMessage);
        }
        return result;
    }
}
