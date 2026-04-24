# Implementation Plan: javafx-tools-services-advanced

## Overview

Six advanced features are added on top of the existing JavaFX CRUD application in strict dependency order: Category foundation → Approval Workflow → Quality Score → Price Suggestion → Category Suggestion → Advanced Search & Filter. All code is Java. A new sub-package `tn.piapp.service` is introduced for the three pure service classes. No database schema changes are required.

---

## Tasks

- [x] 1. Category Foundation — model, DAO, and model additions
  - [x] 1.1 Create `Category` model class in `tn.piapp.model`
    - Add fields `id` (int), `name` (String), `type` (String) with getters/setters and no-arg constructor
    - Override `toString()` to return `name` for ComboBox display
    - _Requirements: 1.1_

  - [x] 1.2 Create `CategoryDao` in `tn.piapp.dao`
    - Implement `findByType(String type)` using `SELECT id, name, type FROM category WHERE type = ?` via `PreparedStatement`
    - Return empty list (not null) when no rows match
    - _Requirements: 1.2, 1.3, 1.4, 1.5_

  - [ ]* 1.3 Write property test for `CategoryDao.findByType` — Property 1
    - **Property 1: CategoryDao type filter correctness**
    - **Validates: Requirements 1.2, 1.3, 1.4**
    - Test class: `CategoryDaoTest`, method: `findByTypeReturnsOnlyMatchingType()`
    - Requires live test DB; annotate: `// Feature: javafx-tools-services-advanced, Property 1: ...`

  - [x] 1.4 Add `categoryId` field to `Tool` and `Service` models
    - Add `private Integer categoryId` (nullable) with getter `getCategoryId()` and setter `setCategoryId(Integer)` to both `Tool` and `Service`
    - _Requirements: 1.6, 1.7_

  - [x] 1.5 Update `ToolDao` to include `category_id` in all SQL operations
    - Update `FIND_ALL` SQL to include `category_id`; read with `rs.getObject("category_id", Integer.class)` to handle SQL NULL → Java `null`
    - Update `INSERT` SQL to include `category_id`; bind with `ps.setObject(N, t.getCategoryId())` (passes `null` correctly)
    - Update `UPDATE` SQL to include `category_id = ?`; bind same way
    - Force `is_active = 0` on all new inserts (hardcode `ps.setInt(..., 0)` regardless of model field)
    - _Requirements: 1.8, 1.10, 2.1_

  - [x] 1.6 Update `ServiceDao` to include `category_id` in all SQL operations
    - Same pattern as task 1.5 but for `ServiceDao` and `service` table
    - Force `is_active = 0` on all new inserts
    - _Requirements: 1.9, 1.11, 2.2_

  - [ ]* 1.7 Write property test for `categoryId` round-trip — Property 2
    - **Property 2: categoryId round-trip through DAO**
    - **Validates: Requirements 1.8, 1.9, 1.10, 1.11**
    - Test classes: `ToolDaoTest`, `ServiceDaoTest`, method: `categoryIdRoundTrip()`
    - Requires live test DB

  - [ ]* 1.8 Write property test for new inserts always inactive — Property 3
    - **Property 3: New listings always inserted as inactive**
    - **Validates: Requirements 2.1, 2.2**
    - Test classes: `ToolDaoTest`, `ServiceDaoTest`, method: `newInsertAlwaysInactive()`
    - Requires live test DB

  - [x] 1.9 Add `ComboBox<Category>` to `ToolFormDialog`
    - Remove the `cbIsActive` checkbox field and its grid row entirely
    - Add `private ComboBox<Category> cbCategory` field
    - In constructor, load categories via `CategoryDao.findByType("tool")` (catch `SQLException`, leave ComboBox empty on error)
    - Add ComboBox row to the grid; in edit mode pre-select the matching `Category` by comparing `id` to `existing.getCategoryId()`
    - In `onSave()`, remove `t.setActive(cbIsActive.isSelected())` and instead call `t.setCategoryId(cbCategory.getValue() != null ? cbCategory.getValue().getId() : null)`; do not set `isActive` (DAO handles it)
    - _Requirements: 1.12, 1.14, 1.16, 2.1_

  - [x] 1.10 Add `ComboBox<Category>` to `ServiceFormDialog`
    - Same changes as task 1.9 but for `ServiceFormDialog` and `CategoryDao.findByType("service")`
    - _Requirements: 1.13, 1.15, 1.16, 2.2_

