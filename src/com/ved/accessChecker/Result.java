package com.ved.accessChecker;

public class Result {
    public final String target;
    public final boolean success;
    public final String message;

    public Result(String target, boolean success, String message) {
        this.target = target;
        this.success = success;
        this.message = message;
    }
}
