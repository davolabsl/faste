package io.github.davolabsl.web.rest.dto;

import io.github.davolabsl.util.Parentheses;
import lombok.Data;

@Data
public class FasteCondition {
    private String where;
    private String condition;
    private String value;
    private String logicalOperator = null;
    private Parentheses parentheses = Parentheses.DEFAULT;
}
