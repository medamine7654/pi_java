package tn.piapp.dao;

import tn.piapp.db.DbConnection;
import tn.piapp.model.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceDao {

    private static final String FIND_ALL =
            "SELECT id, name, description, base_price, duration_minutes, location, is_active, " +
            "created_at, updated_at, host_id, image_name, category_id FROM service";

    private static final String INSERT =
            "INSERT INTO service (name, description, base_price, duration_minutes, location, " +
            "is_active, created_at, updated_at, host_id, image_name, category_id) " +
            "VALUES (?,?,?,?,?,0,?,?,?,?,?)";

    private static final String UPDATE =
            "UPDATE service SET name=?, description=?, base_price=?, duration_minutes=?, " +
            "location=?, is_active=?, updated_at=?, image_name=?, category_id=? WHERE id=?";

    private static final String DELETE =
            "DELETE FROM service WHERE id=?";

    private static final String SET_ACTIVE =
            "UPDATE service SET is_active=? WHERE id=?";

    private static final String PRICES_BY_CATEGORY =
            "SELECT base_price FROM service WHERE category_id = ? AND is_active = 1";

    private static final String RESOLVE_HOST =
            "SELECT MIN(id) FROM user";

    public List<Service> findAll() throws SQLException {
        List<Service> list = new ArrayList<>();
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Service s = new Service();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setDescription(rs.getString("description"));
                s.setBasePrice(rs.getBigDecimal("base_price"));
                s.setDurationMinutes(rs.getInt("duration_minutes"));
                s.setLocation(rs.getString("location"));
                s.setActive(rs.getInt("is_active") != 0);
                s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                s.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                s.setHostId(rs.getInt("host_id"));
                s.setImageName(rs.getString("image_name"));
                // category_id may be SQL NULL → getObject returns null safely
                s.setCategoryId(rs.getObject("category_id", Integer.class));
                list.add(s);
            }
        }
        return list;
    }

    public void insert(Service s) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s.setHostId(resolveDefaultHostId());
        // is_active is hardcoded to 0 in the SQL (new listings always start inactive)

        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getDescription());
            ps.setBigDecimal(3, s.getBasePrice());
            ps.setInt(4, s.getDurationMinutes());
            ps.setString(5, s.getLocation());
            // param 6 is is_active = 0 (hardcoded in SQL)
            ps.setTimestamp(6, Timestamp.valueOf(s.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.valueOf(s.getUpdatedAt()));
            ps.setInt(8, s.getHostId());
            ps.setString(9, s.getImageName());
            ps.setObject(10, s.getCategoryId()); // null-safe
            ps.executeUpdate();
        }
    }

    public void update(Service s) throws SQLException {
        s.setUpdatedAt(LocalDateTime.now());

        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getDescription());
            ps.setBigDecimal(3, s.getBasePrice());
            ps.setInt(4, s.getDurationMinutes());
            ps.setString(5, s.getLocation());
            ps.setInt(6, s.isActive() ? 1 : 0);
            ps.setTimestamp(7, Timestamp.valueOf(s.getUpdatedAt()));
            ps.setString(8, s.getImageName());
            ps.setObject(9, s.getCategoryId()); // null-safe
            ps.setInt(10, s.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Toggles the is_active flag for a single service row. */
    public void setActive(int id, boolean active) throws SQLException {
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Returns all base_price values for active services in the given category. */
    public List<BigDecimal> getPricesByCategory(int categoryId) throws SQLException {
        List<BigDecimal> prices = new ArrayList<>();
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(PRICES_BY_CATEGORY)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prices.add(rs.getBigDecimal("base_price"));
                }
            }
        }
        return prices;
    }

    public int resolveDefaultHostId() throws SQLException {
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(RESOLVE_HOST);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getObject(1) == null) {
                throw new IllegalStateException(
                        "No users exist in the database. Please create a host user first.");
            }
            return rs.getInt(1);
        }
    }
}
