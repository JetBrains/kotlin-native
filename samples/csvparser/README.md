# CSV parser

 This example shows how one could implement simple comma separated values reader and parser in Kotlin.
A sample data [European Mammals Red List for 2009] (https://data.europa.eu/euodp/en/data/dataset?res_format=CSV)
from EU is being used.

To build use `./build.sh` script without arguments (or specify `TARGET` variable if cross-compiling).

To run use

    ./CsvParser.kexe ./European_Mammals_Red_List_Nov_2009.csv 4 100

It will print number of all unique entries in fifth column
(Family, zero-based index) in first 100 rows of the CSV file.
