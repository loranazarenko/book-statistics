# Book Statistics Parser

Console application for parsing JSON files with books and generating statistics on various attributes using multithreaded processing.

## Subject Area

The project works with two entities in a many-to-one relationship:

### Primary Entity: Book
**Attributes:**
- `title` (String) - book title
- `author` (Author) - book author (supports both String and Author object)
- `year_published` (Integer) - year of publication
- `genre` (List<String>) - list of genres (comma-separated)

### Secondary Entity: Author
**Attributes:**
- `name` (String) - author's name
- `country` (String) - country
- `birth_year` (Integer) - year of birth

**Relationship:** Book → Author (many-to-one)

## Project Structure

```
src/main/java/com/profitsoft/application/
├── entities/
│   ├── Book.java              # Primary entity
│   ├── Author.java            # Secondary entity
│   └── StatisticsItem.java    # Statistics element
├── service/
│   └── StatisticsService.java # Multithreaded file processing service
├── utils/
│   ├── BookJsonParser.java         # JSON parser (streaming API)
│   ├── StatisticsCalculator.java   # Statistics calculator
│   ├── XmlStatisticsWriter.java    # XML generator
│   ├── ResultPrinter.java          # Output formatter
│   └── TestDataGenerator.java      # Test data generator
├── Application.java                # Main entry point
└── PerformanceTest.java            # Performance testing tool
```

## Features

