package io.jenkins.plugins.servicenow;

import io.jenkins.plugins.servicenow.api.ActionStatus;
import io.jenkins.plugins.servicenow.api.model.LinkObject;
import io.jenkins.plugins.servicenow.api.model.Links;
import io.jenkins.plugins.servicenow.api.model.Result;
import org.apache.commons.lang.StringUtils;

public abstract class BaseAPICallResultTest {

    protected Result getPendingResult() {
        final Result result = new Result();
        result.setStatus(ActionStatus.PENDING.getStatus());
        result.setLinks(new Links());
        result.getLinks().setResults(new LinkObject());
        result.getLinks().setProgress(new LinkObject());
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
