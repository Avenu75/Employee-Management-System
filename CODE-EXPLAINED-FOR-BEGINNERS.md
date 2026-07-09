# 📖 The Java Code Explained — A Study Guide for Your Presentation

This document explains **every piece of Java code** in the project in plain
language, as if teaching someone who is new to Spring Boot. Read it fully at
least once before your presentation, then use it to answer follow-up
questions from your lecturer.

> **Updated for the industry-ready version of the project**: single Super
> Admin, dynamic action-based permissions (no fixed roles), a public landing
> page, privilege-based login redirects, and immediate permission
> enforcement. Sections 8 and 9 cover all of this new security architecture
> in full; every other section still applies unchanged.

Each section follows the same pattern:
- **What it is** (the concept, in plain English)
- **The actual code**
- **Line-by-line explanation**
- **"Why does it matter?"**
- **Likely exam/viva questions + model answers**

---

## 0. Before we start: 5 core ideas you must understand cold

If you remember nothing else, remember these five ideas — almost every
question your lecturer asks will trace back to one of them.

1. **Annotations** (things starting with `@`, like `@Entity` or `@GetMapping`)
   are not code that "runs" in the normal sense. They are **metadata** —
   labels that tell Spring "treat this class/method/field specially." Spring
   reads these labels at startup and wires everything together automatically.
   This automatic wiring is called **Inversion of Control (IoC)**.

2. **Dependency Injection (DI)** means a class does not create the objects
   it needs itself (with `new`) — instead, Spring hands ("injects") them in,
   usually through the constructor. This makes classes easier to test and
   loosely coupled.

3. **Layers**: Controller → Service → Repository → Database. Each layer only
   talks to the layer directly below it. This is called a **layered
   architecture**, and it exists so each part of the code has one clear job.

4. **An "Entity"** is just a normal Java class that Hibernate has been told
   (via annotations) to treat as a row in a database table. When you call
   `.save()` on it, Hibernate writes an `INSERT` or `UPDATE` SQL statement
   for you — you never write that SQL by hand.

5. **MVC (Model-View-Controller)**: the Controller handles the request and
   decides what data is needed (Model); the View (a Thymeleaf `.html` file)
   is responsible only for displaying that data. This keeps "what to show"
   separate from "how to get the data."

---

## 1. `ThymeleafdemoApplication.java` — the entry point

```java
package com.rainbow.springboot.thymeleafdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ThymeleafdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThymeleafdemoApplication.class, args);
    }
}
```

**What it is:** Every Java program needs a `main` method — this is where the
program starts running. In a Spring Boot app, `main` doesn't do much itself;
it just hands control over to Spring.

**Line-by-line:**
- `@SpringBootApplication` is actually **three annotations combined into
  one**:
  1. `@Configuration` — this class can define beans/configuration
  2. `@EnableAutoConfiguration` — tells Spring Boot to automatically set up
     things like the embedded web server, the database connection pool, and
     Thymeleaf, based on what libraries (dependencies) it finds in the
     project
  3. `@ComponentScan` — tells Spring to look through this package and every
     sub-package (`controller`, `service`, `dao`, etc.) for classes
     annotated with things like `@Controller`, `@Service`, `@Repository`
     and register them automatically
- `SpringApplication.run(...)` actually starts everything: it starts an
  embedded Tomcat web server on port 8080, connects to the database, and
  keeps the application running until you stop it.

**Why it matters:** Without this class, none of the other code would ever
run — it's the ignition switch for the whole app.

**Likely Q&A:**
> **Q: Do you need to install a separate web server like Apache Tomcat?**
> A: No — Spring Boot bundles an "embedded" Tomcat server inside the
> application itself. Running `java -jar app.jar` starts everything in one
> process, which is simpler to deploy.

> **Q: What does "auto-configuration" actually mean?**
> A: Spring Boot looks at what's on the classpath (e.g., it sees the MySQL
> driver and Spring Data JPA are present) and automatically configures a
> database connection for you, instead of you writing that setup code by
> hand.

---

## 2. The Entity Layer — `Employee.java`, `Department.java`, `EmployeeStatus.java`

### 2.1 What is a JPA Entity?

JPA stands for **Java Persistence API** — a standard way to map Java objects
to database tables. **Hibernate** is the library that actually implements
JPA in this project. An "entity" class is a plain Java class with special
annotations that tell Hibernate: "every object of this class corresponds to
one row in this database table."

### 2.2 `Employee.java` (key excerpt)

```java
@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @NotBlank(message = "First name is required")
    @Size(max = 45, message = "First name must be at most 45 characters")
    @Column(name = "first_name")
    private String firstName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Column(name = "email")
    private String email;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Salary cannot be negative")
    @Column(name = "salary")
    private BigDecimal salary;

    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be in the future")
    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // constructors, getters, setters, toString...
}
```

**Line-by-line:**

| Annotation | Meaning |
|---|---|
| `@Entity` | "This class represents a database table." |
| `@Table(name = "employee")` | "Specifically, the `employee` table." (If you left this out, Hibernate would guess the table name from the class name.) |
| `@Id` | "This field is the Primary Key" — the unique identifier for each row. |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | "Don't make me set the id myself — let the database auto-increment it" (this matches MySQL's `AUTO_INCREMENT`). |
| `@Column(name = "first_name")` | "This Java field (`firstName`, camelCase) maps to this database column (`first_name`, snake_case)." |
| `@NotBlank`, `@Email`, `@DecimalMin`, `@PastOrPresent`, `@NotNull`, `@Size` | **Bean Validation** annotations (from the Jakarta Validation API). They don't come from Hibernate — they come from a separate specification. Spring automatically checks these rules when `@Valid` is used in a controller (explained later). |
| `@Enumerated(EnumType.STRING)` | Tells Hibernate to store the enum as readable text (`"ACTIVE"`) in the database column, rather than a number (`0`, `1`), which would be unreadable if you looked at the raw table. |
| `@ManyToOne(fetch = FetchType.LAZY)` | "Many Employees can belong to the same one Department." `FetchType.LAZY` means: don't bother loading the Department from the database until someone actually calls `.getDepartment()` — this avoids unnecessary database work. |
| `@JoinColumn(name = "department_id")` | "The foreign key column in the `employee` table is called `department_id`." |

**Why validation annotations matter (this is a big one for your demo):**
Without them, if a user submitted a blank first name, Hibernate would happily
save a broken row into the database — bad data in, bad data out. With
`@NotBlank`, Spring checks the rule *before* anything touches the database,
rejects the bad data, and shows the user a helpful message like "First name
is required."

**Likely Q&A:**
> **Q: What's the difference between `@Column` and `@NotBlank`?**
> A: `@Column` is a **JPA/Hibernate** annotation — it's about how the field
> maps to the database (persistence). `@NotBlank` is a **Bean Validation**
> annotation — it's about data quality/business rules, checked in memory
> before persistence happens. They come from completely different
> specifications but are often used together.

> **Q: Why use `BigDecimal` for salary instead of `double`?**
> A: `double` uses binary floating-point representation, which can introduce
> tiny rounding errors when doing money calculations (e.g., 0.1 + 0.2 isn't
> exactly 0.3 in binary floating point). `BigDecimal` represents decimal
> numbers exactly, which is the standard practice for anything involving
> money.

> **Q: Why `LocalDate` instead of the old `java.util.Date`?**
> A: `LocalDate` is part of the modern `java.time` package introduced in
> Java 8. It's immutable (can't accidentally be changed after creation),
> thread-safe, and has a much clearer API. `java.util.Date` is considered
> legacy and error-prone (e.g., months used to be zero-indexed, which caused
> constant bugs).

