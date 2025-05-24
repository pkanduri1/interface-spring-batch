#!/bin/bash

# Set the base directory
BASE_DIR="/Users/pavankanduri/Downloads/interface-spring-batch/src/main/java/com/truist/batch"

# List of packages to create
PACKAGES=(
  ""
  "config"
  "controller"
  "exception"
  "listener"
  "mapping"
  "model"
  "processor"
  "reader"
  "service"
  "service/impl"
  "skip"
  "util"
  "writer"
)

# Create each package directory
for pkg in "${PACKAGES[@]}"; do
  mkdir -p "$BASE_DIR/$pkg"
done

echo "All packages created successfully under $BASE_DIR"
