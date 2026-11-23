package com.rainbow.springboot.thymeleafdemo.service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.rainbow.springboot.thymeleafdemo.dao.EmployeeDocumentRepository;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeDocument;

@Service
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private final EmployeeDocumentRepository employeeDocumentRepository;

    public EmployeeDocumentServiceImpl(EmployeeDocumentRepository theEmployeeDocumentRepository) {
        employeeDocumentRepository = theEmployeeDocumentRepository;
    }

    @Override
    @Transactional
    public EmployeeDocument saveDocument(Employee employee, MultipartFile file, String description) {
        if (employee == null || file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Employee and uploaded file are required.");
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank() || "application/octet-stream".equalsIgnoreCase(contentType)) {
            // Some browsers send no useful type (common for .docx and .xlsx): go by the file extension.
            contentType = typeFromExtension(file.getOriginalFilename());
        }

        String[] allowedTypes = {
                "image/png",
                "image/jpeg",
                "image/jpg",
                "image/gif",
                "image/webp",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain"
        };

        boolean allowed = false;
        for (String allowedType : allowedTypes) {
            if (allowedType.equalsIgnoreCase(contentType)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new IllegalArgumentException("Unsupported file type. Upload an image, PDF, Word document, Excel file, or text file.");
        }

        try {
            String fileData = Base64.getEncoder().encodeToString(file.getBytes());
            EmployeeDocument document = new EmployeeDocument(
                    employee,
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "document",
                    contentType,
                    file.getSize(),
                    fileData,
                    description
            );
            return employeeDocumentRepository.save(document);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to process the uploaded file.", e);
        }
    }

    private static String typeFromExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            case "pdf": return "application/pdf";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt": return "text/plain";
            default: return "application/octet-stream";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDocument> findByEmployee(Employee employee) {
        if (employee == null) {
            return List.of();
        }
        return employeeDocumentRepository.findByEmployeeOrderByUploadedAtDesc(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocument findById(int documentId) {
        return employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
    }

    @Override
    @Transactional
    public void deleteById(int documentId) {
        employeeDocumentRepository.deleteById(documentId);
    }

    @Override
    public byte[] getDocumentBytes(EmployeeDocument document) {
        if (document == null || document.getFileData() == null || document.getFileData().isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(document.getFileData());
    }
}
