package com.example.smartfoodordering.service;

import com.example.smartfoodordering.dto.request.AddressRequest;
import com.example.smartfoodordering.dto.response.AddressResponse;
import com.example.smartfoodordering.entity.DeliveryAddress;
import com.example.smartfoodordering.exception.ResourceNotFoundException;
import com.example.smartfoodordering.repository.DeliveryAddressRepository;
import com.example.smartfoodordering.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Address book. Every query is scoped by the authenticated user's id. */
@Service
@Transactional
public class AddressService {
    private final DeliveryAddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(DeliveryAddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(a -> new AddressResponse(a.getId(), a.getLabel(), a.asSingleLine(), a.getContactPhone(),
                        a.isDefaultAddress()))
                .toList();
    }

    public void add(Long userId, AddressRequest request) {
        List<DeliveryAddress> existing = addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
        boolean makeDefault = request.isDefaultAddress() || existing.isEmpty();
        if (makeDefault) {
            existing.forEach(a -> a.setDefaultAddress(false));
            addressRepository.saveAll(existing);
        }
        DeliveryAddress address = new DeliveryAddress();
        address.setUser(userRepository.getReferenceById(userId));
        address.setLabel(request.getLabel().trim());
        address.setLine1(request.getLine1().trim());
        address.setLine2(request.getLine2() == null || request.getLine2().isBlank() ? null : request.getLine2().trim());
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPostalCode(request.getPostalCode().trim());
        address.setContactPhone(request.getContactPhone().trim());
        address.setDefaultAddress(makeDefault);
        addressRepository.save(address);
    }

    public void delete(Long userId, Long addressId) {
        DeliveryAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }
}
