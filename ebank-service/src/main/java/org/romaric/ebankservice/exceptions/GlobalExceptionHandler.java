package org.romaric.ebankservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankAccountNotFoundException.class)
    public ProblemDetail handleBankAccountNotFound(BankAccountNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Bank account not found");
        return problem;
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFound(CustomerNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Customer not found");
        return problem;
    }

    // customer-service arrete ou injoignable
    @ExceptionHandler(CustomerServiceUnavailableException.class)
    public ProblemDetail handleCustomerServiceUnavailable(CustomerServiceUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Customer service unavailable");
        return problem;
    }

    // @Valid : un champ du compte est invalide
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid bank account");
        problem.setTitle("Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }

    // corps de requete absent ou JSON mal forme
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request body is missing or is not valid JSON");
        problem.setTitle("Malformed request");
        return problem;
    }
}
