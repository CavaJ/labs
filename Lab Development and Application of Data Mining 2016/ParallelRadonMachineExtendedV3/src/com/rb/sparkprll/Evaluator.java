package com.rb.sparkprll;

import org.apache.spark.mllib.linalg.Matrix;

//class to represent evaluation metrics of algorithm on a dataset
public class Evaluator 
{
	private double[] confusionArray;
	private double truePositives;
	private double trueNegatives;
	private double falsePositives;
	private double falseNegatives;
	
	//constructor accepts confusion matrix
	public Evaluator(Matrix confusion)
	{
		confusionArray = confusion.toArray();
		
		// true positives at index 0
		truePositives = confusionArray[0];

		// true negatives at index 3
		trueNegatives = confusionArray[3];

		// false positives at index 2
		falsePositives = confusionArray[2];

		// false negatives at at index 1
		falseNegatives = confusionArray[1];
		
		/*System.out.println();
		System.out.println("true pos: " + truePositives);
		System.out.println("false pos: " + falsePositives);
		System.out.println("false neg: " + falseNegatives);
		System.out.println("true neg: " + trueNegatives);*/
	} // Evaluator
	
	//helper method for precision
	public double precision()
	{
		//precision is tp / (tp + fp) 
		double precision = truePositives / (truePositives + falsePositives);
		
		return precision;
	} // precision
	
	//recall
	public double recall()
	{
		//recall is tp / (tp + fp) 
		double recall = truePositives / (truePositives + falseNegatives);
		
		return recall;
	} // recall
	
	//F1 - score
	public double fMeasure()
	{
		//fmeasure is 2 * precision * recall / (precision + recall)
		double fMeasure = 2 * precision() * recall() / (precision() + recall());
		
		return fMeasure;
	} // fMeasure
	
	//accuracy
	public double accuracy()
	{
		//accuary is (tp + tn) / (tp + tn + fp + fn)
		double accuracy = (truePositives + trueNegatives) 
							/ (truePositives + trueNegatives + falsePositives + falseNegatives);
		
		return accuracy;
	} // accuracy
	
} // class Evaluator
