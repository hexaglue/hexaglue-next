package com.example.domain;
import java.time.Instant;
public class Task {
    private final TaskId id;
    private String title;
    private String description;
    private boolean completed;
    private final Instant createdAt;

    public Task(TaskId id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = false;
        this.createdAt = Instant.now();
    }

    public TaskId getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public Instant getCreatedAt() { return createdAt; }
    public void complete() { this.completed = true; }
}
