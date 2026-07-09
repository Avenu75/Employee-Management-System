package com.rainbow.springboot.thymeleafdemo.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Represents a Department within the company (e.g. "Sales", "IT & Support").
 *
 * This is the "one" side of a One-to-Many relationship with {@link Employee}:
 * one Department can have many Employees.
 */
@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @NotBlank(message = "Department name is required")
    @Size(max = 60, message = "Department name must be at most 60 characters")
    @Column(name = "name")
    private String name;

    @Size(max = 100, message = "Location must be at most 100 characters")
    @Column(name = "location")
    private String location;

    // mappedBy = "department" refers to the "department" field inside Employee.java
    // This side is not the database owner of the relationship (Employee holds the foreign key).
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = jakarta.persistence.FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    public Department() {
    }

    public Department(String name, String location) {
        this.name = name;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }

    @Override
    public String toString() {
        return "Department [id=" + id + ", name=" + name + ", location=" + location + "]";
    }
}
