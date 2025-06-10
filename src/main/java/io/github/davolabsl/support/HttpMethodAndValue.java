package io.github.davolabsl.support;

import org.springframework.http.HttpMethod;

/**
 * @author - Shehara
 * @date - 2/3/2022
 */

public class HttpMethodAndValue {
    private HttpMethod httpMethod;
    private String value;
    private AdditionalHttpInfo additionalHttpInfo;

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AdditionalHttpInfo getAdditionalHttpInfo() {
        return additionalHttpInfo;
    }

    public void setAdditionalHttpInfo(AdditionalHttpInfo additionalHttpInfo) {
        this.additionalHttpInfo = additionalHttpInfo;
    }
}