> **Q: What does `@GeneratedValue(strategy = GenerationType.IDENTITY)` actually do at the database level?**
> A: It tells Hibernate to rely on the database's own auto-increment
> mechanism (MySQL's `AUTO_INCREMENT`) to generate the id, rather than
> Hibernate generating IDs itself in a separate step. This is the simplest
> and most common strategy for MySQL.

### 2.3 `Department.java` (key excerpt)

```java
@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "Department name is required")
    private String name;

    private String location;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();
}
```

**Line-by-line — the important one is `@OneToMany`:**
- `@OneToMany` means "one Department can have many Employees."
- `mappedBy = "department"` is crucial and often misunderstood: it says
  "the relationship is actually owned by the `department` **field inside
  the Employee class**, not here." In database terms: the foreign key
  column (`department_id`) lives in the `employee` table, not in the
  `department` table. This side (`Department`) is just a convenient way to
  ask, in Java, "give me all employees in this department" — it doesn't
  create any extra column in the `department` table.
- `cascade = CascadeType.ALL` means "if you delete/save a Department,
  cascade that operation to its employees too" (used cautiously in this
  project — deleting a department actually just unsets the employee's
  department, per the SQL foreign key rule `ON DELETE SET NULL`).

**Why this is a One-to-Many / Many-to-One pair (not two separate
relationships):** In a relational database, there's really only **one**
relationship between two tables. But because we want to navigate it from
*both* directions in Java (`employee.getDepartment()` AND
`department.getEmployees()`), we describe it twice — once as `@ManyToOne` in
`Employee`, once as `@OneToMany` in `Department` — and link them with
`mappedBy`.

**Likely Q&A:**
> **Q: If I delete a Department, what happens to its Employees?**
> A: Looking at the SQL schema, the foreign key was created with
> `ON DELETE SET NULL`, meaning the employees are NOT deleted — their
> `department_id` is simply set to `NULL` (unassigned). This is a safer
> default for a business application than accidentally deleting employee
> records.

> **Q: Why is `fetch = FetchType.LAZY` used here too?**
> A: Same reason as in Employee — we don't want to automatically pull every
> employee record every single time we just want to display a department's
> name and location.

### 2.4 `EmployeeStatus.java`

```java
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE
}
```

**What it is:** A Java `enum` (short for "enumeration") — a special type
that represents a **fixed set of constant values**. Here, an employee's
status can only ever be `ACTIVE` or `INACTIVE` — nothing else is possible.

**Why not just use a `String`?** If we used `String status`, nothing stops
someone from typing `"Actve"` (typo) or `"active"` (wrong case) somewhere in
the code, creating inconsistent data. With an enum, the Java compiler itself
prevents invalid values — you can only ever assign `EmployeeStatus.ACTIVE`
or `EmployeeStatus.INACTIVE`, so a typo is a compile error, not a silent bug
found in production months later.

**Likely Q&A:**
> **Q: Could you add a third status later, like "ON_LEAVE"?**
> A: Yes — just add `ON_LEAVE` to the enum. Because we used
> `@Enumerated(EnumType.STRING)`, the database stores the name as text, so
> adding a new enum constant doesn't break existing data (unlike
> `EnumType.ORDINAL`, which stores position numbers and would break if you
> inserted a new value in the middle of the list).

---

## 3. The Repository (DAO) Layer — talking to the database without writing SQL

### 3.1 What is Spring Data JPA doing here?

```java
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findAllByOrderByLastNameAsc();

    Page<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    long countByStatus(EmployeeStatus status);

    @Query("SELECT e.department.name, COUNT(e) FROM Employee e WHERE e.department IS NOT NULL GROUP BY e.department.name")
    List<Object[]> countEmployeesGroupedByDepartment();
}
```

**The single most important thing to understand:** `EmployeeRepository` is
an **interface** — there is no `.java` file anywhere with actual code
implementing these methods! And yet, when the app runs, calling
`employeeRepository.findAll()` really does return a list of employees from
MySQL. How?

At startup, Spring Data JPA scans for interfaces that extend
`JpaRepository`, and **automatically generates a real class at runtime**
that implements the interface, writing all the SQL for you behind the
scenes. This is a form of "magic" that's worth being able to explain simply:

- `extends JpaRepository<Employee, Integer>` — the `<Employee, Integer>`
  part tells Spring "this repository manages `Employee` entities, whose
  primary key type is `Integer`." Just from this line, you get `save()`,
  `findAll()`, `findById()`, `deleteById()`, `count()` for free — no code
  needed.
- `findAllByOrderByLastNameAsc()` — Spring Data JPA parses the **method
  name itself** and builds a query from it. Breaking down the name:
  `findAll` + `OrderBy` + `LastName` + `Asc` → "find all employees, order by
  the lastName field, ascending." This technique is called a **"derived
  query method"** — one of Spring Data JPA's signature features.
- `findByFirstNameContainingIgnoreCaseOr...` — same idea, just a longer
  method name: "find where firstName contains (case-insensitive) OR
  lastName contains OR email contains" — this is what powers the search box.
- `countByStatus(EmployeeStatus status)` — "count employees where status
  equals the given value."
- `@Query(...)` — for the one query too complex to express as a method name
  (grouping employees by department and counting them), we drop down to
  writing **JPQL** (Java Persistence Query Language) directly. JPQL looks
  like SQL but works with entity/field names (`Employee`, `e.department.name`)
  instead of table/column names (`employee`, `department_id`) — Hibernate
  translates it into real SQL underneath.

**Why this matters:** In older Java web frameworks, you had to hand-write
every SQL query and manually map each database row back into a Java object.
Spring Data JPA eliminates almost all of that boilerplate for straightforward
queries, while still allowing hand-written JPQL/SQL for complex cases.

**Likely Q&A:**
> **Q: If there's no implementation class, how does Spring know what SQL to run?**
> A: At application startup, Spring Data JPA creates a **dynamic proxy**
> object that implements the interface. For simple derived-query methods, it
> parses the method name using a defined naming convention and builds the
> query. For `@Query`-annotated methods, it just runs the JPQL you wrote.

> **Q: What is `Pageable` and `Page<Employee>`?**
> A: `Pageable` is an object that bundles together "which page number,"
> "how many results per page," and "how to sort" into one parameter. `Page`
> is the return type — it's not just a `List`, it also carries metadata like
> total number of pages and total number of records, which the UI uses to
> draw the "1 2 Next" pagination buttons.

> **Q: What's the difference between JPQL and SQL?**
> A: SQL operates on database tables and columns (`employee`,
> `department_id`). JPQL operates on Java entities and their fields
> (`Employee`, `e.department`) — it is database-agnostic and gets translated
> into real SQL by Hibernate at runtime, which means the same JPQL would
> work whether the underlying database is MySQL, PostgreSQL, or something
> else.

---

## 4. The Service Layer — where business logic lives

### 4.1 `EmployeeServiceImpl.java` (key excerpt)

