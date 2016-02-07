import java.io.Serializable;

//import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.flink.ml.math.DenseVector;
import smile.stat.distribution.*;

public class Cluster implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double mu[];
	private double cov[][];
	private double pi;
	//private MultivariateNormalDistribution mnd;
	private MultivariateGaussianDistribution mgd;
	private int d;
	private int id;
	private double llh;
	
	
	public Cluster(double[] mu, double[][] cov, int id, double pi) {
		super();
		this.mu = mu;
		this.cov = cov;
		this.id = id;
		this.d = mu.length;
		//this.mnd = new MultivariateNormalDistribution(this.mu, this.cov);
		this.mgd = new MultivariateGaussianDistribution(this.mu, this.cov);
		
		this.pi = pi;
	}
	
	public double[] getMu() {
		return mu;
	}
	public void setMu(double[] mu) {
		this.mu = mu;
	}
	public double[][] getCov() {
		return cov;
	}
	public void setCov(double[][] cov) {
		this.cov = cov;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public double getPi() {
		return pi;
	}

	public void setPi(double pi) {
		this.pi = pi;
	}
	
	public int getDim(){
		return this.d;
	}
	
	public double getPDF(DenseVector vector){
		double pdf = 0.0;
		//pdf=mnd.density(vector.data());
		//System.out.println("PDF: "+pdf);
		return pdf;
	}
	
	public double getLogPDF(DenseVector vector){
		double logpdf = 0.0;
		//logpdf=Math.log(getPDF(vector));
		return logpdf;
	}

	public double densityProbability(DenseVector vector) {
		//double pdf = getPDF(vector);
		//double pdf = mgd.p(vector.data());
		double pdf = mgd.logp(vector.data());
		//pdf = Math.exp(pdf);
		//System.out.println("PI: "+this.pi+" PDF: "+pdf+" pi*PDF: "+(pi*pdf));
		//double newval = this.pi*pdf;
		return this.pi*Math.exp(pdf);
	}
	
	public void setLogLikelihood(double [][]data){
		this.llh = this.mgd.logLikelihood(data);
	}
	
	public void setLogLikelihood(double llh){
		this.llh = llh;
	}
	
	public double getLogLikelihood(){
		return this.llh;
	}
	
	

}
