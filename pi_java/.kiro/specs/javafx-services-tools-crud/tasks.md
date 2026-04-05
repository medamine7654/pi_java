# Implementation Plan: javafx-services-tools-crud

## Overview

Incremental build of a JavaFX + MySQL JDBC CRUD app for Services and Tools. Each step compiles and integrates cleanly into the previous one. The stack is Maven, JavaFX 21, MySQL Connector/J, and jqwik for property-based tests.

Base package: `tn.piapp` — sub-packages: `db`, `model`, `dao`, `ui`, `util`.

---

## Tasks

- [x] 1. Maven project scaffold and pom.xml
  - Create `pi_java/` as a standard Maven project (`src/main/java`, `src/main/resources`, `src/test/java`)
  - Write `pom.xml` with: JavaFX 21 (controls, fxml), `mysql-connector-j`, `jqwik 1.8.4`, `maven-compiler-plugin` (Java 21), `javafx-maven-plugin`
  - Add `module-info.java` (or open module) that requires `javafx.controls`, `javafx.fxml`, `java.sql`
  - _Requirements: 7.1_

- [x] 2. DbConnection singleton
  - [x] 2.1 Implement `tn.piapp.db.DbConnection`
    - Singleton with private constructor; JDBC URL `jdbc:mysql://127.0.0.1:3306/smart_rental_platform`, user `root`, empty password
    - `getConnection()` checks `connection == null || !connection.isValid(2)` and calls `connect()` to reconnect
    - Constructor-level `SQLException` propagates to caller (caught in `MainApp`)
    - _Requirements: 1.1, 1.3, 1.4_

  - [ ]* 2.2 Write property test for connection resilience
    - **Property 1: Connection resilience**
    - **Validates: Requirements 1.4**
    - Test class: `DbConnectionTest.connectionAlwaysValidAfterReconnect()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 1: connectionAlwaysValidAfterReconnect`

  - [ ]* 2.3 Write unit test — singleton identity
    - Assert `DbConnection.getInstance() == DbConnection.getInstance()`
    - _Requirements: 1.3_

- [x] 3. Model POJOs
  - [x] 3.1 Implement `tn.piapp.model.Service`
    - Fields: `id`, `name`, `description`, `basePrice` (BigDecimal), `durationMinutes`, `location`, `isActive`, `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime), `hostId`, `imageName`
    - Full getters and setters; no-arg constructor + all-arg constructor
    - _Requirements: 7.4_

  - [x] 3.2 Implement `tn.piapp.model.Tool`
    - Fields: `id`, `name`, `description`, `pricePerDay` (BigDecimal), `stockQuantity`, `location`, `isActive`, `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime), `hostId`, `imageName`
    - Full getters and setters; no-arg constructor + all-arg constructor
    - _Requirements: 7.4_

- [x] 4. Validation utility
  - [x] 4.1 Implement `tn.piapp.util.Validation`
    - `validateService(Service s)` — returns error string or null; checks: name not blank, basePrice >= 0, durationMinutes > 0
    - `validateTool(Tool t)` — returns error string or null; checks: name not blank, pricePerDay >= 0, stockQuantity >= 0
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.7_

  - [ ]* 4.2 Write property test — blank name always invalid (Property 9)
    - **Property 9: Blank name is always invalid**
    - **Validates: Requirements 5.1**
    - Test class: `ValidationTest.blankNameAlwaysInvalid()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 9: blankNameAlwaysInvalid`

  - [ ]* 4.3 Write property test — negative numeric fields always invalid (Property 10)
    - **Property 10: Negative numeric fields are always invalid**
    - **Validates: Requirements 5.2, 5.3, 5.4, 5.5**
    - Test class: `ValidationTest.negativeNumericFieldsAlwaysInvalid()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 10: negativeNumericFieldsAlwaysInvalid`

  - [ ]* 4.4 Write property test — valid inputs pass validation (Property 11)
    - **Property 11: Valid inputs pass validation**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**
    - Test class: `ValidationTest.validInputsPassValidation()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 11: validInputsPassValidation`

- [x] 5. Alerts utility
  - Implement `tn.piapp.util.Alerts`
  - `showError(String title, String message)` — shows `Alert.AlertType.ERROR`; handles null message gracefully
  - `showConfirmation(String title, String message)` — shows `Alert.AlertType.CONFIRMATION`; returns `true` if user clicks OK
  - _Requirements: 1.2, 1.5_

