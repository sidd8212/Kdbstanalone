# Knowledge Base — Standalone Edition

A self-contained, hierarchical knowledge management application built with **Spring Boot** and **Vaadin Flow**. Topics are stored in an embedded H2 database with no external infrastructure required.

---

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Build & Run](#build--run)
- [Configuration](#configuration)
- [Usage Guide](#usage-guide)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Database Console](#database-console)

---

## Features

| Category | Capability |
|---|---|
| **Topics** | Create root topics and nested child topics |
| **Hierarchy** | Unlimited-depth tree with parent → child relationships |
| **Editing** | Title and content editor with auto-save (1.5 s debounce) |
| **Search** | Real-time full-text search across titles and content |
| **Highlighting** | Matched keywords highlighted in yellow in search results |
| **Keyboard Nav** | Navigate search results with `↑` `↓` `Enter` `Escape` |
| **Import** | Paste content to create a new topic or append to the current one |
| **Breadcrumbs** | Clickable ancestor path shown above every topic |
| **View Count** | Tracks how many times each topic has been opened |
| **Timestamps** | Auto-generated created-at and last-updated timestamps |
| **URL Routing** | Direct link to any topic via `/?<id>` |
| **Delete** | Cascade-delete with confirmation dialog |
| **Notifications** | Success, warning, and error notifications for all actions |
| **Save Status** | Inline Saving… / Saved ✓ / Save failed indicator |

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.1.4 |
| UI | Vaadin Flow | 24.2.1 |
| Persistence | Spring Data JPA / Hibernate | 3.1.4 |
| Database | SQLite (embedded file mode) | 3.44.1.0 (via `sqlite-jdbc`) |
| Hibernate dialect | hibernate-community-dialects | 6.2.9.Final |
| Boilerplate | Lombok | 1.18.30 |
| Build | Maven | 3.x |
| Packaging | WAR | — |

---

## Project Structure

```
src/main/java/com/knowledgebase/
├── KnowledgeBaseApplication.java   # Spring Boot entry point
├── config/
│   └── DatabaseConfig.java         # JPA / DataSource / transaction config
├── model/
│   └── KnowledgeEntry.java         # JPA entity (id, title, content, parent, timestamps, viewCount)
├── repository/
│   └── KnowledgeEntryRepository.java  # Spring Data JPA + custom JPQL search queries
├── service/
│   └── KnowledgeEntryService.java  # Business logic: CRUD, search, view-count increment
└── ui/
    └── MainView.java               # Vaadin full-page view (tree, search, editor)

src/main/resources/
└── application.properties          # H2 URL, Hibernate DDL, Vaadin package whitelist
```

---

## Prerequisites

- **Java 17** or higher (`java -version`)
- **Maven 3.6+** (`mvn -version`)
- No database server, no Docker — everything is embedded.

---

## Build & Run

### Development mode

```bash
mvn spring-boot:run
```

The application starts at **http://localhost:8080**.

### Production WAR

```bash
mvn clean package -Pproduction
```

Deploy the resulting `target/knowledge-base-1.0-SNAPSHOT.war` to any Jakarta EE-compatible servlet container (Tomcat 10+, WildFly, etc.).

---

## Configuration

All settings are in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:sqlite:./knowledgebase.db` | SQLite file path (relative to working directory) |
| `spring.datasource.driver-class-name` | `org.sqlite.JDBC` | SQLite JDBC driver |
| `spring.jpa.database-platform` | `org.hibernate.community.dialect.SQLiteDialect` | Hibernate dialect for SQLite |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema strategy (`update` keeps data between restarts) |
| `spring.jpa.show-sql` | `true` | Log generated SQL to console |
| `spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation` | `true` | Required for SQLite LOB handling |
| `vaadin.whitelisted-packages` | `com.knowledgebase` | Vaadin component scanning scope |

---

## Usage Guide

### Creating topics

- Click **New Topic** in the left panel to create a root-level topic.
- Select an existing topic, then click **New Child** to add a child topic beneath it.

### Editing topics

- Select any topic from the tree to open it in the editor.
- Edit the **Title** or **Content** — changes are auto-saved after 1.5 seconds.
- Press **Ctrl+S** or click **Save** to save immediately.
- The **Save Status** indicator (`Saving… → Saved ✓`) confirms every write.

### Searching

- Type in the **Search topics…** field at the top of the left panel.
- The tree switches to a flat results list showing:
  - Highlighted keyword matches (yellow)
  - Ancestor path for each result
  - Content snippet around the match
  - Creation date
- Click any result to navigate to that topic.
- Use `↑` / `↓` to move focus, `Enter` to open, `Escape` to clear the search.

### Importing content

- Click the **Import** button in the editor toolbar.
- Paste any text content.
- Choose:
  - **Create New Topic** — creates a new root topic with the pasted content.
  - **Append to Current** — appends the pasted text to the currently open topic.

### Deleting topics

- Click **Delete** in the editor toolbar.
- A confirmation dialog appears. Confirming deletes the topic and **all its descendants** (cascade delete).

### Direct links

- Each topic has a unique URL: `http://localhost:8080/<id>`
- The URL updates automatically as you navigate, so you can bookmark or share any topic directly.

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl + S` | Save current topic |
| `↓` | Move focus to next search result |
| `↑` | Move focus to previous search result |
| `Enter` | Open focused search result |
| `Escape` | Clear search and return to tree view |

---

## Database File

The SQLite database is stored as **`knowledgebase.db`** in the working directory (wherever you launch the application from).

You can inspect or query it directly with any SQLite client:

```bash
sqlite3 knowledgebase.db
sqlite> SELECT id, title, view_count FROM knowledge_entries;
sqlite> .quit
```

Popular GUI tools: [DB Browser for SQLite](https://sqlitebrowser.org/), [DBeaver](https://dbeaver.io/), [TablePlus](https://tableplus.com/).
