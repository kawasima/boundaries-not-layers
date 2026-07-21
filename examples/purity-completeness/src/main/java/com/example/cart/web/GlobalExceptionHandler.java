package com.example.cart.web;

import com.example.cart.domain.CartEmptyException;
import com.example.cart.domain.CartFullException;
import com.example.cart.domain.IndividualCannotQuoteException;
import com.example.cart.domain.SaleEndedException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 入力が不正（UUID 形式でない、数量が0以下など） → 400。 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    /** 入力は正しいが業務上できない（販売終了・上限到達・空カート・個人は見積不可） → 422。 */
    @ExceptionHandler({SaleEndedException.class, CartFullException.class,
            CartEmptyException.class, IndividualCannotQuoteException.class})
    public ResponseEntity<Map<String, String>> handleUnprocessable(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", e.getMessage()));
    }
}
