package com.example.app.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task(request.title(), request.description(), request.status(), request.dueDate());
        return TaskResponse.from(repository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(TaskStatus status) {
        List<Task> tasks = status == null
                ? repository.findAllByOrderByIdAsc()
                : repository.findByStatusOrderByIdAsc(status);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(find(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = find(id);
        LocalDate dueDate = request.dueDate();
        if (dueDate != null && dueDate.isBefore(LocalDate.now()) && !dueDate.equals(task.getDueDate())) {
            // Same shape as a Bean Validation failure, so clients read errors.dueDate on create and update alike.
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
            problem.setProperty("errors", Map.of("dueDate", "dueDate cannot be moved into the past"));
            throw new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
        }
        task.update(request.title(), request.description(), request.status(), dueDate);
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Task find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task " + id + " not found"));
    }
}
