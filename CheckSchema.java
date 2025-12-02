import java.sql.*;

public class CheckSchema {
    public static void main(String[] args) {
        String url = "jdbc:h2:~/car_rental_db";
        String user = "sa";
        String password = "";
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, password);
                    ResultSet rs = conn.getMetaData().getColumns(null, null, "BOOKINGS", null)) {
                System.out.println("Columns in BOOKINGS table:");
                while (rs.next()) {
                    System.out.println(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
