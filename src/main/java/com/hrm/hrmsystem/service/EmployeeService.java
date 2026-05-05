package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.EmployeeDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Department;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.DepartmentRepository;
import com.hrm.hrmsystem.repository.UserRepository;
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
    private final UserRepository userRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getAllEmployees() {
        // Use findAllWithDepartment to eagerly load department and avoid lazy loading issues
        return employeeRepository.findAllWithDepartment()
                .stream()
                .filter(emp -> emp.getStatus() != Employee.EmployeeStatus.TERMINATED)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(Long id) {
        // Use findByIdWithDepartment to eagerly load department
        Employee employee = employeeRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        // Return null for terminated employees
        if (employee.getStatus() == Employee.EmployeeStatus.TERMINATED) {
            return null;
        }
        
        return convertToDTO(employee);
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found with email: " + email));
    }

    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        Employee employee = convertToEntity(dto);
        employee = employeeRepository.save(employee);
        return convertToDTO(employee);
    }

    @Transactional
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
        employee.setGender(dto.getGender() != null ? Employee.Gender.valueOf(dto.getGender().toUpperCase()) : null);
        employee.setProbationPeriodMonths(dto.getProbationPeriodMonths());
        employee.setBasicSalary(dto.getBasicSalary());
        employee.setDa(dto.getDa());
        employee.setHra(dto.getHra());
        employee.setOtherAllowance(dto.getOtherAllowance());
        employee.setPf(dto.getPf());
        employee.setTax(dto.getTax());
        employee.setInsuranceName(dto.getInsuranceName());
        employee.setInsurancePercentage(dto.getInsurancePercentage());

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
            try {
                employee.setStatus(Employee.EmployeeStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Handle legacy status values or log warning
                employee.setStatus(Employee.EmployeeStatus.ACTIVE);
            }
        }

        employee = employeeRepository.save(employee);

        // Also update the User entity's email and username if it exists
        try {
            userRepository.findByEmployeeId(employee.getId()).ifPresent(user -> {
                if (!user.getEmail().equals(dto.getEmail())) {
                    user.setEmail(dto.getEmail());
                    user.setUsername(dto.getEmail()); // Update username to match email for login
                    userRepository.save(user);
                }
            });
        } catch (Exception e) {
            System.err.println("Error updating user email: " + e.getMessage());
        }

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
                .gender(employee.getGender() != null ? employee.getGender().name() : null)
                .shiftId(employee.getShift() != null ? employee.getShift().getId() : null)
                .probationPeriodMonths(employee.getProbationPeriodMonths())
                .basicSalary(employee.getBasicSalary())
                .da(employee.getDa())
                .hra(employee.getHra())
                .otherAllowance(employee.getOtherAllowance())
                .pf(employee.getPf())
                .tax(employee.getTax())
                .insuranceName(employee.getInsuranceName())
                .insurancePercentage(employee.getInsurancePercentage())
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
                .gender(dto.getGender() != null ? Employee.Gender.valueOf(dto.getGender().toUpperCase()) : null)
                .probationPeriodMonths(dto.getProbationPeriodMonths())
                .basicSalary(dto.getBasicSalary())
                .da(dto.getDa())
                .hra(dto.getHra())
                .otherAllowance(dto.getOtherAllowance())
                .pf(dto.getPf())
                .tax(dto.getTax())
                .insuranceName(dto.getInsuranceName())
                .insurancePercentage(dto.getInsurancePercentage())
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