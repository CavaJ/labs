package com.rb.sparkprll;

import java.util.*;

import org.apache.spark.mllib.regression.LabeledPoint;

//class to shuffle the Dataset
public class DatasetShuffler
{
	private List<LabeledPoint> instances;
	
	//constructor method
	public DatasetShuffler(List<LabeledPoint> instances)
	{
		this.instances = new ArrayList<LabeledPoint>(instances);
	} // DatasetShuffler
	
	//shuffle method to random shuffle the dataset
	public void shuffle()
	{
		Collections.shuffle(instances);
	} // shuffle
	
	//get method to get the shuffled or normal instances back
	public List<LabeledPoint> getInstances()
	{
		return instances;
	} // getInstances
} // class DatasetShuffler