package org.dspace.app.rest.exception;


import java.sql.SQLException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.dspace.authorize.AuthorizeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class DSpaceApiExceptionControllerAdvice extends ResponseEntityExceptionHandler{

    @ExceptionHandler(AuthorizeException.class)
    @ResponseBody
    protected ResponseEntity<String> handleAuthorizeException(HttpServletRequest request, Exception e) {
        return new ResponseEntity(e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SQLException.class)
    @ResponseBody
    protected ResponseEntity<String> handleSQLException(HttpServletRequest request, Exception e) {
        String errorMessage = "An internal database error occurred. Please contact the repository administrator. Timestamp: " + new Date().toString();
        return new ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
