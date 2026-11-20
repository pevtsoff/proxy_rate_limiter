package com.example.targetapi.web;

import com.example.targetapi.stub.StubRecord;
import com.example.targetapi.stub.StubStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Stub {@code /users} resource: GET and PUT are idempotent, POST and PATCH are not. */
@RestController
@RequestMapping("/users")
@Tag(name = "users")
public class UsersController {

    private final StubStore store = new StubStore("users");

    @GetMapping
    @Operation(summary = "List users (idempotent)")
    public List<StubRecord> list() {
        return store.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one item (idempotent)")
    public StubRecord get(@PathVariable("id") long id) {
        return store.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an item (NOT idempotent: new id every call)")
    public StubRecord create(@RequestBody(required = false) Map<String, Object> body) {
        return store.create(body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an item (idempotent)")
    public StubRecord replace(
            @PathVariable("id") long id, @RequestBody(required = false) Map<String, Object> body) {
        return store.replace(id, body);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partially update an item (NOT idempotent: bumps the revision)")
    public StubRecord patch(
            @PathVariable("id") long id, @RequestBody(required = false) Map<String, Object> body) {
        return store.patch(id, body);
    }
}
