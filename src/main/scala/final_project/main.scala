package final_project

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._
import org.apache.spark.storage.StorageLevel
import org.apache.log4j.{Level, Logger}

object main{
  val rootLogger = Logger.getRootLogger()
  rootLogger.setLevel(Level.ERROR)

  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.spark-project").setLevel(Level.WARN)

  def LubyMIS(g_in: Graph[Double, Int]): Graph[(Double, Double), (Int, Double, Double)] = {
    var active_v = 2L
    var counter = 0
    val r = scala.util.Random
    var g = g_in.mapEdges((i) => (-1, -1D, -1D)).mapVertices((id, i) => (-1D, -1D)) //[(float),(status, float)]
	/*
		active = -1
		deactivate = 0
		selected = 1
	*/
    while (active_v >= 1) { // remaining edges
	  counter += 1
      g = g.mapEdges((i) => (i.attr._1, r.nextDouble, r.nextDouble)) //give active edges random number
      var v_in = g.aggregateMessages[(Double, Double)]( 
        d => { // Map Function
			if (d.attr._1 == 1 || d.attr._1 == 0) { //edge is already deactive
				d.sendToDst(d.attr._1,d.attr._1); //mark deactive
				d.sendToSrc(d.attr._1, d.attr._1);
			} else {
	            d.sendToDst(d.attr._2, d.attr._3);
	            d.sendToSrc(d.attr._2, d.attr._3);
			}
           
          },
          (a,b) => (if (a._1 > b._1) a else b)//take the max (not active = 1)
      )
      var g2 = Graph(v_in, g.edges) 
    
	  
	  //produce new edges
	  var n_edges = g2.triplets.map(
		  t => {if ((t.attr._1) == 1 || (t.attr._1) == 0) {Edge(t.srcId, t.dstId,(t.attr._1, t.attr._2, t.attr._3));} //remain
		  		else if ((t.srcAttr._1 == 1) || ( t.dstAttr._1 == 1)) {Edge(t.srcId, t.dstId,(0, t.attr._2, t.attr._3));} 
				else {// (0,0), (0, -1), (-1,-1)
						if ((t.srcAttr._1 == t.dstAttr._1) && (t.srcAttr._2 == t.dstAttr._2)) (Edge(t.srcId, t.dstId,(1, t.attr._2, t.attr._3))) else (Edge(t.srcId, t.dstId,(t.attr._1, t.attr._2, t.attr._3)))
		  		}
	  	  }
  		)

      g = Graph(g2.vertices, n_edges)

      g.cache()
      active_v = g.edges.filter({case (i) => (i.attr._1 == -1)} ).count()
      println("***********************************************")
      println("Iteration# =" + counter + "remaining vertices = " + active_v)
      println("***********************************************")
    }
    println("***********************************************")
    println("#Iteration = " + counter)
    println("***********************************************")
	
    return g
  }
def augmenting(g_matching:Graph[(Float, Long), (Int, Float)]):Graph[(Float, Long), (Int, Float)]={

  // Change vertices
  val vertices = g_matching.vertices.map(vid=>{
    val r = scala.util.Random
    (vid._1,r.nextInt(4))
  })

  // g is the new graph with changed vertices
  val g = Graph(vertices,g_matching.edges)

  // You may want to delete the original g_matching here

  return g_matching
}
  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("final_project")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.config(conf).getOrCreate()
/* You can either use sc or spark */

    if(args.length == 0) {
      println("Usage: final_project = undirectgraph.csv saveFilePath")
      sys.exit(1)
    }

    val startTimeMillis = System.currentTimeMillis()
    val edges = sc.textFile(args(0)).map(line => {val x = line.split(","); Edge(x(0).toLong, x(1).toLong , 1)} )
    val g = Graph.fromEdges[Double, Int](edges, 0D, edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)
    var g2 = LubyMIS(g)
	val ans = g2.mapEdges((i) => i.attr._1)
	

    val endTimeMillis = System.currentTimeMillis()
    val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
    println("==================================")
    println("Luby's algorithm completed in " + durationSeconds + "s.")
    println("==================================")
	
    var g2df = spark.createDataFrame(ans.edges.filter({case (id) => (id.attr == 1)}))
    g2df = g2df.drop(g2df.columns.last)               
    g2df.coalesce(1).write.format("csv").mode("overwrite").save(args(1))
	 
  }
}