```java
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository theEmployeeRepository) {
        employeeRepository = theEmployeeRepository;
    }

    @Override
    public Employee findById(int theId) {
        Optional<Employee> result = employeeRepository.findById(theId);

        if (result.isEmpty()) {
            throw new EmployeeNotFoundException("Did not find employee id - " + theId);
        }

        return result.get();
    }

    @Override
    public void save(Employee theEmployee) {
        log.info("Saving employee: {}", theEmployee);
        employeeRepository.save(theEmployee);
    }
}
```

**Line-by-line:**
- `@Service` — tells Spring "create exactly one instance of this class (a
  'bean') and make it available for injection wherever it's needed." This is
  functionally similar to `@Component`, but `@Service` communicates the
  *intent* that this class holds business logic — good practice for
  readability.
- The **constructor** `EmployeeServiceImpl(EmployeeRepository theEmployeeRepository)`
  is how **Dependency Injection** happens here. Spring sees that
  `EmployeeServiceImpl` needs an `EmployeeRepository` to be constructed, and
  since `EmployeeRepository` is itself a Spring-managed bean (see section 3),
  Spring automatically passes in the right instance when it creates
  `EmployeeServiceImpl`. Note: **we never write `new EmployeeRepository()`
  anywhere** — Spring handles that entirely.
- `Optional<Employee>` — instead of `findById` possibly returning `null`
  (which could later cause a `NullPointerException` if you forget to check),
  Spring Data JPA returns an `Optional`, a container that explicitly forces
  you to handle the "not found" case (`.isEmpty()` / `.get()`).
- Throwing `EmployeeNotFoundException` instead of returning `null` — this
  makes the "not found" case impossible to silently ignore; it will always
  be handled explicitly (see section 6, exception handling).
- `Logger log = LoggerFactory.getLogger(...)` and `log.info(...)` /
  `log.warn(...)` — this is **SLF4J** (Simple Logging Facade for Java), the
  industry-standard way to write log messages in Java, instead of
  `System.out.println`. Log messages can be filtered by severity (`info`,
  `warn`, `error`), timestamped automatically, and redirected to log files —
  none of which `System.out.println` can do.

**Why have a separate Service layer at all — why not let the Controller call
the Repository directly?**
This is one of the most important architecture questions you might be
asked. The answer:
1. **Single Responsibility** — the Controller's job is to handle HTTP
   details; the Service's job is business rules. Mixing them makes the code
   harder to read and maintain.
2. **Reusability** — if we later built a REST API or a scheduled batch job
   that also needs to "find an employee by id and throw a custom exception
   if missing," that logic already exists in the Service and can be reused,
   instead of being duplicated in every Controller.
3. **Testability** — `EmployeeServiceImplTest` can test all this business
   logic completely independently of the web layer or the database (see
   section 7).

**Likely Q&A:**
> **Q: What is Dependency Injection in one sentence?**
> A: Instead of a class creating the objects it depends on with `new`, those
> objects are handed to it from outside (usually via the constructor) — this
> makes classes independent of exactly how their dependencies are built,
> which makes testing and swapping implementations much easier.

> **Q: Why is the field `private final EmployeeRepository employeeRepository`?**
> A: `final` means the reference can only be assigned once (in the
> constructor) and can never be reassigned afterward. This guarantees the
> Service always has a valid Repository reference for its whole lifetime —
> a good defensive-programming practice, and it also allows the field to be
> thread-safe without extra effort.

> **Q: Could you use `@Autowired` on the field instead of the constructor?**
> A: Yes, older Spring code often did `@Autowired private EmployeeRepository
> employeeRepository;` directly on the field. However, **constructor
> injection (what we used) is now considered the best practice**, because it
> allows the field to be `final`, makes the class's dependencies explicit
> and visible in one place, and makes the class much easier to unit test
> (you can just call `new EmployeeServiceImpl(mockRepository)` in a test).

---

## 5. The Controller Layer — where HTTP meets Java

### 5.1 `EmployeeController.java` (key excerpt)

```java
@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;

    public EmployeeController(EmployeeService theEmployeeService, DepartmentService theDepartmentService) {
        employeeService = theEmployeeService;
        departmentService = theDepartmentService;
    }

    @GetMapping("/list")
    public String listEmployees(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "sortBy", defaultValue = "lastName") String sortBy,
            Model theModel) {

        Page<Employee> employeePage = employeeService.search(keyword, page, size, sortBy);

        theModel.addAttribute("employeePage", employeePage);
        theModel.addAttribute("employees", employeePage.getContent());
        ...
        return "employees/list-employees";
    }

    @PostMapping("/save")
    public String saveEmployee(@Valid @ModelAttribute("employee") Employee theEmployee,
                                BindingResult bindingResult,
                                Model theModel,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            theModel.addAttribute("departments", departmentService.findAll());
            return "employees/employee-form";
        }

        boolean isNewEmployee = (theEmployee.getId() == 0);
        employeeService.save(theEmployee);

        redirectAttributes.addFlashAttribute("successMessage", ...);

        return "redirect:/employees/list";
    }
}
```

**Line-by-line:**

- `@Controller` — tells Spring "this class handles web requests and returns
  the *name* of a view (an HTML template) to render." (Contrast with
  `@RestController`, used in REST APIs, which returns raw data like JSON
  instead of a view name.)
- `@RequestMapping("/employees")` — a **class-level** annotation that says
  "every method in this class handles a URL that starts with `/employees`."
  This avoids repeating `/employees` in every method below.
- `@GetMapping("/list")` — combined with the class-level mapping, this
  method specifically handles `GET /employees/list` requests. `GET` is the
  HTTP method browsers use when you click a link or type a URL — it should
  never change data on the server, only retrieve it.
- `@RequestParam(value = "keyword", required = false)` — reads a query
  string parameter, e.g. `?keyword=ross` from the URL
  `/employees/list?keyword=ross`. `required = false` means it's fine if the
  parameter is missing (search box was left blank).
- `@RequestParam(value = "page", defaultValue = "0")` — if no `page`
  parameter is given in the URL, default to page `0` (Spring's pagination is
  zero-indexed).
- `Model theModel` — Spring automatically creates and passes in this object;
  anything added to it with `.addAttribute("name", value)` becomes available
  inside the Thymeleaf template under that name, e.g., `${employees}` in the
  HTML.
- The method **returns a `String`**: `"employees/list-employees"`. This is
  **not** the actual content sent to the browser — it's the **logical view
  name**. Spring's `ViewResolver` (configured automatically thanks to the
  Thymeleaf starter dependency) translates this into the real file path:
  `src/main/resources/templates/employees/list-employees.html`.
- `@PostMapping("/save")` — handles `POST /employees/save`, used when the
  add/edit form is submitted. `POST` is the correct HTTP method to use when
  data will be created/changed on the server (unlike `GET`, which should be
  "safe" and not change anything).
- `@Valid @ModelAttribute("employee") Employee theEmployee` — this is where
  the validation from Section 2 actually gets triggered:
  - `@ModelAttribute("employee")` tells Spring "take all the submitted form
    fields (firstName, lastName, email, etc.) and automatically build an
    `Employee` object out of them" (this process is called **data binding**).
  - `@Valid` tells Spring "after building that object, run all its Bean
    Validation annotations (`@NotBlank`, `@Email`, etc.) and record any
    failures."
