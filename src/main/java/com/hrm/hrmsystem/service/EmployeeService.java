package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.EmployeeDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Department;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.DepartmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @PersistenceContext
    private EntityManager entityManager;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        // Clear cache to ensure fresh department data
        entityManager.clear();
        
        return employeeRepository.findAll()
                .stream()
                .filter(emp -> emp.getStatus() != Employee.EmployeeStatus.TERMINATED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public EmployeeDTO getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        // Return null for terminated employees
        if (employee.getStatus() == Employee.EmployeeStatus.TERMINATED) {
            return null;
        }
        
        return convertToDTO(employee);
    }

    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        Employee employee = convertToEntity(dto);
        employee = employeeRepository.save(employee);
        return convertToDTO(employee);
    }

    public EmployeeDTO updateEmployee(Long id, EmployeeDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setPhone(dto.getPhone());
        employee.setEmail(dto.getEmail());
        employee.setDesignation(dto.getDesignation());
        employee.setJoiningDate(dto.getJoiningDate());
        employee.setSalary(dto.getSalary());
        employee.setAddress(dto.getAddress());

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            employee.setDepartment(department);
        } else if (dto.getDepartmentName() != null && !dto.getDepartmentName().isEmpty()) {
            Department department = departmentRepository.findByName(dto.getDepartmentName())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + dto.getDepartmentName()));
            employee.setDepartment(department);
        }

        if (dto.getStatus() != null) {
        employee.setStatus(Employee.EmployeeStatus.valueOf(dto.getStatus()));
    }

        employee = employeeRepository.save(employee);
        return convertToDTO(employee);
    }

    @Transactional
public void deleteEmployee(Long id) {
    if (!employeeRepository.existsById(id)) {
        throw new RuntimeException("Employee not found");
    }
    employeeRepository.deleteById(id);
}

    private EmployeeDTO convertToDTO(Employee employee) {
        return EmployeeDTO.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .departmentId(employee.getDepartment() != null ? employee.getDepartment().getId() : null)
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .designation(employee.getDesignation())
                .joiningDate(employee.getJoiningDate())
                .salary(employee.getSalary())
                .status(employee.getStatus() != null ? employee.getStatus().name() : null)
                .address(employee.getAddress())
                .build();
    }

    private Employee convertToEntity(EmployeeDTO dto) {
        Employee employee = Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .designation(dto.getDesignation())
                .joiningDate(dto.getJoiningDate())
                .salary(dto.getSalary())
                .status(dto.getStatus() != null ? Employee.EmployeeStatus.valueOf(dto.getStatus()) : Employee.EmployeeStatus.ACTIVE)
                .address(dto.getAddress())
                .build();

        if (dto.getDepartmentId() != null) {
    Department department = departmentRepository.findById(dto.getDepartmentId())
            .orElseThrow(() -> new RuntimeException("Department not found"));
    employee.setDepartment(department);

} else if (dto.getDepartmentName() != null && !dto.getDepartmentName().isEmpty()) {

    Department department = departmentRepository.findByName(dto.getDepartmentName())
            .orElseThrow(() -> new RuntimeException("Department not found"));

    employee.setDepartment(department);
}

        return employee;
    }
}