package ru;

import java.sql.*;
import java.util.List;

public class Employee {
    public static void main (String[]args){
        executeEmployeeTasks();
    }

        public static void executeEmployeeTasks() {

            try {
                // Пробуем подключиться к существующей базе
                //Используем in-memory H2 базу для тестов
                /*строка подключения к базе данных H2, которая указывает на создание in-memory базы данных с именем test и обеспечивает ее незакрываемость до явного завершения работы приложения.*/
                try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:office;DB_CLOSE_DELAY=-1")) {
                    System.out.println("Успешное подключение к базе данных");
                    createTables(connection);
                    insertTestData(connection);
                    executeTasks(connection);
                }

            } catch (SQLException e) {
                System.out.println("Ошибка подключения: " + e.getMessage());
            }
        }

        private static void recreateDatabase() {

            //
            //org.h2.jdbc.JdbcSQLNonTransientConnectionException: Файл поврежден при чтении строки: null. Возможные решения: используйте утилиту восстановления (recovery tool)
            //File corrupted while reading record: null. Possible solution: use the recovery tool [90030-199]
            try (Connection connection = DriverManager.getConnection("jdbc:h2:./Office;DB_CLOSE_DELAY=-1")) { //отрубаем лавочку Office.mv.db

                //удалим старые таблицы (хвосты) если есть
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS Employee");
                    stmt.execute("DROP TABLE IF EXISTS Department");
                }

                // (+)Создадим новые таблицы
                createTables(connection);

                // (+)Подготовим тестовые данные
                insertTestData(connection);

                System.out.println("Дубль базы данных создали (восстановили)!"); //отобразим результат готовности

                /* (+)Выполним задачи:
                Подключитесь программно к базе данных и выполните следующие операции:
                Найдите ID сотрудника с именем Ann. Если такой сотрудник только один, то установите его
                департамент в HR.
                Проверьте имена всех сотрудников. Если чьё-то имя написано с маленькой буквы, исправьте её на большую. Выведите на экран количество исправленных имён.
                Выведите на экран количество сотрудников в IT-отделе*/
                executeTasks(connection);

            } catch (SQLException e) {
                System.err.println("Опять не смогла.. (восстановление базы)" + e.getMessage());
            }
        }

        private static void createTables (Connection connection) throws SQLException {
            try (Statement stmt = connection.createStatement()) {

                //таблица отделов
                String createDepartment = """
                        CREATE TABLE Department (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE
                        )
                        """;

                //таблица сотрудников
                String createEmployee = """
                        CREATE TABLE Employee (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            department_id INT,
                            FOREIGN KEY (department_id) REFERENCES Department(id)
                        )
                        """;

                stmt.execute(createDepartment);
                stmt.execute(createEmployee);
                System.out.println("Таблицы созданы!");
            }
        }

        private static void insertTestData (Connection connection) throws SQLException {
            //добавим отделы
            String[] departments = {"Accounting", "IT", "HR"};
            try (PreparedStatement deptStmt = connection.prepareStatement(
                    "INSERT INTO Department (name) VALUES (?)")) {
                for (String dept : departments) {
                    deptStmt.setString(1, dept);
                    deptStmt.executeUpdate();
                }
            }

            //добавим сотрудников
            Object[][] employees = {
                    {"Pete", 1},      // Accounting
                    {"Ann", 1},     // Accounting
                    {"Liz", 2},     // IT
                    {"Tom", 2},    // IT
                    {"Todd", 2},     // IT
                    {"peet", 3}     // HR
            };

            try (PreparedStatement empStmt = connection.prepareStatement(
                    "INSERT INTO Employee (name, department_id) VALUES (?, ?)")) {
                for (Object[] emp : employees) {
                    empStmt.setString(1, (String) emp[0]);
                    empStmt.setInt(2, (Integer) emp[1]);
                    empStmt.executeUpdate();
                }
            }

            System.out.println("Тестовые данные готовы");
        }

        //используют Service класс вместо прямой работы с бд
        private static void executeTasks (Connection connection) throws SQLException {

        Service service = new Service(connection);
        System.out.println("\n ++ ВЫПОЛНИМ ЗАДАЧИ ++ ");

            /*Задача 1
            Найдите ID сотрудника с именем Ann. Если такой сотрудник только один, то установите его
            департамент в HR.*/
            List<Integer> annIds = service.findEmployeeByName("Ann");
            if (annIds.size() == 1) {
                service.updateEmployeeDepartment(annIds.get(0), "HR");
                System.out.println("Ann переведена в HR отдел");

            /*Задача 2
            Проверьте имена всех сотрудников. Если чьё-то имя написано с маленькой буквы, исправьте её на большую. Выведите на экран количество исправленных имён.*/
            int correctedCount = service.correctEmployeeNames();
                System.out.println("Исправлено имен - " + correctedCount);

            /*Задача 3
            Выведите на экран количество сотрудников в IT-отделе*/
            int itCount = service.countEmployeesInDepartment("IT");
                System.out.println("отрудников в IT отделе: " + itCount);
        }

        /*
        private static void task1 (Connection connection) throws SQLException {
            System.out.println("\n ++ Задача 1 ++ ");

            String findAnnSQL = "SELECT id FROM Employee WHERE name = 'Ann'"; //найдем Ann
            try (PreparedStatement stmt = connection.prepareStatement(findAnnSQL);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    int annId = rs.getInt("id");
                    System.out.println("Найден сотрудник Ann с ID: " + annId); // +

                    // Обновляем департамент на HR
                    String updateSQL = "UPDATE Employee SET department_id = (SELECT id FROM Department WHERE name = 'HR') WHERE id = ?";  // обновим департамент для WHERE id = ?
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                        updateStmt.setInt(1, annId);
                        updateStmt.executeUpdate();
                        System.out.println("Ann переведена в HR отдел");
                    }
                } else {
                    System.out.println("Сотрудник Ann не найден ");
                }
            }
        }

        private static void task2 (Connection connection) throws SQLException {
            System.out.println("\n ++ Задача 2 ++ ");

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

            System.out.println("Исправлено имен - " + correctedCount);
        }

        private static void task3 (Connection connection) throws SQLException {
            System.out.println("\n ++ Задача 3 ++ ");

            String sql = "SELECT COUNT(*) as count FROM Employee e JOIN Department d ON e.department_id = d.id WHERE d.name = 'IT'";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("отрудников в IT отделе: " + count);
                }
            }   */
        }
}