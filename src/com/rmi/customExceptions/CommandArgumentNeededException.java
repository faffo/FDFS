package com.rmi.customExceptions;

/**
 * Simple exception thrown when required arguments are missing from the command requested in the client console
 */
public class CommandArgumentNeededException extends RuntimeException{
    /**
     * Constructor.
     * @param exception
     */
    public CommandArgumentNeededException(String exception){
        super(exception);
    }
}
