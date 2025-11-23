#!/bin/bash

# Performance test runner script
# Usage: ./run-performance-test.sh <directory> <attribute>

if [ "$#" -lt 2 ]; then
    echo "Usage: ./run-performance-test.sh <directory> <attribute>"
    echo "Example: ./run-performance-test.sh ./test-data genre"
    exit 1
fi

DIRECTORY=$1
ATTRIBUTE=$2
JAR_FILE="target/book-statistics.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Building project..."
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
fi

if [ ! -d "$DIRECTORY" ]; then
    echo "Directory not found: $DIRECTORY"
    exit 1
fi

echo "Starting performance test..."
echo "Directory: $DIRECTORY"
echo "Attribute: $ATTRIBUTE"
echo ""

# Run the performance test
java -cp "$JAR_FILE" com.profitsoft.application.PerformanceTest "$DIRECTORY" "$ATTRIBUTE" 2>&1 | tee performance-results.log

echo ""
echo "Performance test completed. Results saved to performance-results.log"