- [x] 6. ServiceDao
  - [x] 6.1 Implement `tn.piapp.dao.ServiceDao`
    - `resolveDefaultHostId()` — `SELECT MIN(id) FROM user`; throws `IllegalStateException` if result is NULL
    - `findAll()` — SELECT all columns (excluding category_id, image_size, image_updated_at); map ResultSet using `rs.getInt("is_active") != 0` for boolean; return `List<Service>`
    - `insert(Service s)` — sets `createdAt`/`updatedAt` to `LocalDateTime.now()`, resolves `hostId` via `resolveDefaultHostId()`, executes INSERT with PreparedStatement
    - `update(Service s)` — resets `updatedAt` to `LocalDateTime.now()`, executes UPDATE with PreparedStatement
    - `delete(int id)` — executes DELETE with PreparedStatement
    - All methods use try-with-resources for PreparedStatement and ResultSet
    - _Requirements: 2.2, 2.3, 3.3, 3.5, 3.7, 3.9, 3.10, 3.11_

  - [ ]* 6.2 Write property test — host_id matches MIN(user.id) after insert (Property 2)
    - **Property 2: host_id is always resolved from the user table**
    - **Validates: Requirements 2.2, 2.3**
    - Test class: `ServiceDaoTest.hostIdMatchesMinUserIdAfterInsert()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 2: hostIdMatchesMinUserIdAfterInsert`

  - [ ]* 6.3 Write property test — insert round-trip (Property 3)
    - **Property 3: Service insert round-trip**
    - **Validates: Requirements 3.3**
    - Test class: `ServiceDaoTest.insertedServiceAppearsInFindAll()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 3: insertedServiceAppearsInFindAll`

  - [ ]* 6.4 Write property test — update round-trip (Property 4)
    - **Property 4: Service update round-trip**
    - **Validates: Requirements 3.5**
    - Test class: `ServiceDaoTest.updatedServiceReflectedInFindAll()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 4: updatedServiceReflectedInFindAll`

  - [ ]* 6.5 Write property test — delete removes record (Property 5)
    - **Property 5: Service delete removes record**
    - **Validates: Requirements 3.7**
    - Test class: `ServiceDaoTest.deletedServiceAbsentFromFindAll()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 5: deletedServiceAbsentFromFindAll`

- [x] 7. ToolDao
  - [x] 7.1 Implement `tn.piapp.dao.ToolDao`
    - Same structure as `ServiceDao` but for the `tool` table and `Tool` model
    - `resolveDefaultHostId()` — same `SELECT MIN(id) FROM user` logic
    - `findAll()`, `insert(Tool t)`, `update(Tool t)`, `delete(int id)` — mirror ServiceDao patterns
    - All methods use try-with-resources
    - _Requirements: 2.2, 2.3, 4.3, 4.5, 4.7, 4.9, 4.10, 4.11_

  - [ ]* 7.2 Write property test — insert round-trip (Property 6)
    - **Property 6: Tool insert round-trip**
    - **Validates: Requirements 4.3**
    - Test class: `ToolDaoTest.insertedToolAppearsInFindAll()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 6: insertedToolAppearsInFindAll`

  - [ ]* 7.3 Write property test — update round-trip (Property 7)
    - **Property 7: Tool update round-trip**
    - **Validates: Requirements 4.5**
    - Test class: `ToolDaoTest.updatedToolReflectedInFindAll()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 7: updatedToolReflectedInFindAll`

  - [ ]* 7.4 Write property test — delete removes record (Property 8)
    - **Property 8: Tool delete removes record**
    - **Validates: Requirements 4.7**
    - Test class: `ToolDaoTest.deletedToolAbsentFromFindAll()`
    - Comment: `// Feature: javafx-services-tools-crud, Property 8: deletedToolAbsentFromFindAll`

- [ ] 8. Checkpoint — DAO and utility layer complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. MainApp and MainController
  - [x] 9.1 Implement `tn.piapp.ui.MainApp` (extends `Application`)
    - `start()` loads `main.fxml`, sets stage title, shows stage
    - Catches `SQLException` from `DbConnection.getInstance()` at startup; calls `Alerts.showError()` and `Platform.exit()` if DB is unreachable
    - _Requirements: 1.2, 7.3_

  - [x] 9.2 Implement `tn.piapp.ui.MainController`
    - FXML controller for `main.fxml`; owns a `TabPane` with two tabs: "Services" and "Tools"
    - Each tab embeds the corresponding sub-controller via `fx:include`
    - _Requirements: 7.3_

  - [x] 9.3 Create `main.fxml` in `src/main/resources/tn/piapp/ui/`
    - Root `TabPane` with two `Tab` nodes, each including the relevant sub-FXML
    - _Requirements: 7.3_

