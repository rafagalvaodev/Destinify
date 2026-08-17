package com.maisprati.destinify.backend.utils;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.UserCreate;
import com.maisprati.destinify.backend.domain.dto.UserResponse;
import com.maisprati.destinify.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    @Autowired
    private UserRepository userRepository;

    public User userCreateMapper(UserCreate userDto){
        if(userDto==null) return null;

        User userCreate = new User();

        userCreate.setName(userDto.name());
        userCreate.setEmail(userDto.email());
        userCreate.setPassword(userDto.password());
        userCreate.setBirthdate(userDto.birthdate());

        return userCreate;
    }

    public UserResponse userResponseMapper(User user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getBirthdate(),
                user.getRole());
    }
}
