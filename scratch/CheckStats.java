package scratch;
import java.sql.*;
public class CheckStats {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:h2:file:./data/hrm_db;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE", "sa", "password");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT SUM(salary) FROM employee WHERE status = 'ACTIVE' AND username != 'admin'");
        if (rs.next()) System.out.println("Total Salary: " + rs.getDouble(1));
        
        rs = stmt.executeQuery("SELECT SUM(net_salary), status FROM payslip WHERE month_year = '2026-05' GROUP BY status");
        while (rs.next()) System.out.println("Payslips " + rs.getString(2) + ": " + rs.getDouble(1));
    }
}
