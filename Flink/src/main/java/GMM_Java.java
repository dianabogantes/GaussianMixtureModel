import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
//import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.aggregation.Aggregations;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.ml.math.BLAS;
import org.apache.flink.ml.math.DenseMatrix;
import org.apache.flink.ml.math.DenseVector;
import org.apache.flink.ml.math.Vector;
import breeze.linalg.DenseVector$;
import breeze.linalg.*;
import breeze.numerics.*;
import breeze.linalg.DenseMatrix$;
import breeze.linalg.diag;



public class GMM_Java {
	

	private int k;
	private int iterations;
	private double threshold;
	private List<Cluster> clusters;
	private static String data="";
	private List<DenseVector> dataPoints;
	private double epsilon;
	
	public GMM_Java(DataSet<Cluster> cs, DataSet<DenseVector> dP, int it, double t){
		try {
			clusters = cs.collect();
			dataPoints = dP.collect();
			iterations = it;
			threshold = t;
		} catch (Exception e) {
		}
		double eps = 1.0;
		while ((1.0 + (eps / 2.0)) != 1.0) {
			      eps /= 2.0;
		}
		this.epsilon=eps;
	}
	
	private double getLogLikelihood(boolean isFirst)
	{
		double llh = 0.0;
		//printClusters(newClusters);
		
		
		/*for(DenseVector vector : dataPoints){
			double densities[] = new double[clusters.size()];
			double sum = 0.0;
			
			Iterator<Cluster> it = clusters.iterator();
			int i=0;
			while(it.hasNext()){
				Cluster c = (Cluster)it.next();
				
				densities[i] = c.densityProbability(vector);
				sum += densities[i];
				i++;
			}
			llh += Math.log(sum);
		}*/
		double sumLogQms = 0.0;
		if(isFirst){
			double[][] matrix = new double[dataPoints.size()][];
			
			
			for (int j = 0; j < dataPoints.size(); j++) {
			    matrix[j] = dataPoints.get(j).data();
			}
			Iterator<Cluster> it = clusters.iterator();
			int i=0;
			while(it.hasNext()){
				Cluster c = (Cluster)it.next();
				//c.setLogLikelihood(matrix);
				//llh += c.getLogLikelihood();
				for(int m=0; m<dataPoints.size(); m++){
					sumLogQms += c.densityProbability(dataPoints.get(m));
				}
				llh = sumLogQms;
			}
			//sumLogQms = Math.log(sumLogQms);
		}else{
			Iterator<Cluster> it = clusters.iterator();
			int i=0;
			while(it.hasNext()){
				Cluster c = (Cluster)it.next();
				llh += c.getLogLikelihood();
			}
		}
		
					
		return llh;
	}
	
