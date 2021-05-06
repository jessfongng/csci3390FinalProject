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

/* example undirected graph
 val edges: RDD[Edge[Int]] =
 sc.parallelize(Seq(Edge(1L, 2L), Edge(2L,3L), Edge(5L,3L), Edge(3L,4L),
                    Edge(4L,5L), Edge(5L,1L), Edge(1L,3L), Edge(1L,4L)))
 val g_in = Graph.fromEdges[(Int, Long), Int](edges, (0, 0L))

var k = result
val v_in = result.vertices.filter({case (id, x) => (x._1 == 1)}).collect()
for (x <- v_in){
  k = k.mapEdges(
   //id => if ((id.srcId == 1 && id.dstId == 2) || (id.dstId == 2 && id.srcId == 1)) (1) else (id.attr)
   id => if ((id.srcId == x._2._2 && id.dstId == x._1) || (id.dstId == x._2._2 && id.srcId == x._1)) (1) else (id.attr)
   )
}


k.edges.collect()

*/
  val rootLogger = Logger.getRootLogger()
  rootLogger.setLevel(Level.ERROR)

  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
  Logger.getLogger("org.spark-project").setLevel(Level.WARN)


/*
select a random neighbors
*/
  def select_neighbor(g_in: Graph[Int, Int], vertex: VertexId): VertexId = {
    val r = scala.util.Random
    val neighbors = g.triplets.collect{
      case t if (t.srcId == vertex) => t.dstId
      case l if (l.dstId == vertex) => l.srcId}.collect()
    return(neighbors(r.nextInt(neighbors.size)))
  }

  def Israli(g_in: Graph[(Int, Long), Int]): Graph[(Int, Long), Int] = {
    val r = scala.util.Random
    var active_v = 2L
    var g = g_in.mapVertices((i, from) => (0, -1L)) //everyone is not selected
    g = g.mapEdges((id) => 0) //no selected
    var counter = 0
    while (active_v > 1) {
    counter+=1;
      //decide to propose to who
      var v_propose = g.aggregateMessages[(Int, Long, Float)]( //(status, to, random_value)
          d => { // Map Function
            if (d.dstAttr._1 == 1 || d.srcAttr._1 == 1) { // vertex used
              d.sendToDst((d.dstAttr._1, d.dstAttr._2, -1F));
              d.sendToSrc((d.srcAttr._1, d.srcAttr._2, -1F));
            } else {
              d.sendToDst((d.dstAttr._1, d.srcId, r.nextFloat));
              d.sendToSrc((d.srcAttr._1, d.dstId, r.nextFloat));
            }
          },
          (a,b) => (if (a._3 > b._3) a else (b))
      )
      var g2 = Graph(v_propose, g.edges)

      //propose and accept
      var v_p = g2.aggregateMessages[(Int, Long, Float, Int)]( //(status, from, value, 0or1)
        d => {
          if (d.dstAttr._1 == 1 || d.srcAttr._1 == 1) { // vertex used
            d.sendToDst((d.dstAttr._1, d.dstAttr._2, -1F, -1));
            d.sendToSrc((d.srcAttr._1, d.srcAttr._2, -1F, -1));
          } else {
            d.sendToDst(if (d.srcAttr._2 == d.dstId) (d.dstAttr._1, d.srcId, r.nextFloat, r.nextInt(2)) else (d.dstAttr._1, -1L, 1.1F, r.nextInt(2)))
            d.sendToSrc(if (d.dstAttr._2 == d.srcId) (d.srcAttr._1, d.dstId, r.nextFloat, r.nextInt(2)) else (d.srcAttr._1, -1L, 1.1F, r.nextInt(2)))
            }
        },
        (a,b) => (if (a._3 > -1) {if ((a._3) < (b._3)) (b) else (a) } else b) //select
      )
      var g3= Graph(v_p, g.edges)

      //fitler edges
      var v_deactivate = g3.aggregateMessages[(Int, Long)]( //(status, otherVertex)
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
    */
    val v_in = g.vertices.filter({case (id, x) => (x._1 == 1)}).collect()
    for (x <- v_in){
      g = g.mapEdges(
       id => if ((id.srcId == x._2._2 && id.dstId == x._1) || (id.dstId == x._2._2 && id.srcId == x._1)) (1) else (id.attr)
       )
    }

    return g
  }

//var result = Israli(g_in)


  def main(args: Array[String]) {

    val conf = new SparkConf().setAppName("final_project")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.config(conf).getOrCreate()
/* You can either use sc or spark */

    if(args.length == 0) {
      println("Usage: final_projec = undirectgraph.csv saveFilePath")
      sys.exit(1)
    }

      val startTimeMillis = System.currentTimeMillis()
      val edges = sc.textFile(args(1)).map(line => {val x = line.split(","); Edge(x(0).toLong, x(1).toLong , 1)} )
      val g = Graph.fromEdges[(Int, Long), Int](edges, (0, 0L), edgeStorageLevel = StorageLevel.MEMORY_AND_DISK, vertexStorageLevel = StorageLevel.MEMORY_AND_DISK)

      val g2 = Israli(g)

      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
      println("==================================")
      println("Israli's algorithm completed in " + durationSeconds + "s.")
      println("==================================")
//val g2df = spark.createDataFrame(result.vertices)
      val g2df = spark.createDataFrame(g2.vertices)
      g2df.coalesce(1).write.format("csv").mode("overwrite").save(args(2))

  }
}
