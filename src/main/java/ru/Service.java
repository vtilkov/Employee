package ru;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Service {
    private Connection connection;

    public Service(Connection connection) {
        this.connection = connection;
    }

    //поиск сотруднкиа по имени
    public List<Integer> findEmployeeByName(String name) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM Employee WHERE name = ?"; //

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        }
        return ids;
    }

    //обновим отедл у сотрудника
    public boolean updateEmployeeDepartment(int employeeId, String departmentName) throws SQLException {
        String sql = "UPDATE Employee SET department_id = (SELECT id FROM Department WHERE name = ?) WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, departmentName);
            stmt.setInt(2, employeeId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    //исправим имя с прописной буквы
    public int correctEmployeeNames() throws SQLException {
        String selectSQL = "SELECT id, name FROM Employee";
        String updateSQL = "UPDATE Employee SET name = ? WHERE id = ?";
        int correctedCount = 0;

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
             ResultSet rs = selectStmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                if (name != null && !name.isEmpty() && Character.isLowerCase(name.charAt(0))) {
                    String correctedName = name.substring(0, 1).toUpperCase() + name.substring(1);

                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                        updateStmt.setString(1, correctedName);
                        updateStmt.setInt(2, id);
                        updateStmt.executeUpdate();
                    }
                    correctedCount++;
                }
            }
        }
        return correctedCount;
    }

    //подсчет сотрудников в отделе
    public int countEmployeesInDepartment(String departmentName) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM Employee e JOIN Department d ON e.department_id = d.id WHERE d.name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, departmentName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    //метод для удаления отдела (новое требование!)
    public boolean deleteDepartment(String departmentName) throws SQLException {
        String sql = "DELETE FROM Department WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, departmentName);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    //получить id отдела по имени
    public Integer getDepartmentIdByName(String departmentName) throws SQLException {
        String sql = "SELECT id FROM Department WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, departmentName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }
}
