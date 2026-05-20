package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.EmployeeDocumentDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.EmployeeDocument;
import com.hrm.hrmsystem.repository.EmployeeDocumentRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class EmployeeDocumentController {

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final Path fileStorageLocation;

    public EmployeeDocumentController(EmployeeDocumentRepository documentRepository, EmployeeRepository employeeRepository) {
        this.documentRepository = documentRepository;
        this.employeeRepository = employeeRepository;
        this.fileStorageLocation = Paths.get(System.getProperty("user.dir") + "/uploads/documents").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // Generate unique filename to prevent overwrites
            String uniqueFileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation);

            EmployeeDocument document = EmployeeDocument.builder()
                    .employee(employee)
                    .documentType(documentType)
                    .fileName(originalFileName != null ? originalFileName : uniqueFileName)
                    .filePath(uniqueFileName)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            EmployeeDocument saved = documentRepository.save(document);

            EmployeeDocumentDTO dto = new EmployeeDocumentDTO(
                    saved.getId(),
                    employee.getId(),
                    employee.getFirstName() + " " + employee.getLastName(),
                    saved.getDocumentType(),
                    saved.getFileName(),
                    saved.getUploadedAt()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not store file. Error: " + ex.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EmployeeDocumentDTO>> getEmployeeDocuments(@PathVariable Long employeeId) {
        List<EmployeeDocument> docs = documentRepository.findByEmployeeId(employeeId);
        List<EmployeeDocumentDTO> dtos = docs.stream().map(doc -> new EmployeeDocumentDTO(
                doc.getId(),
                doc.getEmployee().getId(),
                doc.getEmployee().getFirstName() + " " + doc.getEmployee().getLastName(),
                doc.getDocumentType(),
                doc.getFileName(),
                doc.getUploadedAt()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @RequestParam(value = "download", defaultValue = "false") boolean download) {
        EmployeeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        try {
            Path filePath = this.fileStorageLocation.resolve(document.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = "application/octet-stream";
                try {
                    String detected = Files.probeContentType(filePath);
                    if (detected != null) {
                        contentType = detected;
                    }
                } catch (IOException ex) {
                    // fall back to octet-stream
                }

                String disposition = download ? "attachment" : "inline";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + document.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        EmployeeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        try {
            // Delete file from filesystem
            Path filePath = this.fileStorageLocation.resolve(document.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
            
            // Delete record from database
            documentRepository.delete(document);
            
            return ResponseEntity.ok().body("Document deleted successfully");
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not delete file from storage. Error: " + ex.getMessage());
        }
    }
}
