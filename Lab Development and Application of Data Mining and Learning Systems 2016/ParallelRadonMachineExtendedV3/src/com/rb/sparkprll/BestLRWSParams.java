package com.rb.sparkprll;

//class to hold best training params for 
//LogisticRegressionWithSGD algorithm in spark
public class BestLRWSParams 
{
	//instance variables with default values
	private double bestMiniBatchFraction = 0.1;
	private double bestStepSize = 1.0;
	private int bestNumIter = 1;
	
	//default empty constructor method
	public BestLRWSParams() { } // BestLRWSParams

	//getter method for bestMiniBatchFraction
	public double getBestMiniBatchFraction() 
	{
		return bestMiniBatchFraction;
	} // getBestMiniBatchFraction

	public void setBestMiniBatchFraction(double bestMiniBatchFraction) 
	{
		this.bestMiniBatchFraction = bestMiniBatchFraction;
	} // setBestMiniBatchFraction

	public double getBestStepSize() 
	{
		return bestStepSize;
	} // getBestStepSize

	public void setBestStepSize(double bestStepSize) 
	{
		this.bestStepSize = bestStepSize;
	} // setBestStepSize

	public int getBestNumIter() 
	{
		return bestNumIter;
	} // getBestNumIter

	public void setBestNumIter(int bestNumIter) 
	{
		this.bestNumIter = bestNumIter;
	} // setBestNumIter
	
} // class BestLRWSParams
