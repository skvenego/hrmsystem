package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.DepartmentDTO;
import com.hrm.hrmsystem.model.Department;
import com.hrm.hrmsystem.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        return convertToDTO(department);
    }

    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        if (departmentRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Department already exists");
        }
        Department department = convertToEntity(dto);
        department.setCreatedDate(LocalDate.now());
        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        department.setName(dto.getName());
        department.setDescription(dto.getDescription());
        department.setHeadOfDepartment(dto.getHeadOfDepartment());

        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }

    private DepartmentDTO convertToDTO(Department department) {
        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .headOfDepartment(department.getHeadOfDepartment())
                .createdDate(department.getCreatedDate())
                .employeeCount(department.getEmployees() != null ? department.getEmployees().size() : 0)
                .build();
    }

    private Department convertToEntity(DepartmentDTO dto) {
        return Department.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .headOfDepartment(dto.getHeadOfDepartment())
                .build();
    }
}