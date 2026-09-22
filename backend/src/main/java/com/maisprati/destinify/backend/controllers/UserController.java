package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdatePassword;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdateUser;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserCreate;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserResponse;
import com.maisprati.destinify.backend.exceptions.ForbiddenException;
import com.maisprati.destinify.backend.servicies.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateUserProfile(
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody UpdateUser updatedUser) {
        return ResponseEntity.ok(userService.updateProfile(authenticatedUser.getId(), updatedUser));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody UpdatePassword updatePassword){
        userService.updatePassword(authenticatedUser.getId(), updatePassword);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUserById (
            @AuthenticationPrincipal User authenticatedUser){

        userService.deleteUserById(authenticatedUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all-users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> findById(@AuthenticationPrincipal User loggedId) {
        return ResponseEntity.ok(userService.findById(loggedId.getId()));
    }

    @GetMapping("/me/name")
    public ResponseEntity<String> findUser(
            @AuthenticationPrincipal User loggedUser){
        return ResponseEntity.ok(userService.findUserById(loggedUser.getId()));
    }


}
