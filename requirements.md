# Knowledge Base Application — Requirements Specification

## Project Overview

The Knowledge Base is a standalone, browser-based knowledge management system. It allows users to create, organise, search, and maintain a hierarchical collection of topics. The system operates entirely with an embedded database; no external servers or services are required.

**Key characteristics:**
- Topics are arranged in an unlimited-depth parent–child tree.
- The UI is a single-page application rendered server-side with Vaadin Flow.
- All data persists in a local H2 file-based database.
- The application is packaged as a WAR for deployment to any Jakarta EE servlet container.

---

## 1. Technical Requirements

### 1.1 Technology Stack

| Component | Technology | Required Version |
|---|---|---|
| Language | Java | 17 or higher |
| Framework | Spring Boot | 3.1.x or higher |
| UI Framework | Vaadin Flow | 24.x |
| Persistence | Spring Data JPA + Hibernate | (managed by Spring Boot BOM) |
| Database | SQLite (embedded, file mode) | `sqlite-jdbc` 3.44.x |
| Hibernate dialect | hibernate-community-dialects | 6.2.x (matching Hibernate version) |
| Boilerplate reduction | Lombok | 1.18.x |
| Build tool | Maven | 3.6 or higher |
| Packaging | WAR | — |

### 1.2 Package Structure

```
com.knowledgebase
├── config        # Spring configuration beans
├── model         # JPA entity classes
├── repository    # Spring Data repositories
├── service       # Business logic services
└── ui            # Vaadin view classes
```

### 1.3 Architecture

- **Layered architecture**: UI → Service → Repository → Database.
- The UI layer must not access the repository directly; all data access goes through the service layer.
- Transaction management must be enabled via `@EnableTransactionManagement`.
- JPA repositories must be enabled via `@EnableJpaRepositories`.

---

## 2. Configuration Requirements

### 2.1 Database Configuration

- Database: SQLite in file mode. JDBC URL: `jdbc:sqlite:./knowledgebase.db`
- Driver class: `org.sqlite.JDBC` (provided by `org.xerial:sqlite-jdbc`)
- DDL strategy: `hibernate.hbm2ddl.auto=update` (schema evolves without data loss on restart)
- Dialect: `org.hibernate.community.dialect.SQLiteDialect` (provided by `org.hibernate.orm:hibernate-community-dialects`)
- Additional Hibernate property: `hibernate.jdbc.lob.non_contextual_creation=true` (required for SQLite LOB compatibility)
- Connection is managed via `DriverManagerDataSource` configured as a Spring `@Bean`
- Database file stored as `knowledgebase.db` in the application working directory

### 2.2 Application Properties

The following properties must be present in `src/main/resources/application.properties`:

```properties
# SQLite Database Configuration
spring.datasource.url=jdbc:sqlite:./knowledgebase.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect

# Hibernate configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Vaadin configurations
vaadin.whitelisted-packages=com.knowledgebase
```

### 2.3 Application Entry Point

- Main class: `KnowledgeBaseApplication` extending `SpringBootServletInitializer`
- Overrides `configure(SpringApplicationBuilder)` for WAR deployment support
- Annotated with `@SpringBootApplication`

---

## 3. Data Model

### 3.1 `KnowledgeEntry` Entity

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `Long` | Primary key, auto-generated | Identity strategy |
| `title` | `String` | Not null | Max length: default column size |
| `content` | `String` | Nullable | Max length: 20 000 characters |
| `parent` | `KnowledgeEntry` | Nullable (null = root) | Lazy-loaded `@ManyToOne` |
| `children` | `List<KnowledgeEntry>` | — | `@OneToMany(cascade=ALL)` |
| `createdAt` | `LocalDateTime` | Auto, not updatable | `@CreationTimestamp` |
| `updatedAt` | `LocalDateTime` | Auto | `@UpdateTimestamp` |
| `viewCount` | `int` | Default 0 | Incremented on each open |

