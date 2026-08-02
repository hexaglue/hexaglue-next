package com.acme.hangar.core;

import org.springframework.stereotype.Service;

@Service
public class Dispatch {

    public void send(String reference) {
        // Nothing here reaches outside, and nothing calls it from outside.
    }
}
