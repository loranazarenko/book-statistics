# Book Statistics Application

RESTful API and console application for managing books and authors, parsing JSON files, and generating statistics reports.

This project provides a backend REST API for handling book and author data. It supports CRUD operations, bulk importing books from JSON files, pagination and filtering, and generating CSV statistics reports.  
A legacy console application for JSON parsing and statistics generation is also included for backward compatibility.

---

## Overview

The backend is built with **Spring Boot 3.x** and **Spring Data JPA**.

Main features:
- CRUD operations for Authors and Books
- Paginated, sorted, and filtered book listings
- Bulk upload of books from JSON files
- Flexible author formats in JSON (string or object)
- CSV statistics report generation
- Input validation and centralized error handling
- Integration and controller tests using MockMvc and H2
- Legacy multithreaded console application

---

## Domain Model

### Author
- `id` (Long) — auto-generated identifier  
- `name` (String) — unique author name  
- `country` (String) — author country  
- `birthYear` (Integer) — year of birth  

### Book
- `id` (Long) — auto-generated identifier  
- `title` (String) — book title  
- `author` (Author) — reference to author  
- `yearPublished` (Integer) — publication year  
- `genres` (List<String>) — list of genres  

Relationship: **Book → Author (many-to-one)**

---

## DTO Examples

### AuthorDto
```json
{
  "id": 1,
  "name": "Jane Austen",
  "country": "England",
  "birthYear": 1775
}
```

### BookCreateDto
```json
{
  "title": "Pride and Prejudice",
  "authorId": 1,
  "yearPublished": 1813,
  "genres": ["Romance", "Satire"]
}
```

### BookDto
```json
{
  "id": 1,
  "title": "Pride and Prejudice",
  "author": {
    "id": 1,
    "name": "Jane Austen"
  },
  "yearPublished": 1813,
  "genres": ["Romance", "Satire"]
}
```

---

## API Endpoints

All endpoints are prefixed with `/api`.  
Request and response bodies use JSON unless stated otherwise.

---

## Authors API

### GET /api/author
Retrieve all authors.

```bash
curl http://localhost:8080/api/author
```

Response: `200 OK` — list of `AuthorDto`

---

### GET /api/author/{id}
Retrieve author by ID.

```bash
curl http://localhost:8080/api/author/1
```

Responses:
- `200 OK` — `AuthorDto`
- `404 Not Found`

---

### POST /api/author
Create a new author.

```bash
curl -X POST -H "Content-Type: application/json" \
-d '{"name":"Jane Austen","country":"England","birthYear":1775}' \
http://localhost:8080/api/author
```

Responses:
- `201 Created`
- `400 Bad Request` — validation error or duplicate name

---

### PUT /api/author/{id}
Update an existing author.

```bash
curl -X PUT -H "Content-Type: application/json" \
-d '{"name":"Jane Austen Updated","country":"England","birthYear":1775}' \
http://localhost:8080/api/author/1
```

Responses:
- `200 OK`
- `404 Not Found`

---

### DELETE /api/author/{id}
Delete an author.  
Deletion fails if books are associated with the author.

```bash
curl -X DELETE http://localhost:8080/api/author/1
```

Response: `204 No Content`

---

## Books API

### POST /api/book
Create a new book.

```bash
curl -X POST -H "Content-Type: application/json" \
-d '{"title":"Pride and Prejudice","authorId":1,"yearPublished":1813,"genres":["Romance","Satire"]}' \
http://localhost:8080/api/book
```

Response: `201 Created`

---

### GET /api/book/{id}
Retrieve book by ID.

```bash
curl http://localhost:8080/api/book/1
```

Responses:
- `200 OK`
- `404 Not Found`

---

### PUT /api/book/{id}
Update an existing book.

```bash
curl -X PUT -H "Content-Type: application/json" \
-d '{"title":"Updated Title","authorId":1,"yearPublished":1813,"genres":["Romance"]}' \
http://localhost:8080/api/book/1
```

Response: `200 OK`

---

### DELETE /api/book/{id}
Delete a book (idempotent).

```bash
curl -X DELETE http://localhost:8080/api/book/1
```

Response: `204 No Content`

---

### POST /api/book/_list
Retrieve a paginated and filtered list of books.

```bash
curl -X POST -H "Content-Type: application/json" \
-d '{"page":0,"size":10,"sortBy":"title","sortOrder":"ASC","title":"Pride"}' \
http://localhost:8080/api/book/_list
```

Response: `200 OK` — paginated list

---

## File Upload and Reports

### POST /api/book/upload
Bulk import books from a JSON file.

```bash
curl -X POST -F "file=@books.json" \
http://localhost:8080/api/book/upload
```

Supported author formats in JSON:

**String**
```json
"author": "George Orwell"
```

**Object**
```json
"author": {
  "name": "George Orwell",
  "country": "United Kingdom",
  "birthYear": 1903
}
```

Response: `201 Created` with import statistics

---

### POST /api/book/_report
Generate a CSV statistics report.

```bash
curl -X POST -H "Content-Type: application/json" \
-d '{"authorId":1}' \
http://localhost:8080/api/book/_report --output books_report.csv
```

Response: `200 OK` — CSV file attachment

---

## Error Handling

All errors use a unified JSON structure:

```json
{
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "Author not found with id: 999",
  "path": "/api/author/999",
  "timestamp": "2025-12-21T12:00:00"
}
```

Common error codes:
- `400 BAD_REQUEST`
- `404 NOT_FOUND`
- `409 CONFLICT`
- `500 INTERNAL_SERVER_ERROR`

---

## Running the Application

### Prerequisites
- Java 17+
- Maven 3.6+

### Local Run
```bash
mvn clean install
mvn spring-boot:run
```

Application URL:
```
http://localhost:8080
```

### Build Executable JAR
```bash
mvn package
java -jar target/book-statistics.jar
```

---

## Testing

Run all tests:
```bash
mvn test
```

Includes:
- Service unit tests (Mockito)
- Controller tests (MockMvc, H2)
- Integration tests

---

## Legacy Console Application

The legacy console version supports:
- Parsing multiple JSON files from a directory
- Streaming JSON parsing
- Multithreaded processing
- XML statistics generation
- Backward-compatible author formats

Example:
```bash
java -jar target/book-statistics.jar ./input-books ./output/stats.xml 4 genre
```

---

## Dependencies

- Spring Boot 3.x
- Spring Data JPA
- MapStruct
- Jackson
- Lombok
- JUnit 5
- Mockito
- AssertJ

---

## License

MIT License
