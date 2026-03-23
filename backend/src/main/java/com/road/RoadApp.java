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

        st.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE," +
                "email TEXT," +
                "password TEXT)");

        st.execute("CREATE TABLE IF NOT EXISTS complaints (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "description TEXT," +
                "location TEXT," +
                "image TEXT," +
                "status TEXT DEFAULT 'PENDING')");

        SpringApplication.run(RoadApp.class, args);
    }

    // ================= SIGNUP =================
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
        } else {
            res.put("status", "fail");
        }

        return res;
    }

    // ================= ADD COMPLAINT =================
    @PostMapping("/addComplaint")
    public String addComplaint(
            @RequestParam int userId,
            @RequestParam String description,
            @RequestParam String location,
            @RequestParam MultipartFile image) throws Exception {

        String uploadDir = System.getProperty("user.dir") + "/uploads/";

File dir = new File(uploadDir);
if (!dir.exists()) dir.mkdirs();

String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
String path = uploadDir + fileName;

image.transferTo(new File(path));

        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO complaints(user_id,description,location,image) VALUES(?,?,?,?)");

        ps.setInt(1, userId);
        ps.setString(2, description);
        ps.setString(3, location);
        ps.setString(4, path);

        ps.executeUpdate();

        return "Complaint Added";
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
            String fileName = fullPath.substring(fullPath.lastIndexOf("/") + 1);

            m.put("id", rs.getInt("id"));
            m.put("description", rs.getString("description"));
            m.put("location", rs.getString("location"));
            m.put("status", rs.getString("status"));
            m.put("image", fileName);

            list.add(m);
        }

        return list;
    }

    // ================= IMAGE VIEW =================
    @GetMapping("/uploads/{name}")
public ResponseEntity<byte[]> getImage(@PathVariable String name) throws Exception {

    Path path = Paths.get(System.getProperty("user.dir") + "/uploads/" + name);

    byte[] image = Files.readAllBytes(path);

    return ResponseEntity
            .ok()
            .header("Content-Type", Files.probeContentType(path)) // auto detect type
            .body(image);
}
    }
