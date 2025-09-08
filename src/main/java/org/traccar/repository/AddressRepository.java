package org.traccar.repository;

import jakarta.inject.Inject;
import org.traccar.dto.AddressDistanceDTO;
import org.traccar.model.Address;

import java.util.logging.Logger;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.Statement;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Optional;

public class AddressRepository {

    @Inject
    private DataSource dataSource;

    public Address save(Address address) {
        String sql = "INSERT INTO addresses (user_id, name, latitude, longitude, city, state, country,"
                + " postal_code, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, address.getUserId()); // <--- new
            statement.setString(2, address.getName());
            statement.setDouble(3, address.getLatitude());
            statement.setDouble(4, address.getLongitude());
            statement.setString(5, address.getCity());
            statement.setString(6, address.getState());
            statement.setString(7, address.getCountry());
            statement.setString(8, address.getPostalCode());
            statement.setTimestamp(9, new Timestamp(address.getCreatedAt().getTime()));
            statement.setTimestamp(10, new Timestamp(address.getUpdatedAt().getTime()));

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    address.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save address", e);
        }
        return address;
    }


    public Optional<Address> findById(Long id) {
        String sql = "SELECT * FROM addresses WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRowToAddress(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find address by ID", e);
        }
        return Optional.empty();
    }

    public List<Address> findAll(int offset, int limit) {
        List<Address> addresses = new ArrayList<>();
        String sql = "SELECT * FROM addresses LIMIT ? OFFSET ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    addresses.add(mapRowToAddress(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve addresses", e);
        }
        return addresses;
    }

    public List<Address> findAllWithLatLng(Double lat, Double lng, int offset, int limit) {
        List<Address> addresses = new ArrayList<>();
        String sql = "SELECT * FROM addresses";

        // If lat and lng are provided, add WHERE clause
        if (lat != null && lng != null) {
            sql += " WHERE latitude BETWEEN (? - (100 / 111111)) AND (? + (100 / 111111))"
                    + " AND longitude BETWEEN (? - (100 / (111111 * COS(RADIANS(?))))) AND (? + (100 / (111111 * COS(RADIANS(?)))))";
        }

        sql += " LIMIT ? OFFSET ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int paramIndex = 1;

            // Set lat and lng if provided
            if (lat != null && lng != null) {
                statement.setDouble(paramIndex++, lat);
                statement.setDouble(paramIndex++, lat); // Same lat for upper bound
                statement.setDouble(paramIndex++, lng);
                statement.setDouble(paramIndex++, lng); // Same lng for lower bound
                statement.setDouble(paramIndex++, lng); // Cosine adjustment
                statement.setDouble(paramIndex++, lng); // Cosine adjustment
            }

            // Set limit and offset
            statement.setInt(paramIndex++, limit);
            statement.setInt(paramIndex, offset);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    addresses.add(mapRowToAddress(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve addresses", e);
        }
        return addresses;
    }
    public long count(Double lat,Double lng) {
        String sql = "SELECT COUNT(*) FROM addresses";
        // If lat and lng are provided, add WHERE clause
        if (lat != null && lng != null) {
                sql += " WHERE latitude BETWEEN (? - (100 / 111111)) AND (? + (100 / 111111))"
                        + " AND longitude BETWEEN (? - (100 / (111111 * COS(RADIANS(?))))) AND (? + (100 / (111111 * COS(RADIANS(?)))))";
            }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
         ) {
            int paramIndex = 1;
            // Set lat and lng if provided
            if (lat != null && lng != null) {
                statement.setDouble(paramIndex++, lat);
                statement.setDouble(paramIndex++, lat); // Same lat for upper bound
                statement.setDouble(paramIndex++, lng);
                statement.setDouble(paramIndex++, lng); // Same lng for lower bound
                statement.setDouble(paramIndex++, lng); // Cosine adjustment
                statement.setDouble(paramIndex++, lng); // Cosine adjustment
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count addresses", e);
        }
        return 0;
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM addresses";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count addresses", e);
        }
        return 0;
    }

    public void update(Address address) {
        String sql = "UPDATE addresses SET name = ?, latitude = ?, longitude = ?, city = ?, state = ?, country = ?,"
                + " postal_code = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, address.getName());
            statement.setDouble(2, address.getLatitude());
            statement.setDouble(3, address.getLongitude());
            statement.setString(4, address.getCity());
            statement.setString(5, address.getState());
            statement.setString(6, address.getCountry());
            statement.setString(7, address.getPostalCode());
            statement.setLong(8, address.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update address", e);
        }
    }

    public void delete(Address address) {
        String sql = "DELETE FROM addresses WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, address.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete address", e);
        }
    }

    private Address mapRowToAddress(ResultSet resultSet) throws SQLException {
        Address address = new Address();
        address.setId(resultSet.getLong("id"));
        address.setUserId(resultSet.getLong("user_id")); // <--- new
        address.setName(resultSet.getString("name"));
        address.setLatitude(resultSet.getDouble("latitude"));
        address.setLongitude(resultSet.getDouble("longitude"));
        address.setCity(resultSet.getString("city"));
        address.setState(resultSet.getString("state"));
        address.setCountry(resultSet.getString("country"));
        address.setPostalCode(resultSet.getString("postal_code"));
        address.setCreatedAt(resultSet.getTimestamp("created_at"));
        address.setUpdatedAt(resultSet.getTimestamp("updated_at"));
        return address;
    }

    public List<Address> findByUserId(Long userId, int offset, int limit) {
        List<Address> addresses = new ArrayList<>();
        String sql = "SELECT * FROM addresses WHERE user_id = ? LIMIT ? OFFSET ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    addresses.add(mapRowToAddress(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve addresses by user", e);
        }
        return addresses;
    }

    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM addresses WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count addresses for user", e);
        }
        return 0;
    }

    public Address getAddressById(long addressId) {
        String sql = "SELECT * FROM addresses WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, addressId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Address address = new Address();
                    address.setId(resultSet.getLong("id"));
                    address.setUserId(resultSet.getLong("user_id"));
                    address.setName(resultSet.getString("name"));
                    address.setLatitude(resultSet.getDouble("latitude"));
                    address.setLongitude(resultSet.getDouble("longitude"));
                    address.setCity(resultSet.getString("city"));
                    address.setState(resultSet.getString("state"));
                    address.setCountry(resultSet.getString("country"));
                    address.setPostalCode(resultSet.getString("postal_code"));
                    return address;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch address", e);
        }
        return null;
    }

    public List<AddressDistanceDTO> findWithinDistanceForUser(
            long userId, double latitude, double longitude, double distanceKm, int limit) {

        Logger logger = Logger.getLogger(AddressRepository.class.getName());
        List<AddressDistanceDTO> dtos = new ArrayList<>();
        String sql = """
                SELECT 
                    a.id,
                    a.user_id,
                    a.name,
                    a.latitude,
                    a.longitude,
                    a.city,
                    a.state,
                    a.country,
                    a.postal_code,
                    (6371 * ACOS(
                        COS(RADIANS(?)) 
                      * COS(RADIANS(a.latitude)) 
                      * COS(RADIANS(a.longitude) - RADIANS(?)) 
                      + SIN(RADIANS(?)) 
                      * SIN(RADIANS(a.latitude))
                    )) AS distance
                FROM addresses a
                WHERE a.user_id = ?  
                HAVING distance <= ? 
                ORDER BY distance ASC, name DESC
                LIMIT ?;  
                """;

        logger.info("Executing query to find addresses within distance...");
        logger.info("Query: " + sql);
        logger.info(String.format("Parameters: userId=%d, latitude=%.6f, longitude=%.6f, distanceKm=%.2f, limit=%d",
                userId, latitude, longitude, distanceKm, limit));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, latitude);
            statement.setDouble(2, longitude);
            statement.setDouble(3, latitude);
            statement.setLong(4, userId);
            statement.setDouble(5, distanceKm);
            statement.setInt(6, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    AddressDistanceDTO dto = new AddressDistanceDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setUserId(rs.getLong("user_id"));
                    dto.setName(rs.getString("name"));
                    dto.setLatitude(rs.getDouble("latitude"));
                    dto.setLongitude(rs.getDouble("longitude"));
                    dto.setCity(rs.getString("city"));
                    dto.setState(rs.getString("state"));
                    dto.setCountry(rs.getString("country"));
                    dto.setPostalCode(rs.getString("postal_code"));
                    dto.setDistanceFromQueryPoint(rs.getDouble("distance"));

                    dtos.add(dto);
                }

                logger.info("Fetched " + dtos.size() + " addresses from the database.");
                for (AddressDistanceDTO dto : dtos) {
                    logger.info("Fetched Address: " + dto.getName() +
                            " [id=" + dto.getId() + ", distance=" + dto.getDistanceFromQueryPoint() + "]");
                }
            }
        } catch (SQLException e) {
            logger.severe("SQL exception occurred: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve addresses within distance", e);
        }
        return dtos;
    }

    public List<AddressDistanceDTO> findWithinDistance(
            double latitude, double longitude, double distanceKm, int limit) {

        Logger logger = Logger.getLogger(AddressRepository.class.getName());
        List<AddressDistanceDTO> dtos = new ArrayList<>();
        String sql = """
                SELECT 
                    a.id,
                    a.user_id,
                    a.name,
                    a.latitude,
                    a.longitude,
                    a.city,
                    a.state,
                    a.country,
                    a.postal_code,
                    (6371 * ACOS(
                        COS(RADIANS(?)) 
                      * COS(RADIANS(a.latitude)) 
                      * COS(RADIANS(a.longitude) - RADIANS(?)) 
                      + SIN(RADIANS(?)) 
                      * SIN(RADIANS(a.latitude))
                    )) AS distance
                FROM addresses a
                HAVING distance <= ? 
                ORDER BY distance ASC, name DESC
                LIMIT ?;  
                """;

        logger.info("Executing query to find ALL addresses within distance...");
        logger.info("Query: " + sql);
        logger.info(String.format("Parameters: latitude=%.6f, longitude=%.6f, distanceKm=%.2f, limit=%d",
                latitude, longitude, distanceKm, limit));

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, latitude);
            statement.setDouble(2, longitude);
            statement.setDouble(3, latitude);
            statement.setDouble(4, distanceKm);
            statement.setInt(5, limit);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    AddressDistanceDTO dto = new AddressDistanceDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setUserId(rs.getLong("user_id"));
                    dto.setName(rs.getString("name"));
                    dto.setLatitude(rs.getDouble("latitude"));
                    dto.setLongitude(rs.getDouble("longitude"));
                    dto.setCity(rs.getString("city"));
                    dto.setState(rs.getString("state"));
                    dto.setCountry(rs.getString("country"));
                    dto.setPostalCode(rs.getString("postal_code"));
                    dto.setDistanceFromQueryPoint(rs.getDouble("distance"));

                    dtos.add(dto);
                }

                logger.info("Fetched " + dtos.size() + " addresses from the database (no user filter).");
            }
        } catch (SQLException e) {
            logger.severe("SQL exception occurred: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve addresses within distance (no user filter)", e);
        }
        return dtos;
    }

}



