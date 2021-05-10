# Large Scale Data Processing: Final Project
## Authors: Jessica Fong Ng & Qingwei Meng
## Project Summary
This project is to find as many matchings as possible in the graphs. Our plan is to use a modified luby's algorithm to obtain a maximal matchings, and then use our personalized algorithm to augment along paths. However, due to time constraint, we only did the first part. But we will talk about our entire plan in the report. We use the [dataset](https://drive.google.com/file/d/1khb-PXodUl82htpyWLMGGNrx-IzC55w8/view?usp=sharing) obtained from [final project description](https://github.com/CSCI3390/final_project#readme).

To run the code:
1.  ```sbt clean package```
2.  The function accepts 2 file paths as arguments. The first argument is the path to the file containing the initial graph and the second is the path to output. It can be ran locally with the following command (keep in mind that your file paths may be different). The output will be a folder that contains the matching.  
```
//Linux
spark-submit --class final_project.main --master local[*] target/scala-2.12/final_project_2.12-1.0.jar [path_to_input_graph] [path_to_output]

//Unix
spark-submit --class "final_project.main" --master "local[*]" target/scala-2.12/final_project_2.12-1.0.jar [path_to_input_graph] [path_to_output]

```
3. Use the verifier to verify if the output is a matching. The verifier accepts 2 file paths as arguments. The first is the path to the file containing the initial graph and the second is the path to the file containing the matching. It can be ran locally with the following command (keep in mind that your file paths may be different):
```
// Linux
spark-submit --master local[*] --class final_project.verifier target/scala-2.12/final_project_2.12-1.0.jar graph.csv log_normal_result/matching.csv/


// Unix
spark-submit --master local[*] --class "final_project.verifier" target/scala-2.12/final_project_2.12-1.0.jar graph.csv log_normal_result/matching.csv/
```

## Report Findings

|           File name           |        Number of edges       |       # Matching       | Machine| Run time (s)|
| ------------------------------| ---------------------------- | ---------------------- |--------|-------------|
| com-orkut.ungraph.csv         | 117185083                    |-                       | -       | -          |
| twitter_original_edges.csv    | 63555749                     |-| - | -|
| soc-LiveJournal1.csv          | 42851237                     |-| - | -|
| soc-pokec-relationships.csv   | 22301964                     |598356| - | 2793|
| musae_ENGB_edges.csv          | 35324                        |2310| CPU | 9|
| log_normal_100.csv            | 2671                         | 38| CPU | 5|


## Algorithms
### Finding a Maximal Matching
```
 def LubyMIS(g_in: Graph[Float, Int]): Graph[(Float, Long), (Int, Float)] = {
    var active_v = 2L
    var counter = 0
    val r = scala.util.Random
    var g = g_in.mapEdges((i) => (-1, 0F)).mapVertices((id, i) => (-1F, 0L)) //[(float, to),(status, float)]
	/*
		active = -1
		deactivate = 0
		selected = 1
	*/
    while (active_v > 1) { // remaining edges
      g = g.mapEdges((i) => (i.attr._1, r.nextFloat)) //give active edges random number
      var v_in = g.aggregateMessages[(Float, Long)]( 
        d => { // Map Function
			if (d.attr._1 == 1 || d.attr._1 == 0) { //edge is already deactive
				d.sendToDst(d.attr._1, d.dstAttr._2); 
				d.sendToSrc(d.attr._1, d.srcAttr._2);
			} else {
	            d.sendToDst(d.attr._2, 0L);
	            d.sendToSrc(d.attr._2, 0L);
			}
           
          },
          (a,b) => (if (a._1 > b._1) a else b)//take the max (not active = 1)
      )
      var g2 = Graph(v_in, g.edges) 
    
	  
	  //produce new edges
	  var n_edges = g2.triplets.map(
		  t => {if ((t.attr._1) == 1) {Edge(t.srcId, t.dstId,(1, t.attr._2));}
		  		else if ((t.srcAttr._1 == 1) || ( t.dstAttr._1 == 1)) {Edge(t.srcId, t.dstId,(0, t.attr._2));}
				else {
		  			if (t.srcAttr._1 == t.dstAttr._1) (Edge(t.srcId, t.dstId,(1, t.attr._2))) else (Edge(t.srcId, t.dstId,(-1, t.attr._2)))
		  		}
	  	  }
  		)
		
  	  var v_deactivate = g2.aggregateMessages[(Float, Long)]( //return neighbors of selected
          d => {
  			if (d.attr._1 == 1 || d.attr._1 == 0) { //edge is already deactive
  				d.sendToDst(d.attr._1, d.dstAttr._2); 
  				d.sendToSrc(d.attr._1, d.srcAttr._2);
  			} else {
  				if (d.dstAttr._1 == d.srcAttr._1) { //selected 
  					d.sendToDst(1F, d.srcId);
  					d.sendToSrc(1F, d.dstId);
  				} else {
  					d.sendToDst(-1F, 0L);
  					d.sendToSrc(-1F, 0L);
  				}
  			}
			
              },
              (a,b) => (if (a._1 > b._1) a else b) 
        )

      g = Graph(v_deactivate, n_edges)

      g.cache()
      active_v = g.vertices.filter({case (id, i) => (i._1 == -1F)} ).count()
      println("***********************************************")
      println("Iteration# =" + counter + "remaining vertices = " + active_v)
      println("***********************************************")
    }
    println("***********************************************")
    println("#Iteration = " + counter)
    println("***********************************************")
	

    return g
  }
```
We run Luby's Algorithm on line. One novelty about our algorithm is that we did not create a line graph, which saves nearly half of the spaces.  
### Augmenting augmenting apths
We did not finish this part, but below is our pseudo-code.
```
Suppose we want to delete all the augmenting paths with length k in the graph.
For each round:
Generating Random number 1,2,3,...,k+1 for each vertex
Find the path with vertex pattern (1,2,3,...,k+1), if it is an augmenting path, augment it.
Run this algorithm enough rounds to get a good result.


```
## Deliverables
* The output file (matching) for each test case.
  * For naming conventions, if the input file is `XXX.csv`, please name the output file `XXX_matching.csv`.
  * You'll need to compress the output files into a single ZIP or TAR file before pushing to GitHub. If they're still too large, you can upload the files to Google Drive and include the sharing link in your report.
