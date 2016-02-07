
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.MapPartitionFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.AggregateOperator;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.record.io.FileInputFormat;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.util.Collector;
import org.apache.flink.ml.math.Vector;
import org.apache.flink.ml.math.DenseVector;
import org.apache.flink.ml.math.Matrix;
import org.apache.flink.ml.math.DenseMatrix;
import org.apache.flink.ml.math.BLAS;

public class VectorTest {
	
	
	public static void main(String args[]){
		String path = "src/main/resources/gmm_data2.txt";
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		DataSource<String> data = env.readTextFile(path);
		DataSet<DenseVector> parsedData = data.map(new ReadVectors());
		
		//MultivariateNormalDistribution mnd= new MultivariateNormalDistribution(null, null);
		
		DataSet<Tuple1<Double>> val = parsedData.map(new TestVectors());
		//DataSet<Tuple1<Double>> val2 = parsedData.map(new TestDot());
		try {
			val.print();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//DataSet<DenseMatrix> matrix = data.map(new ReadMatrix());
		
		
	}
	
	private static class ReadVectors implements MapFunction<String, DenseVector> {
		
		public DenseVector map(String data) throws Exception {
			String[] sarray = data.trim().split(" ");
			double[] values = new double[sarray.length];
			for (int i = 0; i < sarray.length; i++)
				values[i] = Double.parseDouble(sarray[i]);
			return new DenseVector(values);
		}
	}
	
	private static class ReadMatrix implements MapFunction<String, DenseMatrix> {
		
		public DenseMatrix map(String data) throws Exception {
			double m[] = {1,2,2,1,1,1};
			
			return new DenseMatrix(3,2,m);
		}
	}
	
	
	private static class TestVectors implements MapFunction<DenseVector, Tuple1<Double>> {
		
		public Tuple1<Double> map(DenseVector v) throws Exception {
			double mu[] = {1,2};
			double cov[][] = {{1.9837212,0.8039848},{0.8039848,0.6288061}};
			
			MultivariateNormalDistribution mnd= new MultivariateNormalDistribution(mu, cov);
			
			
			Double value=0.0;
			value = mnd.density(v.data());
			System.out.println("VAL: "+value);
			return new Tuple1<Double>(value);
		}
	}
	
	
	private static class TestDot implements MapFunction<Vector, Tuple1<Double>> {
		
		public Tuple1<Double> map(Vector v) throws Exception {
			double v2[] = {1.0, 2.0};
			DenseVector vector2 = new DenseVector(v2);
			double dot = v.dot(vector2);
			System.out.println(dot);
			return new Tuple1<Double>(dot);
		}
	}
	
	
	private static class TestMatrix implements MapFunction<DenseMatrix, Tuple1<Double>> {
		
		public Tuple1<Double> map(DenseMatrix m) throws Exception {
			double dot = 0.0;
			System.out.println(dot);
			return new Tuple1<Double>(dot);
		}
	}
	
	
	
	
	

}
