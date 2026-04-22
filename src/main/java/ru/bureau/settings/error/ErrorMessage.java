package ru.bureau.settings.error;

public class ErrorMessage {


    private String errorMessage;

    public ErrorMessage() {
    }

    public ErrorMessage(String message) {
        this.errorMessage = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}