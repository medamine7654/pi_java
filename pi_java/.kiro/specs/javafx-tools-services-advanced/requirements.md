# Requirements Document

## Introduction

This spec covers six advanced features added on top of the existing JavaFX CRUD application for Tools and Services (`javafx-services-tools-crud`). The features are ported from the Symfony web application (`smart_rental_platform`) and must integrate cleanly with the existing layered architecture (UI → DAO → DB). No database schema changes are required; all referenced columns (`category_id`, `is_active`) already exist in the MySQL tables.

The six features, in build order, are:

1. **Category Model + DAO + ComboBox** — foundation that unblocks all other features
2. **Approval Workflow** — per-row Approve/Hide actions and a status filter
3. **Quality Score** — computed badge and detail panel per listing
4. **Price Suggestion** — median-based price hint when a category is selected in the form
5. **Category Suggestion** — keyword-driven category hint as the user types
6. **Advanced Search & Filter** — extended client-side filtering with category, location, price range, and sort controls

---

## Glossary

- **Category**: A record in the `category` table with columns `id` (INT), `name` (VARCHAR), and `type` (ENUM: `'tool'` or `'service'`). Shared with the Symfony app.
- **CategoryDao**: The DAO class responsible for reading `Category` records from the database.
- **Tool**: Existing model (`tn.piapp.model.Tool`) representing a row in the `tool` table.
- **Service**: Existing model (`tn.piapp.model.Service`) representing a row in the `service` table.
- **ToolDao**: Existing DAO (`tn.piapp.dao.ToolDao`) extended in this spec.
- **ServiceDao**: Existing DAO (`tn.piapp.dao.ServiceDao`) extended in this spec.
- **ToolController**: Existing JavaFX controller for the Tools tab, extended in this spec.
- **ServiceController**: Existing JavaFX controller for the Services tab, extended in this spec.
- **ToolFormDialog**: Existing modal dialog for adding/editing a Tool, extended in this spec.
- **ServiceFormDialog**: Existing modal dialog for adding/editing a Service, extended in this spec.
- **QualityScoreService**: New service class (`tn.piapp.service.QualityScoreService`) that computes a quality score for a Tool or Service.
- **PriceSuggestionService**: New service class (`tn.piapp.service.PriceSuggestionService`) that computes median/mean/min/max prices for a category.
- **CategorySuggestionService**: New service class (`tn.piapp.service.CategorySuggestionService`) that matches free text to a category using a keyword map.
- **FilteredList**: JavaFX `javafx.collections.transformation.FilteredList` used for client-side filtering.
- **MasterList**: The `ObservableList<T>` that holds all records loaded from the database, backing the `FilteredList`.
- **Quality Score**: An integer from 0 to 100 computed by `QualityScoreService` for a single Tool or Service.
- **Rating**: A label derived from the Quality Score — `Excellent` (score ≥ 81), `Good` (score ≥ 51), `Needs Improvement` (score < 51).
- **Approval Workflow**: The process by which a listing's `isActive` flag is toggled between `true` (approved/visible) and `false` (hidden/pending).
- **Price Suggestion**: The median price of all existing listings in the same category, shown as a hint in the form dialog.
- **Category Suggestion**: The best-matching category name and confidence percentage derived from keyword matching against the listing's name and description.

---

## Requirements

### Requirement 1: Category Model and DAO

**User Story:** As a developer, I want a `Category` model and `CategoryDao` so that category data from the shared MySQL database can be read and used throughout the application.

#### Acceptance Criteria

