package ru;

import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceTest {
    private Connection connection;
    private Service service;

    @BeforeAll
    void setUp() throws SQLException {
        //Используем in-memory H2 базу для тестов
        /*строка подключения к базе данных H2, которая указывает на создание in-memory базы данных с именем test и обеспечивает ее незакрываемость до явного завершения работы приложения.*/
        connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        createTestTables();
        insertTestData();
        service = new Service(connection);
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    //подготовка
    //создадим таблицы Department/Employee
    private void createTestTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String createDepartment = """
                CREATE TABLE Department (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE)""";

            String createEmployee = """
                CREATE TABLE Employee (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, department_id INT, FOREIGN KEY (department_id) REFERENCES Department(id) ON DELETE CASCADE)""";

            stmt.execute(createDepartment);
            stmt.execute(createEmployee);
        }
    }

    //добавим отделы/сотрудников
    private void insertTestData() throws SQLException {
        //отделы
        String[] departments = {"Accounting", "IT", "HR"};
        try (PreparedStatement deptStmt = connection.prepareStatement("INSERT INTO Department (name) VALUES (?)")) {
            for (String dept : departments) {
                deptStmt.setString(1, dept);
                deptStmt.executeUpdate();
            }
        }

        //сотрудники
        Object[][] employees = {
                {"Pete", 1},    // Accounting
                {"Ann", 1},     // Accounting
                {"Liz", 2},     // IT
                {"Tom", 2},     // IT
                {"Todd", 2},    // IT
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
    }

    @Test
    //поиск сотрудника Ann
    void tesT1() throws SQLException {
        List<Integer> annIds = service.findEmployeeByName("Ann");   //сотрудник существует
        assertEquals(1, annIds.size());

        List<Integer> unknownIds = service.findEmployeeByName("Unknown");   //сотрудник не существует
        assertTrue(unknownIds.isEmpty());
    }

    @Test
    //обновим отдел
    void tesT2() throws SQLException {
        List<Integer> annIds = service.findEmployeeByName("Ann");   //сотрудник найден
        assertEquals(1, annIds.size());

        int annId = annIds.get(0);

        boolean updated = service.updateEmployeeDepartment(annId, "HR");    //обновили отдел на HR
        assertTrue(updated);

        int countInHR = service.countEmployeesInDepartment("HR");   //проверим что отдел обновился
        assertTrue(countInHR >= 1); //как минимум Ann теперь в HR
    }

    @Test
    //правка имени
    void tesT3() throws SQLException {
        int correctedCount = service.correctEmployeeNames();
        assertEquals(1, correctedCount); //исправить peet на Peet

        List<Integer> peetIds = service.findEmployeeByName("Peet"); //имя дествительно исправилось
        assertFalse(peetIds.isEmpty());
    }

    @Test
    //проверим количество сотрдуников в отделе
    void tesT4() throws SQLException {
        int itCount = service.countEmployeesInDepartment("IT"); //отдел IT
        assertEquals(3, itCount); // (3) Liz, Tom, Todd

        int accountingCount = service.countEmployeesInDepartment("Accounting"); //отдел Accounting
        assertEquals(2, accountingCount); // (2) Pete, Ann
    }

    @Test
    //сотрудник удален (отдел удален)
    void tesT5() throws SQLException {
        Integer deptId = service.getDepartmentIdByName("Accounting");   //отдле существует Accounting
        assertNotNull(deptId);

        boolean deleted = service.deleteDepartment("Accounting");   //удалим отдел Accounting
        assertTrue(deleted);

        Integer deletedDeptId = service.getDepartmentIdByName("Accounting");    //отдел удален Accounting
        assertNull(deletedDeptId);

        //сотрудники удаленного отдела также удалены
        int remainingEmployees = getTotalEmployeeCount();
        assertTrue(remainingEmployees < 6); //должно быть меньше первоначального значения 6
    }

    @Test
    //получить id отедла
    void tesT6() throws SQLException {
        Integer itId = service.getDepartmentIdByName("IT"); //получить id отдела IT
        assertNotNull(itId);

        Integer unknownId = service.getDepartmentIdByName("Unknown");   //получить id отдела Unknown
        assertNull(unknownId);
    }

    private int getTotalEmployeeCount() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM Employee";      //получить общее количество сотрудников
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }
}
