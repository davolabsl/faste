package io.github.davolabsl.support;

import org.springframework.http.HttpMethod;

/**
 * @author - Shehara
 * @date - 1/29/2022
 */

public class FasteResourceDetails<T> {
    private HttpMethod httpMethod;
    private String accessUrl;
    private T requestDetails;


    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public T getRequestDetails() {
        return requestDetails;
    }

    public void setRequestDetails(T requestDetails) {
        this.requestDetails = requestDetails;
    }
}
