package io.github.davolabsl.support;

import java.util.List;

/**
 * @author - Shehara
 * @date - 2/4/2022
 */

public class AdditionalHttpInfo {
    private boolean isApplyRequestBody = false;
    private boolean isApplyRequestParam = false;
    private boolean isApplyPathVariable = false;
    private List<String> pathVariables;
    private List<String> requestParams;

    public boolean isApplyRequestBody() {
        return isApplyRequestBody;
    }

    public void setApplyRequestBody(boolean applyRequestBody) {
        isApplyRequestBody = applyRequestBody;
    }

    public boolean isApplyRequestParam() {
        return isApplyRequestParam;
    }

    public void setApplyRequestParam(boolean applyRequestParam) {
        isApplyRequestParam = applyRequestParam;
    }

    public boolean isApplyPathVariable() {
        return isApplyPathVariable;
    }

    public void setApplyPathVariable(boolean applyPathVariable) {
        isApplyPathVariable = applyPathVariable;
    }

    public List<String> getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(List<String> pathVariables) {
        this.pathVariables = pathVariables;
    }

    public List<String> getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(List<String> requestParams) {
        this.requestParams = requestParams;
    }
}
