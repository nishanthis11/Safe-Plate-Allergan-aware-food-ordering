package com.example.smartfoodordering.dto.response;

public record AddressResponse(Long id, String label, String fullAddress, String contactPhone, boolean defaultAddress) {
}
