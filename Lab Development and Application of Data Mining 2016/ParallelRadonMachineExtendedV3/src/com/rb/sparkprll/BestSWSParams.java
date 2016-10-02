package com.rb.sparkprll;

//class to hold best training params for 
//SVMWithSGD algorithm in spark
public class BestSWSParams 
{
	//instance variables with default values
	//we are going to use regParam instead of miniBatchFraction as in LRWS
	private double bestStepSize = 1.0;
	private int bestNumIter = 1;
	private double bestRegParam = 0.1;
	
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
	
	public double getBestRegParam() 
	{
		return bestRegParam;
	} // getBestRegParam
	
	public void setBestRegParam(double bestRegParam) 
	{
		this.bestRegParam = bestRegParam;
	} // setBestRegParam
	
} // class BestSWSParams
