package scratch;
import java.sql.Connection;
import java.sql.DriverManager;
public class TestDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://enego.ct2au2680qip.ap-south-1.rds.amazonaws.com:3306/hrmsystem?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        System.out.println("Connecting to " + url);
        try {
            Connection conn = DriverManager.getConnection(url, "root", "Rishu952818");
            System.out.println("Success! Connected to MySQL RDS.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
