@echo off
REM File: build.bat
echo Building Graph Partitioner...

REM Create directories
if not exist build mkdir build
if not exist build\classes mkdir build\classes
if not exist build\resources mkdir build\resources
if not exist build\native mkdir build\native
if not exist build\resources\native mkdir build\resources\native
if not exist build\resources\native\windows-x86-64 mkdir build\resources\native\windows-x86-64

REM Try to locate JDK include directories
set JDK_INCLUDE="%JAVA_HOME%\include"
set JDK_INCLUDE_WIN="%JAVA_HOME%\include\win32"

REM Check if JNI headers exist, otherwise try common locations
if not exist %JDK_INCLUDE%\jni.h (
    echo Warning: jni.h not found in %JDK_INCLUDE%
    echo Trying to find JDK installation...
    
    REM Try common JDK locations
    if exist "C:\Program Files\Java\jdk*\include\jni.h" (
        for /d %%i in ("C:\Program Files\Java\jdk*") do (
            set JDK_INCLUDE="%%i\include"
            set JDK_INCLUDE_WIN="%%i\include\win32"
        )
    ) else if exist "C:\Program Files (x86)\Java\jdk*\include\jni.h" (
        for /d %%i in ("C:\Program Files (x86)\Java\jdk*") do (
            set JDK_INCLUDE="%%i\include"
            set JDK_INCLUDE_WIN="%%i\include\win32"
        )
    ) else (
        echo Error: Cannot find JDK with JNI headers. Please set JAVA_HOME to a valid JDK installation.
        exit /b 1
    )
)

echo Using JNI headers from: %JDK_INCLUDE%
echo Using JNI platform headers from: %JDK_INCLUDE_WIN%

REM Compile C to shared library
gcc -Wall -Wextra -fPIC -I%JDK_INCLUDE% -I%JDK_INCLUDE_WIN% -I. ^
    graf.c podzial.c algorytm_kl.c algorytm_hybrydowy.c utils.c src\native\jni_wrapper.c ^
    -o build\native\graphpartitioner.dll -shared

REM Copy native library to resources
copy build\native\graphpartitioner.dll build\resources\native\windows-x86-64\

REM Create manifest
echo Main-Class: pl.edu.graph.MainApplication > Manifest.txt
echo Class-Path: . >> Manifest.txt

REM Compile Java
javac -d build\classes src\main\java\pl\edu\graph\model\*.java src\main\java\pl\edu\graph\jni\*.java src\main\java\pl\edu\graph\ui\*.java src\main\java\pl\edu\graph\*.java

REM Create JAR
jar cfm build\graphpartitioner.jar Manifest.txt -C build\classes . -C build\resources .

echo Build complete! Run with: java -jar build\graphpartitioner.jar