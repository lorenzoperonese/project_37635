CP := src
BUILD_DIR := build
JAVA := java
JAVAC := javac
MAIN_CLASS := connectx.CXGame
TESTER_CLASS := connectx.CXPlayerTester

1 := PLAYER1 MISSING
2 := PLAYER2 MISSING
board :=6 7 4
reps := 1
timeout := 10

build:
	$(JAVAC) -d $(BUILD_DIR) -cp $(CP) $(CP)/*/*.java $(CP)/*/*/*.java

HvsC:
	$(JAVA) -cp $(BUILD_DIR) $(MAIN_CLASS) $(board) connectx.$(1).$(1)

CvsC:
	$(JAVA) -cp $(BUILD_DIR) $(MAIN_CLASS) $(board) connectx.$(1).$(1) connectx.$(2).$(2)

test:
	$(JAVA) -cp $(BUILD_DIR) $(TESTER_CLASS) $(board) connectx.$(1).$(1) connectx.$(2).$(2) -r $(reps) -t $(timeout)

testv:
	$(JAVA) -cp $(BUILD_DIR) $(TESTER_CLASS) $(board) connectx.$(1).$(1) connectx.$(2).$(2) -v -r $(reps) -t $(timeout)

clean:
	rm -rf $(BUILD_DIR)/*

.PHONY: build