- [x] 2. Checkpoint — Category foundation complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Approval Workflow — `setActive` DAO methods and per-row action buttons
  - [x] 3.1 Add `setActive(int id, boolean active)` to `ToolDao`
    - SQL: `UPDATE tool SET is_active = ? WHERE id = ?`
    - _Requirements: 2.7, 2.8, 2.11_

  - [x] 3.2 Add `setActive(int id, boolean active)` to `ServiceDao`
    - SQL: `UPDATE service SET is_active = ? WHERE id = ?`
    - _Requirements: 2.9, 2.10, 2.12_

  - [ ]* 3.3 Write property test for `setActive` toggles correctly — Property 4
    - **Property 4: setActive toggles isActive correctly**
    - **Validates: Requirements 2.7, 2.8, 2.9, 2.10**
    - Test classes: `ToolDaoTest`, `ServiceDaoTest`, method: `setActiveTogglesCorrectly()`
    - Requires live test DB

  - [x] 3.4 Add per-row Approve/Hide action column to `ToolController`
    - Add `private TableColumn<Tool, Void> colActions` field; create it programmatically in `initialize()`
    - Cell factory: show "Approve" button when `item.isActive() == false`, "Hide" button when `true`
    - Button handler: call `dao.setActive(item.getId(), !item.isActive())` then `loadData()`; catch `SQLException` with `Alerts.showError`
    - Add `colActions` to `tableView.getColumns()`
    - _Requirements: 2.3, 2.4, 2.7, 2.8_

  - [x] 3.5 Add per-row Approve/Hide action column to `ServiceController`
    - Same pattern as task 3.4 but for `ServiceController` and `ServiceDao`
    - _Requirements: 2.5, 2.6, 2.9, 2.10_

  - [x] 3.6 Add 3-way approval filter toggle to `ToolController` and update `updatePredicate()`
    - Add `@FXML private ToggleGroup tgApproval` and `@FXML private RadioButton rbAll, rbActiveOnly, rbPending` (or create programmatically if not in FXML)
    - Refactor the existing `tfSearch` listener into a new `private void updatePredicate()` method on `filteredList`
    - Add approval condition: `rbAll` → no filter; `rbActiveOnly` → `item.isActive() == true`; `rbPending` → `item.isActive() == false`
    - Wire `tgApproval.selectedToggleProperty()` listener to call `updatePredicate()`
    - _Requirements: 2.13, 2.15, 2.16, 2.17_

  - [x] 3.7 Add 3-way approval filter toggle to `ServiceController` and update `updatePredicate()`
    - Same pattern as task 3.6 but for `ServiceController`
    - _Requirements: 2.14, 2.18, 2.19, 2.20_

  - [ ]* 3.8 Write property test for approval filter predicate — Property 5
    - **Property 5: Approval filter predicate correctness**
    - **Validates: Requirements 2.15, 2.16, 2.17, 2.18, 2.19, 2.20**
    - Test class: `ApprovalFilterTest`, method: `approvalPredicateFiltersCorrectly()`
    - Extract the approval predicate logic into a testable static helper to avoid requiring JavaFX runtime

