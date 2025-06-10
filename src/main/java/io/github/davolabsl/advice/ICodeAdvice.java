package io.github.davolabsl.advice;

import io.github.davolabsl.advice.exception.ExceptionResponse;
import io.github.davolabsl.advice.exception.FasteException;
import io.github.davolabsl.advice.exception.FasteExceptionNoData;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.persistence.NoResultException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author - Shehara
 * @date - 1/25/2022
 */

@ControllerAdvice
public class ICodeAdvice {

    private static final Map<String, Object> response = new HashMap<>();

    @ExceptionHandler(FasteException.class)
    public ResponseEntity<?> handleICodeException(FasteException exception) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FasteExceptionNoData.class)
    public ResponseEntity<?> handleFasteExceptionNoData(FasteExceptionNoData exception) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.OK);
    }

    @ExceptionHandler(NoResultException.class)
    public ResponseEntity<?> handleNoResultException(NoResultException exception) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.OK);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<?> handleEmptyResultDataAccessException(EmptyResultDataAccessException exception) {
        String message = exception.getMessage();
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        return new ResponseEntity<>(exceptionResponse, HttpStatus.OK);
    }
}
