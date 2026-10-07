package com.example.app.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Controllers nested in test classes are excluded from scanning, so register the sample one explicitly.
@WebMvcTest(ApiExceptionHandlerTest.SampleController.class)
@AutoConfigureMockMvc(addFilters = false) // MVC behaviour only; security is tested in auth/
@Import(ApiExceptionHandlerTest.SampleController.class)
class ApiExceptionHandlerTest {

    @RestController
    static class SampleController {

        record SampleRequest(@NotBlank String name) {}

        @PostMapping("/sample")
        String create(@Valid @RequestBody SampleRequest request) {
            return request.name();
        }

        @GetMapping("/sample/missing")
        String missing() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sample 1 not found");
        }

        @GetMapping("/sample/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationErrorListsInvalidFields() throws Exception {
        mockMvc.perform(post("/sample").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void notFoundUsesTheGivenReason() throws Exception {
        mockMvc.perform(get("/sample/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Sample 1 not found"));
    }

    @Test
    void unexpectedErrorReturnsGeneric500() throws Exception {
        mockMvc.perform(get("/sample/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
