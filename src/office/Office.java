package office;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Office {

    public static void main(String[] args) {
        Option opt=Option.AddDepartment;
        Scanner sc = new Scanner(System.in);
        
        while(!opt.equals(Option.EXIT)){
            System.out.println("Введите число:");
            for(Option o: Option.values()) System.out.println(o.getText());
            opt=Option.values()[sc.nextInt()];    
            opt.action();
        }        
    }
}
