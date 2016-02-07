import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.ExecutionConfig.GlobalJobParameters;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.aggregation.Aggregations;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.ml.math.DenseVector;
import org.apache.flink.util.Collector;
import org.apache.flink.ml.math.BLAS;
import org.apache.flink.ml.math.DenseMatrix;

import java.util.Iterator;


public class GaussianMixtureModel {
	
	private int k;
	private long n;
	private int iterations;
	private double threshold;
	private DataSet<Cluster> clusters;
	//private static String data="";
	private double epsilon;
	private String path;

	
	public GaussianMixtureModel(int k, int i, double t, String p){
		this.k = k;
		this.iterations = i;
		this.threshold = t;
		this.n = 0;
		this.path = p;
		double eps = 1.0;
		while ((1.0 + (eps / 2.0)) != 1.0) {
			      eps /= 2.0;
		}
		this.epsilon=eps;
	}
	
	public DataSet<DenseVector> getDataPoints(ExecutionEnvironment env){
		//String path = "src/main/resources/gmm_data.txt";
		//String path = "src/main/resources/data2D_new.csv";
		//String path = "src/main/resources/data_2Mill2D.csv";
		DataSource<String> data = env.readTextFile(path);
		return data.map(new ReadVectors());
	}
	
	private static class ReadVectors implements MapFunction<String, DenseVector> {
		
		public DenseVector map(String data) throws Exception {
			String[] sarray = data.trim().split(" ");
			double[] values = new double[sarray.length-1]; //for R data to remove first column
			//double[] values = new double[sarray.length]; //for Spark test data
			for (int i = 0; i < values.length; i++){
				values[i] = Double.parseDouble(sarray[i+1]); //for R data
				//values[i] = Double.parseDouble(sarray[i]);
			}
			return new DenseVector(values);
		}
	}
	
	
	public static DataSet<Cluster> initializeClusters(DataSet<DenseVector> dataPoints,int k,int numIterations,ExecutionEnvironment env){
		//DataSet<DenseVector> somePoints = dataPoints.first(50);
		return KMeansNDimension.kmeans(dataPoints, k, env, numIterations);
	}
	
	//private static class TestVectors implements MapFunction<DenseVector, Tuple1<Double>> {
	private static class ExpectationMapper extends RichFlatMapFunction<DenseVector, 
		Tuple4<Integer, Double, DenseVector, Double>> {
		
		private List<Cluster> clusters;
		private double epsilon;
		
		public ExpectationMapper(double e){
			this.epsilon = e;
		}

		/** Reads the clusters values from a broadcast variable into a collection. */
		@Override
		public void open(Configuration parameters) throws Exception {
			this.clusters = getRuntimeContext().getBroadcastVariable("clusters");
			parameters.setInteger(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY,8196);
			
		}

		@Override
		public void flatMap(DenseVector vector, Collector<Tuple4<Integer, Double, DenseVector, Double>> out) throws Exception {
			double densities[] = new double[clusters.size()];
			double sum = 0.0;
			double sumLogSumQms = 0.0;
			//data += "\nData Vector: "+vector.toString()+"\n";
			Iterator<Cluster> it = clusters.iterator();
			for(int i=0; i<clusters.size(); i++){
				Cluster c = clusters.get(i);
				double density = this.epsilon + c.densityProbability(vector);
				densities[i] = density;
				sumLogSumQms += density;
				sum += density;
			}
			
			//double llh = Math.log(sum);
			
			for (int j = 0; j < densities.length; j++) {
				double q = densities[j]/sum;
				out.collect(new Tuple4<Integer, Double, DenseVector, Double>
								(clusters.get(j).getId(), q, vector, sumLogSumQms));
			}
			//System.out.println("\nFINISHED EXPECTATION\n");
		}
		
	}
	
	private static class MaximizationReducer implements GroupReduceFunction<Tuple4<Integer, Double, DenseVector, Double>, Cluster> {
		