- `BindingResult bindingResult` — **must appear immediately after** the
  `@Valid` parameter (this is a strict Spring rule). It captures the results
  of the validation check. If we didn't include this parameter, Spring would
  instead throw an exception and show an error page whenever validation
  failed — by including it, we get the chance to handle the failure
  gracefully ourselves.
- `if (bindingResult.hasErrors())` — if any validation rule failed, we
  redisplay the same form (returning the *same* view name,
  `"employees/employee-form"`) instead of saving. Because the invalid
  `theEmployee` object (with its error messages) is automatically still
  available to that view, the form can show exactly which fields were wrong.
- `redirectAttributes.addFlashAttribute("successMessage", ...)` — a "flash"
  attribute survives exactly one redirect and is then discarded. This is
  what makes the green "Employee added successfully" banner appear once,
  right after saving, without needing to store anything in a database or
  session permanently.
- `return "redirect:/employees/list"` — the `"redirect:"` prefix is special:
  instead of rendering a view directly, Spring tells the *browser* to make a
  brand new `GET` request to `/employees/list`. This is the
  **Post/Redirect/Get (PRG) pattern**.

**Why PRG (Post/Redirect/Get) matters — a classic exam question:**
If we simply rendered the list view directly after a `POST` (without
redirecting), and the user hit the browser's Refresh button, the browser
would re-submit the exact same `POST` request — potentially creating a
duplicate employee record. By redirecting to a fresh `GET` request instead,
refreshing the page afterward just re-runs a safe `GET`, with no risk of
duplicate submissions.

**Likely Q&A:**
> **Q: What's the difference between `@Controller` and `@RestController`?**
> A: `@Controller` methods return the *name* of an HTML view to render (used
> for a traditional server-rendered website, like this project).
> `@RestController` (which is `@Controller` + `@ResponseBody` combined)
> methods return data directly (e.g., JSON), used for building REST APIs
> consumed by JavaScript front-ends or mobile apps.

> **Q: Why use `GET` for delete instead of the more "correct" HTTP `DELETE` verb?**
> A: In this project (matching the original tutorial's simple design), delete
> is triggered by clicking a plain HTML link (`<a href="...">Delete</a>`),
> and plain HTML links can only ever issue `GET` requests — HTML forms
> support `GET`/`POST` only, not `PUT`/`DELETE`, without extra JavaScript. A
> more RESTful design would use JavaScript (fetch/AJAX) to send a real
> `DELETE` request instead. This is a known simplification and a good thing
> to mention proactively as a "future improvement" if asked.

> **Q: What exactly does `Model` do?**
> A: It's essentially a `Map<String, Object>` that Spring creates for each
> request. Whatever you put into it in the Controller becomes available by
> that same name inside the Thymeleaf template.

---

## 6. Exception Handling — `GlobalExceptionHandler.java`

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public String handleEmployeeNotFound(EmployeeNotFoundException ex, Model model) {
        log.error("Employee not found: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unexpected error occurred", ex);
        model.addAttribute("errorMessage", "Something went wrong...");
        return "error";
    }
}
```

**What it is:** Normally, if code in a Controller throws an exception and
nothing catches it, Spring Boot shows its default "Whitelabel Error Page" —
an ugly generic error screen (or, worse, a raw stack trace, which could leak
sensitive information about your code to end users).

**Line-by-line:**
- `@ControllerAdvice` — a special annotation that means "this class's
  `@ExceptionHandler` methods apply globally, across **every** Controller in
  the application," not just one. This is called a **cross-cutting
  concern** — behavior that applies broadly across the app rather than being
  tied to one specific feature.
- `@ExceptionHandler(EmployeeNotFoundException.class)` — "if any Controller
  method throws this specific exception type, run this method instead of
  crashing."
- The more general `@ExceptionHandler(Exception.class)` acts as a catch-all
  safety net for anything unexpected we didn't specifically plan for.
- Both methods build a friendly message and return `"error"` — Spring
  renders `templates/error.html`, a clean page instead of a stack trace.

**Why this matters (a great "professional practices" talking point):**
Real applications never want to show users a raw Java stack trace — it looks
unprofessional and can expose internal implementation details that could be
a security risk. Centralizing all error handling in one place also means we
don't have to repeat `try/catch` blocks in every single Controller method.

**Likely Q&A:**
> **Q: What would happen if `GlobalExceptionHandler` didn't exist at all?**
> A: If `EmployeeService.findById()` threw `EmployeeNotFoundException` and
> nothing caught it, Spring Boot's default behavior would kick in: it would
> return an HTTP 500 error and show its generic Whitelabel Error Page (or a
> stack trace, depending on configuration).

> **Q: Is `@ControllerAdvice` similar to a try/catch block?**
> A: Conceptually, yes — it serves the same purpose (catching and handling
> exceptions), but instead of writing a `try/catch` around every single
> method in every Controller, you write the handling logic **once**, and
> Spring applies it everywhere automatically.

---

## 7. Custom Exceptions — `EmployeeNotFoundException`, `DepartmentNotFoundException`

```java
public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
```

**What it is:** A custom exception class. `extends RuntimeException` means
it's an "unchecked" exception — the Java compiler does not force calling
code to explicitly `catch` it or declare `throws EmployeeNotFoundException`
(unlike a "checked" exception like `IOException`).

**Why create a custom exception instead of just using `RuntimeException`
directly?**
1. **Clarity** — `throw new EmployeeNotFoundException(...)` documents
   exactly what went wrong, right there in the code.
2. **Selective handling** — `GlobalExceptionHandler` can specifically catch
   `EmployeeNotFoundException` differently from other unrelated errors (e.g.
   a database connection failure), because they are now distinguishable
   types.

**Likely Q&A:**
> **Q: What's the difference between a checked and an unchecked exception?**
> A: A **checked** exception (extends `Exception` but not `RuntimeException`)
> must be either caught or declared in the method signature (`throws ...`) —
> the compiler enforces this. An **unchecked** exception (extends
> `RuntimeException`) does not require this — it can be thrown and will
> propagate up the call stack until something catches it, without the
> compiler forcing you to handle it everywhere.

---

## 8. Security — The Dynamic Permission Model

This is the most architecturally significant part of the whole project, so
it gets the longest treatment. The system evolved from a fixed-role model
(`ROLE_EMPLOYEE`/`ROLE_MANAGER`/`ROLE_ADMIN`) to a **single Super Admin +
individually-granted permissions** model. Here's every piece involved.

### 8.1 `Permission.java` — the catalog of grantable actions

```java
@Entity
@Table(name = "permission")
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String code;          // e.g. "CAN_ADD_EMPLOYEE"

    private String displayName;   // e.g. "Add Employee"
    private String description;
    private String category;      // e.g. "Employee Management"
}
```

**Why a database table instead of a Java `enum`?** Because the whole point
of this design is that granting/revoking a permission should never require
changing code. Storing permissions as rows means the "grant privilege" UI
can simply list whatever exists in the table, and adding a brand new
grantable action later is just one new row (plus one new constant in
`PermissionCode` so `@PreAuthorize` checks stay typo-proof) — no
`SecurityConfig` changes required.

### 8.2 `AppUser.java` — login accounts, separate from `Employee`

```java
@Entity
@Table(name = "app_user")
public class AppUser {
    private String username;
    private String password;       // BCrypt hash, never plain text
    private boolean superAdmin;    // true for EXACTLY one row in the system