1. THE `Category` model SHALL have fields: `id` (int), `name` (String), and `type` (String).
2. THE `CategoryDao` SHALL provide a `findByType(String type)` method that returns a `List<Category>` containing only records whose `type` column equals the given argument.
3. WHEN `CategoryDao.findByType` is called with `"tool"`, THE `CategoryDao` SHALL return only categories whose `type` is `"tool"`.
4. WHEN `CategoryDao.findByType` is called with `"service"`, THE `CategoryDao` SHALL return only categories whose `type` is `"service"`.
5. IF the `category` table contains no rows matching the given type, THEN THE `CategoryDao` SHALL return an empty list without throwing an exception.
6. THE `Tool` model SHALL include a `categoryId` field (Integer, nullable) with getter and setter.
7. THE `Service` model SHALL include a `categoryId` field (Integer, nullable) with getter and setter.
8. WHEN `ToolDao.findAll` is called, THE `ToolDao` SHALL read the `category_id` column from the result set and populate `Tool.categoryId` (null if the column value is SQL NULL).
9. WHEN `ServiceDao.findAll` is called, THE `ServiceDao` SHALL read the `category_id` column from the result set and populate `Service.categoryId` (null if the column value is SQL NULL).
10. WHEN `ToolDao.insert` or `ToolDao.update` is called, THE `ToolDao` SHALL write `Tool.categoryId` to the `category_id` column (NULL if `categoryId` is null).
11. WHEN `ServiceDao.insert` or `ServiceDao.update` is called, THE `ServiceDao` SHALL write `Service.categoryId` to the `category_id` column (NULL if `categoryId` is null).
12. THE `ToolFormDialog` SHALL display a `ComboBox<Category>` populated with all categories of type `"tool"`, loaded via `CategoryDao.findByType("tool")`.
13. THE `ServiceFormDialog` SHALL display a `ComboBox<Category>` populated with all categories of type `"service"`, loaded via `CategoryDao.findByType("service")`.
14. WHEN a `ToolFormDialog` is opened in edit mode with a Tool that has a non-null `categoryId`, THE `ToolFormDialog` SHALL pre-select the matching `Category` in the ComboBox.
15. WHEN a `ServiceFormDialog` is opened in edit mode with a Service that has a non-null `categoryId`, THE `ServiceFormDialog` SHALL pre-select the matching `Category` in the ComboBox.
16. WHEN the user submits the form without selecting a category, THE `ToolFormDialog` and `ServiceFormDialog` SHALL accept the submission and set `categoryId` to null on the resulting model object.

---

### Requirement 2: Approval Workflow

**User Story:** As a host, I want to approve or hide my listings directly from the table view so that I can control which tools and services are visible to renters.

#### Acceptance Criteria

1. WHEN a new Tool is created via `ToolFormDialog`, THE `ToolDao` SHALL insert the record with `is_active = 0`. THE `ToolFormDialog` SHALL NOT display an `isActive` checkbox; approval is exclusively controlled through the table view action buttons.
2. WHEN a new Service is created via `ServiceFormDialog`, THE `ServiceDao` SHALL insert the record with `is_active = 0`. THE `ServiceFormDialog` SHALL NOT display an `isActive` checkbox; approval is exclusively controlled through the table view action buttons.
3. THE `ToolController` table view SHALL display an "Approve" action button in each row where `isActive` is `false`.
4. THE `ToolController` table view SHALL display a "Hide" action button in each row where `isActive` is `true`.
5. THE `ServiceController` table view SHALL display an "Approve" action button in each row where `isActive` is `false`.
6. THE `ServiceController` table view SHALL display a "Hide" action button in each row where `isActive` is `true`.
7. WHEN the user clicks "Approve" on a Tool row, THE `ToolDao` SHALL execute `UPDATE tool SET is_active = 1 WHERE id = ?` and THE `ToolController` SHALL refresh the table view.
8. WHEN the user clicks "Hide" on a Tool row, THE `ToolDao` SHALL execute `UPDATE tool SET is_active = 0 WHERE id = ?` and THE `ToolController` SHALL refresh the table view.
9. WHEN the user clicks "Approve" on a Service row, THE `ServiceDao` SHALL execute `UPDATE service SET is_active = 1 WHERE id = ?` and THE `ServiceController` SHALL refresh the table view.
10. WHEN the user clicks "Hide" on a Service row, THE `ServiceDao` SHALL execute `UPDATE service SET is_active = 0 WHERE id = ?` and THE `ServiceController` SHALL refresh the table view.
11. THE `ToolDao` SHALL provide a `setActive(int id, boolean active)` method that executes the targeted `UPDATE` statement described in criteria 7 and 8.
12. THE `ServiceDao` SHALL provide a `setActive(int id, boolean active)` method that executes the targeted `UPDATE` statement described in criteria 9 and 10.
13. THE `ToolController` SHALL display a filter toggle control with three options: `All`, `Active Only`, and `Pending`.
14. THE `ServiceController` SHALL display a filter toggle control with three options: `All`, `Active Only`, and `Pending`.
15. WHEN the filter is set to `Active Only`, THE `ToolController` SHALL show only rows where `isActive` is `true` using the `FilteredList` predicate.
16. WHEN the filter is set to `Pending`, THE `ToolController` SHALL show only rows where `isActive` is `false` using the `FilteredList` predicate.
17. WHEN the filter is set to `All`, THE `ToolController` SHALL show all rows regardless of `isActive`.
18. WHEN the filter is set to `Active Only`, THE `ServiceController` SHALL show only rows where `isActive` is `true` using the `FilteredList` predicate.
19. WHEN the filter is set to `Pending`, THE `ServiceController` SHALL show only rows where `isActive` is `false` using the `FilteredList` predicate.
20. WHEN the filter is set to `All`, THE `ServiceController` SHALL show all rows regardless of `isActive`.

