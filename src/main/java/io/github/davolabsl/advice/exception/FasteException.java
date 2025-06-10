package io.github.davolabsl.advice.exception;

/**
 * The type Code exception.
 *
 * @author - Shehara
 * @date - 1/25/2022
 */
public class FasteException extends RuntimeException {
    /**
     * Instantiates a new Code exception.
     */
    public FasteException() {
        super();
    }

    /**
     * Instantiates a new Code exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public FasteException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Code exception.
     *
     * @param message the message
     */
    public FasteException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Code exception.
     *
     * @param cause the cause
     */
    public FasteException(Throwable cause) {
        super(cause);
    }
}
