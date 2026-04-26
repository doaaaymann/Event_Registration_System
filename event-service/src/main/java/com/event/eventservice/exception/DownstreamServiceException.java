package com.event.eventservice.exception;

public class DownstreamServiceException extends RuntimeException {

    public DownstreamServiceException(String message) {
        super(message);
    }
}
