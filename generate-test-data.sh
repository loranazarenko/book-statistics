#!/bin/bash

# Script to generate test data for Book Statistics Parser
# Usage: ./generate-test-data.sh [small|medium|large|custom]

set -e

JAR_FILE="target/book-statistics.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo " JAR file not found. Building project..."
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo " Build failed!"
        exit 1
    fi
    echo " Build successful!"
fi

# Function to generate data
generate_data() {
    local output_dir=$1
    local file_count=$2
    local books_per_file=$3
    local total_books=$((file_count * books_per_file))

    echo "   Generating test data..."
    echo "   Output directory: $output_dir"
    echo "   Files: $file_count"
    echo "   Books per file: $books_per_file"
    echo "   Total books: $total_books"
    echo ""

    java -cp "$JAR_FILE" \
        com.profitsoft.application.utils.TestDataGenerator \
        "$output_dir" "$file_count" "$books_per_file"

    if [ $? -eq 0 ]; then
        echo ""
        echo "Test data generated successfully!"
        echo "Location: $output_dir"
        echo "Run analysis: java -jar $JAR_FILE --dir $output_dir --attribute genre --threads 4"
    else
        echo "Failed to generate test data"
        exit 1
    fi
}

# Main logic
case "${1:-medium}" in
    small)
        echo "Generating SMALL dataset (for quick tests)"
        generate_data "./test-data-small" 5 100
        ;;
    medium)
        echo "Generating MEDIUM dataset (for normal testing)"
        generate_data "./test-data-medium" 20 1000
        ;;
    large)
        echo "Generating LARGE dataset (for performance testing)"
        generate_data "./test-data-large" 100 5000
        ;;
    custom)
        if [ $# -lt 4 ]; then
            echo "Usage: $0 custom <output_dir> <file_count> <books_per_file>"
            echo "Example: $0 custom ./my-data 50 2000"
            exit 1
        fi
        echo "Generating CUSTOM dataset"
        generate_data "$2" "$3" "$4"
        ;;
    *)
        echo "Usage: $0 [small|medium|large|custom]"
        echo ""
        echo "Presets:"
        echo "  small  - 5 files × 100 books = 500 books"
        echo "  medium - 20 files × 1000 books = 20,000 books (default)"
        echo "  large  - 100 files × 5000 books = 500,000 books"
        echo ""
        echo "Custom:"
        echo "  $0 custom <output_dir> <file_count> <books_per_file>"
        exit 1
        ;;
esac