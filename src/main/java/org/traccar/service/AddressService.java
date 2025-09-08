package org.traccar.service;

import jakarta.inject.Inject;
import org.traccar.api.security.PermissionsService;
import org.traccar.dto.AddressDistanceDTO;
import org.traccar.model.Address;
import org.traccar.repository.AddressRepository;
import org.traccar.storage.StorageException;

import java.util.logging.Logger;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;


public class AddressService {

    @Inject
    private AddressRepository addressRepository;

    @Inject
    private PermissionsService permissionsService;

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

    public Address updateAddress(long addressId, Address updatedAddress) {
        Address existingAddress = getAddressById(addressId);

        if (existingAddress == null) {
            throw new IllegalArgumentException("Address not found for ID: " + addressId);
        }

        existingAddress.setName(updatedAddress.getName());
        existingAddress.setLatitude(updatedAddress.getLatitude());
        existingAddress.setLongitude(updatedAddress.getLongitude());
        existingAddress.setCity(updatedAddress.getCity());
        existingAddress.setState(updatedAddress.getState());
        existingAddress.setCountry(updatedAddress.getCountry());
        existingAddress.setPostalCode(updatedAddress.getPostalCode());

        addressRepository.update(existingAddress);
        return existingAddress;
    }


    public void deleteAddress(Long id) {
        Address existingAddress = getAddressById(id);
        addressRepository.delete(existingAddress);
    }

    public List<Address> getAllAddresses(int page, int size) {
        int offset = page * size;
        return addressRepository.findAll(offset, size);
    }

    public List<Address> getAddressesByLocation(Double lat,Double lng,int page, int size) {
        int offset = page * size;
        return addressRepository.findAllWithLatLng(lat,lng,offset, size);
    }

    public long getTotalAddressCount(Double lat,Double lng) {
        return addressRepository.count(lat,lng);
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

    public boolean canDeleteAddress(long userId, long addressId) throws StorageException {
        Address address = addressRepository.getAddressById(addressId);

        if (address == null) {
            throw new IllegalArgumentException("Address not found for ID: " + addressId);
        }
        return permissionsService.getUser(userId).getAdministrator() || address.getUserId() == userId;
    }

    public boolean canUpdateAddress(long userId, long addressId) throws StorageException {
        // Fetch the address to get the associated user ID
        Address address = addressRepository.getAddressById(addressId);

        if (address == null) {
            throw new IllegalArgumentException("Address not found for ID: " + addressId);
        }
        return permissionsService.getUser(userId).getAdministrator() || address.getUserId() == userId;
    }

    public List<AddressDistanceDTO> getAddressesWithinDistanceForUser(
            long userId, double latitude, double longitude, double distanceKm, int limit) {

        Logger logger = Logger.getLogger(AddressService.class.getName());

        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values");
        }
        logger.info(String.format("Finding addresses for userId=%d within distance=%.2f km from (%.6f, %.6f)",
                userId, distanceKm, latitude, longitude));

        List<AddressDistanceDTO> addresses = addressRepository.findWithinDistanceForUser(
                userId, latitude, longitude, distanceKm, limit);

        logger.info("Found " + addresses.size() + " addresses for userId=" + userId);
        return addresses;
    }

    public List<AddressDistanceDTO> getAddressesWithinDistance(
            double latitude, double longitude, double distanceKm, int limit) {

        Logger logger = Logger.getLogger(AddressService.class.getName());

        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude values");
        }
        logger.info(String.format("Finding ALL addresses within distance=%.2f km from (%.6f, %.6f)",
                distanceKm, latitude, longitude));

        List<AddressDistanceDTO> addresses = addressRepository.findWithinDistance(
                latitude, longitude, distanceKm, limit);

        logger.info("Found " + addresses.size() + " addresses (no user filter)");
        return addresses;
    }


}



