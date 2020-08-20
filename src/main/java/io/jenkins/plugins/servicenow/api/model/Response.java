package io.jenkins.plugins.servicenow.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "response")
public class  Response extends JsonResponseObject {

    @JsonProperty
    @XmlElement
    private Result result;

    @JsonProperty
    @XmlElement
    private Error error;

    @JsonProperty
    @XmlElement
    private String status;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if(StringUtils.isNotBlank(status)) {
            sb.append("'status': ");
            sb.append(status);
        }
        if(result != null) {
            sb.append("\n'result':");
            sb.append(result);
        }
        if(error != null) {
            sb.append("\n'error: '");
            sb.append(error);
        }
        return sb.toString();
    }
}