    @OneToOne
    private Employee employee;     // optional link to an HR record
}
```

**Why is this a separate entity from `Employee`?** A login account
(*"who can sign in and what can they do"*) is a fundamentally different
concept from an HR record (*"this person's salary, department, hire
date"*). The Super Admin is the clearest example: they need to log in and
manage the system, but have no HR record at all — `employee` stays `null`
for that account. Not every `Employee` needs a login either. Keeping them
separate, linked optionally, is the standard way real systems separate
*authentication* concerns from *business data*.

### 8.3 `UserPermission.java` — the join entity WITH an audit trail

```java
@Entity
@Table(name = "user_permission")
public class UserPermission {
    @ManyToOne private AppUser user;
    @ManyToOne private Permission permission;
    @ManyToOne private AppUser grantedBy;     // who granted it
    private LocalDateTime grantedAt;          // when
}
```

**Why not just a plain `@ManyToMany` between `AppUser` and `Permission`?**
A bare many-to-many would only answer "does this user have this
permission?" — it couldn't answer "**who** gave them this permission, and
**when**?" Making the join itself a full entity (a technique sometimes
called an "association class") lets us store that extra information. This
is exactly the kind of audit trail a real business system needs to be able
to answer questions like "who authorized Monica to delete employee
records?" months later.

### 8.4 `CustomUserDetailsService.java` — turning permissions into Spring Security authorities

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)...;

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (appUser.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("SUPER_ADMIN"));
        }
        for (UserPermission up : userPermissionRepository.findByUser(appUser)) {
            authorities.add(new SimpleGrantedAuthority(up.getPermission().getCode()));
        }

        return new User(appUser.getUsername(), appUser.getPassword(),
                appUser.isEnabled(), true, true, true, authorities);
    }
}
```

**What this replaces:** the earlier version of this project used Spring
Security's built-in `JdbcUserDetailsManager`, which expects a fixed
`users`/`authorities` table pair populated with static `ROLE_*` strings.
This class replaces that entirely with **custom logic**: every time someone
logs in, it looks up whatever `UserPermission` rows currently exist for
them **right now** and turns each one into a Spring Security "authority" —
a plain string like `"CAN_ADD_EMPLOYEE"` that `@PreAuthorize` checks
against.

**Why `@Transactional(readOnly = true)` matters here (a real bug we found
and fixed):** `AppUser.grantedPermissions` and `UserPermission.permission`
are `LAZY`-loaded associations — Hibernate doesn't actually fetch them from
the database until you call a getter on them. Without `@Transactional`
keeping the database connection ("Hibernate session") open for the whole
method, calling `up.getPermission().getCode()` after the initial query has
already finished would throw a `LazyInitializationException`. This is a
classic, very common real-world Spring + Hibernate bug, and recognizing it
is a great thing to be able to explain if asked.