- [x] 4. Checkpoint — Approval Workflow complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Quality Score — service class, badge column, and detail popup
  - [x] 5.1 Create `QualityScoreResult` in `tn.piapp.service`
    - Immutable class with fields: `score` (int), `percentage` (int), `rating` (String), `suggestions` (List<String>), `checklist` (List<String>)
    - Constructor takes all five fields; provide getters only (no setters)
    - _Requirements: 3.3_

  - [x] 5.2 Create `QualityScoreService` in `tn.piapp.service`
    - Implement `public QualityScoreResult score(Tool tool)` with point allocation: imageName non-blank → 30 pts; description ≥ 100 chars → 30 pts, ≥ 50 chars → 15 pts; categoryId non-null → 15 pts; location non-blank → 10 pts; pricePerDay > 0 && ≤ 500 → 10 pts; createdAt within 30 days → 5 pts
    - Implement `public QualityScoreResult score(Service service)` with same structure but `basePrice > 0 && ≤ 1000` for the price criterion
    - Build `suggestions` list (hints for unmet criteria) and `checklist` list (`"✓ Has image"` / `"✗ No image"` etc.)
    - Derive `rating`: score ≥ 81 → `"Excellent"`, ≥ 51 → `"Good"`, < 51 → `"Needs Improvement"`
    - No DB access, no JavaFX imports
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.14_

  - [ ]* 5.3 Write property test for quality score always in [0, 100] — Property 6
    - **Property 6: Quality score is always in [0, 100]**
    - **Validates: Requirements 3.15, 3.16**
    - Test class: `QualityScoreServiceTest`, method: `scoreAlwaysInRange()`
    - Pure in-memory; no DB required

  - [ ]* 5.4 Write property test for quality score rating matches score range — Property 7
    - **Property 7: Quality score rating matches score range**
    - **Validates: Requirements 3.4, 3.5, 3.6**
    - Test class: `QualityScoreServiceTest`, method: `ratingMatchesScoreRange()`

  - [ ]* 5.5 Write property test for quality score equals sum of criteria points — Property 8
    - **Property 8: Quality score equals sum of awarded criteria points**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - Test class: `QualityScoreServiceTest`, method: `scoreEqualsSumOfCriteria()`

  - [x] 5.6 Add quality score badge column to `ToolController`
    - Add `private TableColumn<Tool, Void> colQuality` field; create programmatically in `initialize()`
    - Cell factory: call `QualityScoreService.score(item)`, create a `Label` with text `score% · rating`; apply CSS style class `badge-quality-excellent` / `badge-quality-good` / `badge-quality-poor` based on rating
    - Guard `if (item == null) return;` before scoring
    - Add `colQuality` to `tableView.getColumns()`
    - _Requirements: 3.7, 3.9, 3.10, 3.11_

  - [x] 5.7 Add quality score badge column to `ServiceController`
    - Same pattern as task 5.6 but for `ServiceController` and `Service`
    - _Requirements: 3.8, 3.9, 3.10, 3.11_

  - [x] 5.8 Add quality score detail popup on badge click in `ToolController`
    - In the badge cell factory, set `setOnMouseClicked` on the Label: show an `Alert` (or small `Stage`) listing `result.getChecklist()` and `result.getSuggestions()`
    - Guard against null item before opening popup
    - _Requirements: 3.12_

  - [x] 5.9 Add quality score detail popup on badge click in `ServiceController`
    - Same pattern as task 5.8 but for `ServiceController`
    - _Requirements: 3.13_

- [x] 6. Checkpoint — Quality Score complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Price Suggestion — DAO methods, service class, and live label in form dialogs
  - [x] 7.1 Add `getPricesByCategory(int categoryId)` to `ToolDao`
    - SQL: `SELECT price_per_day FROM tool WHERE category_id = ? AND is_active = 1`
    - Return `List<BigDecimal>`; return empty list if no rows
    - _Requirements: 4.1_

  - [x] 7.2 Add `getPricesByCategory(int categoryId)` to `ServiceDao`
    - SQL: `SELECT base_price FROM service WHERE category_id = ? AND is_active = 1`
    - Return `List<BigDecimal>`; return empty list if no rows
    - _Requirements: 4.2_

  - [x] 7.3 Create `PriceSuggestionResult` in `tn.piapp.service`
    - Immutable class with fields: `suggested` (BigDecimal), `median` (BigDecimal), `mean` (BigDecimal), `min` (BigDecimal), `max` (BigDecimal), `count` (int)
    - Constructor takes all six fields; provide getters only
    - _Requirements: 4.3_

  - [x] 7.4 Create `PriceSuggestionService` in `tn.piapp.service`
    - Implement `public PriceSuggestionResult suggest(List<BigDecimal> prices)`
    - Sort the list; compute median (middle element for odd count, average of two middle elements for even count), mean (arithmetic, rounded to 2 dp), min, max
    - Throw `IllegalArgumentException` if `prices` is null or empty
    - No DB access, no JavaFX imports
    - _Requirements: 4.3, 4.4, 4.5, 4.9_

  - [ ]* 7.5 Write property test for median and mean bounded by min/max — Property 9
    - **Property 9: Median is bounded by min and max**
    - **Validates: Requirements 4.10, 4.11**
    - Test class: `PriceSuggestionServiceTest`, method: `medianAndMeanBoundedByMinMax()`
    - Pure in-memory; no DB required

  - [ ]* 7.6 Write property test for median computation correctness — Property 10
    - **Property 10: Median computation correctness**
    - **Validates: Requirements 4.3, 4.4, 4.5**
    - Test class: `PriceSuggestionServiceTest`, method: `medianComputationCorrect()`

  - [x] 7.7 Add price suggestion live label to `ToolFormDialog`
    - Add `private Label lblPriceSuggestion` field; add it below the category ComboBox row in the grid
    - Add a `ChangeListener` on `cbCategory.valueProperty()`: when a category is selected, run `ToolDao.getPricesByCategory` inside a `Task<List<BigDecimal>>`; on success call `PriceSuggestionService.suggest` and update `lblPriceSuggestion` with suggested, min, max, mean, count; if list is empty show "No price data available for this category"; on `Task` failure show "Could not load price data"
    - _Requirements: 4.6, 4.8_

  - [x] 7.8 Add price suggestion live label to `ServiceFormDialog`
    - Same pattern as task 7.7 but for `ServiceFormDialog` and `ServiceDao.getPricesByCategory`
    - _Requirements: 4.7, 4.8_

