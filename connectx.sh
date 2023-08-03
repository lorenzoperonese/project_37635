# Elenco dei parametri da passare al programma Java
parametri=("4 4 4" "5 4 4" "6 4 4" "7 4 4" "4 5 4" "5 5 4" "6 5 4" "7 5 4" "4 6 4")

# File di output dove verranno salvati i risultati
output_file="connectx_output.txt"

# Assicurati che il file di output sia vuoto all'inizio
> "$output_file"

# Loop sui parametri e esecuzione del programma Java per ciascun parametro
for parametro in "${parametri[@]}"; do
    echo "Esecuzione con parametro: $parametro"
    echo "$parametro" >> "$output_file"
    java -cp "/home/lorenzo/Projects/Algoritmi/project_37635" connectx.CXPlayerTester $parametro connectx.L4.L4 connectx.L1.L1 -r 10 >> "$output_file"
    echo " " >> "$output_file"
    java -cp "/home/lorenzo/Projects/Algoritmi/project_37635" connectx.CXPlayerTester $parametro connectx.L1.L1 connectx.L4.L4 -r 10 >> "$output_file"
    echo "--------------------------------------" >> "$output_file"
done

echo "Tutte le esecuzioni sono state completate. I risultati sono stati salvati in $output_file"

# java -cp ".." connectx.CXPlayerTester 5 5 4 connectx.L1.L1 connectx.L4.L4 -v -r 10