- Bidirectional relationship managed via `addChild(entry)` / `removeChild(entry)` helpers.
- Cascade type `ALL` on children ensures recursive delete propagates to all descendants.

### 3.2 Repository

`KnowledgeEntryRepository` extends `JpaRepository<KnowledgeEntry, Long>` and must expose:

| Method | Query |
|---|---|
| `findRootEntries()` | `SELECT k FROM KnowledgeEntry k WHERE k.parent IS NULL` |
| `findByParentId(Long)` | Derived query by Spring Data |
| `searchEntries(String query)` | JPQL `LIKE` on both `title` and `content` (case-insensitive) |
| `searchRootEntries(String query)` | Same as above but restricted to root entries |

---

## 4. Service Layer

`KnowledgeEntryService` must provide:

| Method | Description |
|---|---|
| `getRootEntries()` | Returns all root-level entries |
| `getChildEntries(Long parentId)` | Returns direct children of a parent |
| `getEntry(Long id)` | Returns `Optional<KnowledgeEntry>` |
| `createEntry(KnowledgeEntry)` | Persists and returns the saved entry |
| `updateEntry(KnowledgeEntry)` | Persists updates and returns the saved entry |
| `deleteEntry(Long id)` | Deletes entry and all descendants (via cascade) |
| `searchEntries(String query)` | Full-text search; returns all entries if query is blank |
| `searchRootEntries(String query)` | Full-text search limited to root entries; returns all roots if blank |
| `incrementViewCount(Long id)` | Increments and persists `viewCount` for the given entry |

All methods must be `@Transactional`.

---

## 5. Main View (`MainView`)

### 5.1 Route & Page Title

- Route: `@Route("")` — serves the application root (`/`)
- Page title: `@PageTitle("Knowledge Base")`
- Implements `HasUrlParameter<Long>` for optional direct navigation to a topic by ID

### 5.2 Overall Layout

The view is composed of three vertical zones stacked full-height:

```
┌──────────────────────────────────────────────┐
│  Header bar                                  │
├─────────────────┬────────────────────────────┤
│  Left panel     │  Right panel               │
│  (flex-grow 1)  │  (flex-grow 3)             │
│                 │                            │
└─────────────────┴────────────────────────────┘
```

- No outer padding or spacing on the root layout.
- Left panel: min-width 240 px, max-width 320 px, separated by a 1 px solid border (`#e0e0e0`).

### 5.3 Header Bar

- Background: `#1565C0` (dark blue)
- Contains: book icon + application title "Knowledge Base" + subtitle "Standalone Edition"
- Title: white, `1.4 rem`
- Subtitle: `rgba(255,255,255,0.7)`, `0.85 rem`
- Box shadow: `0 2px 4px rgba(0,0,0,0.2)`

---

## 6. Left Panel — Navigation

### 6.1 Panel Header

- Label: "Topics" — font-weight 600, `0.9 rem`, color `#424242`
- Background: `#f5f5f5`, bottom border `1px solid #e0e0e0`

### 6.2 Search Field

- Placeholder: "Search topics…"
- Prefix: search icon
- Clear button visible
- Value change mode: `LAZY`, timeout 300 ms
- On change: if blank → show tree view; if non-blank → show search results panel

#### 6.2.1 Search Keyboard Interactions

| Key | Behaviour |
|---|---|
| `↓` | Move focus to the next search result |
| `↑` | Move focus to the previous search result |
| `Enter` | Load the currently focused search result |
| `Escape` | Clear the search field and return to tree view |

### 6.3 Tree View (shown when search is empty)

- Component: `TreeGrid<KnowledgeEntry>`
- Hierarchy column: entry title, `flex-grow 1`
- Second column: direct child count (`"N child(ren)"`), width 90 px, no flex grow
- Data provider: `AbstractBackEndHierarchicalDataProvider` backed by the service layer
- On selection: load the selected entry in the right panel

### 6.4 Search Results Panel (shown when search has text)

