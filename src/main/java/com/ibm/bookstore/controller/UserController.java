package com.ibm.bookstore.controller;

import com.ibm.bookstore.dto.AddressDto;
import com.ibm.bookstore.dto.CreateAddressRequest;
import com.ibm.bookstore.dto.UserProfileResponse;
import com.ibm.bookstore.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(@AuthenticationPrincipal String username) {
        return userService.getProfile(username);
    }

    @GetMapping("/me/addresses")
    public List<AddressDto> getUserAddresses(@AuthenticationPrincipal String username) {
        return userService.getUserAddresses(username);
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto addUserAddress(
            @AuthenticationPrincipal String username,
            @Valid @RequestBody CreateAddressRequest request
    ) {
        return userService.addUserAddress(username, request);
    }
}
