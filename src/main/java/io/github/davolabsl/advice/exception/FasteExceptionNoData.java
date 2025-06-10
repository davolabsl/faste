package io.github.davolabsl.advice.exception;

/**
 * The type Code exception.
 *
 * @author - Shehara
 * @date - 1/25/2022
 */
public class FasteExceptionNoData extends RuntimeException {
    /**
     * Instantiates a new Code exception.
     */
    public FasteExceptionNoData() {
        super();
    }

    /**
     * Instantiates a new Code exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public FasteExceptionNoData(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Code exception.
     *
     * @param message the message
     */
    public FasteExceptionNoData(String message) {
        super(message);
    }

    /**
     * Instantiates a new Code exception.
     *
     * @param cause the cause
     */
    public FasteExceptionNoData(Throwable cause) {
        super(cause);
    }
}