* The code you've applied to produce the matchings.
  * You should add your source code to the same directory as `verifier.scala` and push it to your repository.
* A project report that includes the following:
  * A table containing the size of the matching you obtained for each test case. The sizes must correspond to the matchings in your output files.
  * An estimate of the amount of computation used for each test case. For example, "the program runs for 15 minutes on a 2x4 N1 core CPU in GCP." If you happen to be executing mulitple algorithms on a test case, report the total running time.
  * Description(s) of your approach(es) for obtaining the matchings. It is possible to use different approaches for different cases. Please describe each of them as well as your general strategy if you were to receive a new test case.
  * Discussion about the advantages of your algorithm(s). For example, does it guarantee a constraint on the number of shuffling rounds (say `O(log log n)` rounds)? Does it give you an approximation guarantee on the quality of the matching? If your algorithm has such a guarantee, please provide proofs or scholarly references as to why they hold in your report.
* A live Zoom presentation during class time on 5/4 or 5/6.
  * Note that the presentation date is before the final project submission deadline. This means that you could still be working on the project when you present. You may present the approaches you're currently trying. You can also present a preliminary result, like the matchings you have at the moment. After your presentation, you'll be given feedback to help you complete or improve your work.
  * If any members of your group attend class in a different time zone, you may record and submit your presentation **by midnight on 5/3**.

## Grading policy
* Quality of matchings (40%)
  * For each test case, you'll receive at least 70% of full credit if your matching size is at least half of the best answer in the class.
  * **You will receive a 0 for any case where the verifier does not confirm that your output is a matching.** Please do not upload any output files that do not pass the verifier.
* Project report (35%)
  * Your report grade will be evaluated using the following criteria:
    * Discussion of the merits of your algorithms
    * Depth of technicality
    * Novelty
    * Completeness
    * Readability
* Presentation (15%)
* Formatting (10%)
  * If the format of your submission does not adhere to the instructions (e.g. output file naming conventions), points will be deducted in this category.

## Submission via GitHub
Delete your project's current **README.md** file (the one you're reading right now) and include your report as a new **README.md** file in the project root directory. Have no fear—the README with the project description is always available for reading in the template repository you created your repository from. For more information on READMEs, feel free to visit [this page](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/about-readmes) in the GitHub Docs. You'll be writing in [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown). Be sure that your repository is up to date and you have pushed all of your project's code. When you're ready to submit, simply provide the link to your repository in the Canvas assignment's submission.

## You must do the following to receive full credit:
1. Create your report in the ``README.md`` and push it to your repo.
2. In the report, you must include your (and any partner's) full name in addition to any collaborators.
3. Submit a link to your repo in the Canvas assignment.

## Late submission penalties
Beginning with the minute after the deadline, your submission will be docked a full letter grade (10%) for every 
day that it is late. For example, if the assignment is due at 11:59 PM EST on Friday and you submit at 3:00 AM EST on Sunday,
then you will be docked 20% and the maximum grade you could receive on that assignment is an 80%. 
Late penalties are calculated from the last commit in the Git log.
**If you make a commit more than 48 hours after the deadline, you will receive a 0.**
