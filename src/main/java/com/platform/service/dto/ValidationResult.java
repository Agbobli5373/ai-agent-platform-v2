package com.platform.service.dto;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    public boolean valid;
    public List<String> errors;

    public ValidationResult() {
        this.valid = true;
        this.errors = new ArrayList<>();
    }

    public ValidationResult(boolean valid) {
        this.valid = valid;
        this.errors = new ArrayList<>();
    }

    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }

    public static ValidationResult success() {
        return new ValidationResult(true);
    }

    public static ValidationResult failure(String error) {
        ValidationResult result = new ValidationResult(false);
        result.errors.add(error);
        return result;
    }
}
