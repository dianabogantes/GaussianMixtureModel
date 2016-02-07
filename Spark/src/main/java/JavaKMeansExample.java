import java.util.regex.Pattern;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.VectorUDT;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.catalyst.expressions.GenericRow;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

/**
 * An example demonstrating a k-means clustering. Run with
 * 
 * <pre>
 * bin/run-example ml.JavaSimpleParamsExample <file> <k>
 * </pre>
 */
public class JavaKMeansExample {

	private static class ParsePoint implements Function<String, Row> {
		private static final Pattern separator = Pattern.compile(" ");

		public Row call(String line) {
			String[] tok = separator.split(line);
			double[] point = new double[tok.length];
			for (int i = 0; i < tok.length; ++i) {
				point[i] = Double.parseDouble(tok[i]);
			}
			Vector[] points = { Vectors.dense(point) };
			return new GenericRow(points);
		}
	}

	public static void main(String[] args) {
		args = new String[2];
		args[0] = "src/main/resources/gmm_data3.txt";
		args[1] = "2";
		if (args.length != 2) {
			System.err.println("Usage: ml.JavaKMeansExample <file> <k>");
			System.exit(1);
		}
		String inputFile = args[0];
		int k = Integer.parseInt(args[1]);

		// Parses the arguments
		SparkConf conf = new SparkConf().setAppName("JavaKMeansExample").setMaster("local[2]")
				.set("spark.executor.memory", "1g");
		JavaSparkContext jsc = new JavaSparkContext(conf);
		SQLContext sqlContext = new SQLContext(jsc);

		// Loads data
		JavaRDD<Row> points = jsc.textFile(inputFile).map(new ParsePoint());
		StructField[] fields = { new StructField("features", new VectorUDT(), false, Metadata.empty()) };
		StructType schema = new StructType(fields);
		DataFrame dataset = sqlContext.createDataFrame(points, schema);

		// Trains a k-means model
		KMeans kmeans = new KMeans().setK(k);
		KMeansModel model = kmeans.fit(dataset);

		// Shows the result
		Vector[] centers = model.clusterCenters();
		System.out.println("Cluster Centers: ");
		for (Vector center : centers) {
			System.out.println(center);
		}

		jsc.stop();
	}
}