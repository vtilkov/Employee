package ru;

import java.sql.*;

public class DepartmentDel {

    /*Проверить:
    1. Сколько сотрудников осталось с department_id = 1 после удаления отдела Accounting
    2. Если количество = 0 - требование выполняется
    3. Если количество > 0 - требование не выполняется*/
    public static void main(String[] args) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:./Office");

        // Проверяем сотрудников удаленного отдела
        String sql = "SELECT COUNT(*) as count FROM Employee WHERE department_id = 1";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            int employeeCount = rs.getInt("count");
            System.out.println("Количество сотрудников в удаленном отделе Accounting: " + employeeCount);

            if (employeeCount == 0) {
                System.out.println("Результат: ТРЕБОВАНИЕ ВЫПОЛНЕНО");
            } else {
                System.out.println("Результат: ТРЕБОВАНИЕ НЕ ВЫПОЛНЕНО: " + employeeCount +
                        " сотрудников осталось в удаленном отделе");
            }
        }
        conn.close();
    }
}