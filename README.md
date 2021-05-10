# Large Scale Data Processing: Final Project
## Authors: Jessica Fong Ng & Qingwei Meng
## Graph matching
The project implements a variation of the Bidding Variant of the Luby algorithm to find a maximal matching. The graphs that we used to generate these result can be found [here](https://drive.google.com/file/d/1khb-PXodUl82htpyWLMGGNrx-IzC55w8/view?usp=sharing). 
### Result

|           File name           |        Number of edges       |       # Matching       | Machine| Run time (s)|
| ------------------------------| ---------------------------- | ---------------------- |--------|-------------|
| com-orkut.ungraph.csv         | 117185083                    |-                       | -       | -          |
| twitter_original_edges.csv    | 63555749                     |-| - | -|
| soc-LiveJournal1.csv          | 42851237                     |-| 3x4 N1 core CPU in GCP | -|
| soc-pokec-relationships.csv   | 22301964                     |598356| 4x4 N1 core CPU in GCP | 2793|
| musae_ENGB_edges.csv          | 35324                        |2310| CPU | 9|
| log_normal_100.csv            | 2671                         | 38| CPU | 5|

All files had been verified through the verifier. They are contained in the `csci3390_final_project_JFQM_result.zip` located at the github repository. We were not able to run com-orkut.ungraph.csv on time, thus the result and corresponding data is not included in the table.

## About the Program
### Input format
Each input file consists of multiple lines, where each line contains 2 numbers that denote an undirected edge. For example, the input below is a graph with 3 edges.  
1,2  
3,2  
3,4  

### Output format
Your output should be a CSV file listing all of the matched edges, 1 on each line. For example, the ouput below is a 2-edge matching of the above input graph. Note that `3,4` and `4,3` are the same since the graph is undirected.  
1,2  
4,3  

### Compiling
First run the sbt to build the .jar file.
```
sbt clean package
```  
The Luby function accetps 2 file path as arguments, the first being the path to the file containing the initial graph and the second being the path to output.  It can be ran locally with the following command (keep in mind that your file paths may be different):
```
//Linux
spark-submit --class final_project.main --master local[*] target/scala-2.12/final_project_2.12-1.0.jar [path_to_input_graph] [path_to_output]

//Unix
spark-submit --class "final_project.main" --master "local[*]" target/scala-2.12/final_project_2.12-1.0.jar [path_to_input_graph] [path_to_output]

```

The verifier accepts 2 file paths as arguments, the first being the path to the file containing the initial graph and the second being the path to the file containing the matching. It can be ran locally with the following command (keep in mind that your file paths may be different):
```
// Linux
spark-submit --master local[*] --class final_project.verifier target/scala-2.12/final_project_2.12-1.0.jar graph.csv log_normal_result/matching.csv/


// Unix
spark-submit --master local[*] --class "final_project.verifier" target/scala-2.12/final_project_2.12-1.0.jar graph.csv log_normal_result/matching.csv/

```

## Report
* The output file (matching) for each test case.
  * For naming conventions, if the input file is `XXX.csv`, please name the output file `XXX_matching.csv`.
  * You'll need to compress the output files into a single ZIP or TAR file before pushing to GitHub. If they're still too large, you can upload the files to Google Drive and include the sharing link in your report.

* A project report that includes the following:
  * A table containing the size of the matching you obtained for each test case. The sizes must correspond to the matchings in your output files.
  * An estimate of the amount of computation used for each test case. For example, "the program runs for 15 minutes on a 2x4 N1 core CPU in GCP." If you happen to be executing mulitple algorithms on a test case, report the total running time.
  * Description(s) of your approach(es) for obtaining the matchings. It is possible to use different approaches for different cases. Please describe each of them as well as your general strategy if you were to receive a new test case.
  * Discussion about the advantages of your algorithm(s). For example, does it guarantee a constraint on the number of shuffling rounds (say `O(log log n)` rounds)? Does it give you an approximation guarantee on the quality of the matching? If your algorithm has such a guarantee, please provide proofs or scholarly references as to why they hold in your report.
### Implementation of Bidding Variant of Luby Algorithm
* We are using a modification of Bidding Variant of Luby Algorithm. The pseudocode is shown below. 
```
R = {}
while (there is active edges) {
 for each edges e {
  generate a random number b_e = [0, 1)
  send b_e to each vertices
  if b_e is equal on both vertices 
    add the edge e to R
  If e is added to R, deactivate neighbor edges
  }
}
```
* There is an implementation problem with assigning random number when applying this alogirhtm on a large data set. If `b_e` is a type float (32 bits), there is 1/(10^8) chances of generating same float, and with double (64 bits), there is 1/(10^16) chances of generating the same double. It can be a problem because if the same float/double is generated to adjacent edges, they both can be activated and violate the maximal match definition. The following table shows the different strageties to generate the random numbers for Luby algorithm with different edge sizes.

|           File name           |        Number of edges       |       Method of Random Number       | # Bits (only consider the type of the number)|
| ------------------------------| ---------------------------- | ---------------------- | ---------------------- |
| log_normal_100.csv| 2671  | Float| 32|
| musae_ENGB_edges.csv | 35324 | Float | 32| 
| soc-pokec-relationships.csv | 22301964 | Double | 64|
| soc-LiveJournal1.csv | 42851237 | (Double, Double)| 128|
| twitter_original_edges.csv | 63555749 | (Double, Double)| 128|
| com-orkut.ungraph.csv | 117185083 | -| -|

* It is worth noticed that the #bits needed increase with the number of edges. If we were to receive a new case, we will use these #edges of these files as reference to choose the type of random variable. The running time of each case is shown in the [Result](#Result). If we were to compile all of the files using `(Doube,Double)`, the running time of each file is shown below.  
 

|           File name           |        Number of edges       |       # Matching       | Machine| Run time (s)|
| ------------------------------| ---------------------------- | ---------------------- |--------|-------------|
| com-orkut.ungraph.csv         | 117185083                    |-                       | -       | -          |
| twitter_original_edges.csv    | 63555749                     |-| - | -|
| soc-LiveJournal1.csv          | 42851237                     |-| - | -|
| soc-pokec-relationships.csv   | 22301964                     |598356| - | 2793|
| musae_ENGB_edges.csv          | 35324                        |2310| CPU | 9|
| log_normal_100.csv            | 2671                         | 38| CPU | 5|
* _Note: We physically change the variable type in the code, thus only the `(Double, Double)` type is shown in the main.scala. We were not able to run com-orkut.ungraph.csv on time, thus the result and corresponding data is not included in the table._
* 
### Idea of Augmenting paths (Planned but Unfinished)
We would use our personalized algorithm to augment along the paths. Supposed we want to delete the augmenting paths with length k
```
repeat k^k iterations{
 Generate a random integer for each vertex from the set {0,1,...,k}
 Find every path p such that its vertices form the pattern (0,1,...,k)
 If p is an augmenting path:
  augment along p
}
```
In this algorithm, we randomly find paths with length k and check if it is an augmenting path. If it is, we augment along it. 
### Advantages and Novelties 
#### Bidding Variant of Luby's Algorithm
1. It terminates in O(log n) rounds with probablity <img src="https://render.githubusercontent.com/render/math?math=1-\frac{1}{n^2}">, which means that this algorithm has a good chance to finish in limited rounds.
2. Our version of Luby's Algorithm does not create a new line graph, which saves half of the spaces.
#### Algorithm for the Augmenting paths
1. This algorithm is relative easy to implement and easy to understand(Compared with Blossom Algorithm).
2. This algorithm can expectedly delete 64% of augmenting paths with length k, for k<14. If the user wants to delete more augmenting paths, he just needs to repeat more iterations.

2 is true because, according to the graphing calculator, 

<img src="https://render.githubusercontent.com/render/math?math=1-(\frac{1}{n^n})^{n^n} \approx 0.64">


