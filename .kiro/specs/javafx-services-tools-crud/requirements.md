# Requirements Document

## Introduction

A JavaFX desktop CRUD application that connects directly to an existing local MySQL database shared with a Symfony web application. The app provides two modules — Services and Tools — each accessible via a tab in a TabPane. Users can create, read, update, and delete records through a table view and modal dialog forms. No changes are made to the Symfony application or its database schema.

## Glossary

- **App**: The JavaFX desktop application being built.
- **DB**: The local MySQL database shared with the Symfony application.
- **Service**: A record in the `service` table representing an offered service with pricing and scheduling metadata.
- **Tool**: A record in the `tool` table representing a rentable tool with stock and pricing metadata.
- **DAO**: Data Access Object — a class responsible for all SQL operations for a given entity.
- **TableView**: A JavaFX UI component that displays a list of records in a tabular format.
- **Modal Dialog**: A JavaFX window that blocks interaction with the main window until dismissed.
- **PreparedStatement**: A JDBC interface used to execute parameterized SQL queries safely.
- **Background Task**: A JavaFX `Task` run on a non-UI thread to prevent freezing the interface.
- **Validation**: Input checking performed before any DB write operation.
- **FK**: Foreign key — a column referencing the primary key of another table.

---

## Requirements

### Requirement 1: Database Connection

**User Story:** As a developer, I want the App to connect to the local MySQL DB at startup, so that all CRUD operations can be performed against the live database.

#### Acceptance Criteria

1. THE App SHALL establish a JDBC connection to the local MySQL DB using credentials defined in a single configuration file.
2. WHEN the DB is unreachable at startup, THE App SHALL display a user-friendly error screen with a descriptive message instead of a raw stack trace.
3. THE DatabaseConnection SHALL use a singleton pattern to provide a single shared `Connection` instance across all DAOs.
4. BEFORE each DAO operation, THE DatabaseConnection SHALL verify the connection is still valid (e.g. `connection.isValid(2)`) and attempt to reconnect if it is stale or closed.
5. WHEN a SQL error occurs during any operation, THE App SHALL display a friendly error message to the user without exposing raw exception stack traces.

---

### Requirement 2: Schema Introspection Before SQL

**User Story:** As a developer, I want the DAO layer to be built against the actual DB schema, so that SQL queries match the real column names and constraints.

#### Acceptance Criteria

1. BEFORE writing any SQL, THE Developer SHALL execute `SHOW CREATE TABLE service` and `SHOW CREATE TABLE tool` against the DB to extract the exact schema.
2. WHEN a column has a NOT NULL foreign key constraint (e.g. `category_id`, `host_id`), THE DAO SHALL resolve a default value by executing `SELECT MIN(id)` on the referenced table and use that value in INSERT/UPDATE statements.
3. WHEN `SELECT MIN(id)` returns NULL (i.e. the referenced table is empty), THE App SHALL abort the operation and display a descriptive error message informing the user that a required reference record (e.g. a category or host) must exist before records can be created.
4. THE App SHALL NOT expose category or host selection in the UI — those FK values are resolved automatically by the DAO.

---

### Requirement 3: Service CRUD

**User Story:** As a user, I want to manage Services in the App, so that I can create, view, update, and delete service records.

#### Acceptance Criteria

1. THE App SHALL display all Service records in a TableView on the Services tab with columns for: name, description, basePrice, durationMinutes, location, isActive, imageName.
2. WHEN the user clicks "Add", THE App SHALL open a Modal Dialog with input fields for: name, description, basePrice, durationMinutes, location, isActive (checkbox), imageName (plain text filename — no file picker).
3. WHEN the user submits the Add form and all inputs pass Validation, THE ServiceDAO SHALL execute an INSERT using a PreparedStatement and refresh the TableView.
4. WHEN the user selects a row and clicks "Edit", THE App SHALL open a Modal Dialog pre-populated with the selected Service's data.
5. WHEN the user submits the Edit form and all inputs pass Validation, THE ServiceDAO SHALL execute an UPDATE using a PreparedStatement and refresh the TableView.
6. WHEN the user selects a row and clicks "Delete", THE App SHALL display a confirmation prompt before executing a DELETE via PreparedStatement.
7. WHEN the user confirms deletion, THE ServiceDAO SHALL execute the DELETE and refresh the TableView.
8. WHEN the user clicks "Refresh", THE App SHALL reload all Service records from the DB using a Background Task.
9. THE ServiceDAO SHALL use executeQuery with a ResultSet for all SELECT operations.
10. THE ServiceDAO SHALL use executeUpdate for all INSERT, UPDATE, and DELETE operations.
11. THE ServiceDAO SHALL use try-with-resources for all PreparedStatement and ResultSet usage.

