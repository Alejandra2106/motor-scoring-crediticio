package com.crediticio.shared.response;

import java.util.List;

public class ApiError {

    private final String errorCode;
    private final String message;
    private final List<String> details;
    private final String traceId;

    public ApiError(String errorCode, String message, List<String> details, String traceId) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.traceId = traceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }

    public String getTraceId() {
        return traceId;
    }
}
