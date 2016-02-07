/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

//import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.FunctionAnnotation.ForwardedFields;
import org.apache.flink.api.java.operators.IterativeDataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.ml.math.BLAS;
import org.apache.flink.ml.math.DenseMatrix;
import org.apache.flink.ml.math.DenseVector;
import org.apache.flink.ml.metrics.distances.EuclideanDistanceMetric;
import org.apache.flink.util.Collector;

/**
 * TAKEN FROM FLINK JAVA-EXAMPLES
 * This example implements a basic K-Means clustering algorithm.
 * 
 * <p>
 * K-Means is an iterative clustering algorithm and works as follows:<br>
 * K-Means is given a set of data points to be clustered and an initial set of <i>K</i> cluster centers.
 * In each iteration, the algorithm computes the distance of each data point to each cluster center.
 * Each point is assigned to the cluster center which is closest to it.
 * Subsequently, each cluster center is moved to the center (<i>mean</i>) of all points that have been assigned to it.
 * The moved cluster centers are fed into the next iteration. 
 * The algorithm terminates after a fixed number of iterations (as in this implementation) 
 * or if cluster centers do not (significantly) move in an iteration.<br>
 * This is the Wikipedia entry for the <a href="http://en.wikipedia.org/wiki/K-means_clustering">K-Means Clustering algorithm</a>.
 * 
 * <p>
 * This implementation works on two-dimensional data points. <br>
 * It computes an assignment of data points to cluster centers, i.e., 
 * each data point is annotated with the id of the final cluster (center) it belongs to.
 * 
 * <p>
 * Input files are plain text files and must be formatted as follows:
 * <ul>
 * <li>Data points are represented as two double values separated by a blank character.
 * Data points are separated by newline characters.<br>
 * For example <code>"1.2 2.3\n5.3 7.2\n"</code> gives two data points (x=1.2, y=2.3) and (x=5.3, y=7.2).
 * <li>Cluster centers are represented by an integer id and a point value.<br>
 * For example <code>"1 6.2 3.2\n2 2.9 5.7\n"</code> gives two centers (id=1, x=6.2, y=3.2) and (id=2, x=2.9, y=5.7).
 * </ul>
 * 
 * <p>
 * Usage: <code>KMeans &lt;points path&gt; &lt;centers path&gt; &lt;result path&gt; &lt;num iterations&gt;</code><br>
 * If no parameters are provided, the program is run with default data from {@link KMeansData} and 10 iterations. 
 * 
 * <p>
 * This example shows how to use:
 * <ul>
 * <li>Bulk iterations
 * <li>Broadcast variables in bulk iterations
 * <li>Custom Java objects (PoJos)
 * </ul>
 */
@SuppressWarnings("serial")
public class KMeansNDimension {
	
