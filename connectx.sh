#!/bin/bash

# Connect X script for testing
# Lorenzo Peronese

parametri=("4 4 4" "5 4 4" "6 4 4" "7 4 4" "4 5 4" "5 5 4" "6 5 4" "7 5 4" "4 6 4" "5 6 4" "6 6 4" "7 6 4" "4 7 4" "5 7 4" "6 7 4" "7 7 4" "5 4 5" "6 4 5" "7 4 5" "4 5 5" "5 5 5" "6 5 5" "7 5 5" "4 6 5" "5 6 5" "6 6 5" "7 6 5" "4 7 5" "5 7 5" "6 7 5" "7 7 5")
parametri_grossi=("20 20 10" "30 30 10" "40 40 10" "50 50 10")
output_file1="1.out"
output_file2="2.out"

> "$output_file1"
> "$output_file2"

for parametro in "${parametri[@]}"; do
    echo "$parametro" >> "$output_file1"
    echo "$parametro" >> "$output_file2"
    java -cp "/home/students/lorenzo.peronese/ConnectX/project_37635" connectx.CXPlayerTester $parametro connectx.L6.L6 connectx.L5.L5 -v -r 100 >> "$output_file1"
    java -cp "/home/students/lorenzo.peronese/ConnectX/project_37635" connectx.CXPlayerTester $parametro connectx.L5.L5 connectx.L6.L6 -v -r 100 >> "$output_file2"
    echo "--------------------------------------" >> "$output_file1"
    echo "--------------------------------------" >> "$output_file2"
done
for parametro in "${parametri_grossi[@]}"; do
    echo "$parametro" >> "$output_file1"
    echo "$parametro" >> "$output_file2"
    java -cp "/home/students/lorenzo.peronese/ConnectX/project_37635" connectx.CXPlayerTester $parametro connectx.L6.L6 connectx.L5.L5 -v -r 25 >> "$output_file1"
    java -cp "/home/students/lorenzo.peronese/ConnectX/project_37635" connectx.CXPlayerTester $parametro connectx.L5.L5 connectx.L6.L6 -v -r 25 >> "$output_file2"
    echo "--------------------------------------" >> "$output_file1"
    echo "--------------------------------------" >> "$output_file2"
done
