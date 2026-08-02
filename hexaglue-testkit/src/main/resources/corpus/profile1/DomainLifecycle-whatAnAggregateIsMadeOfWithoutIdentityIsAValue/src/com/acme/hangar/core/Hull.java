package com.acme.hangar.core;

public class Hull {

    private String code;
    private int capacity;

    public void recode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public int capacity() {
        return capacity;
    }
}
