package io.github.davolabsl.web.rest.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * The type Api response dto.
 *
 * @param <T> the type parameter
 * @author Sujith T <p> <!In God We Trust>
 */
@JsonIgnoreProperties
public class ApiResponseDto<T> implements Serializable {

    private static final long serialVersionUID = 1031497392232283687L;


    private PaginationDto pagination;
    private T result;

    /**
     * Gets pagination.
     *
     * @return the pagination
     */
    public PaginationDto getPagination() {
        return pagination;
    }

    /**
     * Sets pagination.
     *
     * @param pagination the pagination
     */
    public void setPagination(PaginationDto pagination) {
        this.pagination = pagination;
    }

    /**
     * Gets result.
     *
     * @return the result
     */
    public T getResult() {
        return result;
    }

    /**
     * Sets result.
     *
     * @param result the result
     */
    public void setResult(T result) {
        this.result = result;
    }
}
