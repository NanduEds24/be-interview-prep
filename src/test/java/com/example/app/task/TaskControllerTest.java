package com.example.app.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class TaskControllerTest {

    // A week ahead, so the date can't become "today or past" if the suite runs across midnight.
    private static final String TOMORROW = LocalDate.now().plusDays(7).toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository repository;

    private long createTask(String title, String status) throws Exception {
        String body = """
                {"title": "%s", "description": "Write tests", "status": "%s", "dueDate": "%s"}
                """.formatted(title, status, TOMORROW);
        String json = mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void createsAndGetsTask() throws Exception {
        long id = createTask("Prepare demo", "TODO");

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value(TOMORROW));
    }

    @Test
    void missingStatusDefaultsToTodo() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"No status\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void invalidInputReturnsFieldErrors() throws Exception {
        String body = """
                {"title": "", "dueDate": "2000-01-01"}
                """;
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.title").value("title is required"))
                .andExpect(jsonPath("$.errors.dueDate").value("dueDate cannot be in the past"));
    }

    @Test
    void titleLongerThan100IsRejected() throws Exception {
        String body = "{\"title\": \"" + "x".repeat(101) + "\"}";
        mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("title must be at most 100 characters"));
    }

    @Test
    void filtersByStatus() throws Exception {
        createTask("Open task", "TODO");
        createTask("Finished task", "DONE");

        mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Finished task"));
    }

    @Test
    void unknownStatusFilterReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "LATER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updatesTask() throws Exception {
        long id = createTask("Draft", "TODO");
        String body = """
                {"title": "Final", "status": "IN_PROGRESS", "dueDate": "%s"}
                """.formatted(TOMORROW);

        mockMvc.perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void overdueTaskCanStillBeUpdatedWithItsOwnDueDate() throws Exception {
        String lastWeek = LocalDate.now().minusDays(7).toString();
        long id = repository.save(new Task("Overdue", null, TaskStatus.IN_PROGRESS, LocalDate.parse(lastWeek))).getId();
        String body = """
                {"title": "Overdue", "status": "DONE", "dueDate": "%s"}
                """.formatted(lastWeek);

        mockMvc.perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void movingDueDateIntoThePastReturns400() throws Exception {
        long id = createTask("Future", "TODO");
        String body = "{\"title\": \"Future\", \"dueDate\": \"2000-01-01\"}";

        mockMvc.perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("dueDate cannot be moved into the past"));
    }

    @Test
    void invalidUpdateReturnsFieldErrors() throws Exception {
        long id = createTask("Valid", "TODO");

        mockMvc.perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("title is required"));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(delete("/api/tasks/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void deletesTask() throws Exception {
        long id = createTask("Temporary", "TODO");

        mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tasks/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void unknownTaskReturns404() throws Exception {
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task 999 not found"));
        mockMvc.perform(put("/api/tasks/999").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tasks/999")).andExpect(status().isNotFound());
    }
}
