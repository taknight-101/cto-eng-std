package com.platform.downstream.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class UsersController {

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable String id) {
        return new UserDto(id, "Downstream User " + id);
    }
}