---

### Requirement 3: Quality Score

**User Story:** As a host, I want to see a quality score for each listing so that I know how to improve my listings to attract more renters.

#### Acceptance Criteria

1. THE `QualityScoreService` SHALL compute a quality score for a `Tool` using the following point allocation: has non-blank `imageName` (30 pts), `description` length ≥ 100 characters (30 pts) or ≥ 50 characters (15 pts), non-null `categoryId` (15 pts), non-blank `location` (10 pts), `pricePerDay` > 0 and ≤ 500 (10 pts), `createdAt` within 30 days of the current date (5 pts). Note: the 5-point recency bonus is intentional gamification — it incentivises hosts to keep listings fresh and will naturally expire after 30 days with no user action required.
2. THE `QualityScoreService` SHALL compute a quality score for a `Service` using the following point allocation: has non-blank `imageName` (30 pts), `description` length ≥ 100 characters (30 pts) or ≥ 50 characters (15 pts), non-null `categoryId` (15 pts), non-blank `location` (10 pts), `basePrice` > 0 and ≤ 1000 (10 pts), `createdAt` within 30 days of the current date (5 pts). Note: the 5-point recency bonus is intentional gamification — it incentivises hosts to keep listings fresh and will naturally expire after 30 days with no user action required.
3. THE `QualityScoreService` SHALL return a score result containing: the raw integer score (0–100), the percentage (score / 100 × 100, rounded), the `Rating` label, a list of improvement suggestions, and a checklist of passed/failed criteria.
4. WHEN the percentage is ≥ 81, THE `QualityScoreService` SHALL set the `Rating` to `Excellent`.
5. WHEN the percentage is ≥ 51 and < 81, THE `QualityScoreService` SHALL set the `Rating` to `Good`.
6. WHEN the percentage is < 51, THE `QualityScoreService` SHALL set the `Rating` to `Needs Improvement`.
7. THE `ToolController` table view SHALL display a colored quality score badge in each row, showing the score percentage and `Rating` label.
8. THE `ServiceController` table view SHALL display a colored quality score badge in each row, showing the score percentage and `Rating` label.
9. WHEN the `Rating` is `Excellent`, THE badge SHALL use a green color style.
10. WHEN the `Rating` is `Good`, THE badge SHALL use a yellow/amber color style.
11. WHEN the `Rating` is `Needs Improvement`, THE badge SHALL use a red color style.
12. WHEN the user clicks a quality score badge in the `ToolController` table, THE `ToolController` SHALL display a detail panel or tooltip listing the checklist of passed/failed criteria and the improvement suggestions for that Tool.
13. WHEN the user clicks a quality score badge in the `ServiceController` table, THE `ServiceController` SHALL display a detail panel or tooltip listing the checklist of passed/failed criteria and the improvement suggestions for that Service.
14. THE `QualityScoreService` SHALL be a pure, stateless computation class with no database access.
15. FOR ALL valid `Tool` objects, the sum of all awarded points SHALL be between 0 and 100 inclusive.
16. FOR ALL valid `Service` objects, the sum of all awarded points SHALL be between 0 and 100 inclusive.

---

### Requirement 4: Price Suggestion

**User Story:** As a host, I want to see a suggested price when I select a category in the form so that I can price my listing competitively.

#### Acceptance Criteria

