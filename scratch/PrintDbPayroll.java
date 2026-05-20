import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class PrintDbPayroll {
    public static void main(String[] args) {
        String url = "jdbc:mysql://enego.ct2au2680qip.ap-south-1.rds.amazonaws.com:3306/hrmsystem?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user = "admin";
        String password = "Rishu952818";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("--- PAYROLL FOR Sachin Kumar Verma (ID: 8) ---");
            String query = "SELECT * FROM payroll WHERE employee_id = 8";
            try (ResultSet rs = stmt.executeQuery(query)) {
                int cols = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    System.out.print(rs.getMetaData().getColumnName(i) + "\t| ");
                }
                System.out.println("\n------------------------------------------------");
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        System.out.print(rs.getString(i) + "\t| ");
                    }
                    System.out.println();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
