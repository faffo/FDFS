package com.rmi.customExceptions;

/**
 * Simple exception thrown when an invalid command is given through the client console
 */
public class InvalidCommandException extends RuntimeException {
    /**
     * Constructor.
     * @param exception
     */
    public InvalidCommandException(String exception){
        super(exception);
    }
}