1. THE `ToolDao` SHALL provide a `getPricesByCategory(int categoryId)` method that returns a `List<BigDecimal>` of all `price_per_day` values from the `tool` table where `category_id = ?` and `is_active = 1`.
2. THE `ServiceDao` SHALL provide a `getPricesByCategory(int categoryId)` method that returns a `List<BigDecimal>` of all `base_price` values from the `service` table where `category_id = ?` and `is_active = 1`.
3. THE `PriceSuggestionService` SHALL accept a `List<BigDecimal>` of prices and return a result containing: `suggested` (median, rounded to 2 decimal places), `median`, `mean`, `min`, `max`, and `count`.
4. WHEN the price list has an even number of elements, THE `PriceSuggestionService` SHALL compute the median as the average of the two middle values after sorting.
5. WHEN the price list has an odd number of elements, THE `PriceSuggestionService` SHALL compute the median as the middle value after sorting.
6. WHEN the user selects a category in `ToolFormDialog`, THE `ToolFormDialog` SHALL call `ToolDao.getPricesByCategory` and `PriceSuggestionService` and display a suggestion label showing the suggested price, min, max, mean, and count.
7. WHEN the user selects a category in `ServiceFormDialog`, THE `ServiceFormDialog` SHALL call `ServiceDao.getPricesByCategory` and `PriceSuggestionService` and display a suggestion label showing the suggested price, min, max, mean, and count.
8. IF `getPricesByCategory` returns an empty list, THEN THE `ToolFormDialog` and `ServiceFormDialog` SHALL display a label indicating no price data is available for that category.
9. THE `PriceSuggestionService` SHALL be a pure, stateless computation class with no database access.
10. FOR ALL non-empty price lists, the median computed by `PriceSuggestionService` SHALL be ≥ the minimum value and ≤ the maximum value in the list.
11. FOR ALL non-empty price lists, the mean computed by `PriceSuggestionService` SHALL be ≥ the minimum value and ≤ the maximum value in the list.

---

### Requirement 5: Category Suggestion

**User Story:** As a host, I want the form to suggest a category as I type the name or description so that I can quickly assign the most relevant category without browsing the full list.

#### Acceptance Criteria

1. THE `CategorySuggestionService` SHALL maintain a keyword map with the following entries: `Plumbing` → [plumb, pipe, leak, drain, faucet, toilet, sink, water]; `Electrical` → [electric, wire, light, outlet, switch, circuit, power]; `Gardening` → [garden, lawn, plant, tree, grass, hedge, landscape]; `Cleaning` → [clean, wash, mop, vacuum, dust, sanitize, tidy]; `Painting` → [paint, brush, wall, color, coat, decor]; `Moving` → [move, transport, carry, relocate, delivery, haul]; `Tutoring` → [tutor, teach, lesson, study, learn, education, homework]; `IT Support` → [computer, laptop, software, tech, repair, install, network]; `Power Tools` → [drill, saw, grinder, sander, electric tool]; `Hand Tools` → [hammer, screwdriver, wrench, pliers, manual tool]; `Garden Tools` → [mower, trimmer, rake, shovel, hoe, pruner]; `Ladders` → [ladder, step, scaffold, height, climb]; `Cleaning Equipment` → [vacuum, pressure washer, carpet cleaner, steam]; `Measuring Tools` → [measure, level, tape, ruler, laser]; `Outdoor Equipment` → [tent, camping, bbq, grill, outdoor]; `Party Equipment` → [party, event, chair, table, decoration, tent].
2. WHEN `CategorySuggestionService.suggest(String text, List<Category> candidates)` is called, THE `CategorySuggestionService` SHALL only score against category names present in the `candidates` list — keyword map entries whose category name does not match any candidate SHALL be ignored. THE method SHALL return the best-matching `Category` from `candidates` and a confidence percentage, or an empty result if no keyword matches.
3. THE `CategorySuggestionService` SHALL compute confidence as `round((matchedKeywordCount / totalKeywordsForCategory) * 100)`.
4. WHEN multiple categories match, THE `CategorySuggestionService` SHALL return the one with the highest confidence percentage.
5. IF no keyword from any category matches the input text, THEN THE `CategorySuggestionService` SHALL return an empty result without throwing an exception.
6. WHILE the user is typing in the `name` or `description` field of `ToolFormDialog`, THE `ToolFormDialog` SHALL call `CategorySuggestionService.suggest` with the combined name and description text and display a suggestion label showing the best-matching category name and confidence percentage.
7. WHILE the user is typing in the `name` or `description` field of `ServiceFormDialog`, THE `ServiceFormDialog` SHALL call `CategorySuggestionService.suggest` with the combined name and description text and display a suggestion label showing the best-matching category name and confidence percentage.
8. WHEN a category suggestion is displayed in `ToolFormDialog` and the user clicks the "Apply" button next to the suggestion label, THE `ToolFormDialog` SHALL set the category `ComboBox` selection to the suggested `Category`.
9. WHEN a category suggestion is displayed in `ServiceFormDialog` and the user clicks the "Apply" button next to the suggestion label, THE `ServiceFormDialog` SHALL set the category `ComboBox` selection to the suggested `Category`.
10. WHEN no suggestion is available, THE suggestion label and "Apply" button SHALL be hidden.
11. THE `CategorySuggestionService` SHALL perform case-insensitive keyword matching against the input text.
12. THE `CategorySuggestionService` SHALL be a pure, stateless computation class with no database access.
13. FOR ALL input texts containing at least one keyword from the map, THE `CategorySuggestionService` SHALL return a confidence value between 1 and 100 inclusive.

