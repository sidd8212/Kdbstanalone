# Kdbstanalone

A Knowledge Database Standalone Application built with Java, Spring Boot, Vaadin, and SQLite.

## Features

- Hierarchical knowledge base with topics and subtopics
- SQLite database for persistent storage
- Vaadin-based interactive UI
- Add/delete topics and subtopics
- Like counter for entries
- Timestamps for creation and updates

## Database Structure

Each knowledge entry has:
- **ID**: Unique numeric identifier (auto-generated)
- **Parent ID**: Reference to parent entry (null for top-level topics)
- **Text Content**: Up to 20,000 characters
- **Created At**: Timestamp of creation
- **Updated At**: Timestamp of last update
- **Like Counter**: Number of likes

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Building and Running

1. Clone the repository:
```bash
git clone https://github.com/sidd8212/Kdbstanalone.git
cd Kdbstanalone
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

4. Open your browser and navigate to:
```
http://localhost:8080
```

## Usage

- **Add a Topic**: Enter a title in the "Topic Title" field and click "Add Topic"
- **View Subtopics**: Click on a topic header to expand and see subtopics
- **Add Subtopic**: Expand a topic and use the "Add Subtopic" text area
- **Like an Entry**: Click the heart (♥) button to increment the like counter
- **Delete an Entry**: Click the "Delete" button to remove an entry

## Technology Stack

- **Backend**: Java, Spring Boot, Spring Data JPA
- **Frontend**: Vaadin 23.3
- **Database**: SQLite
- **Build Tool**: Maven