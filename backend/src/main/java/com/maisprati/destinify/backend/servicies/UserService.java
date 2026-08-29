package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.UpdatePassword;
import com.maisprati.destinify.backend.domain.dto.UpdateUser;
import com.maisprati.destinify.backend.domain.dto.UserCreate;
import com.maisprati.destinify.backend.domain.dto.UserResponse;
import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.utils.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow();
                //.orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public UserResponse addUser (UserCreate userCreate) {
        User user = userMapper.userCreateMapper(userCreate);

        String encryptedPassword = passwordEncoder.encode(userCreate.password());
        user.setPassword(encryptedPassword);

        user.setRole(Role.CLIENT);

        User save = userRepository.save(user);
        return userMapper.userResponseMapper(save);

    }

    @Transactional
    public void deleteUserById(Long id){
        Optional<User> user = userRepository.findById(id);
        user.ifPresent( value -> userRepository.delete(value));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(){


        return userRepository.findAll()
                .stream()
                .map(userMapper::userResponseMapper)
                .toList();
    }

    @Transactional
    public UserResponse updateProfile (Long id, UpdateUser updateUser){
       User user = userRepository
               .findById(id)
               .orElseThrow( () -> new RuntimeException("User not found"));

       user.setName(updateUser.name());
       user.setEmail(updateUser.email());

       User updatedUser = userRepository.save(user);
       return userMapper.userResponseMapper(updatedUser);
    }

    @Transactional
    public void updatePassword(Long id, UpdatePassword updatePassword) {
        User user = userRepository
                .findById(id)
                .orElseThrow( () -> new RuntimeException("User not found"));

        boolean currentPasswordIsValid = passwordEncoder.matches(updatePassword.currentPassword(), user.getPassword());

        if(!currentPasswordIsValid){
            throw new RuntimeException("Current password incorrect");
        }

        if(!updatePassword.newPassword().equals(updatePassword.confirmNewPassword())) {
            throw new RuntimeException("The new password and the confirmNewPassword are not equals");
        }

        String encryptedPassword = passwordEncoder.encode(updatePassword.newPassword());

        user.setPassword(encryptedPassword);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.userResponseMapper(user);
    }
}
