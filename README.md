# Kebabo AI: Connect X project for the algorithm course of University of Bologna in 2022/2023
## Lorenzo Peronese, Omar Ayache

Run this commands in the root directory.
The default parameters are: board="6 7 4", timeout=10, reps=1

## Build from source
```
make build
```

## CXGame application:
### - Human vs Computer
```
make HvsC 1=Kebabo
```

### - Computer vs Computer
```
make CvsC board="20 20 10" 1=Kebabo 2=L0
```

## CXPlayerTester application:
### - Output score only:
```
make test 1=Kebabo 2=L0 reps=10 timeout=15
```

### - Verbose output
```
make testv board="7 7 5" 2=Kebabo 1=L0 reps=25
```
