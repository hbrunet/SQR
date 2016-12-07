package com.sge.sqr;

/**
 * Created by hbrunet on 07/12/2016.
 */

public class Result {
    private String text;
    private boolean valid;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Result(String text, boolean valid) {
        this.text = text;
        this.valid = valid;
    }
}