- Replaces the tree while search is active
- **Result count label** above results: `"N result(s) for "<query>""` or `"No results for "<query>""`
- Font: `0.78 rem`, color `#757575`
- Each result card contains:
  - **Title** with highlighted keyword (`<mark>` yellow `#FFF176`)
  - **Ancestor path** (e.g., `Root › Parent › Topic`) — `0.75 rem`, `#9e9e9e`
  - **Content snippet** — 80-character window centred on the first match, prefixed/suffixed with `…`; keyword highlighted
  - **Created date** — `"Created: MMM dd, yyyy HH:mm"`, `0.72 rem`, `#bdbdbd`
- Cards separated by `1px solid #f0f0f0`
- Hover: background `#E3F2FD`
- Keyboard focus: background `#BBDEFB`
- Clicking a card: clears search, selects topic in tree, loads entry in right panel
- Empty state: centred message "No matching topics found."

### 6.5 Action Buttons

- **New Topic** (primary, small) — creates a new root entry, selects it, shows notification
- **New Child** (default, small) — creates a child under the selected entry; if nothing is selected, creates a root entry
- Buttons are full-width inside a `HorizontalLayout` with top border `1px solid #e0e0e0`

---

## 7. Right Panel — Content

### 7.1 Empty State (shown when no entry is selected)

- Full-size centred `Div`
- Large book icon (64 px, `#bdbdbd`)
- Heading: "Select a topic" (`#757575`)
- Hint paragraph: "Choose a topic from the tree on the left, or create a new one."

### 7.2 Breadcrumb Bar (shown when an entry is loaded)

- Displayed above the editor
- Background: `#f5f5f5`, bottom border `1px solid #e0e0e0`
- Ancestors rendered as clickable `LUMO_TERTIARY LUMO_SMALL` buttons that navigate to that ancestor
- Current topic rendered as a non-interactive bold `Span` (`#424242`)
- Separator: ` › ` in `#bdbdbd`
- Updates on every navigation and on title auto-save

### 7.3 Toolbar

Contains (left-aligned):
- **Save** button (primary, small) — manual save with notification
- **Delete** button (error, small) — opens confirmation dialog
- **Import** button (default, small) — opens import dialog

Contains (right-aligned):
- **View count badge** — styled pill, background `#E3F2FD`, text `#1565C0`, border-radius 12 px

**Save Status Indicator** (inline, next to buttons):

| State | Text | Colour |
|---|---|---|
| Writing | `Saving…` | `#FF8F00` (amber) |
| Success | `Saved ✓` | `#388E3C` (green) |
| Failure | `Save failed` | `#D32F2F` (red) |

### 7.4 Title Field

- Label: "Title"
- Full width
- Value change mode: `LAZY`, timeout 1500 ms
- On change: triggers auto-save

### 7.5 Content Area

- Label: "Content"
- Full width, min-height 350 px, expands to fill available space
- Value change mode: `LAZY`, timeout 1500 ms
- On change: triggers auto-save
- Maximum content length: 20 000 characters (enforced by DB column)

### 7.6 Metadata Row

Displayed below the content area:

- **Created:** `"Created: MMM dd, yyyy HH:mm"` — `0.78 rem`, `#9e9e9e`
- **Last updated:** `"Last updated: MMM dd, yyyy HH:mm"` — updated after every save
- Both timestamps auto-generated by Hibernate (`@CreationTimestamp`, `@UpdateTimestamp`)

---

## 8. Entry Management

### 8.1 Create Entry

- Trigger: "New Topic" or "New Child" button
- Interface: creates entity directly (no modal); immediately selects and loads the new entry
- Default title: `"New Topic"`, default content: empty string
- Feedback: success notification `"Topic created successfully"`

### 8.2 Load / View Entry

- Trigger: click in tree, click in search results, breadcrumb click, URL parameter
- Behaviour:
  - Hides empty state, shows content panel
  - Populates title, content, view count, timestamps
  - Rebuilds breadcrumb
  - Increments view count via service
  - Updates URL to `/<id>`

