package com.example.trainticketoffice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Tự động thêm các thuộc tính (attributes) chung cho tất cả các Controller.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * Thêm 'requestURI' vào model cho MỌI request,
     * để Thymeleaf có thể sử dụng nó.
     */
    @ModelAttribute("requestURI")
    public String addRequestUriToModel(HttpServletRequest request) {
        if (request != null) {
            return request.getRequestURI();
        }
        return "";
    }
}