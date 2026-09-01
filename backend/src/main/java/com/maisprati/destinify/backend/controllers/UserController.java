package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdatePassword;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdateUser;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserCreate;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserResponse;
import com.maisprati.destinify.backend.servicies.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> addUser(@Valid @RequestBody UserCreate userCreate) {
        UserResponse userResponse = userService.addUser(userCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUserProfile(@PathVariable Long id, @Valid @RequestBody UpdateUser updatedUser) {
        return ResponseEntity.ok(userService.updateProfile(id, updatedUser));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @Valid @RequestBody UpdatePassword updatePassword){
        userService.updatePassword(id, updatePassword);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserById (@PathVariable Long id){
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all-users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

}
