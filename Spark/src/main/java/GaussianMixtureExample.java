import org.apache.spark.api.java.*;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.clustering.GaussianMixture;
import org.apache.spark.mllib.clustering.GaussianMixtureModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.rdd.RDD;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.spark.SparkConf;

public class GaussianMixtureExample {
	public static void main(String[] args) {
		SparkConf conf = new SparkConf().setAppName("GaussianMixture Example").setMaster("local[2]")
				.set("spark.executor.memory", "1g");
		;
		JavaSparkContext sc = new JavaSparkContext(conf);
		
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
		

		// Load and parse data
		//String path = "src/main/resources/gmm_data.txt";
		//String path = "src/main/resources/data2D_new.csv";
		//String path = "src/main/resources/data_100000_4.csv";
		//double eps = MLUtils.;
		JavaRDD<String> data = sc.textFile(path);
		JavaRDD<Vector> parsedData = data.map(new Function<String, Vector>() {
			public Vector call(String s) {
				String[] sarray = s.trim().split(" ");
				//double[] values = new double[sarray.length-1];
				double[] values = new double[sarray.length];
				for (int i = 0; i < values.length; i++){
					//values[i] = Double.parseDouble(sarray[i+1]);
					values[i] = Double.parseDouble(sarray[i]);
				}
				return Vectors.dense(values);
			}
		});
		parsedData.cache();

		// Cluster the data into two classes using GaussianMixture
		GaussianMixtureModel gmm = new GaussianMixture()
										.setK(k)
										.setMaxIterations(it)
										.setConvergenceTol(t)
										.run(parsedData.rdd());

		// Save and load GaussianMixtureModel
		gmm.save(sc.sc(), "myGMMModel");
		GaussianMixtureModel sameModel = GaussianMixtureModel.load(sc.sc(), "myGMMModel");
		// Output the parameters of the mixture model
		for (int j = 0; j < gmm.k(); j++) {
			System.out.printf("weight=%f\nmu=%s\nsigma=\n%s\n", gmm.weights()[j], gmm.gaussians()[j].mu(),
					gmm.gaussians()[j].sigma());
		}
		JavaRDD<Integer> predictions = gmm.predict(parsedData);
		List<Integer> p = predictions.collect();
		List<Vector> vectors = parsedData.collect();
		printResults(p,vectors);
		
	}
	
	
	public static void printResults(List<Integer> p, List<Vector> vectors){
		
		File tT = new File("GMM_results_Spark.csv");
		DataOutputStream wTT=null;
		try {
			wTT = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tT)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			
			for(int i=0; i<p.size(); i++){
				try {
					String v = vectors.get(i).toString();
					wTT.writeChars(((double)p.get(i)+1)+","+vectors.get(i).toString()
												.replace("[", "")
												.replace("]", "")
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
		
	}
}