### 8.3 Auto-Save

- Triggers: title or content field value change after 1500 ms debounce
- Behaviour:
  - Sets save status to `"Saving…"`
  - Calls `service.updateEntry()`
  - Refreshes tree item and breadcrumb
  - Refreshes `updatedAt` from the persisted entity
  - Sets save status to `"Saved ✓"` on success or `"Save failed"` on error

### 8.4 Manual Save

- Trigger: Save button click or `Ctrl+S`
- Same as auto-save but also shows a `LUMO_SUCCESS` notification `"Saved successfully"`

### 8.5 Delete Entry

- Trigger: Delete button
- Interface: `ConfirmDialog`
  - Header: `"Delete Topic"`
  - Body: `"Are you sure you want to delete "<title>"? This will also delete all child topics."`
  - Confirm button theme: `"error primary"`, label: `"Delete"`
  - Cancel button: `"Cancel"`
- On confirm:
  - Calls `service.deleteEntry(id)` (cascades to all descendants)
  - Deselects tree, refreshes tree, navigates URL to root (`/`)
  - Shows empty state
  - Shows `LUMO_CONTRAST` notification `"Topic deleted"`

---

## 9. Import Functionality

### 9.1 Import Dialog

- Trigger: Import button in editor toolbar
- Components:
  - Instructions paragraph
  - `TextField` for topic title (pre-filled with `"Import into: <currentTitle>"` when a topic is open)
  - `TextArea` for pasted content (min-height 200 px), placeholder `"Paste your content here…"`

### 9.2 Options

| Button | Behaviour |
|---|---|
| **Create New Topic** | Creates a new root `KnowledgeEntry` with the given title and pasted content |
| **Append to Current** | Appends a blank line + pasted content to `currentEntry.content`, saves |
| **Cancel** | Closes dialog without action |

### 9.3 Validation

- Empty content → `LUMO_WARNING` notification, dialog stays open
- No topic selected (for Append) → `LUMO_WARNING` notification
- Service error → `LUMO_ERROR` notification with exception message

---

## 10. Search Functionality

### 10.1 Search Trigger

- Value change on the search field (300 ms debounce)
- Empty query → restore tree view, clear results list

### 10.2 Search Query

- Case-insensitive `LIKE '%query%'` on both `title` and `content`
- Implemented as JPQL named queries in the repository

### 10.3 Result Highlighting

- Matched text wrapped in: `<mark style="background-color:#FFF176;color:#212121;border-radius:2px;">`
- Applied to both the title and the content snippet in each result card
- HTML is injected via `getElement().setProperty("innerHTML", ...)` — user input is HTML-escaped before wrapping

### 10.4 Keyboard Navigation

- `↓` increases focus index (clamped to result count)
- `↑` decreases focus index (clamped to 0)
- Focused card: background `#BBDEFB`
- `Enter`: loads focused result, clears search
- `Escape`: clears search field, restores tree view

---

## 11. URL Routing

- `MainView` implements `HasUrlParameter<Long>` with `@OptionalParameter`
- When `topicId` is present in the URL, the corresponding entry is loaded on page load
- On every topic selection, the URL is updated: `ui.navigate(MainView.class, entry.getId())`
- On delete, the URL is cleared: `ui.navigate(MainView.class)`

---

## 12. Error Handling

- All service calls in the UI layer must be wrapped in `try/catch(Exception)`
- On error: display a `LUMO_ERROR` notification with the exception message
- Save status indicator must reflect failed saves
- Notifications display for 3 seconds at `BOTTOM_END` position

---

## 13. Notifications

| Action | Variant | Message |
|---|---|---|
| Topic created | `LUMO_SUCCESS` | "Topic created successfully" |
| Topic saved | `LUMO_SUCCESS` | "Saved successfully" |
| Topic deleted | `LUMO_CONTRAST` | "Topic deleted" |
| Import success | `LUMO_SUCCESS` | "Topic imported successfully" / "Content appended successfully" |
| Empty content on import | `LUMO_WARNING` | "Content cannot be empty" |
| Append with no topic | `LUMO_WARNING` | "No topic selected to append to" |
| Any service error | `LUMO_ERROR` | Error message from exception |