- Parse multiple JSON files from a directory
- Streaming parsing (doesn't load entire files into memory)
- Multithreaded file processing with configurable thread pool
- Statistics on 4 attributes: title, author, year_published, genre
- XML output generation
- Comma-separated genre processing
- Results sorted by count (highest to lowest)

## Input Data Examples

### Simple Format (String Author)
```json
[
  {
    "title": "1984",
    "author": "George Orwell",
    "year_published": 1949,
    "genre": "Dystopian, Political Fiction"
  },
  {
    "title": "Pride and Prejudice",
    "author": "Jane Austen",
    "year_published": 1813,
    "genre": "Romance, Satire"
  },
  {
    "title": "Romeo and Juliet",
    "author": "William Shakespeare",
    "year_published": 1597,
    "genre": "Romance, Tragedy"
  }
]
```

### Extended Format (Author Object)
```json
[
  {
    "title": "1984",
    "author": {
      "name": "George Orwell",
      "country": "UK",
      "birth_year": 1903
    },
    "year_published": 1949,
    "genre": "Dystopian, Political Fiction"
  }
]
```

## Output Example

### File: `statistics_by_genre.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<statistics>
  <item>
    <value>Romance</value>
    <count>2</count>
  </item>
  <item>
    <value>Dystopian</value>
    <count>1</count>
  </item>
  <item>
    <value>Political Fiction</value>
    <count>1</count>
  </item>
  <item>
    <value>Satire</value>
    <count>1</count>
  </item>
  <item>
    <value>Tragedy</value>
    <count>1</count>
  </item>
</statistics>
```

## Installation and Build

### Requirements
- Java 21+
- Maven 3.8+

### Building the Project
```bash
mvn clean package
```

After building, the JAR file will be located at `target/book-statistics.jar`

## Usage

### Basic Usage
```bash
java -jar target/book-statistics.jar --dir <path> --attribute <name> [--threads <count>]
```

### Parameters
- `--dir <path>` - path to directory with JSON files (required)
- `--attribute <name>` - attribute for statistics (required)
    - Available: `title`, `author`, `year_published`, `genre`
- `--threads <count>` - number of threads (optional, default: 4)

### Examples

**Statistics by genre with 4 threads:**
```bash
java -jar target/book-statistics.jar --dir ./books --attribute genre --threads 4
```

**Statistics by author with 8 threads:**
```bash
java -jar target/book-statistics.jar --dir ./data/books --attribute author --threads 8
```

**Interactive mode (without parameters):**
```bash
java -jar target/book-statistics.jar
```

## Performance Testing

### Step 1: Generate Test Data
```bash
# Generate 10 files with 1000 books each = 10,000 books
java -cp target/book-statistics.jar \
  com.profitsoft.application.utils.TestDataGenerator \
  ./test-data 10 1000

# For large dataset: 100 files with 5000 books = 500,000 books
java -cp target/book-statistics.jar \
  com.profitsoft.application.utils.TestDataGenerator \
  ./test-data-large 100 5000
```

### Step 2: Run Performance Tests
```bash
# Using Java directly
java -cp target/book-statistics.jar \
  com.profitsoft.application.PerformanceTest \
  ./test-data genre

# Using bash script (Linux/Mac)
chmod +x run-performance-test.sh
./run-performance-test.sh ./test-data genre
```

The performance test will:
- Warm up with 2 test runs
- Execute 5 test runs for each thread count (1, 2, 4, 8)
- Display average, min, and max execution times
- Calculate speedup factor
- Show detailed breakdown (parsing, statistics calculation, XML writing)

## Threading Experiments Results

### Test Configuration
- **Processor:** Intel Core i7-9700K (8 cores)
- **Memory:** 16 GB RAM
- **Dataset:** 50 files × 2000 books = 100,000 books
- **Attribute:** genre
- **Runs:** 5 iterations (after 2 warmup runs)

### Results Summary

| Threads | Avg Time (ms) | Min (ms) | Max (ms) | Speedup |
|---------|---------------|----------|----------|---------|
| 1       | 128           | 111      | 161      | 1.00x   |
| 2       | 80            | 74       | 89       | 1.60x   |
| 4       | 60            | 52       | 73       | 2.13x   |
| 8       | 50            | 50       | 51       | 2.56x   |

### Detailed Time Breakdown

| Threads | Parsing (ms) | Statistics (ms) | XML (ms) |
|---------|--------------|-----------------|----------|
| 1       | 105          | 22              | 1        |
| 2       | 56           | 22              | 1        |
| 4       | 39           | 20              | 1        |
| 8       | 26           | 22              | 1        |

### Conclusions

1. **Optimal Thread Count:** 4-8 threads show the best performance for this configuration.

2. **Scalability:**
    - 2 threads: ~1.6x speedup (80% efficiency)
    - 4 threads: ~2.1x speedup (53% efficiency)
    - 8 threads: ~2.6x speedup (32% efficiency)

3. **Bottlenecks:**
    - File parsing benefits most from parallelization
    - Statistics calculation and XML writing are independent of parsing thread count
    - At 8 threads, efficiency decreases due to thread management overhead

4. **Recommendations:**
    - Small datasets (<10,000 books): 2-4 threads
    - Medium datasets (10,000-100,000 books): 4 threads
    - Large datasets (>100,000 books): 4-8 threads
    - Don't exceed 2× the number of physical CPU cores

## Optimizations

### Memory Management
- **Jackson Streaming API:** File parsing doesn't load entire JSON into memory
- **Consumer Pattern:** Books are processed as they're parsed
- **Concurrent Collections:** Using `ConcurrentHashMap` and `LongAdder` for thread-safe aggregation

### Multithreading
- **ExecutorService:** Thread pool for parallel file processing
- **CountDownLatch:** Synchronization of all task completions
- **Auto-configuration:** Maximum thread count limited to `2 × CPU cores`

## Running Tests

```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=StatisticsCalculatorTest

# With verbose output
mvn test -X
```

## Dependencies

- **Jackson 2.15.2** - JSON parsing
- **Lombok 1.18.38** - Reducing boilerplate code
- **JUnit 5.10.0** - Unit testing
- **AssertJ 3.24.1** - Fluent assertions
- **SLF4J 2.0.7** - Logging

## License

MIT License

## Author

Project developed as part of the "Java Core Block 1" assignment