- [x] 8. Checkpoint — Price Suggestion complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Category Suggestion — service class and live suggestion label + Apply button in form dialogs
  - [x] 9.1 Create `CategorySuggestionResult` in `tn.piapp.service`
    - Immutable class with fields: `category` (Category, nullable), `confidence` (int, 0–100)
    - Add `public boolean hasMatch()` returning `category != null && confidence > 0`
    - Provide getters only
    - _Requirements: 5.2, 5.3_

  - [x] 9.2 Create `CategorySuggestionService` in `tn.piapp.service`
    - Implement `public CategorySuggestionResult suggest(String text, List<Category> candidates)`
    - Define the keyword map as a `static final Map<String, List<String>>` with all 16 entries from the design
    - For each entry in the keyword map whose name matches a candidate (case-insensitive name comparison), count how many keywords appear as substrings in `text.toLowerCase()`; compute confidence = `round((matched / total) * 100)`
    - Return the candidate with the highest confidence > 0; return empty result (`category = null`, `confidence = 0`) if no match
    - Throw no exceptions; return empty result for null/empty text
    - No DB access, no JavaFX imports
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.11, 5.12_

  - [ ]* 9.3 Write property test for suggestion result always from candidates — Property 11
    - **Property 11: Category suggestion result is always from candidates**
    - **Validates: Requirements 5.2, 5.4, 5.5**
    - Test class: `CategorySuggestionServiceTest`, method: `resultAlwaysFromCandidates()`
    - Pure in-memory; no DB required

  - [ ]* 9.4 Write property test for confidence in [1, 100] when match exists — Property 12
    - **Property 12: Category suggestion confidence is in [1, 100] when a match exists**
    - **Validates: Requirements 5.3, 5.13**
    - Test class: `CategorySuggestionServiceTest`, method: `confidenceInRangeWhenMatch()`

  - [ ]* 9.5 Write property test for case-insensitive matching — Property 13
    - **Property 13: Category suggestion is case-insensitive**
    - **Validates: Requirements 5.11**
    - Test class: `CategorySuggestionServiceTest`, method: `matchingIsCaseInsensitive()`

  - [x] 9.6 Add category suggestion label and Apply button to `ToolFormDialog`
    - Add `private Label lblCategorySuggestion` and `private Button btnApplySuggestion` fields; add them as a row below the category ComboBox
    - Initially set both to `setVisible(false)` / `setManaged(false)`
    - Add a `ChangeListener` on both `tfName.textProperty()` and `taDescription.textProperty()`: combine name + " " + description, call `CategorySuggestionService.suggest(combined, loadedCategories)` synchronously; if `hasMatch()` show label with "Suggested: {name} ({confidence}%)" and show Apply button; otherwise hide both
    - `btnApplySuggestion.setOnAction`: set `cbCategory.setValue(lastSuggestion.getCategory())`
    - _Requirements: 5.6, 5.8, 5.10_

  - [x] 9.7 Add category suggestion label and Apply button to `ServiceFormDialog`
    - Same pattern as task 9.6 but for `ServiceFormDialog` and `CategoryDao.findByType("service")` candidates
    - _Requirements: 5.7, 5.9, 5.10_

