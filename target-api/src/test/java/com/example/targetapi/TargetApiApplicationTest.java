package com.example.targetapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TargetApiApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentListsAllStubResources() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/orders'].get").exists())
                .andExpect(jsonPath("$.paths['/orders'].post").exists())
                .andExpect(jsonPath("$.paths['/users/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/carts/{id}'].patch").exists());
    }

    @Test
    void getIsIdempotentAndPatchIsNot() throws Exception {
        mockMvc.perform(get("/orders/7")).andExpect(jsonPath("$.revision").value(0));
        mockMvc.perform(patch("/orders/7")).andExpect(jsonPath("$.revision").value(1));
        mockMvc.perform(patch("/orders/7")).andExpect(jsonPath("$.revision").value(2));
        mockMvc.perform(get("/orders/7")).andExpect(jsonPath("$.revision").value(2));
    }

    @Test
    void postCreatesNewIdEveryTime() throws Exception {
        mockMvc.perform(post("/carts")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101));
        mockMvc.perform(post("/carts")).andExpect(jsonPath("$.id").value(102));
    }
}
