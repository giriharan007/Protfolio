package com.giriharan.portfolio;


import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;


/**
 * Contact Form Servlet for Giriharan S Portfolio
 * Handles contact form submissions and stores data in MySQL database
 */
@WebServlet("/contact")
public class ContactServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // Database connection parameters
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    
    @Override
    public void init() throws ServletException {
        // Get database parameters from web.xml
        dbUrl=getServletContext().getInitParameter("DB_URL");
        dbUser=getServletContext().getInitParameter("DB_USER");
        dbPassword=getServletContext().getInitParameter("DB_PASSWORD");
        
        // Load MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ServletException("MySQL JDBC driver not found", e);
        }
        
        // Initialize database
        initializeDatabase();
    }
    
    /**
     * Create database and table if they don't exist
     */
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Create contacts table if it doesn't exist
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS contacts (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(150) NOT NULL,
                    subject VARCHAR(200) NOT NULL,
                    message TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status ENUM('new', 'read', 'replied') DEFAULT 'new',
                    INDEX idx_email (email),
                    INDEX idx_created_at (created_at),
                    INDEX idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                stmt.executeUpdate();
                System.out.println("Database initialized successfully");
            }
            
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle GET requests - show contact form
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        response.sendRedirect("index.html#contact");
    }
    
    /**
     * Handle POST requests - process contact form submission
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        PrintWriter out = response.getWriter();
        
        try {
            // Get form parameters
            String name = request.getParameter("name");
            String email = request.getParameter("email");
            String subject = request.getParameter("subject");
            String message = request.getParameter("message");
            
            // Validate input
            if (!isValidInput(name, email, subject, message)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\": false, \"message\": \"Invalid input data\"}");
                return;
            }
            
            // Save to database
            boolean saved = saveContact(name, email, subject, message);
            
            if (saved) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"success\": true, \"message\": \"Message sent successfully!\"}");
                
                // Log successful submission
                System.out.println(String.format(
                    "New contact submission - Name: %s, Email: %s, Subject: %s", 
                    name, email, subject
                ));
                
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\": false, \"message\": \"Failed to save message\"}");
            }
            
        } catch (Exception e) {
            System.err.println("Error processing contact form: " + e.getMessage());
            e.printStackTrace();
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"Server error occurred\"}");
            
        } finally {
            out.close();
        }
    }
    
    /**
     * Validate form input data
     */
    private boolean isValidInput(String name, String email, String subject, String message) {
        if (name == null || name.trim().isEmpty() || name.length() > 100) {
            return false;
        }
        
        if (email == null || email.trim().isEmpty() || email.length() > 150) {
            return false;
        }
        
        // Basic email validation
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return false;
        }
        
        if (subject == null || subject.trim().isEmpty() || subject.length() > 200) {
            return false;
        }
        
        if (message == null || message.trim().isEmpty() || message.length() > 5000) {
            return false;
        }
        
        return true;
    }
    
 
	/**
     * Save contact information to database
     */
    private boolean saveContact(String name, String email, String subject, String message) {
        String insertSQL = """
            INSERT INTO contacts (name, email, subject, message, created_at) 
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(insertSQL)) {
            
            stmt.setString(1, name.trim());
            stmt.setString(2, email.trim().toLowerCase());
            stmt.setString(3, subject.trim());
            stmt.setString(4, message.trim());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Database error saving contact: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get database connection (for future admin features)
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}