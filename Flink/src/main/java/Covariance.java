/*
 * Licensed to the Apache Software Foundation (ASF) under one or more 
 * contributor license agreements.  See the NOTICE file distributed with 
 * this work for additional information regarding copyright ownership. 
 * The ASF licenses this file to You under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with 
 * the License.  You may obtain a copy of the License at 
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

 
import org.apache.commons.math.MathRuntimeException; 
//import org.apache.commons.math.exception.util.LocalizedFormats; 
import org.apache.commons.math.linear.RealMatrix; 
import org.apache.commons.math.linear.BlockRealMatrix; 
import org.apache.commons.math.stat.descriptive.moment.Mean; 
import org.apache.commons.math.stat.descriptive.moment.Variance; 
 
/**
 * Computes covariances for pairs of arrays or columns of a matrix. 
 * 
 * <p>The constructors that take <code>RealMatrix</code> or 
 * <code>double[][]</code> arguments generate covariance matrices.  The 
 * columns of the input matrices are assumed to represent variable values.</p> 
 * 
 * <p>The constructor argument <code>biasCorrected</code> determines whether or 
 * not computed covariances are bias-corrected.</p> 
 * 
 * <p>Unbiased covariances are given by the formula</p> 
 * <code>cov(X, Y) = Σ[(x<sub>i</sub> - E(X))(y<sub>i</sub> - E(Y))] / (n - 1)</code> 
 * where <code>E(X)</code> is the mean of <code>X</code> and <code>E(Y)</code> 
 * is the mean of the <code>Y</code> values. 
 * 
 * <p>Non-bias-corrected estimates use <code>n</code> in place of <code>n - 1</code> 
 * 
 * @version $Id: Covariance.java 1131229 2011-06-03 20:49:25Z luc $ 
 * @since 2.0 
 */ 
public class Covariance { 
 
    /** covariance matrix */ 
    private final RealMatrix covarianceMatrix; 
    private double mean[];
 
    /**
     * Create an empty covariance matrix. 
     */ 
    /** Number of observations (length of covariate vectors) */ 
    private final int n; 
 
    /**
     * Create a Covariance with no data 
     */ 
    public Covariance() { 
        super(); 
        covarianceMatrix = null; 
        n = 0; 
    } 
 
    /**
     * Create a Covariance matrix from a rectangular array 
     * whose columns represent covariates. 
     * 
     * <p>The <code>biasCorrected</code> parameter determines whether or not 
     * covariance estimates are bias-corrected.</p> 
     * 
     * <p>The input array must be rectangular with at least two columns 
     * and two rows.</p> 
     * 
     * @param data rectangular array with columns representing covariates 
     * @param biasCorrected true means covariances are bias-corrected 
     * @throws IllegalArgumentException if the input data array is not 
     * rectangular with at least two rows and two columns. 
     */ 
    public Covariance(double[][] data, boolean biasCorrected, double mu[]) { 
        this(new BlockRealMatrix(data), biasCorrected, mu); 
    } 
 
    /**
     * Create a Covariance matrix from a rectangular array 
     * whose columns represent covariates. 
     * 
     * <p>The input array must be rectangular with at least two columns 
     * and two rows</p> 
     * 
     * @param data rectangular array with columns representing covariates 
     * @throws IllegalArgumentException if the input data array is not 
     * rectangular with at least two rows and two columns. 
     */ 
    public Covariance(double[][] data, double mu[]) { 
        this(data, false, mu); 
    } 
 
    /**
     * Create a covariance matrix from a matrix whose columns 
     * represent covariates. 
     * 
     * <p>The <code>biasCorrected</code> parameter determines whether or not 
     * covariance estimates are bias-corrected.</p> 
     * 
     * <p>The matrix must have at least two columns and two rows</p> 
     * 
     * @param matrix matrix with columns representing covariates 
     * @param biasCorrected true means covariances are bias-corrected 
     * @throws IllegalArgumentException if the input matrix does not have 
     * at least two rows and two columns 
     */ 
    public Covariance(RealMatrix matrix, boolean biasCorrected, double mu[]) { 
       checkSufficientData(matrix); 
       mean = mu;
       n = matrix.getRowDimension(); 
       covarianceMatrix = computeCovarianceMatrix(matrix, biasCorrected); 
    } 
 
    /**
     * Create a covariance matrix from a matrix whose columns 
     * represent covariates. 
     * 
     * <p>The matrix must have at least two columns and two rows</p> 
     * 
     * @param matrix matrix with columns representing covariates 
     * @throws IllegalArgumentException if the input matrix does not have 
     * at least two rows and two columns 
     */ 
    public Covariance(RealMatrix matrix, double []mu) { 
        this(matrix, true, mu); 
    } 
 
    /**
     * Returns the covariance matrix 
     * 
     * @return covariance matrix 
     */ 
    public RealMatrix getCovarianceMatrix() { 
        return covarianceMatrix; 
    } 
 
    /**
     * Returns the number of observations (length of covariate vectors) 
     * 
     * @return number of observations 
     */ 
 
