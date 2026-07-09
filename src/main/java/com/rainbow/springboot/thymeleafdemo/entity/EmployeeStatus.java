package com.rainbow.springboot.thymeleafdemo.entity;

/**
 * Employment status of an Employee.
 * Using an enum instead of a raw String prevents invalid values
 * (e.g. typos like "activ") from ever being stored in the database.
 */
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE
}
