package com.example.application;
import com.example.ports.out.NotificationGateway;
public class BackgroundProcessor {
    private final NotificationGateway gateway;
    public BackgroundProcessor(NotificationGateway gateway) {
        this.gateway = gateway;
    }
    public void process() { gateway.send("done"); }
}
