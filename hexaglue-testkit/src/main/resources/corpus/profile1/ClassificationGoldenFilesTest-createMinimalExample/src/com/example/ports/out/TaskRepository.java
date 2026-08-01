package com.example.ports.out;
import com.example.domain.Task;
import com.example.domain.TaskId;
import java.util.List;
import java.util.Optional;
public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(TaskId id);
    List<Task> findAll();
    void delete(Task task);
}
