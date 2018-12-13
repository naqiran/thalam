package com.naqiran.thalam.configuration;

public enum FailureType {
    FAIL_SAFE(0), FAIL_PARTIAL(1), FAIL_GROUP(2), FAIL_ALL(3);
    
    private int priority;
    
    FailureType(int priority) {
        this.priority = priority;
    };
    
    public int getPriority() {
        return this.priority;
    }
}