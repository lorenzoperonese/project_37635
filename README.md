# Connect X project for the algorithm course of University of Bologna in 2022/2023
## Lorenzo Peronese, Omar Ayache

Run this commands in the `connectx/` directory.

## Build from source
```
javac -cp ".." *.java */*.java
```

## CXGame application:
### - Human vs Computer
```
java -cp ".." connectx.CXGame 6 7 4 connectx.L0.L0
```

### - Computer vs Computer
```
java -cp ".." connectx.CXGame 6 7 4 connectx.L0.L0 connectx.L1.L1
```

## CXPlayerTester application:
### - Output score only:
```
java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1
```

### - Verbose output
```
java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v
```

### - Verbose output and customized timeout and number of game repetitions
```
java -cp ".." connectx.CXPlayerTester 6 7 4 connectx.L0.L0 connectx.L1.L1 -v -t "timeout_in_secs" -r "rounds_number"
```
