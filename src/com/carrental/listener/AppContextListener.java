package com.carrental.listener;

import com.carrental.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Connection;

@WebListener
public class AppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Application starting... Initializing Database.");
        try {
            // Trigger static block of DBConnection
            Connection conn = DBConnection.getConnection();
            if (conn != null) {
                System.out.println("Database connection established successfully.");
                conn.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize database on startup:");
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup if needed
    }
}
