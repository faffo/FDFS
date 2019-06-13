package com.rmi.customExceptions;

public class CommandArgumentNeededException extends RuntimeException{
    public CommandArgumentNeededException(String exception){
        super(exception);
    }
}
