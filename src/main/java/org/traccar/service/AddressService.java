package org.traccar.service;

import jakarta.inject.Inject;
import org.traccar.model.Address;
import org.traccar.repository.AddressRepository;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;


public class AddressService {

    @Inject
    private AddressRepository addressRepository;

    public Address createAddress(Address address) {
        validateAddress(address);
        address.setCreatedAt(new Date());
        address.setUpdatedAt(new Date());
        return addressRepository.save(address);
    }

    public Address getAddressById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Address not found with ID: " + id));
    }

    public Address updateAddress(Long id, Address updatedAddress) {
        Address existingAddress = getAddressById(id);
        if (updatedAddress.getName() != null) {
            existingAddress.setName(updatedAddress.getName());
        }
        if (updatedAddress.getLatitude() != null) {
            existingAddress.setLatitude(updatedAddress.getLatitude());
        }
        if (updatedAddress.getLongitude() != null) {
            existingAddress.setLongitude(updatedAddress.getLongitude());
        }
        if (updatedAddress.getCity() != null) {
            existingAddress.setCity(updatedAddress.getCity());
        }
        if (updatedAddress.getState() != null) {
            existingAddress.setState(updatedAddress.getState());
        }
        if (updatedAddress.getCountry() != null) {
            existingAddress.setCountry(updatedAddress.getCountry());
        }
        if (updatedAddress.getPostalCode() != null) {
            existingAddress.setPostalCode(updatedAddress.getPostalCode());
        }
        existingAddress.setUpdatedAt(new Date());
        return addressRepository.save(existingAddress);
    }

    public void deleteAddress(Long id) {
        Address existingAddress = getAddressById(id);
        addressRepository.delete(existingAddress);
    }

    public List<Address> getAllAddresses(int page, int size) {
        int offset = page * size;
        return addressRepository.findAll(offset, size);
    }

    public long getTotalAddressCount() {
        return addressRepository.count();
    }


    private void validateAddress(Address address) {
        if (address.getName() == null || address.getName().isEmpty()) {
            throw new IllegalArgumentException("Address name cannot be null or empty.");
        }
        if (address.getLatitude() == null || address.getLongitude() == null) {
            throw new IllegalArgumentException("Address latitude and longitude cannot be null.");
        }
    }

    public List<Address> getAddressesByUserId(Long userId, int offset, int size) {
        return addressRepository.findByUserId(userId, offset, size);
    }

    public long getTotalAddressCountForUser(Long userId) {
        return addressRepository.countByUserId(userId);
    }

}