### 8.5 `SecurityConfig.java` — wiring it all together

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ...) {
        http.authorizeHttpRequests(configurer -> configurer
            .requestMatchers("/", "/login", ...).permitAll()
            .requestMatchers("/admin/**").hasAuthority("SUPER_ADMIN")
            .anyRequest().authenticated());

        http.formLogin(form -> form.loginPage("/login").successHandler(...));
        http.sessionManagement(session -> session
            .maximumSessions(1).sessionRegistry(sessionRegistry()));

        return http.build();
    }
}
```

**Line-by-line, what's new compared to a basic setup:**
- `@EnableMethodSecurity(prePostEnabled = true)` — turns on support for
  `@PreAuthorize` annotations directly on Controller methods, which is how
  most of the fine-grained permission checks in this app are actually
  enforced (see `EmployeeController`, `DepartmentController`, etc.).
- `.requestMatchers("/admin/**").hasAuthority("SUPER_ADMIN")` — a URL-level
  rule that blocks *everyone except* the Super Admin from the entire
  `/admin/**` area, as a first line of defense before individual
  `@PreAuthorize` checks on `AdminController`'s methods even run.
- `SessionRegistry` + `.maximumSessions(1)` — this is what makes permission
  changes take effect **immediately**. Explained fully in section 8.6.
- `.successHandler(...)` — after a successful login, instead of always
  going to one fixed page, a custom handler (`PrivilegeBasedLoginSuccessHandler`)
  decides where to send the user based on what they're actually allowed to
  do (explained in section 8.7).

### 8.6 Why permission changes needed to force a re-login (a real bug we caught)

By default, Spring Security computes a user's authorities **once**, at
login time, and keeps using that same list for their entire session — even
if an admin changes their permissions five seconds later. We discovered
this by testing: after revoking a permission from a logged-in user, they
could *still* perform that action, because their browser session still
held the old (now stale) list of authorities.

**The fix**, in `UserManagementServiceImpl`:

```java
private void forceReauthentication(String username) {
    for (Object principal : sessionRegistry.getAllPrincipals()) {
        if (matchesUsername(principal, username)) {
            for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                info.expireNow();
            }
        }
    }
}
```

Called at the end of `grantPermission()`, `revokePermission()`, and
`setEnabled()`. `SessionRegistry` (registered as a bean in
`SecurityConfig`) keeps track of every currently logged-in user's session.
`info.expireNow()` marks that session as expired; the very next request
that user makes gets redirected to `/login?expired`, and logging back in
triggers `CustomUserDetailsService` to recompute their authorities from
scratch — picking up the change instantly.

**Why this matters for your presentation:** this is a genuinely subtle,
realistic bug that even experienced developers can miss, and finding +
fixing it (with a test to prove it) is a strong example of understanding
*how* a framework really works, not just copying tutorial code.

### 8.7 `PrivilegeBasedLoginSuccessHandler.java` — redirect by privilege, not role

```java
@Component
public class PrivilegeBasedLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication) {

        String targetUrl = determineTargetUrl(authentication);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        // SUPER_ADMIN -> /admin/dashboard
        // any CAN_* management authority -> /dashboard
        // otherwise -> /profile
    }
}
```

This class inspects the freshly-authenticated user's authorities (computed
by `CustomUserDetailsService` moments earlier) and decides which page to
send them to. A user with zero granted permissions has no business seeing
a "staff dashboard" full of buttons that would all just deny them anyway —
they land on a simple, honest self-service `/profile` page instead.

### 8.8 The single-Super-Admin guarantee, enforced twice

1. **At seed time**: `DataInitializer` (a `CommandLineRunner`, which Spring
   Boot automatically runs once at every startup) checks
   `appUserRepository.existsBySuperAdminTrue()` and does nothing if one
   already exists — so restarting the app never creates a second one.
2. **In business logic**: `UserManagementServiceImpl.createStaffUser()`
   has no method parameter for "is this a super admin" at all — it is
   *structurally impossible* to create a second one through that method,
   even by accident. `grantPermission()` and `setEnabled()` also explicitly
   throw `IllegalArgumentException` if ever called against the Super Admin
   account.

**Likely Q&A:**

> **Q: What's the difference between a "role" and a "permission" in this system?**
> A: There is no concept of a "role" left in the code at all (other than
> the single special `SUPER_ADMIN` authority). Every capability is an
> individually grantable `Permission`. Two users could end up with
> completely different, overlapping sets of permissions without either of
> them being assigned to a named "role" — which is the defining
> characteristic of an action-based (as opposed to role-based) access
> control system.

> **Q: If I hid the Delete button in HTML for non-admins, would that be secure?**
> A: No — hiding a button is a UX nicety only. The real security boundary
> is `@PreAuthorize` on the Controller method itself; even a hand-crafted
> HTTP request to that URL is rejected with a 403, regardless of what the
> HTML looked like.

> **Q: How are passwords protected?**
> A: Hashed with BCrypt (`PasswordEncoder` bean in `SecurityConfig`) before
> being stored — never in plain text. `DaoAuthenticationProvider` uses the
> same encoder to check a submitted password against the stored hash at
> login time.

> **Q: Why use `SessionRegistry` instead of just telling users to log out and back in?**
> A: In a real business system, you can't rely on a demoted or disabled
> user to voluntarily log out — if their access needs to be revoked (e.g.
> they left the company), it needs to take effect immediately, not "when
> they get around to it".

---

## 9. The Business Logic & Controllers Behind the Permission System

### 8B.1 `DataInitializer.java` — seeding the ONE super admin, safely

```java
@Component
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        seedPermissionCatalog();
        seedSingleSuperAdmin();
    }

    private void seedSingleSuperAdmin() {
        if (appUserRepository.existsBySuperAdminTrue()) {
            return; // already exists - never create a second one
        }
        AppUser superAdmin = new AppUser();
        superAdmin.setUsername(superAdminUsername);
        superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
        superAdmin.setSuperAdmin(true);
        appUserRepository.save(superAdmin);
    }
}
```

**What is `CommandLineRunner`?** It's a Spring Boot interface with a single
`run()` method that Spring automatically calls exactly once, immediately
after the application has finished starting up. It's the standard way to
run "do this once at startup" logic (seeding data, warming up a cache,
printing a startup banner) without putting that logic inside `main()`.

**Why is this idempotent (safe to run every single restart)?** The very
first line checks `existsBySuperAdminTrue()` and returns immediately if a
super admin already exists. We tested this directly: restarting the
application twice in a row produced exactly one super admin account both
times, never two.

**Where do the actual username/password come from?** `@Value("${app.super-admin.username:superadmin}")`
reads from `application.properties`, which itself reads from an
environment variable with a fallback default:
`app.super-admin.username=${SUPER_ADMIN_USERNAME:superadmin}`. This is the
same "environment variable with a safe local default" pattern used for the
database credentials — real secrets never need to be hard-coded anywhere.

### 8B.2 `UserManagementService(Impl).java` — the only code path that can touch accounts/permissions

```java
public interface UserManagementService {
    AppUser createStaffUser(String username, String rawPassword,
            String fullName, String email, Integer employeeId);
    void grantPermission(int targetUserId, int permissionId, AppUser grantedByUser);
    void revokePermission(int targetUserId, int permissionId);
    void setEnabled(int userId, boolean enabled);
}
```

Notice `createStaffUser()` has **no parameter at all** for "make this a
super admin". This is a deliberate design choice called making an invalid
state *unrepresentable* — rather than adding a boolean parameter and
trusting every caller to always pass `false`, the method signature itself
makes it structurally impossible to create a second super admin through
this path.

The implementation also explicitly guards the existing super admin:

```java
public void grantPermission(int targetUserId, int permissionId, AppUser grantedByUser) {
    AppUser targetUser = findById(targetUserId);
    if (targetUser.isSuperAdmin()) {
        throw new IllegalArgumentException("Cannot modify permissions for the super admin account.");
    }
    ...
}
```

Even though the UI never offers a "grant privilege" button for the super
admin account, this check exists anyway — a principle called **defense in
depth**: never rely on only one layer (the UI) to enforce an important
rule, because UIs can have bugs, and APIs can be called directly.

### 8B.3 `AdminController.java` — the Super Admin's system-settings area

```java
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminController {

    @PostMapping("/privileges/grant")
    public String grantPrivilege(@ModelAttribute GrantPermissionRequest request,
                                  RedirectAttributes redirectAttributes) {
        AppUser currentAdmin = currentUserService.getCurrentUser();
        userManagementService.grantPermission(request.getUserId(), request.getPermissionId(), currentAdmin);
        ...
        return "redirect:/admin/privileges";
    }
}
```

Notice `@PreAuthorize` is placed on the **class itself**, not on each
individual method — this applies the same check to *every* method in the
controller at once, which makes sense here because the entire `/admin/**`
area is Super-Admin-only. (Contrast this with `EmployeeController`, where
different methods need *different* permissions, so each method has its own
`@PreAuthorize`.)

`GrantPermissionRequest` (in the `dto` package) is a tiny class with just
`userId` and `permissionId` fields — used purely to bind the grant-privilege
form's two hidden inputs. Using a small dedicated DTO instead of binding
directly to the `AppUser` or `Permission` entities here means the form can
never accidentally set some other field on those entities that it has no
business touching.

### 8B.4 `PublicController.java` — the pages anyone can see without logging in

```java
@Controller
public class PublicController {
    @GetMapping("/")
    public String landingPage() { return "public/landing"; }

    @GetMapping("/login")
    public String loginPage() { return "public/login"; }

    @GetMapping("/access-denied")
    public String accessDenied() { return "public/access-denied"; }
}
```

This tiny controller is important precisely *because* it's so simple:
notice it never injects `EmployeeService`, `DepartmentService`, or any
other business-data service. That's not an accident — it's what guarantees
the public landing page can never accidentally leak business data, even if
someone later modifies the template carelessly. Compare this to
`DashboardController`, which *does* inject those services and is gated
with `@PreAuthorize`.

### 8B.5 `ProfileController.java` — the plain-employee self-service view

```java
@Controller
public class ProfileController {
    @GetMapping("/profile")
    public String viewProfile(Model model) {
        AppUser currentUser = currentUserService.getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("employee", currentUser != null ? currentUser.getEmployee() : null);
        return "profile";
    }
}
```

This page is **not** gated by any specific `CAN_*` permission — any
authenticated user can view their own profile (it's the fallback landing
page for users with zero granted permissions, chosen by
`PrivilegeBasedLoginSuccessHandler`). It only ever shows information about
the currently logged-in user (via `CurrentUserService`), never any other
employee's data.

### 8B.6 `ReportsController.java` and `AttendanceController.java` — permission independence in action

```java
@GetMapping("/reports")
@PreAuthorize("hasAnyAuthority('CAN_VIEW_REPORTS','SUPER_ADMIN')")
public String reports(Model model) { ... }
```

```java
@RequestMapping("/attendance")
@PreAuthorize("hasAnyAuthority('CAN_MANAGE_ATTENDANCE','SUPER_ADMIN')")
public class AttendanceController { ... }
```

These two controllers exist specifically to prove out the "action-based,
not role-based" claim: a user can be granted `CAN_VIEW_REPORTS` **without**
also being able to add, edit, or delete a single employee — something that
would have been impossible to express cleanly in the old fixed
Employee/Manager/Admin role model, where "can see reports" and "can edit
records" were bundled together into the same role.

### 8B.7 `CurrentUserService.java` — bridging Spring Security and our own entities

```java
@Service
public class CurrentUserService {
    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return appUserRepository.findByUsername(username).orElse(null);
    }
}
```

Spring Security's `SecurityContextHolder` only knows a logged-in user's
**username** (a plain `String`) and their authorities — it has no idea our
application even has an `AppUser` entity. This tiny helper bridges that
gap: "given whoever Spring Security says is logged in right now, fetch the
matching row from our own database" — used anywhere a Controller needs the
full `AppUser` object (e.g. to record who granted a permission, or to show
`/profile`'s data).

### 8B.8 New Exception Types

`UserNotFoundException`, `PermissionNotFoundException`, and
`DuplicateUsernameException` all follow the exact same pattern already
established by `EmployeeNotFoundException` (see Section 7): small
`RuntimeException` subclasses, each caught by its own handler method in
`GlobalExceptionHandler`, so every "this went wrong" scenario in the new
admin/permission features gets the same friendly error page treatment as
the original employee/department features — no raw stack traces anywhere
in the whole application, old or new.

One addition worth highlighting in `GlobalExceptionHandler`:

```java
@ExceptionHandler(AccessDeniedException.class)
@ResponseStatus(HttpStatus.FORBIDDEN)
public String handleAccessDenied(AccessDeniedException ex, Model model) {
    model.addAttribute("errorMessage", "You do not have permission to perform this action.");
    return "error";
}
```

`AccessDeniedException` is thrown automatically by Spring Security whenever
a `@PreAuthorize` check fails. `@ResponseStatus(HttpStatus.FORBIDDEN)`
ensures the browser actually receives a proper HTTP `403` status code (not
just a `200` with an error-looking page) — this was a real bug we caught
during testing: without this annotation, `@ControllerAdvice` methods
default to returning `200 OK` even when displaying an error, which is
technically incorrect and something a security-conscious reviewer (or
browser dev tools) would immediately flag.

---

## 10. The Converter — `DepartmentIdToDepartmentConverter.java`

```java
@Component
public class DepartmentIdToDepartmentConverter implements Converter<String, Department> {

    private final DepartmentRepository departmentRepository;

    public DepartmentIdToDepartmentConverter(DepartmentRepository theDepartmentRepository) {
        departmentRepository = theDepartmentRepository;
    }

    @Override
    public Department convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        int id = Integer.parseInt(source);
        return departmentRepository.findById(id).orElse(null);
    }
}
```

**The problem this solves:** HTML forms can only submit plain text. The
Employee form's Department `<select>` dropdown submits something like
`department=2` — just the number `2` as a string. But the `Employee` class
expects `department` to be a full `Department` **object** (with a name,
location, etc.), not a plain number.

**Line-by-line:**
- `@Component` — a general-purpose Spring annotation (like `@Service` but
  without a more specific meaning) — Spring should manage this class as a
  bean.
- `implements Converter<String, Department>` — this interface, from Spring,
  says "I know how to turn a `String` into a `Department`." Once registered
  as a bean, Spring's data-binding system (the same one that builds the
  `Employee` object from form fields, discussed in Section 5) automatically
  discovers and uses this converter whenever it needs to assign a submitted
  string value to a field of type `Department`.
- Inside `convert()`, we parse the string into an integer id, then use the
  `DepartmentRepository` to actually look up the real `Department` row.

**Why this matters:** Without this converter, Spring would have no idea how
to turn `"2"` into a `Department` object and would throw a data-binding
error when the form was submitted.

**Likely Q&A:**
> **Q: Where does Spring "find" this converter automatically?**
> A: Because the class is annotated with `@Component`, it gets picked up by
> Spring's component scanning at startup (see Section 1) and automatically
> registered into Spring MVC's data-binding/conversion system — no manual
> wiring required elsewhere.

---

## 11. Thymeleaf Templates — the View layer

Although `.html` files aren't Java, your lecturer may still ask about how
the view layer connects to the Java code, so it's worth understanding a few
key attributes:

```html
<tr th:each="tempEmployee : ${employees}">
    <td th:text="${tempEmployee.firstName}"></td>
</tr>

<form th:action="@{/employees/save}" th:object="${employee}" method="POST">
    <input type="text" th:field="*{firstName}">
</form>

<a sec:authorize="hasRole('ADMIN')" th:href="@{/employees/delete(employeeId=${tempEmployee.id})}">Delete</a>
```

- `th:each="tempEmployee : ${employees}"` — a loop, similar to Java's
  enhanced `for` loop (`for (Employee tempEmployee : employees)`). It
  repeats the `<tr>` once per item in the `employees` list that the
  Controller put into the `Model`.
- `th:text="${tempEmployee.firstName}"` — Thymeleaf calls
  `tempEmployee.getFirstName()` behind the scenes (Thymeleaf automatically
  understands that `.firstName` in the template means "call the getter") and
  inserts the returned text into the HTML.
- `th:object="${employee}"` + `th:field="*{firstName}"` — this is
  **two-way data binding**: it pre-fills the input with the current value of
  `employee.getFirstName()` when displaying the form, and when the form is
  submitted, Spring automatically maps the submitted value back onto
  `Employee.firstName` (this is what powers the `@ModelAttribute` binding
  discussed in Section 5).
- `sec:authorize="hasRole('ADMIN')"` — comes from a separate library
  (`thymeleaf-extras-springsecurity6`) that lets a template check the
  current logged-in user's roles and conditionally show/hide HTML.
- `@{/employees/delete(employeeId=${tempEmployee.id})}` — Thymeleaf's URL
  syntax; it builds a proper URL like `/employees/delete?employeeId=3`.

**Likely Q&A:**
> **Q: Is Thymeleaf a programming language?**
> A: No — it's a **template engine**. The `.html` files are still normal
> HTML files, just with extra `th:*` attributes that Thymeleaf processes on
> the server before sending the final, plain HTML to the browser. The
> browser itself never sees any `th:*` attributes.

---

## 12. Testing — `EmployeeServiceImplTest.java`

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void findById_throwsException_whenNotFound() {
        when(employeeRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.findById(999));
    }
}
```

**Line-by-line:**
- `@ExtendWith(MockitoExtension.class)` — tells JUnit 5 to enable Mockito
  features (like `@Mock`) for this test class.
- `@Mock private EmployeeRepository employeeRepository;` — Mockito creates a
  **fake** `EmployeeRepository` — an object that looks and behaves like the
  real one from the outside, but doesn't actually connect to any real
  database. We can control exactly what it returns.
- `@InjectMocks private EmployeeServiceImpl employeeService;` — Mockito
  automatically creates a real `EmployeeServiceImpl` and "injects" the fake
  `employeeRepository` into it (via the constructor), simulating what Spring
  would normally do, but entirely outside of a real running application.
- `when(employeeRepository.findById(999)).thenReturn(Optional.empty());` —
  this line **programs the fake**: "when someone calls
  `findById(999)` on this mock, pretend nothing was found."
- `assertThrows(EmployeeNotFoundException.class, () -> employeeService.findById(999));`
  — runs `employeeService.findById(999)` and asserts (checks) that it throws
  `EmployeeNotFoundException`. If it doesn't throw that exception, the test
  fails.

**Why this matters — Unit test vs Integration test (a very common
question):**
- A **unit test** (this one) tests a single class in isolation, using fakes
  ("mocks") for its dependencies. It's extremely fast (no real database
  connection) and only tests the Service's own logic.
- An **integration test** (like `ThymeleafdemoApplicationTests`, annotated
  with `@SpringBootTest`) starts the *entire* Spring application, including
  a real database connection, and tests how multiple real components work
  together. It's slower but catches problems that mocks might hide (e.g., a
  real SQL error).

**Likely Q&A:**
> **Q: Why not just test against the real database instead of mocking it?**
> A: You could (that's what an integration test does), but unit tests with
> mocks are much faster to run (no network/database I/O), and they isolate
> failures — if this test fails, you know the bug is in `EmployeeServiceImpl`'s
> logic itself, not in the database, network, or some other component.

> **Q: What library provides `assertThrows`?**
> A: JUnit 5's `org.junit.jupiter.api.Assertions` class.

---

## 13. Quick-Reference Cheat Sheet — Annotations Glossary

| Annotation | Layer | Meaning |
|---|---|---|
| `@SpringBootApplication` | Main class | Enables auto-configuration + component scanning |
| `@Entity` / `@Table` | Entity | Maps a Java class to a database table |
| `@Id` / `@GeneratedValue` | Entity | Marks the primary key and how it's generated |
| `@Column` | Entity | Maps a Java field to a database column |
| `@ManyToOne` / `@OneToMany` | Entity | Defines relationships between tables |
| `@NotBlank` / `@Email` / `@DecimalMin` / etc. | Entity | Bean Validation rules |
| `@Enumerated` | Entity | How to store a Java enum in the database |
| `@Repository` (implied by `JpaRepository`) | DAO | Marks a data-access interface |
| `@Query` | DAO | Custom JPQL query |
| `@Service` | Service | Marks a business-logic class |
| `@Controller` | Controller | Marks a class that returns view names |
| `@RequestMapping` / `@GetMapping` / `@PostMapping` | Controller | Maps URLs + HTTP methods to methods |
| `@RequestParam` | Controller | Reads a query string parameter |
| `@ModelAttribute` | Controller | Binds form fields to a Java object |
| `@Valid` | Controller | Triggers Bean Validation checks |
| `@ControllerAdvice` / `@ExceptionHandler` | Cross-cutting | Global error handling |
| `@Configuration` / `@Bean` | Config | Manually defines Spring-managed objects |
| `@Component` | Config | General-purpose "let Spring manage this" |
| `@Mock` / `@InjectMocks` / `@ExtendWith` | Testing | Mockito/JUnit 5 test annotations |
| `@EnableMethodSecurity` | Security config | Turns on `@PreAuthorize` support |
| `@PreAuthorize("hasAuthority('CAN_...')")` | Controller | Blocks a method unless the user has that granted permission |
| `@Transactional(readOnly = true)` | Security/Service | Keeps a Hibernate session open so lazy associations (e.g. granted permissions) can be read safely |
| `@ResponseStatus` | Exception handling | Sets the real HTTP status code an `@ExceptionHandler` returns |
| `CommandLineRunner` (interface, not annotation) | Startup | Runs once automatically right after the app starts (`DataInitializer`) |

---

## 14. "Big Picture" Questions Your Lecturer Might Ask

> **Q: Walk me through what happens, end-to-end, when a user clicks "Save" on the Add Employee form.**
> A:
> 1. The browser sends an HTTP `POST` request to `/employees/save` with the
>    form fields as data.
> 2. Spring MVC routes this to `EmployeeController.saveEmployee()` because
>    of the `@PostMapping("/save")` annotation.
> 3. Spring automatically builds an `Employee` object from the submitted
>    fields (`@ModelAttribute`), using the `DepartmentIdToDepartmentConverter`
>    to resolve the department dropdown value into a real `Department` object.
> 4. `@Valid` triggers all the Bean Validation annotations on `Employee`
>    (`@NotBlank`, `@Email`, etc.). Any failures are captured in
>    `BindingResult`.
> 5. If there are validation errors, the same form view is returned,
>    showing the errors.
> 6. If validation passes, the Controller calls `employeeService.save(...)`.
> 7. The Service logs the action and calls
>    `employeeRepository.save(...)`.
> 8. Spring Data JPA (via Hibernate) generates and runs an `INSERT` (or
>    `UPDATE`) SQL statement against MySQL.
> 9. The Controller adds a flash "success" message and returns
>    `"redirect:/employees/list"`.
> 10. The browser follows the redirect with a fresh `GET` request, which
>     re-renders the employee list, now showing the new employee and the
>     success banner.

> **Q: Why did you choose Spring Boot instead of writing raw Servlets/JSP?**
> A: Spring Boot dramatically reduces boilerplate: it auto-configures the
> web server, database connections, and templating; provides Dependency
> Injection to decouple classes; and includes production-ready features
> (security, validation, data access) as pre-built, well-tested modules
> instead of us writing them from scratch. This lets a small team focus on
> business logic instead of infrastructure code — which is exactly why it's
> the dominant Java web framework in the industry today.

> **Q: What would you change if this had to support 100,000 employees?**
> A: The pagination and lazy-loading (`FetchType.LAZY`) already in place
> would help. I would also add database indexes on frequently searched
> columns (email, last name), consider caching for the dashboard statistics,
> and potentially move to asynchronous CSV export for very large exports
> (currently it streams everything in one request).

> **Q: Is this application secure against SQL Injection?**
> A: Yes — because we use Spring Data JPA / Hibernate (an ORM) rather than
> building raw SQL strings by concatenating user input, all queries use
> parameterized statements under the hood, which is the standard defense
> against SQL injection.

> **Q: Walk me through what happens when the Super Admin grants a privilege to a staff member.**
> A:
> 1. The Super Admin opens `/admin/privileges`, clicks "Grant Privilege" on
>    a user's card, and selects a permission in the modal.
> 2. The form submits to `AdminController.grantPrivilege()`, which is
>    protected by `@PreAuthorize("hasAuthority('SUPER_ADMIN')")` at the
>    class level — only the one true Super Admin account can even reach
>    this method.
> 3. The controller calls `UserManagementService.grantPermission(userId, permissionId, currentAdmin)`.
> 4. The service loads the target `AppUser`, refuses to continue if that
>    user happens to be the super admin (defense in depth), then saves a
>    new `UserPermission` row recording the user, the permission, who
>    granted it, and when.
> 5. The service then calls `forceReauthentication()`, which uses the
>    `SessionRegistry` to immediately expire that user's current login
>    session, if they have one.
> 6. The next time that user makes any request, Spring Security redirects
>    them to `/login?expired`.
> 7. When they log back in, `CustomUserDetailsService` reads their
>    `UserPermission` rows fresh from the database and builds a brand new
>    list of authorities — now including the newly granted permission.
> 8. `PrivilegeBasedLoginSuccessHandler` sends them to `/dashboard` (since
>    they now have at least one management permission), where they can
>    immediately use their new privilege.

> **Q: Why does the Super Admin never need any `CAN_*` permission granted to themselves?**
> A: Every `@PreAuthorize` check in the application uses
> `hasAnyAuthority('CAN_...', 'SUPER_ADMIN')` — the `SUPER_ADMIN` authority
> alone always satisfies the check, regardless of which specific business
> permissions exist. This means the Super Admin can always operate the
> system end-to-end without the awkward step of having to grant
> permissions to themself first.

---

Good luck with your presentation! Re-read Section 14 last, right before you
present — those "big picture" answers are the ones most likely to impress a
lecturer because they show you understand how all the pieces fit together,
not just what each individual file does. Pay special attention to the
permission-granting walkthrough above — it ties together almost every class
discussed in Sections 8 and 9.
