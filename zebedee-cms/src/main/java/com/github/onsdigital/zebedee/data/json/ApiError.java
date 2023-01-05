package com.github.onsdigital.zebedee.data.json;

public class ApiError {

    private int code;
    private String description;

    public ApiError(int code, String description) {
        setCode(code);
        setDescription(description);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