All notifications: duration 3000 ms, position `BOTTOM_END`.

---

## 14. UI Design Standards

### 14.1 Colour Palette

| Token | Hex | Usage |
|---|---|---|
| Primary blue | `#1565C0` | Header background, view-count badge text, links |
| Primary blue light | `#E3F2FD` | View-count badge background |
| Surface | `#f5f5f5` | Panel headers, breadcrumb bar |
| Background | `#fafafa` | Left panel background |
| Border | `#e0e0e0` | Panel separators |
| Result hover | `#E3F2FD` | Search result hover |
| Result focus | `#BBDEFB` | Keyboard-focused search result |
| Highlight | `#FFF176` | Keyword match background |
| Success green | `#388E3C` | Save status (saved) |
| Warning amber | `#FF8F00` | Save status (saving) |
| Error red | `#D32F2F` | Save status (failed) |

### 14.2 Typography

| Element | Size | Weight | Colour |
|---|---|---|---|
| App title | `1.4 rem` | Normal | White |
| Panel label | `0.9 rem` | 600 | `#424242` |
| Result title | `0.88 rem` | 600 | `#212121` |
| Result path | `0.75 rem` | Normal | `#9e9e9e` |
| Result snippet | `0.78 rem` | Normal | `#616161` |
| Result date | `0.72 rem` | Normal | `#bdbdbd` |
| Metadata spans | `0.78 rem` | Normal | `#9e9e9e` |
| Result count | `0.78 rem` | Normal | `#757575` |
| Save status | `0.78 rem` | Normal | State-dependent |

### 14.3 Spacing

- Outer layout: no padding, no spacing
- Left panel buttons: `padding: true` (Vaadin default)
- Editor area: `padding: true`, `spacing: true`
- Breadcrumb: `padding: true`, `spacing: false`

---

## 15. Keyboard Shortcuts

| Shortcut | Scope | Action |
|---|---|---|
| `Ctrl + S` | Editor area | Save current entry |
| `↓` | Search field | Focus next result |
| `↑` | Search field | Focus previous result |
| `Enter` | Search field | Open focused result |
| `Escape` | Search field | Clear search, restore tree |

---

## 16. Performance Requirements

- Search query: must return results in under 500 ms for databases with up to 10 000 entries.
- Tree rendering: root-level items must load in under 200 ms.
- Auto-save debounce: 1500 ms — balances responsiveness with write frequency.
- Search debounce: 300 ms — provides real-time feel without overloading the database.

---

## 17. Accessibility

- All interactive controls (buttons, fields) must carry visible labels or `aria-label` attributes (provided by Vaadin components by default).
- Keyboard navigation must be fully functional without a mouse.
- Colour contrast for body text must meet WCAG 2.1 AA (minimum 4.5:1 for normal text).
- Focus indicators must be visible on all interactive elements.

---

## 18. Testing Requirements

### 18.1 Repository Layer

- `findRootEntries()` returns only entries where `parent IS NULL`
- `findByParentId()` returns only direct children
- `searchEntries()` matches on title, content, case-insensitively
- `searchRootEntries()` limits results to root entries

### 18.2 Service Layer

- `createEntry()` persists and returns entity with auto-generated `id` and `createdAt`
- `updateEntry()` updates `updatedAt` on each call
- `deleteEntry()` cascades to all children
- `incrementViewCount()` atomically increments and persists
- `searchEntries("")` (blank) returns all entries

### 18.3 UI / Integration

- Selecting a tree node loads the entry in the content panel
- Typing in the search field switches to the results panel
- Clearing the search field restores the tree
- `Ctrl+S` triggers save and shows notification
- Delete confirmation dialog appears before deletion
- URL updates on navigation and clears on delete
