package com.example.cart.domain;

public class IndividualCannotQuoteException extends RuntimeException {
    public IndividualCannotQuoteException() {
        super("見積は法人のみ発行できます");
    }
}
