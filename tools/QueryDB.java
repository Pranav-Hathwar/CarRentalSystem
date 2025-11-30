import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryDB {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:~/car_rental_db";
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT id, name, email, password, role FROM users")) {
                while (rs.next()) {
                    System.out.printf("%d | %s | %s | %s | %s\n",
                            rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("password"), rs.getString("role"));
                }
            }
        }
    }
}
