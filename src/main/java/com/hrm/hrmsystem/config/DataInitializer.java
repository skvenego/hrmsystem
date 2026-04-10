package com.hrm.hrmsystem.config;

import com.hrm.hrmsystem.model.Department;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.repository.DepartmentRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner initDepartments(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        return args -> {
            // Initialize departments
            List<String> departmentNames = Arrays.asList(
                "IT", "HR", "Sales", "Design", "Finance", "Marketing", "Product", "Operations"
            );
            
            for (String name : departmentNames) {
                if (!departmentRepository.existsByName(name)) {
                    Department dept = Department.builder()
                        .name(name)
                        .description(name + " Department")
                        .createdDate(LocalDate.now())
                        .build();
                    departmentRepository.save(dept);
                    System.out.println("Created department: " + name);
                }
            }
            
            // Initialize sample employees if none exist
            if (employeeRepository.count() == 0) {
                System.out.println("Creating sample employees...");
                
                Department itDept = departmentRepository.findByName("IT").orElse(null);
                Department hrDept = departmentRepository.findByName("HR").orElse(null);
                Department salesDept = departmentRepository.findByName("Sales").orElse(null);
                Department designDept = departmentRepository.findByName("Design").orElse(null);
                Department financeDept = departmentRepository.findByName("Finance").orElse(null);
                
                Employee emp1 = Employee.builder()
                    .firstName("John").lastName("Doe")
                    .email("john.doe@example.com").phone("1234567890")
                    .department(itDept).designation("Software Engineer")
                    .joiningDate(LocalDate.of(2024, 1, 15))
                    .salary(new BigDecimal("75000.00"))
                    .basicSalary(new BigDecimal("50000.00"))
                    .da(new BigDecimal("10000.00"))
                    .hra(new BigDecimal("15000.00"))
                    .otherAllowance(new BigDecimal("5000.00"))
                    .status(Employee.EmployeeStatus.ACTIVE)
                    .address("123 Main St, City")
                    .build();
                
                Employee emp2 = Employee.builder()
                    .firstName("Jane").lastName("Smith")
                    .email("jane.smith@example.com").phone("0987654321")
                    .department(hrDept).designation("HR Manager")
                    .joiningDate(LocalDate.of(2023, 6, 20))
                    .salary(new BigDecimal("85000.00"))
                    .basicSalary(new BigDecimal("60000.00"))
                    .da(new BigDecimal("12000.00"))
                    .hra(new BigDecimal("18000.00"))
                    .otherAllowance(new BigDecimal("6000.00"))
                    .status(Employee.EmployeeStatus.ACTIVE)
                    .address("456 Oak Ave, City")
                    .build();
                
                Employee emp3 = Employee.builder()
                    .firstName("Mike").lastName("Johnson")
                    .email("mike.johnson@example.com").phone("1122334455")
                    .department(salesDept).designation("Sales Executive")
                    .joiningDate(LocalDate.of(2024, 3, 10))
                    .salary(new BigDecimal("65000.00"))
                    .basicSalary(new BigDecimal("45000.00"))
                    .da(new BigDecimal("9000.00"))
                    .hra(new BigDecimal("13500.00"))
                    .otherAllowance(new BigDecimal("4500.00"))
                    .status(Employee.EmployeeStatus.ACTIVE)
                    .address("789 Pine Rd, City")
                    .build();
                
                Employee emp4 = Employee.builder()
                    .firstName("Sarah").lastName("Williams")
                    .email("sarah.williams@example.com").phone("5566778899")
                    .department(designDept).designation("UI/UX Designer")
                    .joiningDate(LocalDate.of(2024, 2, 5))
                    .salary(new BigDecimal("70000.00"))
                    .basicSalary(new BigDecimal("48000.00"))
                    .da(new BigDecimal("9600.00"))
                    .hra(new BigDecimal("14400.00"))
                    .otherAllowance(new BigDecimal("4800.00"))
                    .status(Employee.EmployeeStatus.ACTIVE)
                    .address("321 Elm St, City")
                    .build();
                
                Employee emp5 = Employee.builder()
                    .firstName("David").lastName("Brown")
                    .email("david.brown@example.com").phone("9988776655")
                    .department(financeDept).designation("Accountant")
                    .joiningDate(LocalDate.of(2023, 11, 12))
                    .salary(new BigDecimal("80000.00"))
                    .basicSalary(new BigDecimal("55000.00"))
                    .da(new BigDecimal("11000.00"))
                    .hra(new BigDecimal("16500.00"))
                    .otherAllowance(new BigDecimal("5500.00"))
                    .status(Employee.EmployeeStatus.ACTIVE)
                    .address("654 Maple Dr, City")
                    .build();
                
                employeeRepository.saveAll(Arrays.asList(emp1, emp2, emp3, emp4, emp5));
                System.out.println("✓ Created 5 sample employees!");
            } else {
                System.out.println("Employees already exist (" + employeeRepository.count() + " records)");
                
                // Fix existing employees with NULL salary components
                System.out.println("Checking and fixing NULL salary components...");
                List<Employee> allEmployees = employeeRepository.findAll();
                int fixedCount = 0;
                
                for (Employee emp : allEmployees) {
                    boolean needsUpdate = false;
                    
                    // If salary components are NULL, set them based on total salary
                    if (emp.getBasicSalary() == null && emp.getSalary() != null) {
                        double salary = emp.getSalary().doubleValue();
                        emp.setBasicSalary(new BigDecimal(salary * 0.60)); // 60% of total
                        emp.setDa(new BigDecimal(salary * 0.15)); // 15% of total
                        emp.setHra(new BigDecimal(salary * 0.20)); // 20% of total
                        emp.setOtherAllowance(new BigDecimal(salary * 0.05)); // 5% of total
                        needsUpdate = true;
                    }
                    
                    if (needsUpdate) {
                        employeeRepository.save(emp);
                        fixedCount++;
                        System.out.println("Fixed salary components for: " + emp.getEmail());
                    }
                }
                
                if (fixedCount > 0) {
                    System.out.println("✓ Fixed " + fixedCount + " employees with NULL salary components");
                }
            }
        };
    }
    
    @Bean
    @Order(2)
    CommandLineRunner initUsers(UserRepository userRepository, EmployeeRepository employeeRepository, 
                                PasswordEncoder passwordEncoder) {
        return args -> {
            String testPassword = "Sachin@123";
            String encodedPassword = passwordEncoder.encode(testPassword);
            
            // Create or update admin user
            User admin = userRepository.findByUsername("admin")
                .orElse(User.builder()
                    .username("admin")
                    .email("admin@hrmsystem.com")
                    .role(User.Role.ROLE_ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build());
            admin.setPassword(encodedPassword);
            admin.setIsActive(true);
            userRepository.save(admin);
            System.out.println("✓ Created/updated admin user");
            
            // Create or update hradmin user
            User hrAdmin = userRepository.findByUsername("hradmin")
                .orElse(User.builder()
                    .username("hradmin")
                    .email("hradmin@hrmsystem.com")
                    .role(User.Role.ROLE_HR)
                    .createdAt(LocalDateTime.now())
                    .build());
            hrAdmin.setPassword(encodedPassword);
            hrAdmin.setIsActive(true);
            userRepository.save(hrAdmin);
            System.out.println("✓ Created/updated hradmin user");
            
            // Create or update accountant user
            User accountant = userRepository.findByUsername("accountant")
                .orElse(User.builder()
                    .username("accountant")
                    .email("accountant@hrmsystem.com")
                    .role(User.Role.ROLE_ACCOUNTANT)
                    .createdAt(LocalDateTime.now())
                    .build());
            accountant.setPassword(encodedPassword);
            accountant.setIsActive(true);
            userRepository.save(accountant);
            System.out.println("✓ Created/updated accountant user");
            
            // Create or update employee1 user
            Employee employee = employeeRepository.findAll().stream().findFirst().orElse(null);
            User employeeUser = userRepository.findByUsername("employee1")
                .orElse(User.builder()
                    .username("employee1")
                    .email("employee1@hrmsystem.com")
                    .role(User.Role.ROLE_EMPLOYEE)
                    .employee(employee)
                    .createdAt(LocalDateTime.now())
                    .build());
            employeeUser.setPassword(encodedPassword);
            employeeUser.setIsActive(true);
            if (employee != null && employeeUser.getEmployee() == null) {
                employeeUser.setEmployee(employee);
            }
            userRepository.save(employeeUser);
            System.out.println("✓ Created/updated employee1 user" + (employeeUser.getEmployee() != null ? " (linked to employee)" : ""));
            
            System.out.println("✓ User initialization complete! All test passwords set to: " + testPassword);
        };
    }
}
