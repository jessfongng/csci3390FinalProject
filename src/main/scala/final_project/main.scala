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

  /*
  def Israli(g_in: Graph[(Int, Long), Int]): Graph[(Int, Long), Int] = {
    val r = scala.util.Random
    var active_v = 2L
    var g = g_in.mapVertices((i, from) => (0, -1L)) //everyone is not selected
    g = g.mapEdges((id) => 0) //no selected
    var counter = 0
    while (active_v > 1) {
    counter+=1;
      //decide to propose to who
      var v_propose = g.aggregateMessages[(Int, Long, Float, Int)]( //(status, to, random_value, random int)
          d => { // Map Function
            if (d.dstAttr._1 == 1 || d.srcAttr._1 == 1) { // vertex used
              d.sendToDst((d.dstAttr._1, d.dstAttr._2, -1F, -1));
              d.sendToSrc((d.srcAttr._1, d.srcAttr._2, -1F, -1));
            } else {
              d.sendToDst((d.dstAttr._1, d.srcId, r.nextFloat, -1));
              d.sendToSrc((d.srcAttr._1, d.dstId, r.nextFloat, -1));
            }
          },
          (a,b) => (if (a._1 == 1) a else if (b._1 == 1) b else if (a._3 > b._3) a else b)
      )
      var g2 = Graph(v_propose, g.edges)

      //propose and accept
      v_propose = g2.aggregateMessages[(Int, Long, Float, Int)]( //(status, from, value, 0or1)
        d => {
          if (d.dstAttr._1 == 1 || d.srcAttr._1 == 1) { // vertex used
            d.sendToDst((d.dstAttr._1, d.dstAttr._2, -1F, -1));
            d.sendToSrc((d.srcAttr._1, d.srcAttr._2, -1F, -1));
          } else {
            d.sendToDst(if (d.srcAttr._2 == d.dstId) (d.dstAttr._1, d.srcId, r.nextFloat, r.nextInt(2)) else (d.dstAttr._1, -1L, 1.1F, r.nextInt(2)))
            d.sendToSrc(if (d.dstAttr._2 == d.srcId) (d.srcAttr._1, d.dstId, r.nextFloat, r.nextInt(2)) else (d.srcAttr._1, -1L, 1.1F, r.nextInt(2)))
            }
        },
        (a,b) => (if (a._1 == 1) a else if (b._1 == 1) b else if ((a._3) < (b._3)) (b) else (a) ) //select
      )
      g2 = Graph(v_propose, g.edges)

      //fitler edges
      var v_deactivate = g2.aggregateMessages[(Int, Long)]( //(status, otherVertex)
        d => {
          if (d.dstAttr._1 == 0 && d.srcAttr._1 == 0){ //not selected vertices
              if ((d.srcId == d.dstAttr._2) && (d.dstAttr._4 == 1) && (d.srcAttr._4 == 0)) { //from src to dst
                d.sendToDst(1, d.srcId);
                d.sendToSrc(1, d.dstId);
                } else if ((d.dstId == d.srcAttr._2) && (d.srcAttr._4 == 1) && (d.dstAttr._4 == 0)) { //from dst to src
                d.sendToDst(1, d.srcId);
                d.sendToSrc(1, d.dstId);
                } else {
                d.sendToDst(0, -1L);
                d.sendToSrc(0, -1L);
              }

          }
          else { //selected vertices
            d.sendToDst(d.dstAttr._1, d.dstAttr._2); //keep track of the vertices from
            d.sendToSrc(d.srcAttr._1, d.dstAttr._2);
            }
        },
        (a,b) => (if (a._1 == 0) b else a)
      )
      v_deactivate.collect()
      g = Graph(v_deactivate, g.edges)
      g.cache()
      active_v = g.vertices.filter({case (id, x) => (x._1 == 0)}).count()
    }

    /* relabeled the edge that is selected
    
    val v_in = result.vertices.filter({case (id, x) => (x._1 == 1)}).collect()
    for (x <- v_in){
      result = result.mapEdges(
       id => if ((id.srcId == x._2._2 && id.dstId == x._1) || (id.dstId == x._2._2 && id.srcId == x._1)) (1) else (id.attr)
       )
    }
	*/

    return g
  }
  */
  
  def LubyMIS(g_in: Graph[Float, Int]): Graph[Float, Int] = {
    var active_v = 2L
    var counter = 0
    val r = scala.util.Random
    val i = g_in.mapEdges((i) => (0, 0F)) //(status, float)
	/*
		active = -1
		deactivate = 0
		selected = 1
	*/
	var g = i.mapVertices((id, i) => (-1F, 0L)) //(float, to)
    while (active_v > 1 && counter < 3) { // remaining edges
      counter += 1
      g = g.mapEdges((i) => (i.attr._1, r.nextFloat)) //give active edges random number
      var v_in = g.aggregateMessages[(Float, Long)]( //return 1 for all selected vertices
        d => { // Map Function
			if (d.attr._1 == 1) { //edge is already selected
				d.sendToDst(1F, d.dstAttr._2); 
				d.sendToSrc(1F, d.srcAttr._2);
			} else if (d.srcAttr._1 == 1F || d.dstAttr._1 == 1F) { // one of the vertices is selected
				d.sendToDst(d.dstAttr._1, d.dstAttr._2);
				d.sendToSrc(d.srcAttr._1, d.srcAttr._2);
			} else {
	            d.sendToDst(d.attr._2, 0L);
	            d.sendToSrc(d.attr._2, 0L);
			}
           
          },
          (a,b) => (if (a._1 > b._1) a else b)//take the max (not active = 1)
      )
      var g2 = Graph(v_in, g.edges) //some how the float of the edge keeps changing
      var v_deactivate = g2.aggregateMessages[(Float, Long)]( //return neighbors of selected
        d => {
			if (d.attr._1 == 1) {//already active
				d.sendToDst(1F, d.dstAttr._2); 
				d.sendToSrc(1F, d.srcAttr._2);
			} 
			
			else if (d.srcAttr._1 == 1F || d.dstAttr._1 == 1F) { // one of the vertices is selected
				d.sendToDst(if(d.srcAttr._1 == 1F) (0F, 0L) else (1F, d.dstAttr._2)); 
				d.sendToSrc(if(d.dstAttr._1 == 1F) (0F, 0L) else (1F, d.srcAttr._2));
			} 
			
			else {
			
				if (d.dstAttr._1 == d.srcAttr._1) {
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

      g = Graph(v_deactivate, g2.edges)
	  /*
      var v = g.vertices.filter({case (id, x) => (x._1 == 1F)}).collect()
      for (x <- v){
        g = g.mapEdges(
         id => if ((id.srcId.toLong == x._2._2 && id.dstId.toLong == x._1) || (id.dstId.toLong == x._2._2 && id.srcId.toLong == x._1)) (1, 0F) else (id.attr._1, 0F)
         )
      }
	  */
      g.cache()
      active_v = g.vertices.filter({case (id, i) => (i._1 == -1F)} ).count()
      println("***********************************************")
      println("Iteration# =" + counter + "remaining vertices = " + active_v)
      println("***********************************************")
    }
    println("***********************************************")
    println("#Iteration = " + counter)
    println("***********************************************")
	
    var v = g.vertices.filter({case (id, x) => (x._1 == 1F)}).collect()
    for (x <- v){
      g = g.mapEdges(
       id => if ((id.srcId.toLong == x._2._2 && id.dstId.toLong == x._1) || (id.dstId.toLong == x._2._2 && id.srcId.toLong == x._1)) (1, 0F) else (id.attr._1, 0F)
       )
    }
	
	val ans = g.mapEdges((i) => i.attr._1).mapVertices((id, i) => i._1)
    return ans
  }

  def main(args: Array[String]) {
	  /*
    val conf = new SparkConf().setAppName("final_project")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.config(conf).getOrCreate()

      val startTimeMillis = System.currentTimeMillis()
      val edges = sc.textFile(args(0)).map(line => {val x = line.split(","); Edge(x(0).toLong, x(1).toLong , 1)} )
      val g = Graph.fromEdges[(Int, Long), Int](edges, (0, 0L), edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)

*/



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
    val g = Graph.fromEdges[Float, Int](edges, 0F, edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)
    val g2 = LubyMIS(g)

    val endTimeMillis = System.currentTimeMillis()
    val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
    println("==================================")
    println("Luby's algorithm completed in " + durationSeconds + "s.")
    println("==================================")
    var g2df = spark.createDataFrame(g2.edges.filter({case (id) => (id.attr == 1)}))
    g2df = g2df.drop(g2df.columns.last)               
    g2df.coalesce(1).write.format("csv").mode("overwrite").save(args(1))
	  


  }
}
