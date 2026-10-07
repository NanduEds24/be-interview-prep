package com.example.app.task;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The past-due-date rule runs on create only (OnCreate group). On update, TaskService only rejects a
 * past date if it was changed, so an overdue task can still be renamed or marked DONE.
 */
public record TaskRequest(
        @NotBlank(message = "title is required") @Size(max = 100, message = "title must be at most 100 characters") String title,
        @Size(max = 1000, message = "description must be at most 1000 characters") String description,
        TaskStatus status,
        @FutureOrPresent(groups = TaskRequest.OnCreate.class, message = "dueDate cannot be in the past") LocalDate dueDate) {

    public interface OnCreate {}
}