		/**
		 * 
		 */
		public void reduce(Iterable<Tuple4<Integer, Double, DenseVector, Double>> qm, Collector<Cluster> out)
				throws Exception {
			int dim = 0;
			int m = 0;
			double sumQms = 0.0;
			DenseVector muVector =null;
			DenseMatrix sigma_sums = null;
			ArrayList<double[]> array= new ArrayList<double[]>();
			int id = 0;
			double sumLLHs = 0.0;
			for(Tuple4<Integer, Double, DenseVector, Double> q : qm){
				id = q.f0;
				double resp = q.f1;
				DenseVector dataPoint = q.f2;
				sumLLHs += q.f3;
				sumQms += resp;
				//BLAS.scal(resp, dataPoint); 
				
				array.add(dataPoint.data().clone()); //add the data point to the array
				
				if(muVector == null){
					muVector = DenseVector.zeros(dataPoint.size());
				}
				if(sigma_sums == null){
					sigma_sums = DenseMatrix.zeros(dataPoint.size(), dataPoint.size());
				}
				
				BLAS.axpy(resp, dataPoint, muVector);
				BLAS.syr(resp, dataPoint, sigma_sums);
				m++;
				
			}
			dim = muVector.size();
			double pi = sumQms/m;
			BLAS.scal(1/sumQms, muVector);
			BLAS.syr(-sumQms, muVector,sigma_sums);
			double sigma_data[] = sigma_sums.data();

			//double[][] matrix = new double[array.size()][];
			double cov[][] = new double[dim][dim];
			for (int i = 0; i < dim; i++) {
			    for(int j=0;j<dim; j++){
			    	cov[i][j] = sigma_data[i*dim + j]/sumQms;
			    }
			}
			/*for (int i = 0; i < array.size(); i++) {
			    matrix[i] = array.get(i);
			}*/

			Cluster c = null;
			try{
				//c = new Cluster(muVector.data(), cv.getCovarianceMatrix().getData(), id, pi);
				c = new Cluster(muVector.data(), cov, id, pi);
				//c.setLogLikelihood(matrix);
				c.setLogLikelihood(sumLLHs);
				out.collect(c);
			}catch(Exception e){
				
				e.printStackTrace();
			}
			//System.out.println("NEW CLUSTER: "+id+" with PI: "+pi+" MU: "+muVector.data().toString());
			//System.out.println("\nFINISHED MAXIMIZATION OF CLUSTER "+id+"\n");
			
		}

	}
	
	
	private static class LogLikelihoodMapper extends RichMapFunction<DenseVector, Tuple1<Double>> {
		
		private List<Cluster> clusters;
		private double epsilon;
		
		public LogLikelihoodMapper(double e){
			this.epsilon = e;
		}

		/** Reads the clusters values from a broadcast variable into a collection. */
		@Override
		public void open(Configuration parameters) throws Exception {
			this.clusters = getRuntimeContext().getBroadcastVariable("clusters");
			parameters.setInteger(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY,8196);
		}

		@Override
		public Tuple1<Double> map(DenseVector vector) throws Exception {
			double densities[] = new double[clusters.size()];
			double sum = 0.0;
			
			Iterator<Cluster> it = clusters.iterator();
			int i=0;
			while(it.hasNext()){
				Cluster c = (Cluster)it.next();
				sum += this.epsilon+c.densityProbability(vector);
			}
			
			//double llhPartial = Math.log(sum);
			double llhPartial = sum;
			//System.out.println("********** PARTIAL LLH: "+llhPartial);
			return new Tuple1<Double>(llhPartial);
		}
		
	}
	
	
	private double getLogLikelihood(DataSet<DenseVector> dataPoints, DataSet<Cluster> newClusters, boolean isFirst)
	{
		double newllh = 0.0;
		if(isFirst){
			List<DenseVector> points = null;
			List<Cluster> clusters = null;
			try{
				points = dataPoints.collect();
				clusters = newClusters.collect();
			}catch(Exception e){
			}
			double [][]dp=null;
			for(int i=0; i<points.size(); i++){
				if(dp==null){
					dp = new double[points.size()][points.get(i).size()];
				}
				dp[i] = points.get(i).data(); 
			}
			for(int i=0; i<clusters.size(); i++){
				clusters.get(i).setLogLikelihood(dp);
				newllh += clusters.get(i).getLogLikelihood();
			}
			
		}else{
			List<Tuple1<Double>> llh = null;
			
			try {
				llh = dataPoints.map(new LogLikelihoodMapper(this.epsilon))
												.withBroadcastSet(newClusters, "clusters")
												.aggregate(Aggregations.SUM, 0)
												.collect();
			} catch (Exception e) {
				e.printStackTrace();
			}
			newllh = llh.get(0).copy().f0;
			
			
		}
		return newllh;
	}
	
	
	//private static class SolutionMapper implements MapFunction<DenseVector, Tuple1<Double>> {
	private static class SolutionMapper extends RichMapFunction<DenseVector, Tuple3<Integer, Double, DenseVector>> {
			
			private List<Cluster> clusters;

