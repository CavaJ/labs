package com.rb.sparkprll;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;

//class to have utitity methods for matrices
public class MatrixUtils implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4499022050829331176L;

	//default empty constructor
	public MatrixUtils() { }
	
	//#rows has to be equal to #cols
	public double[][] toColumnMatrix(double[][] table) 
	{
	    /*for (int i = 0; i < table.length; i ++)
	    {
	    	for (int j = 0; j < table[i].length; j ++)
	    	{
	    		System.out.print(table[i][j] + "  ");
	    	} // for
	    	
	    	System.out.println();
	    } // for
	    */
	    
	    // This code assumes all rows have same number of columns
	    double[][] pivot = new double[table[0].length][];
	    for (int row = 0; row < table[0].length; row++)
	        pivot[row] = new double[table.length];

	    for (int row = 0; row < table.length; row++)
	        for (int col = 0; col < table[row].length; col++)
	            pivot[col][row] = table[row][col];

	    /*for (int row = 0; row < pivot.length; row++)
	        System.out.println(Arrays.toString(pivot[row]));*/
	    
	    return pivot;
	} // toColumnMatrix
	
	//int size - is the size of array
	public double[] getVectorWithOnes(int size)
	{
		double[] array = new double[size];
		
		//all values in b will be 1
		for (int i = 0; i < array.length; i ++)
		{
			array[i] = 1;
		} // for
		
		return array;
	} // getArrayWithOnes
	
	//int size - is the size of array
	public double[] getVectorWithSingleOne(int size)
	{
		double[] array = new double[size];
		
		//initialize first element to 1
		array[0] = 1;
		//array[1] = 1;
		
		//all values in b will be 0 except the element at index 0
		for (int i = 1; i < array.length; i ++)
		{
			array[i] = 0;
		} // for
		
		return array;
	} // getVectorWithSingleOne
	
	// int size - is the size of array
	public double[] getVectorWithSingleOneAtTheEnd(int size) 
	{
		double[] array = new double[size];

		// all values in b will be 0 except the element at index 0
		for (int i = 0; i < array.length - 1; i++) 
		{
			array[i] = 0;
		} // for
		
		//initialize last element to 1
		array[array.length - 1] = 1;

		return array;
	} // getVectorWithSingleOneAtTheEnd
	
	//for printing matrix representation
	public void printMatrixRep(double[][] table)
	{
		String print = "";
		
		print += "\n";
		//System.out.println();
		
		for (int i = 0; i < table.length; i ++)
	    {
	    	for (int j = 0; j < table[i].length; j ++)
	    	{
	    		if (table[i][j] >= 0)
	    		{
	    			print += String.format("%.2f  ", table[i][j]);
	    			//System.out.printf("%.2f  ", table[i][j]);
	    		} // if
	    		else
	    		{
	    			print += String.format("%.2f ", table[i][j]);
	    			//System.out.printf("%.2f ", table[i][j]);
	    		} // else
	    	} // for
	    	
	    	print += "\n";
	    	//System.out.println();
	    } // for
		
		print += "\n";
		//System.out.println();
		
		System.out.print(print);
	} // printMatrixRep
	
	//helper method to Calculate radon point of the given points
	public Vector calculateRadonPoint(double[] X, ArrayList<Vector> vectors)
	{
		double[] sum = new double[vectors.get(0).toArray().length];
		double sumInAlpha = 0;
				
		for (int index = 0; index < X.length; index ++)
		{
			if (X[index] >= 0) // for  < 0, take -i * -alpha_i
			{
				sumInAlpha += X[index];
				
				double[] tempVector = vectors.get(index).toArray();
				for (int j = 0; j < tempVector.length; j ++)
				{
					tempVector[j] = tempVector[j] * X[index];
				} // for
				
				for (int j = 0; j < sum.length; j ++)
				{
					sum[j] += tempVector[j];
				} // for
				
			} // if
			
		} // for
		
		//each component of sum vector has to be divided by sumInAlpha
		for (int component = 0; component < sum.length; component++)
		{
			sum[component] = sum[component] / sumInAlpha;
		} // for
		
		return Vectors.dense(sum);
	} // calculateRadonPoint
	
} // class MatrixUtils
