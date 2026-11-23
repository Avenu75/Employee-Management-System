package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeDocument;

public interface EmployeeDocumentService {

    EmployeeDocument saveDocument(Employee employee, MultipartFile file, String description);

    List<EmployeeDocument> findByEmployee(Employee employee);

    EmployeeDocument findById(int documentId);

    void deleteById(int documentId);

    byte[] getDocumentBytes(EmployeeDocument document);
}
