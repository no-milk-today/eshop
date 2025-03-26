package ru.practicum.spring.data.shop.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex,
                                         Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "not-found";
    }

    // Обработка 400
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException() {
        return "invalid-arguments";
    }

    // Общий обработчик для всех остальных исключений
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException() {
        return "error";
    }
}