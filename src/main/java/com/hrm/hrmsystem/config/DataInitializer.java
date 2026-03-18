package com.hrm.hrmsystem.config;

import com.hrm.hrmsystem.model.Department;
import com.hrm.hrmsystem.repository.DepartmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDepartments(DepartmentRepository departmentRepository) {
        return args -> {
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
        };
    }
}
