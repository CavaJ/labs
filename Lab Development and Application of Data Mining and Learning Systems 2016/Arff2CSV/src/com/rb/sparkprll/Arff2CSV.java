package com.rb.sparkprll;

import java.io.File;
import java.io.IOException;

import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.CSVSaver;

public class Arff2CSV {

	public static void main(String[] args) throws IOException 
	{
		//load arff
		ArffLoader loader = new ArffLoader();
		loader.setSource(new File("C:\\Users\\Rufat Babayev\\Desktop\\SEA_50\\SEA_50.arff"));
		Instances data = loader.getDataSet();
		
		//save csv
		CSVSaver saver = new CSVSaver();
		saver.setInstances(data);
		saver.setFile(new File("C:\\Users\\Rufat Babayev\\Desktop\\SEA_50\\SEA_50.csv"));
		saver.writeBatch();
	} // main

} // class Arff2CSV
