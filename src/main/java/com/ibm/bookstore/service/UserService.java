package com.ibm.bookstore.service;

import com.ibm.bookstore.dto.AddressDto;
import com.ibm.bookstore.dto.CreateAddressRequest;
import com.ibm.bookstore.dto.UserProfileResponse;
import com.ibm.bookstore.entity.User;
import com.ibm.bookstore.entity.UserAddress;
import com.ibm.bookstore.exception.ResourceNotFoundException;
import com.ibm.bookstore.repository.UserAddressRepository;
import com.ibm.bookstore.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserAddressRepository userAddressRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getRewardPoints()
        );
    }

    @Transactional(readOnly = true)
    public List<AddressDto> getUserAddresses(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        return userAddressRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toAddressDto)
                .toList();
    }

    @Transactional
    public AddressDto addUserAddress(String username, CreateAddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        if (isDefault) {
            userAddressRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                    .filter(UserAddress::getIsDefault)
                    .forEach(address -> {
                        address.setIsDefault(false);
                        userAddressRepository.save(address);
                    });
        }

        UserAddress address = UserAddress.builder()
                .id(UUID.randomUUID())
                .user(user)
                .recipientName(request.recipientName())
                .phone(request.phone())
                .street(request.street())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(isDefault)
                .build();

        UserAddress saved = userAddressRepository.save(address);
        return toAddressDto(saved);
    }

    private AddressDto toAddressDto(UserAddress address) {
        return new AddressDto(
                address.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getIsDefault()
        );
    }
}