---

### Requirement 4: Tool CRUD

**User Story:** As a user, I want to manage Tools in the App, so that I can create, view, update, and delete tool records.

#### Acceptance Criteria

1. THE App SHALL display all Tool records in a TableView on the Tools tab with columns for: name, description, pricePerDay, stockQuantity, location, isActive, imageName.
2. WHEN the user clicks "Add", THE App SHALL open a Modal Dialog with input fields for: name, description, pricePerDay, stockQuantity, location, isActive (checkbox), imageName (plain text filename — no file picker).
3. WHEN the user submits the Add form and all inputs pass Validation, THE ToolDAO SHALL execute an INSERT using a PreparedStatement and refresh the TableView.
4. WHEN the user selects a row and clicks "Edit", THE App SHALL open a Modal Dialog pre-populated with the selected Tool's data.
5. WHEN the user submits the Edit form and all inputs pass Validation, THE ToolDAO SHALL execute an UPDATE using a PreparedStatement and refresh the TableView.
6. WHEN the user selects a row and clicks "Delete", THE App SHALL display a confirmation prompt before executing a DELETE via PreparedStatement.
7. WHEN the user confirms deletion, THE ToolDAO SHALL execute the DELETE and refresh the TableView.
8. WHEN the user clicks "Refresh", THE App SHALL reload all Tool records from the DB using a Background Task.
9. THE ToolDAO SHALL use executeQuery with a ResultSet for all SELECT operations.
10. THE ToolDAO SHALL use executeUpdate for all INSERT, UPDATE, and DELETE operations.
11. THE ToolDAO SHALL use try-with-resources for all PreparedStatement and ResultSet usage.

---

### Requirement 5: Input Validation

**User Story:** As a user, I want the App to validate my input before saving, so that invalid data is never written to the DB.

#### Acceptance Criteria

1. THE Validation SHALL reject a Service or Tool record where the name field is empty or blank.
2. THE Validation SHALL reject a Service record where basePrice is less than 0.
3. THE Validation SHALL reject a Service record where durationMinutes is less than or equal to 0.
4. THE Validation SHALL reject a Tool record where pricePerDay is less than 0.
5. THE Validation SHALL reject a Tool record where stockQuantity is less than 0.
6. WHEN Validation rejects an input, THE App SHALL display a descriptive inline error message identifying the invalid field without closing the Modal Dialog.
7. THE Validation class SHALL be located in the `util` package and SHALL be reused by both ServiceDAO and ToolDAO form submissions.

---

### Requirement 6: Background Data Loading

**User Story:** As a user, I want the UI to remain responsive while data loads, so that the app does not freeze during database queries.

#### Acceptance Criteria

1. WHEN the App loads a tab for the first time, THE App SHALL execute the `findAll()` query on a Background Task (JavaFX `Task`) rather than on the UI thread.
2. WHEN the Background Task completes successfully, THE App SHALL update the TableView on the JavaFX Application Thread.
3. WHEN the Background Task fails due to a SQL error, THE App SHALL display a friendly error message on the JavaFX Application Thread.
4. WHILE a Background Task is running, THE App SHALL display a loading indicator in the relevant tab.

---

### Requirement 7: Project Structure and Build

**User Story:** As a developer, I want the project to follow a standard Maven structure with clear package separation, so that the codebase is maintainable and buildable.

#### Acceptance Criteria

1. THE App SHALL be built using Maven with JavaFX and MySQL Connector/J declared as dependencies in `pom.xml`.
2. THE App SHALL use the base package `tn.yourapp` with sub-packages: `model`, `db`, `dao`, `ui`, `util`.
3. THE App SHALL use JavaFX `TabPane` as the root layout with one tab per module (Services, Tools).
4. THE model package SHALL contain `Service` and `Tool` POJOs with fields matching the DB schema columns.
5. THE db package SHALL contain the `DatabaseConnection` singleton class.
6. THE dao package SHALL contain `ServiceDAO` and `ToolDAO` classes.
7. THE ui package SHALL contain the main application class, tab controllers, and dialog classes.
8. THE util package SHALL contain the `Validation` class.
