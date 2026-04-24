package tn.piapp.dao;

import tn.piapp.db.DbConnection;
import tn.piapp.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    private static final String FIND_BY_TYPE =
            "SELECT id, name, type FROM category WHERE type = ?";

    /**
     * Returns all categories of the given type ("tool" or "service").
     * Returns an empty list (never null) if no rows match.
     */
    public List<Category> findByType(String type) throws SQLException {
        List<Category> list = new ArrayList<>();
        Connection conn = DbConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_TYPE)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category c = new Category();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setType(rs.getString("type"));
                    list.add(c);
                }
            }
        }
        return list;
    }
}