			/** Reads the clusters values from a broadcast variable into a collection. */
			@Override
			public void open(Configuration parameters) throws Exception {
				this.clusters = getRuntimeContext().getBroadcastVariable("clusters");
				parameters.setInteger(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 8192);
			}

			@Override
			public Tuple3<Integer, Double, DenseVector> map(DenseVector vector) throws Exception {
			
				double maxDensity = Double.NEGATIVE_INFINITY;
				int id = 0;
				
				Iterator<Cluster> it = clusters.iterator();
				while(it.hasNext()){
					Cluster c = (Cluster)it.next();
					double tempDensity = c.densityProbability(vector);
					if(tempDensity > maxDensity){
						maxDensity = tempDensity;
						id = c.getId();
					}
				}
				System.out.println(id+","+maxDensity+","+vector.toString());
				
				return new Tuple3<Integer, Double, DenseVector>(id, maxDensity, vector);
			}
			
	}
	
	
	
	
	public void runGGM(){
		double llh = 0;
		
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		env.getConfig().disableSysoutLogging();
		Configuration config = GlobalConfiguration.getConfiguration();
		config.setInteger(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 8192);
		
		
		DataSet<DenseVector> dataPoints=this.getDataPoints(env);
		
		//this.clusters = initializeClusters(dataPoints,k,iterations,env);
		
		try {
			this.n = dataPoints.count();
		} catch (Exception e1) { 
		}
		System.out.println("STARTING K-MEANS INITIALIZATION");
		this.clusters = initializeClusters(dataPoints.first((int)n/4),k,3,env);
		String s = "";
		//s = printClusters(this.clusters, -1) + "\n";
		List<String> firstData;
		try {
			firstData = this.clusters.reduceGroup(new PrintReducer()).collect();
			s = firstData.toString();
		} catch (Exception e1) {
		}
		
		System.out.println(s);
		//GMM_Java gmm = new GMM_Java(this.clusters,dataPoints, this.iterations, this.threshold);
		//gmm.runGMM();
		
		
		//llh = getLogLikelihood(dataPoints, this.clusters, true); //initial log likelihood
		llh = Double.MIN_VALUE;
		//System.out.println("*************************** START ITERATIONS ***************************");
		//Start iterations
		//DeltaIteration<Cluster,Cluster> loop = this.clusters.iterateDelta(clusters, this.iterations, 0);
		//IterativeDataSet<Cluster> loop = clusters.iterate(this.iterations);
		
		//DataSet<Cluster> c = loop.getWorkset();
		
		/*File tT = new File("/tmp/temp.txt");
		DataOutputStream wTT=null;
		try {
			wTT = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tT)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			wTT.writeChars("ITERATION: -1"
							+"\nCLUSTERS:\n"+s
							+"\nOLDLLH = "+llh
							+"\nNEWLLH = N/A"
							+"\nDIFF = N/A"
							+"\n\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		int it = 0;
		double diff=this.threshold+1;
		double newLLH = 0.0;
		System.out.println("STARTING ITERATIONS OF GMM");
		while(it<this.iterations && diff>this.threshold){
			DataSet<Cluster> newClusters = dataPoints.flatMap(new ExpectationMapper(this.epsilon))
											.withBroadcastSet(this.clusters, "clusters")
											.groupBy(0)
											.reduceGroup(new MaximizationReducer());
			newLLH = getLogLikelihood(dataPoints, newClusters, false);
			
			this.clusters = newClusters;
			/*try {
				firstData = this.clusters.reduceGroup(new PrintReducer()).collect();
				s = firstData.toString();
			} catch (Exception e1) {
			}
			System.out.println("\n\nITERATION "+it+"\n"+s);*/
			diff = Math.abs(llh-newLLH);
			
			//s = printClusters(this.clusters, it);
			/*try {
				wTT.writeChars("ITERATION: "+it
								+"\nCLUSTERS:\n"+s
								+"\nOLDLLH = "+llh
								+"\nNEWLLH = "+newLLH
								+"\nDIFF = "+diff
								+"\n\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}*/
			//System.out.println(s);
			//System.out.println("LLH - NEWLLH = "+diff);
			llh = newLLH;
			it++;
		}
		
		try {
			firstData = this.clusters.reduceGroup(new PrintReducer()).collect();
			s = firstData.toString();
		} catch (Exception e1) {
		}
		System.out.println("\n\nITERATION FINAL"+"\n"+s);
		
