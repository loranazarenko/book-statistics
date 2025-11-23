# Quick Start Guide

Fast guide for setting up and using Book Statistics Parser.

## 1. Installation

```bash
# Clone the repository
git clone <your-repo-url>
cd book-statistics

# Build the project
mvn clean package
```

Verify the build:
```bash
ls -lh target/book-statistics.jar
```

## 2. Generate Test Data

### Small Dataset (Quick Testing)
```bash
# Create 5 files with 100 books each = 500 books
mkdir test-books
java -cp target/book-statistics.jar \
  com.profitsoft.application.utils.TestDataGenerator \
  ./test-books 5 100
```

### Large Dataset (Performance Testing)
```bash
# Create 50 files with 2000 books each = 100,000 books
mkdir perf-test-data
java -cp target/book-statistics.jar \
  com.profitsoft.application.utils.TestDataGenerator \
  ./perf-test-data 50 2000
```

Expected output:
```
Generating test data...
Output directory: /path/to/test-books
Files: 5
Books per file: 100
Total books: 500
Generated 5 / 5 files
```

## 3. Run Analysis

Basic run:
```bash
java -jar target/book-statistics.jar \
  --dir ./test-books \
  --attribute genre \
  --threads 4
```

Expected output:
```
=== Results ===
Files processed: 5
Total books parsed: 500
Unique values: 20

=== Top 10 ===
  Fiction: 87
  Science Fiction: 65
  Romance: 54
  ...
Output: /path/to/statistics_by_genre.xml
Total execution time: 145 ms
```

## 4. Analyze by Different Attributes

By author:
```bash
java -jar target/book-statistics.jar \
  --dir ./test-books \
  --attribute author \
  --threads 4
```

By publication year:
```bash
java -jar target/book-statistics.jar \
  --dir ./test-books \
  --attribute year_published \
  --threads 4
```

By title:
```bash
java -jar target/book-statistics.jar \
  --dir ./test-books \
  --attribute title \
  --threads 2
```

## 5. Performance Testing

### Generate Test Data
```bash
java -cp target/book-statistics.jar \
  com.profitsoft.application.utils.TestDataGenerator \
  ./perf-test-data 50 2000
```

### Run Performance Test
```bash
java -cp target/book-statistics.jar \
  com.profitsoft.application.PerformanceTest \
  ./perf-test-data genre
```

Expected output:
```
================================================================================
PERFORMANCE TEST
Directory: /path/to/perf-test-data
Attribute: genre
Warmup runs: 2
Test runs per configuration: 5
================================================================================

Testing with 1 thread(s)
  Run completed: 3842 ms (Files: 50, Books: 100000)
  ...
Average total time: 3822 ms

================================================================================
SUMMARY
================================================================================
Threads    | Avg Time(ms) | Min(ms) | Max(ms) | Speedup   
--------------------------------------------------------------------------------
1          | 3822         | 3789    | 3891    | 1.00x
2          | 2134         | 2089    | 2198    | 1.79x
4          | 1287         | 1245    | 1356    | 2.97x
8          | 1156         | 1098    | 1234    | 3.31x
```

Results are saved to: `performance-results.log`

## 6. Working with Custom Data

Create JSON files in a directory:

**books_001.json:**
```json
[
  {
    "title": "1984",
    "author": "George Orwell",
    "year_published": 1949,
    "genre": "Dystopian, Political Fiction"
  }
]
```

Analyze:
```bash
java -jar target/book-statistics.jar \
  --dir ./my-books \
  --attribute genre \
  --threads 4
```

## 7. Verify Results

View console output:
```
=== Results ===
Files processed: 2
Total books parsed: 3
Unique values: 5
```

Check XML output:
```bash
cat statistics_by_genre.xml
```

Verify statistics:
```bash
# Count unique values
grep '<value>' statistics_by_genre.xml | wc -l

# View top results
grep -A 1 '<value>' statistics_by_genre.xml | head -10
```

## 8. Run Tests

```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=BookJsonParserTest

# With verbose output
mvn test -X
```

Expected output:
```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 9. Interactive Mode

Run without parameters:
```bash
java -jar target/book-statistics.jar
```

The program will prompt for input:
```
Directory path: 
./test-books

Attribute (title/author/year_published/genre): 
genre

Threads (default 4): 
4
```

## 10. Complete Workflow

```bash
# Build project
mvn clean package

# Generate test data
java -cp target/book-statistics.jar \
  com.profitsoft.application.utils.TestDataGenerator \
  ./my-test-data 20 1000

# Run analysis by genre
java -jar target/book-statistics.jar \
  --dir ./my-test-data \
  --attribute genre \
  --threads 4

# Run performance test
java -cp target/book-statistics.jar \
  com.profitsoft.application.PerformanceTest \
  ./my-test-data genre

# View results
ls -lh statistics_*.xml
cat performance-results.log
```

## Troubleshooting

### No JSON files found
Ensure files have `.json` extension:
```bash
ls -la ./test-books/*.json
```

### OutOfMemoryError
Increase heap memory:
```bash
java -Xmx4g -jar target/book-statistics.jar \
  --dir ./books --attribute genre
```

### Slow Performance
Increase thread count:
```bash
# Check CPU cores
nproc  # Linux
sysctl -n hw.ncpu  # Mac
```

Use more threads:
```bash
java -jar target/book-statistics.jar \
  --dir ./books --attribute genre --threads 8
```

### JAR not found
Build the project:
```bash
mvn clean package
ls -la target/book-statistics.jar
```

## Useful Commands

```bash
# Check Java version
java -version

# Clean project
mvn clean

# Rebuild without tests
mvn clean package -DskipTests

# Run with debug logging
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG \
  -jar target/book-statistics.jar \
  --dir ./books --attribute genre

# Check JAR contents
jar tf target/book-statistics.jar | grep Application

# Format XML output
xmllint --format statistics_by_genre.xml
```

## Quick Reference

| Task             |  Command                                                                                                |
|------------------|---------------------------------------------------------------------------------------------------------|
| Build            | `mvn clean package`                                                                                     |
| Generate data    | `java -cp target/book-statistics.jar com.profitsoft.application.utils.TestDataGenerator ./data 10 1000` |
| Run analysis     | `java -jar target/book-statistics.jar --dir ./data --attribute genre --threads 4`                       |
| Performance test | `java -cp target/book-statistics.jar com.profitsoft.application.PerformanceTest ./data genre`           |
| Run tests        | `mvn test`                                                                                              |

## Additional Resources

- Full documentation: [README.md](README.md)
- More examples: [EXAMPLES.md](EXAMPLES.md)
- Jackson Documentation: https://github.com/FasterXML/jackson-docs
- Java Concurrency Tutorial: https://docs.oracle.com/javase/tutorial/essential/concurrency/
- Maven Guide: https://maven.apache.org/guides/