- [x] 10. ServiceController and ServiceFormDialog
  - [x] 10.1 Implement `tn.piapp.ui.ServiceController`
    - `TableView<Service>` with columns: name, description, basePrice, durationMinutes, location, isActive, imageName
    - Buttons: Add, Edit, Delete, Refresh
    - Edit and Delete disabled when no row is selected (`tableView.getSelectionModel().selectedItemProperty().isNull()`)
    - `loadData()` runs `ServiceDao.findAll()` inside a `Task<List<Service>>`; shows `ProgressIndicator` while running; updates TableView on success; calls `Alerts.showError()` on failure
    - Add/Edit open `ServiceFormDialog`; on result call `dao.insert()` or `dao.update()`, then `loadData()`
    - Delete shows `Alerts.showConfirmation()`, then calls `dao.delete()`, then `loadData()`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 6.1, 6.2, 6.3, 6.4_

  - [x] 10.2 Create `service.fxml` in `src/main/resources/tn/piapp/ui/`
    - VBox layout: TableView, HBox of buttons, ProgressIndicator
    - _Requirements: 3.1_

  - [x] 10.3 Implement `tn.piapp.ui.ServiceFormDialog`
    - Extends `Dialog<Service>` (or wraps a modal `Stage`)
    - Fields: name (TextField), description (TextArea), basePrice (TextField), durationMinutes (TextField), location (TextField), isActive (CheckBox), imageName (TextField)
    - Pre-populates all fields when an existing `Service` is passed (edit mode)
    - On OK: calls `Validation.validateService()`; if error, sets inline `Label` text and consumes the event (dialog stays open); otherwise returns populated `Service`
    - _Requirements: 3.2, 3.4, 5.6_

  - [ ]* 10.4 Write unit test — ServiceFormDialog pre-populates fields in edit mode
    - Assert each field value matches the passed Service object
    - _Requirements: 3.4_

- [x] 11. ToolController and ToolFormDialog
  - [x] 11.1 Implement `tn.piapp.ui.ToolController`
    - Mirror of `ServiceController` for the `tool` table
    - `TableView<Tool>` with columns: name, description, pricePerDay, stockQuantity, location, isActive, imageName
    - Same button/selection/background-task/dialog pattern as ServiceController
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 6.1, 6.2, 6.3, 6.4_

  - [x] 11.2 Create `tool.fxml` in `src/main/resources/tn/piapp/ui/`
    - VBox layout: TableView, HBox of buttons, ProgressIndicator
    - _Requirements: 4.1_

  - [x] 11.3 Implement `tn.piapp.ui.ToolFormDialog`
    - Mirror of `ServiceFormDialog` for Tool
    - Fields: name, description, pricePerDay (TextField), stockQuantity (TextField), location, isActive (CheckBox), imageName
    - Pre-populates fields in edit mode; validates via `Validation.validateTool()` before returning
    - _Requirements: 4.2, 4.4, 5.6_

  - [ ]* 11.4 Write unit test — ToolFormDialog pre-populates fields in edit mode
    - Assert each field value matches the passed Tool object
    - _Requirements: 4.4_

- [x] 12. Wire everything together
  - [x] 12.1 Connect MainController tabs to ServiceController and ToolController via `fx:include`
    - Verify `fx:id` bindings resolve correctly; inject sub-controllers via `@FXML` nested controller pattern
    - _Requirements: 7.3_

  - [x] 12.2 Verify Edit/Delete button disable logic across both tabs
    - Confirm buttons are disabled on startup and re-enabled only when a row is selected
    - _Requirements: 3.4, 4.4_

  - [x] 12.3 Verify loading indicator visibility during background tasks
    - `ProgressIndicator` visible = true when task starts, false when task ends (success or failure)
    - _Requirements: 6.4_

  - [ ]* 12.4 Write unit test — background task failure calls Alerts.showError on FX thread
    - Mock DAO to throw SQLException; assert Alerts.showError is invoked on the JavaFX Application Thread
    - _Requirements: 6.3_

- [ ] 13. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- DAO property tests (tasks 6.2–6.5, 7.2–7.4) require a live MySQL test database — do not run against the production `smart_rental_platform` schema
- `category_id`, `image_size`, and `image_updated_at` columns are intentionally omitted from all SQL and model fields
- `host_id` is never shown in the UI; it is resolved automatically via `SELECT MIN(id) FROM user`
- `created_at` and `updated_at` have no DB default — always set in Java before INSERT/UPDATE
