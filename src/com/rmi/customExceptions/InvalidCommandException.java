package com.rmi.customExceptions;

public class InvalidCommandException extends RuntimeException {
    public InvalidCommandException(String exception){
        super(exception);
    }
}
