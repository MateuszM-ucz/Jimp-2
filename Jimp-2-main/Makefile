# Directories
SRC_DIR = src\main\java
NATIVE_DIR = .
BUILD_DIR = build
CLASSES_DIR = $(BUILD_DIR)\classes
RESOURCES_DIR = $(BUILD_DIR)\resources
NATIVE_BUILD_DIR = $(BUILD_DIR)\native
OUTPUT_NATIVE_DIR = $(RESOURCES_DIR)\native

# Native sources
NATIVE_SRCS = $(NATIVE_DIR)\graf.c $(NATIVE_DIR)\podzial.c $(NATIVE_DIR)\algorytm_kl.c \
    $(NATIVE_DIR)\algorytm_hybrydowy.c $(NATIVE_DIR)\utils.c $(NATIVE_DIR)\jni_wrapper.c

# Output files
NATIVE_LIB = $(NATIVE_BUILD_DIR)\graphpartitioner.dll
NATIVE_RES_DIR = $(OUTPUT_NATIVE_DIR)\windows-x86-64
JAR_FILE = $(BUILD_DIR)\graphpartitioner.jar

# Java compilation and packaging
JAVAC = javac
JAR = jar
JAVA = java

# C compilation
CC = gcc
CFLAGS = -Wall -Wextra -fPIC
LDFLAGS = -shared

# Default target
all: $(JAR_FILE)

# Create directories
directories:
    if not exist $(BUILD_DIR) mkdir $(BUILD_DIR)
    if not exist $(CLASSES_DIR) mkdir $(CLASSES_DIR)
    if not exist $(RESOURCES_DIR) mkdir $(RESOURCES_DIR)
    if not exist $(NATIVE_BUILD_DIR) mkdir $(NATIVE_BUILD_DIR)
    if not exist $(NATIVE_RES_DIR) mkdir $(NATIVE_RES_DIR)

# Compile C to shared library
$(NATIVE_LIB): directories
    $(CC) $(CFLAGS) -I"$(JAVA_HOME)\include" -I"$(JAVA_HOME)\include\win32" $(NATIVE_SRCS) -o $@ $(LDFLAGS)

# Copy native library to resources
$(NATIVE_RES_DIR)\graphpartitioner.dll: $(NATIVE_LIB)
    copy $< $@

# Compile Java
compile_java: directories
    $(JAVAC) -d $(CLASSES_DIR) $(SRC_DIR)\pl\edu\graph\model\*.java $(SRC_DIR)\pl\edu\graph\jni\*.java $(SRC_DIR)\pl\edu\graph\ui\*.java $(SRC_DIR)\pl\edu\graph\*.java

# Create manifest
Manifest.txt:
    @echo Main-Class: pl.edu.graph.MainApplication > $@
    @echo Class-Path: . >> $@

# Create JAR
$(JAR_FILE): compile_java $(NATIVE_RES_DIR)\graphpartitioner.dll Manifest.txt
    $(JAR) cfm $@ Manifest.txt -C $(CLASSES_DIR) . -C $(RESOURCES_DIR) .

# Run the application
run: $(JAR_FILE)
    $(JAVA) -jar $(JAR_FILE)

# Clean build artifacts
clean:
    if exist $(BUILD_DIR) rmdir /s /q $(BUILD_DIR)
    if exist Manifest.txt del Manifest.txt

.PHONY: all directories compile_java run clean