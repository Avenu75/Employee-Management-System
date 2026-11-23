package com.rainbow.springboot.thymeleafdemo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.EmployeeDocument;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Integer> {

    List<EmployeeDocument> findByEmployeeOrderByUploadedAtDesc(Employee employee);

    void deleteByEmployee(Employee employee);
}
