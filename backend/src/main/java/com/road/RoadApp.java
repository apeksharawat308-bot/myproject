package com.road;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.sql.*;
import java.util.*;
import java.io.File;
import java.nio.file.*;

@SpringBootApplication
@RestController
@CrossOrigin
public class RoadApp {

    static Connection conn;

    public static void main(String[] args) throws Exception {

        conn = DriverManager.getConnection("jdbc:sqlite:road.db");
        Statement st = conn.createStatement();

        // USERS TABLE
        st.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE," +
                "email TEXT," +
                "password TEXT)");

        // COMPLAINTS TABLE (FIXED)
        st.execute("CREATE TABLE IF NOT EXISTS complaints (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "description TEXT," +
                "location TEXT," +
                "image TEXT," +
                "category TEXT DEFAULT 'General'," +
                "status TEXT DEFAULT 'PENDING')");

        SpringApplication.run(RoadApp.class, args);
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> data) {

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users(username,email,password) VALUES(?,?,?)");

            ps.setString(1, data.get("username"));
            ps.setString(2, data.get("email"));
            ps.setString(3, data.get("password"));

            ps.executeUpdate();
            return "Registered Successfully";

        } catch (Exception e) {
            return "User already exists";
        }
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> data) throws Exception {

        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM users WHERE username=? AND password=?");

        ps.setString(1, data.get("username"));
        ps.setString(2, data.get("password"));

        ResultSet rs = ps.executeQuery();

        Map<String, Object> res = new HashMap<>();

        if (rs.next()) {
            res.put("status", "success");
            res.put("userId", rs.getInt("id"));
            res.put("message", "Login successful");
        } else {
            res.put("status", "fail");
            res.put("message", "Invalid credentials");
        }

        return res;
    }

    // ================= ADD COMPLAINT (FIXED) =================
    @PostMapping("/addComplaint")
    public String addComplaint(
            @RequestParam("userId") int userId,
            @RequestParam("description") String description,
            @RequestParam("location") String location,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {

            // SAFE CATEGORY
            String safeCategory = (category == null || category.trim().isEmpty())
                    ? "General"
                    : category.trim();

            String path = "";

            // IMAGE UPLOAD SAFE
            if (image != null && !image.isEmpty()) {

                String uploadDir = System.getProperty("user.dir") + "/uploads/";

                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                path = uploadDir + fileName;

                image.transferTo(new File(path));
            }

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO complaints(user_id,description,location,image,category) VALUES(?,?,?,?,?)");

            ps.setInt(1, userId);
            ps.setString(2, description);
            ps.setString(3, location);
            ps.setString(4, path);
            ps.setString(5, safeCategory);

            ps.executeUpdate();

            return "SUCCESS";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    // ================= VIEW COMPLAINTS =================
    @GetMapping("/complaints")
    public List<Map<String, Object>> getComplaints() throws Exception {

        List<Map<String, Object>> list = new ArrayList<>();

        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM complaints");

        while (rs.next()) {

            Map<String, Object> m = new HashMap<>();

            String fullPath = rs.getString("image");
            String fileName = "";

            if (fullPath != null && fullPath.contains("/")) {
                fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);
            }

            m.put("id", rs.getInt("id"));
            m.put("description", rs.getString("description"));
            m.put("location", rs.getString("location"));
            m.put("status", rs.getString("status"));
            m.put("category", rs.getString("category"));
            m.put("image", fileName);

            list.add(m);
        }

        return list;
    }

    // ================= IMAGE API =================
    @GetMapping("/uploads/{name}")
    public ResponseEntity<byte[]> getImage(@PathVariable String name) throws Exception {

        Path path = Paths.get(System.getProperty("user.dir") + "/uploads/" + name);

        byte[] image = Files.readAllBytes(path);

        return ResponseEntity
                .ok()
                .header("Content-Type", Files.probeContentType(path))
                .body(image);
    }
}