	public static DataSet<Cluster> kmeans(DataSet<DenseVector> points,int k,ExecutionEnvironment env, int numIterations)
	{
		 DataSet<Cluster> clusters=null;
		/*String s = "";
		File tT = new File("/tmp/kmeans.txt");
		DataOutputStream wTT=null;
		try {
			wTT = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tT)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		DataSet<Centroid> centroids = getCentroidDataSet(points,k,env);
		
		// set number of bulk iterations for KMeans algorithm
				IterativeDataSet<Centroid> loop = centroids.iterate(numIterations);
				
				DataSet<Centroid> newCentroids = points
					// compute closest centroid for each point
					.map(new SelectNearestCenter()).withBroadcastSet(loop, "centroids")
					// count and sum point coordinates for each centroid
					.map(new CountAppender())
					.groupBy(0)
					.reduce(new CentroidAccumulator())
					// compute new centroids from point counts and coordinate sums
					.map(new CentroidAverager());
				
				// feed new centroids back into next iteration
				DataSet<Centroid> finalCentroids = loop.closeWith(newCentroids);
				
				clusters = points
						// assign points to final clusters
						.map(new SelectNearestCenter()).withBroadcastSet(finalCentroids, "centroids")
						.groupBy(0)
						.reduceGroup(new CalculateCluster(k));
				//s+=printClusters(clusters,0) + "\n\n";
				
				
				/*try {
					wTT.writeChars(s+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
		return clusters;
	}
	
	// *************************************************************************
	//     PROGRAM
	// *************************************************************************
	
	/**
	 * Initialize the centroids with the first k points of the datasets
	 * @param points Dataset
	 * @param k number of clusters to generate
	 * @param env ExecutionEnvironment
	 * @return DataSet<Centroid> centroids
	 */
	private static DataSet<Centroid> getCentroidDataSet(
			DataSet<DenseVector> points, int k,ExecutionEnvironment env ) {
		DataSet<Centroid> dtDataSet=null;
		
		List<Centroid> centroids= new ArrayList<Centroid>();
		
		try {
			List<DenseVector> gro=points.first(k).collect();
			Iterator<DenseVector> iter=gro.iterator();
			
			int c=1;
			
			while(iter.hasNext())
			{
				centroids.add(new Centroid(c,iter.next()));	
				c++;
			}
			dtDataSet= env.fromCollection(centroids);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dtDataSet;
	}
	

	
	/**
	 * A simple two-dimensional centroid, basically a point with an ID. 
	 */
	public static class Centroid  {
		
		public int id;
		public DenseVector v;
		
		public Centroid() {}
		
		public Centroid(Integer id, DenseVector v) {
			this.id = id;
			this.v=v;
		}

		@Override
		public String toString() {
			return id + " " + super.toString();
		}

		public DenseVector getMean() {
			// TODO Auto-generated method stub
			return v;
		}
	}
	
	// *************************************************************************
	//     USER FUNCTIONS
	// *************************************************************************
	
	
	
	/** Determines the closest cluster center for a data point. */
	@ForwardedFields("*->1")
	public static final class SelectNearestCenter extends RichMapFunction<DenseVector, Tuple2<Integer, DenseVector>> {
		private Collection<Centroid> centroids;

		/** Reads the centroid values from a broadcast variable into a collection. */
		@Override
		public void open(Configuration parameters) throws Exception {
			this.centroids = getRuntimeContext().getBroadcastVariable("centroids");
			parameters.setInteger(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY,8196);
		}
		
		@Override
		public Tuple2<Integer, DenseVector> map(DenseVector v) throws Exception {
			
			double minDistance = Double.MAX_VALUE;
			int closestCentroidId =  -1;
			EuclideanDistanceMetric ed = new EuclideanDistanceMetric();
			
			// check all cluster centers
			for (Centroid centroid : centroids) {
				// compute distance
				double distance = ed.distance(centroid.getMean(), v);
				
				// update nearest cluster if necessary 
				if (distance < minDistance) {
					minDistance = distance;
					closestCentroidId = centroid.id;
				}
			}

			// emit a new record with the center id and the data point.
			return new Tuple2<Integer, DenseVector>(closestCentroidId, v);
		}
	}
	
	/** Appends a count variable to the tuple. */
	@ForwardedFields("f0;f1")
	public static final class CountAppender implements MapFunction<Tuple2<Integer, DenseVector>, Tuple3<Integer, DenseVector, Long>> {

		@Override
		public Tuple3<Integer, DenseVector, Long> map(Tuple2<Integer, DenseVector> t) {
			return new Tuple3<Integer, DenseVector, Long>(t.f0, t.f1, 1L);
		} 
	}
	
	/** Sums and counts point coordinates. */
	@ForwardedFields("0")
	public static final class CentroidAccumulator implements ReduceFunction<Tuple3<Integer, DenseVector, Long>> {

		@Override
		public Tuple3<Integer, DenseVector, Long> reduce(Tuple3<Integer, DenseVector, Long> val1, Tuple3<Integer, DenseVector, Long> val2) {
			
			DenseVector v=val1.f1;
			BLAS.axpy(1, val2.f1,v);
			
			
			return new Tuple3<Integer, DenseVector, Long>(val1.f0, v, val1.f2 + val2.f2);
		}
	}
	
	/** Computes new centroid from coordinate sum and count of points. */
	@ForwardedFields("0->id")
	public static final class CentroidAverager implements MapFunction<Tuple3<Integer, DenseVector, Long>, Centroid> {

		@Override
		public Centroid map(Tuple3<Integer, DenseVector, Long> value) {
			DenseVector v=value.f1.copy();
			BLAS.scal((double)1/(double)value.f2, v);
			
			return new Centroid(value.f0, v);
		}
	}
	
	public static final class CalculateCluster implements GroupReduceFunction<Tuple2<Integer,DenseVector>,Cluster> {
		private int k;
		public CalculateCluster(int k){
			this.k = k;
		}
		
		@Override
		public void reduce(Iterable<Tuple2<Integer, DenseVector>> values,
				Collector<Cluster> out) throws Exception {
			
			DenseVector v=null;
			ArrayList<double[]> array= new ArrayList<double[]>();
			int id=-1;
			for (Tuple2<Integer, DenseVector> t : values) {
				array.add(t.f1.data());
				
				if(v==null)
					v=new DenseVector(t.f1.data().clone());
				else
					BLAS.axpy(1, t.f1,v);
				id=t.f0;
			}
			
			double[][] matrix = new double[array.size()][];
			for (int i = 0; i < array.size(); i++) {
			    matrix[i] = array.get(i);
			}
			
			//Average
			BLAS.scal((double)1/(double)array.size(), v);
			double cov_matrix[][] = initCovariance(v.data(), array);
			//Covariance cv= new Covariance(cov_matrix, v.data());
			
			//Cluster c = new Cluster(v.data(), cv.getCovarianceMatrix().getData(), id, (double)1/(double)this.k);
			Cluster c = new Cluster(v.data(), cov_matrix, id, (double)1/(double)this.k);
			//Cluster c = new Cluster(muVector.data(), cov, id, (double)1/(double)this.k);
			out.collect(c);
			
		}
		
		
	
	
	}

	private static double[][] initCovariance(double mu[], ArrayList<double[]> array){
		double cov[][] = new double[mu.length][mu.length];
		double []zeros = new double[mu.length*2];
		Arrays.fill(zeros, 0.0);
		DenseMatrix matrix = new DenseMatrix(mu.length, mu.length, zeros);
		int n=array.size();
		double diff[] = new double[mu.length];
		Arrays.fill(diff, 0.0);
		for(double[] data : array){
			for(int i=0; i<data.length; i++){
				diff[i] += Math.pow((data[i]-mu[i]),2);
				for(int j=0; j<mu.length; j++){
					cov[i][j] += ((data[i]-mu[i])*(data[j]-mu[j]));
				}
			}
		}
		/*for(int i=0; i<cov.length; i++){
			cov[i][i] = diff[i];
		}*/
		
		for (int j = 0; j < mu.length; j++) {
			for (int l = 0; l <= j; l++) {
				cov[j][l] /= (n - 1);
				cov[l][j] = cov[j][l];
			}
		}
		
		return cov;
	}
	
	public static DataSet<Cluster> getKmeansResult(DataSet<DenseVector> points, int k, ExecutionEnvironment env, int numIterations){
		
		DataSet<Centroid> centroids = getCentroidDataSet(points,k,env);
		DataSet<Cluster> clusters=null;
		
		// set number of bulk iterations for KMeans algorithm
				IterativeDataSet<Centroid> loop = centroids.iterate(numIterations);
				
				DataSet<Centroid> newCentroids = points
					// compute closest centroid for each point
					.map(new SelectNearestCenter()).withBroadcastSet(loop, "centroids")
					// count and sum point coordinates for each centroid
					.map(new CountAppender())
					.groupBy(0)
					.reduce(new CentroidAccumulator())
					// compute new centroids from point counts and coordinate sums
					.map(new CentroidAverager());
				
				// feed new centroids back into next iteration
				DataSet<Centroid> finalCentroids = loop.closeWith(newCentroids);
		
		
		clusters = points
				// assign points to final clusters
				.map(new SelectNearestCenter()).withBroadcastSet(finalCentroids, "centroids")
				.groupBy(0)
				.reduceGroup(new CalculateCluster(k));
		try {
			System.out.println("TOTAL CLUSTERS: "+clusters.count());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DataSet<Tuple2<Integer,DenseVector>> res = points.map(new SelectNearestCluster())
				.withBroadcastSet(clusters, "centroids");
		System.out.println("FINISHED K-MEANS");
		solutionWriter(res);
		
		
		return clusters;
	}
	
	private static void solutionWriter(DataSet<Tuple2<Integer,DenseVector>> res){
		File tT = new File("/Kmeans_results.csv");
		DataOutputStream wTT=null;
		try {
			wTT = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tT)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			List<Tuple2<Integer,DenseVector>> tuples = res.collect();

			for(Tuple2<Integer,DenseVector> t : tuples){
				try {
					wTT.writeChars((double)t.f0+","+t.f1.toString()
												.replace("DenseVector", "")
												.replace("(", "")
												.replace(")", "")
												.trim()
										+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			
			wTT.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//System.out.println(id+","+maxDensity+","+vector.toString());
	}
	
	
	public static final class SelectNearestCluster extends RichMapFunction<DenseVector, Tuple2<Integer, DenseVector>> {
		private List<Cluster> centroids;

		/** Reads the centroid values from a broadcast variable into a collection. */
		@Override
		public void open(Configuration parameters) throws Exception {
			this.centroids = getRuntimeContext().getBroadcastVariable("centroids");
			parameters.setInteger(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY,8196);
		}
		
		@Override
		public Tuple2<Integer, DenseVector> map(DenseVector v) throws Exception {
			
			double minDistance = Double.MAX_VALUE;
			int closestCentroidId =  -1;
			EuclideanDistanceMetric ed = new EuclideanDistanceMetric();
			
			// check all cluster centers
			for (Cluster centroid : centroids) {
				// compute distance
				double distance = ed.distance(new DenseVector(centroid.getMu()), v);
				
				// update nearest cluster if necessary 
				if (distance < minDistance) {
					minDistance = distance;
					closestCentroidId = centroid.getId();
				}
			}

			// emit a new record with the center id and the data point.
			return new Tuple2<Integer, DenseVector>(closestCentroidId, v);
		}
	}
	
	
		
}
