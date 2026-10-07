package com.example.app.task;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank(message = "title is required") @Size(max = 100, message = "title must be at most 100 characters") String title,
        @Size(max = 1000, message = "description must be at most 1000 characters") String description,
        TaskStatus status,
        @FutureOrPresent(message = "dueDate cannot be in the past") LocalDate dueDate) {}
