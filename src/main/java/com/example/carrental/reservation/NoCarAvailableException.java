package com.example.carrental.reservation;

public final class NoCarAvailableException extends RuntimeException {
    public NoCarAvailableException() {
        super("No car of the requested type is available for this period");
    }
}
