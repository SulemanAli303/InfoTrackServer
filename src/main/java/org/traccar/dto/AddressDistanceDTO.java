package org.traccar.dto;

public class AddressDistanceDTO {

    private Long id;
    private Long userId;
    private String name;
    private Double latitude;
    private Double longitude;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private Double distanceFromQueryPoint;

    public AddressDistanceDTO() {
        // Default constructor
    }

    public AddressDistanceDTO(Long id, Long userId, String name, Double latitude, Double longitude,
                              String city, String state, String country, String postalCode,
                              Double distanceFromQueryPoint) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.state = state;
        this.country = country;
        this.postalCode = postalCode;
        this.distanceFromQueryPoint = distanceFromQueryPoint;
    }

    // Getters and setters (omitted for brevity)
    // ...
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Double getDistanceFromQueryPoint() {
        return distanceFromQueryPoint;
    }
    public void setDistanceFromQueryPoint(Double distanceFromQueryPoint) {
        this.distanceFromQueryPoint = distanceFromQueryPoint;
    }
}
