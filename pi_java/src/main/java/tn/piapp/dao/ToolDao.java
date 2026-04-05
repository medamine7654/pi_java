package tn.piapp.dao;

import tn.piapp.db.DbConnection;
import tn.piapp.model.Tool;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ToolDao {

    private static final String FIND_ALL =
            "SELECT id, name, description, price_per_day, stock_quantity, location, is_active, " +
            "created_at, updated_at, host_id, image_name FROM tool";

    private static final String INSERT =
            "INSERT INTO tool (name, description, price_per_day, stock_quantity, location, " +
            "is_active, created_at, updated_at, host_id, image_name) VALUES (?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE =
            "UPDATE tool SET name=?, description=?, price_per_day=?, stock_quantity=?, " +
            "location=?, is_active=?, updated_at=?, image_name=? WHERE id=?";

    private static final String DELETE =
            "DELETE FROM tool WHERE id=?";

    private static final String RESOLVE_HOST =
            "SELECT MIN(id) FROM user";

    public List<Tool> findAll() throws SQLException {
        List<Tool> list = new ArrayList<>();
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Tool t = new Tool();
                t.setId(rs.getInt("id"));
                t.setName(rs.getString("name"));
                t.setDescription(rs.getString("description"));
                t.setPricePerDay(rs.getBigDecimal("price_per_day"));
                t.setStockQuantity(rs.getInt("stock_quantity"));
                t.setLocation(rs.getString("location"));
                t.setActive(rs.getInt("is_active") != 0);
                t.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                t.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                t.setHostId(rs.getInt("host_id"));
                t.setImageName(rs.getString("image_name"));
                list.add(t);
            }
        }
        return list;
    }

    public void insert(Tool t) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        t.setHostId(resolveDefaultHostId());

        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getDescription());
            ps.setBigDecimal(3, t.getPricePerDay());
            ps.setInt(4, t.getStockQuantity());
            ps.setString(5, t.getLocation());
            ps.setInt(6, t.isActive() ? 1 : 0);
            ps.setTimestamp(7, Timestamp.valueOf(t.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.valueOf(t.getUpdatedAt()));
            ps.setInt(9, t.getHostId());
            ps.setString(10, t.getImageName());
            ps.executeUpdate();
        }
    }

    public void update(Tool t) throws SQLException {
        t.setUpdatedAt(LocalDateTime.now());

        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, t.getName());
            ps.setString(2, t.getDescription());
            ps.setBigDecimal(3, t.getPricePerDay());
            ps.setInt(4, t.getStockQuantity());
            ps.setString(5, t.getLocation());
            ps.setInt(6, t.isActive() ? 1 : 0);
            ps.setTimestamp(7, Timestamp.valueOf(t.getUpdatedAt()));
            ps.setString(8, t.getImageName());
            ps.setInt(9, t.getId());
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
