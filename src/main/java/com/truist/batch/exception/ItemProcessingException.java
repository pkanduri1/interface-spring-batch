package com.truist.batch.exception;

/**
 * Custom exception class used for errors occurring during item processing
 * in a Spring Batch job.
 */
public class ItemProcessingException extends RuntimeException {

    /**
     * Constructs a new ItemProcessingException with the specified detail message.
     *
     * @param message the detail message
     */
    public ItemProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new ItemProcessingException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public ItemProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new ItemProcessingException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ItemProcessingException(Throwable cause) {
        super(cause);
    }
}