		/*try {
			//wTT.writeChars("ITERATION: "+it+"\nCLUSTERS:\n"+s+"\nLLH - NEWLLH = "+diff+"\n\n");
			wTT.writeChars("ITERATION: "+it
					+"\nCLUSTERS:\n"+s
					+"\nOLDLLH = "+llh
					+"\nNEWLLH = "+newLLH
					+"\nDIFF = "+diff
					+"\n\n");
			wTT.close();
		} catch (IOException e) {
		}*/
		
		//DataSet<Cluster> nextWorkSet=newClusters;
		
		
		/*if(llh-newLLH<=threshold)
		{
			nextWorkSet= nextWorkSet.filter(new FilterFunction(){

				@Override
				public boolean filter(Object arg0) throws Exception {
					return false;
				}
				
			});
		}*/
		
		//clusters=loop.closeWith(newClusters, nextWorkSet);
		System.out.println("\nWRITE FINAL CLUSTER ASSOCIATIONS\n");
		solutionWriter(dataPoints);
		/*dataPoints.map(new SolutionMapper()).
				withBroadcastSet(this.clusters, "clusters")
				//.writeAsCsv("src/main/resources/GMM_results.csv", "\n", ",", FileSystem.WriteMode.OVERWRITE)
				.writeAsCsv("/Users/diana/datafiles/GMM_results.csv", "\n", ",", FileSystem.WriteMode.OVERWRITE)
				.setParallelism(1);*/
		//end iteration
		
		//COMPUTE K-MEANS RESULTS FOR COMPARISSON
		DataSet<Cluster> clustersKMeans = KMeansNDimension.getKmeansResult(dataPoints, k, env,this.iterations);
		try {
			firstData = clustersKMeans.reduceGroup(new PrintReducer()).collect();
			s = firstData.toString();
			System.out.println("KMEANS RESULT:\n\n"+s);
		} catch (Exception e1) {
		}
	}
	
	
	
	public static void main(String args[]){
		
		if(args.length != 4){
			System.out.println("Incorrect number of arguments. Needed values:"
								+"[0]: path to source data"
								+"[1]: number of clusters"
								+"[2]: number of iterations"
								+"[3]: threshold");
			System.exit(1);
		}
		String path = args[0];
		int k = Integer.parseInt(args[1]);
		int it = Integer.parseInt(args[2]);
		double t = Double.parseDouble(args[3]);
		
		GaussianMixtureModel gmm = new GaussianMixtureModel(k,it, t, path);
		gmm.runGGM();
		
	}
	
	/*public String printClusters(DataSet<Cluster> cls, int it){
		String s="";
		try {
			List<Cluster> cs = cls.collect();
			for(Cluster c : cs){
				s += "ITERATION: "+it+" Cluster "+c.getId()+" mean: [";
				double mu[] = c.getMu();
				for(int i=0; i<mu.length;i++){
					s += mu[i]+" ";
				}
				s += "] PI: "+c.getPi()+" COV: [";
				double cov[][] = c.getCov();
				for(int i=0; i<cov.length; i++){
					for(int j=0; j<cov[0].length;j++){
						s += cov[i][j] + " ";
					}
				}
				s += "]\n";
				//System.out.println(s);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}*/
	
	private static class PrintReducer implements GroupReduceFunction<Cluster, String> {

		@Override
		public void reduce(Iterable<Cluster> clusters, Collector<String> out) throws Exception {
			String s = "";
			for(Cluster c : clusters){
				s += "Cluster "+c.getId()+" PI: "+c.getPi()+" mu = [";
				double mu[] = c.getMu();
				for(int i=0; i<mu.length;i++){
					s += mu[i]+" ";
				}
				s += "] sigma = [";
				double cov[][] = c.getCov();
				for(int i=0; i<cov.length; i++){
					for(int j=0; j<cov[0].length;j++){
						s += cov[i][j] + " ";
					}
				}
				s += "]\n";
			}
			out.collect(s);
		}

	}
	
	private void solutionWriter(DataSet<DenseVector> dataPoints){
		File tT = new File("/tmp/GMM_results.csv");
		DataOutputStream wTT=null;
		try {
			wTT = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tT)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			List<DenseVector> points = dataPoints.collect();
			List<Cluster> cs = this.clusters.collect();
			for(DenseVector vector : points){
				double maxDensity = Double.NEGATIVE_INFINITY;
				int id = 0;
				
				Iterator<Cluster> it = cs.iterator();
				while(it.hasNext()){
					Cluster c = (Cluster)it.next();
					double tempDensity = c.densityProbability(vector);
					if(tempDensity > maxDensity){
						maxDensity = tempDensity;
						id = c.getId();
					}
				}
				
				try {
					wTT.writeChars((double)id+","+vector.toString()
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
	

}
