package io.github.davolabsl.web.rest.dto;

import lombok.Data;

import java.util.List;

@Data
public class FasteQuery {
    private String[] select = null;
    private String from;
    private List<FasteCondition> logic;
    private Integer page;
    private Integer size;
    private String[] orderBy;
    private String resultType;
}