- [x] 10. Checkpoint — Category Suggestion complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Advanced Search & Filter — filter bar UI, combined predicate, SortedList wiring, Clear Filters
  - [x] 11.1 Add filter bar controls to `ToolController` (FXML + field declarations)
    - Add `@FXML` fields: `ComboBox<Category> cbCategoryFilter`, `TextField tfLocationFilter`, `TextField tfMinPrice`, `TextField tfMaxPrice`, `ComboBox<String> cbSortBy`, `Button btnClearFilters`
    - In `tool.fxml`, add a filter bar `HBox` above the table containing all controls; populate `cbCategoryFilter` with an "All Categories" null entry plus all tool categories loaded via `CategoryDao.findByType("tool")` in a `Task`; populate `cbSortBy` with the six sort options
    - _Requirements: 6.1, 6.3, 6.5, 6.7_

  - [x] 11.2 Add filter bar controls to `ServiceController` (FXML + field declarations)
    - Same pattern as task 11.1 but for `service.fxml` and `ServiceController`; use `CategoryDao.findByType("service")` for the category filter
    - _Requirements: 6.2, 6.4, 6.6, 6.8_

  - [x] 11.3 Extend `updatePredicate()` in `ToolController` with all advanced filter conditions
    - Extend the existing `updatePredicate()` method (introduced in task 3.6) to AND in: category filter (`cbCategoryFilter.getValue() != null` → `item.getCategoryId() != null && item.getCategoryId() == selected.getId()`); location filter (case-insensitive contains); min price (parse `tfMinPrice`, skip silently on `NumberFormatException`); max price (parse `tfMaxPrice`, skip silently)
    - Wire `ChangeListener` on `cbCategoryFilter`, `tfLocationFilter`, `tfMinPrice`, `tfMaxPrice` to call `updatePredicate()`
    - _Requirements: 6.9, 6.11, 6.13, 6.14, 6.18, 6.23, 6.25_

  - [x] 11.4 Extend `updatePredicate()` in `ServiceController` with all advanced filter conditions
    - Same pattern as task 11.3 but for `ServiceController` and `Service.basePrice`
    - _Requirements: 6.10, 6.12, 6.16, 6.17, 6.18, 6.23, 6.25_

  - [x] 11.5 Wire sort-by ComboBox in `ToolController`
    - Add `ChangeListener` on `cbSortBy.valueProperty()`: set a `Comparator<Tool>` on the `SortedList` based on the selected option (date newest/oldest, price high/low, name A–Z/Z–A); `SortedList` is already bound to `tableView.comparatorProperty()` — when the dropdown sets a comparator directly, column-header clicks still work
    - _Requirements: 6.7, 6.19, 6.24_

  - [x] 11.6 Wire sort-by ComboBox in `ServiceController`
    - Same pattern as task 11.5 but for `ServiceController` and `Service`
    - _Requirements: 6.8, 6.20, 6.24_

  - [x] 11.7 Implement `onClearFilters()` in `ToolController`
    - Reset `tfSearch`, `cbCategoryFilter` (to "All Categories"), `tfLocationFilter`, `tfMinPrice`, `tfMaxPrice`, `cbSortBy` (to default), and approval toggle (`rbAll`) to defaults; call `updatePredicate()`
    - Wire `btnClearFilters.setOnAction` to this method
    - _Requirements: 6.21_

  - [x] 11.8 Implement `onClearFilters()` in `ServiceController`
    - Same pattern as task 11.7 but for `ServiceController`
    - _Requirements: 6.22_

  - [ ]* 11.9 Write property test for combined filter predicate AND-correctness — Property 14
    - **Property 14: Combined filter predicate AND-correctness**
    - **Validates: Requirements 6.9, 6.10, 6.11, 6.12, 6.13, 6.14, 6.15, 6.16, 6.17, 6.23, 6.25**
    - Test class: `FilterPredicateTest`, method: `combinedPredicateAndCorrectness()`
    - Extract predicate logic into a testable static helper; pure in-memory, no DB or JavaFX runtime required

  - [ ]* 11.10 Write property test for sort-by ordering correctness — Property 15
    - **Property 15: Sort-by ordering correctness**
    - **Validates: Requirements 6.19, 6.20**
    - Test class: `SortOrderTest`, method: `sortByOrderingCorrect()`
    - Pure in-memory; test the `Comparator<Tool>` and `Comparator<Service>` instances directly

- [x] 12. Final Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- DAO property tests (Properties 1–4) require a live test database — use a separate test schema, not the production `smart_rental_platform` database
- Service class property tests (Properties 5–15) are pure in-memory and require no database
- Filter predicate tests (Properties 5, 14, 15) must extract predicate/comparator logic into static helpers to avoid requiring a JavaFX runtime in tests
- Each property test must include the comment: `// Feature: javafx-tools-services-advanced, Property N: <property_text>`
- jqwik is already present in `pom.xml`; each property test runs a minimum of 100 tries
- The `isActive` checkbox is removed from both form dialogs — approval is exclusively controlled via the per-row table buttons
- All new inserts hardcode `is_active = 0` in the DAO, not in the form dialog
- `CategorySuggestionService.suggest()` scores only against the `candidates` list passed in — keyword map entries with no matching candidate are ignored
- All filter controls (text, approval toggle, category, location, min/max price) are AND-ed in a single `FilteredList` predicate via `updatePredicate()`
- `SortedList` wraps `FilteredList` and is bound to `tableView.comparatorProperty()`
