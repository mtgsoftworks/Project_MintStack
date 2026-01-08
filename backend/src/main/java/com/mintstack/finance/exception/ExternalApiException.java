package com.mintstack.finance.exception;

public class ExternalApiException extends RuntimeException {
    
    private final String apiName;
    
    public ExternalApiException(String apiName, String message) {
        super(String.format("%s API hatası: %s", apiName, message));
        this.apiName = apiName;
    }
    
    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(String.format("%s API hatası: %s", apiName, message), cause);
        this.apiName = apiName;
    }
    
    public String getApiName() {
        return apiName;
    }
}