    public int getN() { 
        return n; 
    } 
    
    
    public void setMean(double mu[]){
    	this.mean = mu;
    }
    
 
    /**
     * Compute a covariance matrix from a matrix whose columns represent 
     * covariates. 
     * @param matrix input matrix (must have at least two columns and two rows) 
     * @param biasCorrected determines whether or not covariance estimates are bias-corrected 
     * @return covariance matrix 
     */ 
    protected RealMatrix computeCovarianceMatrix(RealMatrix matrix, boolean biasCorrected) { 
        int dimension = matrix.getColumnDimension(); 
        Variance variance = new Variance(biasCorrected); 
        RealMatrix outMatrix = new BlockRealMatrix(dimension, dimension); 
        for (int i = 0; i < dimension; i++) { 
            for (int j = 0; j < i; j++) { 
              double cov = covariance(matrix.getColumn(i),i, matrix.getColumn(j),j, biasCorrected); 
              outMatrix.setEntry(i, j, cov); 
              outMatrix.setEntry(j, i, cov); 
            } 
            outMatrix.setEntry(i, i, variance.evaluate(matrix.getColumn(i))); 
        } 
        return outMatrix; 
    } 
 
    /**
     * Create a covariance matrix from a matrix whose columns represent 
     * covariates. Covariances are computed using the bias-corrected formula. 
     * @param matrix input matrix (must have at least two columns and two rows) 
     * @return covariance matrix 
     * @see #Covariance 
     */ 
    protected RealMatrix computeCovarianceMatrix(RealMatrix matrix) { 
        return computeCovarianceMatrix(matrix, true); 
    } 
 
    /**
     * Compute a covariance matrix from a rectangular array whose columns represent 
     * covariates. 
     * @param data input array (must have at least two columns and two rows) 
     * @param biasCorrected determines whether or not covariance estimates are bias-corrected 
     * @return covariance matrix 
     */ 
    protected RealMatrix computeCovarianceMatrix(double[][] data, boolean biasCorrected) { 
        return computeCovarianceMatrix(new BlockRealMatrix(data), biasCorrected); 
    } 
 
    /**
     * Create a covariance matrix from a rectangual array whose columns represent 
     * covariates. Covariances are computed using the bias-corrected formula. 
     * @param data input array (must have at least two columns and two rows) 
     * @return covariance matrix 
     * @see #Covariance 
     */ 
    protected RealMatrix computeCovarianceMatrix(double[][] data) { 
        return computeCovarianceMatrix(data, true); 
    } 
 
    /**
     * Computes the covariance between the two arrays. 
     * 
     * <p>Array lengths must match and the common length must be at least 2.</p> 
     * 
     * @param xArray first data array 
     * @param yArray second data array 
     * @param biasCorrected if true, returned value will be bias-corrected 
     * @return returns the covariance for the two arrays 
     * @throws  IllegalArgumentException if the arrays lengths do not match or 
     * there is insufficient data 
     */ 
    public double covariance(final double[] xArray, int x_index, final double[] yArray, int y_index, boolean biasCorrected) 
        throws IllegalArgumentException { 
        Mean mean = new Mean(); 
        double result = 0d; 
        int length = xArray.length; 
        if (length != yArray.length) { 
            //throw MathRuntimeException.createIllegalArgumentException( 
                  //LocalizedFormats.DIMENSIONS_MISMATCH_SIMPLE, length, yArray.length); 
        } else if (length < 2) { 
            //throw MathRuntimeException.createIllegalArgumentException( 
                  //LocalizedFormats.INSUFFICIENT_DIMENSION, length, 2); 
        } else { 
            //double xMean = mean.evaluate(xArray); 
        	double xMean = this.mean[x_index];
            //double yMean = mean.evaluate(yArray);
        	double yMean = this.mean[y_index];
            for (int i = 0; i < length; i++) { 
                double xDev = xArray[i] - xMean; 
                double yDev = yArray[i] - yMean; 
                result += (xDev * yDev - result) / (i + 1);
                //result += (xDev * yDev);
            } 
        } 
        //return result/(double)(length -1);
        biasCorrected = true;
        return biasCorrected ? result * ((double) length / (double)(length - 1)) : result; 
    } 
 
    /**
     * Computes the covariance between the two arrays, using the bias-corrected 
     * formula. 
     * 
     * <p>Array lengths must match and the common length must be at least 2.</p> 
     * 
     * @param xArray first data array 
     * @param yArray second data array 
     * @return returns the covariance for the two arrays 
     * @throws  IllegalArgumentException if the arrays lengths do not match or 
     * there is insufficient data 
     */ 
    /*public double covariance(final double[] xArray, final double[] yArray) 
        throws IllegalArgumentException { 
        return covariance(xArray, yArray, true); 
    } */
 
    /**
     * Throws IllegalArgumentException of the matrix does not have at least 
     * two columns and two rows 
     * @param matrix matrix to check 
     */ 
    private void checkSufficientData(final RealMatrix matrix) { 
        int nRows = matrix.getRowDimension(); 
        int nCols = matrix.getColumnDimension(); 
        if (nRows < 2 || nCols < 2) { 
            //throw MathRuntimeException.createIllegalArgumentException( 
              //      LocalizedFormats.INSUFFICIENT_ROWS_AND_COLUMNS, 
                //    nRows, nCols); 
        } 
    } 
}