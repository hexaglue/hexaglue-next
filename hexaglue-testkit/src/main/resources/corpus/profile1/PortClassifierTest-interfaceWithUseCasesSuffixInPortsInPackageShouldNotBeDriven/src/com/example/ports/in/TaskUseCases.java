package com.example.ports.in;
import com.example.domain.Task;
public interface TaskUseCases {
    void createTask(String name);
    Task getTask(String id);
    void completeTask(String id);
}
