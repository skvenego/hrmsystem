package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    public UserController(UserRepository userRepository, EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        String username = auth.getName();
        System.out.println("Profile request for username: " + username);
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            System.err.println("User not found for username: " + username);
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        String email = user.getEmail();
        Optional<Employee> empOpt = employeeRepository.findByEmail(email);

        Map<String, Object> res = new HashMap<>();
        res.put("username", user.getUsername());
        res.put("email", user.getEmail());
        res.put("role", user.getRole());

        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            res.put("firstName", emp.getFirstName());
            res.put("lastName", emp.getLastName());
            res.put("phone", emp.getPhone());
            res.put("designation", emp.getDesignation());
            res.put("department", emp.getDepartment() != null ? emp.getDepartment().getName() : null);
        }

        return ResponseEntity.ok(res);
    }
}