	private double[][] expectationStep(){
		double results[][] = new double[dataPoints.size()][clusters.size()];
		int dP = 0;
		double llh = 0.0;
		for(DenseVector vector : dataPoints){
			double densities[] = new double[clusters.size()];
			double sum = 0.0;
			data += "\nData Vector: "+vector.toString()+"\n";
			Iterator<Cluster> it = clusters.iterator();
			int i=0;
			while(it.hasNext()){
				Cluster c = (Cluster)it.next();
				double density = this.epsilon + c.densityProbability(vector);
				densities[i] += density;
				llh += density;	
				sum += density;
				i++;
			}
			
			for (int j = 0; j < densities.length; j++) {
				double q = densities[j]/sum;
				results[dP][j] = q;
			}
			dP++;
		}
		System.out.println("LLH: "+llh);
		return results;
	}
	
	
	private double maximizationStep(double res[][]){
		double sumLogSumQms = 0.0;
		Iterator<Cluster> it = clusters.iterator();
		int i=0;
		while(it.hasNext()){
			Cluster c = (Cluster)it.next();
			int m = 0;
			double sumQms = 0.0;
			DenseVector muVector =DenseVector.zeros(this.clusters.size());
			DenseMatrix sigmas_sum = DenseMatrix.zeros(this.clusters.size(), this.clusters.size());
			ArrayList<double[]> array= new ArrayList<double[]>();
			int id = c.getId();
			
			for(int dP=0; dP<res.length; dP++){
				DenseVector dataPoint = this.dataPoints.get(dP);
				sumLogSumQms += this.epsilon+c.densityProbability(dataPoint);
				double resp = res[dP][i];
				sumQms += resp;
				
				array.add(dataPoint.data()); //add data point to the array
				//BLAS.scal(resp, dataPoint); //multiply data point against responsibility
				BLAS.axpy(resp, dataPoint, muVector);
				BLAS.syr(resp, dataPoint, sigmas_sum);
				/*if(muVector == null){
					muVector = dataPoint.copy();
					//BLAS.scal(q.f1, muVector);
				}else{
					BLAS.axpy(1, dataPoint, muVector);
				}*/
				m++;
			}
			
			
			int dim = muVector.size();
			double pi = sumQms/m;
			BLAS.scal(1/sumQms, muVector);
			BLAS.syr(-sumQms, muVector,sigmas_sum);
			double sigma_data[] = sigmas_sum.data();
			
			double[][] matrix = new double[array.size()][];
			double cov[][] = new double[dim][dim];
			for (int p = 0; p < dim; p++) {
			    for(int j=0;j<dim; j++){
			    	cov[p][j] = sigma_data[p*dim + j]/sumQms;
			    }
			}
			for (int t = 0; t < array.size(); t++) {
			    matrix[t] = array.get(t);
			}
			
			Cluster newc = null;
			try{
				//newc = new Cluster(muVector.data(), cv.getCovarianceMatrix().getData(), id, pi);
				//newc = new Cluster(muVector.data(), cov_matrix, id, pi);
				newc = new Cluster(muVector.data(), cov, id, pi);
				//newc.setLogLikelihood(matrix);
				this.clusters.set(i, newc); //replaces cluster data
			}catch(Exception e){
				
				e.printStackTrace();
			}
			
			i++;
		}
		//sumLogSumQms = Math.log(sumLogSumQms);
		System.out.println("SUM LOG QMS: "+sumLogSumQms);
		return sumLogSumQms;
	}
	
	private double[][] initCovariance(double mu[], ArrayList<double[]> array){
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
		
		for (int j = 0; j < mu.length; j++) {
			for (int l = 0; l <= j; l++) {
				cov[j][l] /= (n - 1);
				cov[l][j] = cov[j][l];
			}
		}
		
		
		return cov;
	}
	
	
	public void runGMM(){
		double llh = 0.0;
		
		llh = getLogLikelihood(true); //initial log likelihood
		
		
		//System.out.println("*************************** START ITERATIONS ***************************");
		//Start iterations
		
		File tT = new File("/tmp/tempJava_new.txt");
		DataOutputStream wTT=null;
		try {
			wTT = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tT)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String s = printClusters(this.clusters, -1);
		try {
			wTT.writeChars("ITERATION: -1"
					+"\nCLUSTERS:\n"+s
					+"\nOLDLLH = "+llh
					+"\nNEWLLH = N/A"
					+"\nDIFF = N/A"
					+"\n\n");
		} catch (IOException e1) {
		}
		int it = 0;
		double diff=this.threshold+1;
		llh = Double.MIN_VALUE;
		while(it<this.iterations && diff>this.threshold){
			
			double results[][] = expectationStep();
			double nLLH = maximizationStep(results);
			
			double newLLH = getLogLikelihood(false);
			newLLH = nLLH;
			diff = Math.abs(llh-newLLH);
			//System.out.println("LLH - NEWLLH = "+diff);
			s = printClusters(this.clusters, it);
			try {
				wTT.writeChars("ITERATION: "+it
						+"\nCLUSTERS:\n"+s
						+"\nOLDLLH = "+llh
						+"\nNEWLLH = "+newLLH
						+"\nDIFF = "+diff
						+"\n\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			llh = newLLH;
			it++;
		}
		try {
			wTT.writeChars("\n\n********* ENDED EXECUTION *************\n\n");
			wTT.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(s);
		
	}
	
	public String printClusters(List<Cluster> cls, int it){
		String s="";
		try {
			for(Cluster c : cls){
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
	}
	

}
