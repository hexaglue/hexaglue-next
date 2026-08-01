package com.example.ports.in;
import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.List;
import java.util.Optional;
public interface TaskUseCases {
    Task createTask(String title, String description);
    Optional<Task> getTask(TaskId id);
    List<Task> listAllTasks();
    void completeTask(TaskId id);
    void deleteTask(TaskId id);
}
