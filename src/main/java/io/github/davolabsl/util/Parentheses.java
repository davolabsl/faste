package io.github.davolabsl.util;

import lombok.Getter;

@Getter
public enum Parentheses {
    OPEN ("("),
    CLOSE (")"),
    DEFAULT ("");

    private final String value;

    Parentheses(String value) {
        this.value = value;
    }

}
