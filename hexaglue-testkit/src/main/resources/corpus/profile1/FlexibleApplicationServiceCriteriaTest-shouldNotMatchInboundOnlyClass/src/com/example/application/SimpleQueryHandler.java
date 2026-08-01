package com.example.application;
import com.example.ports.in.QueryHandler;
public class SimpleQueryHandler implements QueryHandler {
    public String query() { return "result"; }
}
