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
            "created_at, updated_at, host_id, image_name FROM service";

    private static final String INSERT =
            "INSERT INTO service (name, description, base_price, duration_minutes, location, " +
            "is_active, created_at, updated_at, host_id, image_name) VALUES (?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE =
            "UPDATE service SET name=?, description=?, base_price=?, duration_minutes=?, " +
            "location=?, is_active=?, updated_at=?, image_name=? WHERE id=?";

    private static final String DELETE =
            "DELETE FROM service WHERE id=?";

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

        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getDescription());
            ps.setBigDecimal(3, s.getBasePrice());
            ps.setInt(4, s.getDurationMinutes());
            ps.setString(5, s.getLocation());
            ps.setInt(6, s.isActive() ? 1 : 0);
            ps.setTimestamp(7, Timestamp.valueOf(s.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.valueOf(s.getUpdatedAt()));
            ps.setInt(9, s.getHostId());
            ps.setString(10, s.getImageName());
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
            ps.setInt(9, s.getId());
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