---

### Requirement 6: Advanced Search and Filter

**User Story:** As a host, I want to filter and sort my listings by category, location, price range, and date so that I can quickly find specific items in a large list.

#### Acceptance Criteria

1. THE `ToolController` SHALL display a category filter `ComboBox` populated with all tool categories plus an "All Categories" option.
2. THE `ServiceController` SHALL display a category filter `ComboBox` populated with all service categories plus an "All Categories" option.
3. THE `ToolController` SHALL display a location filter `TextField` that filters rows by partial, case-insensitive match against `Tool.location`.
4. THE `ServiceController` SHALL display a location filter `TextField` that filters rows by partial, case-insensitive match against `Service.location`.
5. THE `ToolController` SHALL display a minimum price `TextField` and a maximum price `TextField` that filter rows by `Tool.pricePerDay`.
6. THE `ServiceController` SHALL display a minimum price `TextField` and a maximum price `TextField` that filter rows by `Service.basePrice`.
7. THE `ToolController` SHALL display a sort-by `ComboBox` with options: `Date (Newest First)`, `Date (Oldest First)`, `Price (High to Low)`, `Price (Low to High)`, `Name (A–Z)`, `Name (Z–A)`.
8. THE `ServiceController` SHALL display a sort-by `ComboBox` with options: `Date (Newest First)`, `Date (Oldest First)`, `Price (High to Low)`, `Price (Low to High)`, `Name (A–Z)`, `Name (Z–A)`.
9. WHEN any filter control value changes, THE `ToolController` SHALL update the `FilteredList` predicate to apply all active filters simultaneously without reloading data from the database.
10. WHEN any filter control value changes, THE `ServiceController` SHALL update the `FilteredList` predicate to apply all active filters simultaneously without reloading data from the database.
11. WHEN the category filter is set to a specific category, THE `ToolController` SHALL show only rows where `Tool.categoryId` equals the selected category's `id`.
12. WHEN the category filter is set to a specific category, THE `ServiceController` SHALL show only rows where `Service.categoryId` equals the selected category's `id`.
13. WHEN the category filter is set to "All Categories", THE `ToolController` and `ServiceController` SHALL not filter by category.
14. WHEN a minimum price is entered, THE `ToolController` SHALL show only rows where `Tool.pricePerDay` is ≥ the entered value.
15. WHEN a maximum price is entered, THE `ToolController` SHALL show only rows where `Tool.pricePerDay` is ≤ the entered value.
16. WHEN a minimum price is entered, THE `ServiceController` SHALL show only rows where `Service.basePrice` is ≥ the entered value.
17. WHEN a maximum price is entered, THE `ServiceController` SHALL show only rows where `Service.basePrice` is ≤ the entered value.
18. IF the minimum price field contains a non-numeric value, THEN THE `ToolController` and `ServiceController` SHALL ignore that filter field without showing an error.
19. WHEN the sort-by selection changes, THE `ToolController` SHALL re-sort the visible rows in the `FilteredList` according to the selected sort option without reloading data from the database.
20. WHEN the sort-by selection changes, THE `ServiceController` SHALL re-sort the visible rows in the `FilteredList` according to the selected sort option without reloading data from the database.
21. WHEN the user clicks a "Clear Filters" button, THE `ToolController` SHALL reset all filter controls to their default values and show all rows.
22. WHEN the user clicks a "Clear Filters" button, THE `ServiceController` SHALL reset all filter controls to their default values and show all rows.
23. THE existing text search field SHALL continue to work in combination with all new filter controls, applying all predicates simultaneously.
24. THE `ToolController` and `ServiceController` SHALL wrap the `FilteredList` in a `SortedList` bound to the `TableView` comparator property to enable column-header sorting and sort-by dropdown sorting simultaneously.
25. WHEN both the approval status filter (Requirement 2, criteria 13–20) and any advanced filter control are active simultaneously, THE `ToolController` and `ServiceController` SHALL apply both as a logical AND within a single `FilteredList` predicate — a row is visible only if it satisfies all active conditions at once. The approval status toggle and the advanced filter controls SHALL be visually grouped in the same filter bar to make their combined effect clear to the user.
