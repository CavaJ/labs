package com.rb.sparkprll;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithSGD;
import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.classification.SVMWithSGD;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.mllib.linalg.Matrix;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.util.MLUtils;
import org.apache.spark.sql.SQLContext;

import scala.Tuple2;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.CVParameterSelection;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.core.converters.LibSVMLoader;

public class ParallelRadonMachine 
{
	//output files for PRM and Spark
	private static final String PRM_OUTPUT_FILE = "PRM_Models.txt";
	private static final String SPARK_OUTPUT_FILE = "Spark_Models.txt";
	
	public static void run(String inputFilePath, String outputDirectory, 
			int noOfIterations_H, BaseLearner baseLearner, SparkLearner sparkLearner)
	{	
		//Input: Algorithm L, dataset D <= X * Y, Radon number r from N 
		//and number of iterations h from N
		
		//N is definening number of n-folds in cross validation, by default it is 10.
		int noOfFolds = 10;
		
		//get file name here
		File file = new File(inputFilePath);
		String datasetName = file.getName();
		
		//global string to hold the results
	    String globalResultString = "";
		
		//INFO some parts of logging disabled
		Logger.getLogger("org").setLevel(Level.OFF);
		Logger.getLogger("akka").setLevel(Level.OFF);
		
		//initially print algorithm parameters
		System.out.println("============================================================");
		System.out.println("Dataset: " + datasetName);
		
		//do the same for global resultstring
		globalResultString += "============================================================\n";
		globalResultString += "Dataset: " + datasetName + "\n";
				
		//determine number of features of one data instance beforehand
		Integer numFeatures = getNumFeatures(inputFilePath);
		if(numFeatures == null) { System.exit(0); } // if
		
		
		//radon number, if our feature space is 8-dimensional so r = 8 + 2.
		//because of intercept we will add 1 more, so r = 10 + 1 = 11
		//Dimension: d = #features + 1 (including intercept), Radon number: r = d + 2
		int radonNumber_R = numFeatures + 1 + 2;
		System.out.println("Radon number R = " + radonNumber_R);
		System.out.println("Number of iterations H = " + noOfIterations_H);
		
		//write to global result string too
		globalResultString += "Number of features of data instance: " + numFeatures + "\n";
		globalResultString += "Radon number R = " + radonNumber_R + "\n";
		globalResultString += "Number of iterations H = " + noOfIterations_H + "\n";
		
		//requiredPartitionCount is always R ^ H
		int requiredPartitionCount = (int) Math.pow(radonNumber_R, noOfIterations_H);
		
		//number of processors C is always R ^ H
		int noOfProcessors_C = requiredPartitionCount;
		System.out.println("Number of required processors = " + noOfProcessors_C);
		System.out.println("Number of required partitions = " + requiredPartitionCount);
		
		//again for global resultstring
		globalResultString += "Number of required processors = " + noOfProcessors_C + "\n";
		globalResultString += "Number of required partitions = " + requiredPartitionCount + "\n";
		
		System.out.println("Chosen base-learner: " + baseLearner);
		System.out.println("Chosen spark-learner: " + sparkLearner);
		globalResultString += "Chosen base-learner: " + baseLearner + "\n";
		globalResultString += "Chosen spark-learner: " + sparkLearner + "\n";
		
		//use ending line here
		System.out.println("============================================================");
		globalResultString += "============================================================\n";
		
		//set hadoop home directory, in my case it is bin directry in C:\winutils
		System.setProperty("hadoop.home.dir", "C:\\winutils\\");
		
		//TODO change "your_master_ip" to your master ip
		//TODO change "your_local_ip" to your local ip
		//TODO please read some instructions below
		//get spark configuration, it will be different for local mode and cluster mode
		SparkConf configuration = new SparkConf()
										.setAppName("ParallelRadonMachine")
										
										//FOR CLUSTER MODE
										.setMaster("spark://your_master_ip:7077")
										.set("spark.local.ip", "your_local_ip");
										//.set("spark.executor.memory", "1g") //1GB
										
										//TODO We need to have noOfProcessors_C 
										//TODO processors according to paper
										// 4 workers
	      								//.set("spark.executor.instances", "4")
	      								// 5 cores on each workers
	      								//.set("spark.executor.cores", "5");
										
		
		
		
		
										//FOR LOCAL MODE
										//.setMaster("local[" + radonNumber_R + "]");
										//or
										//.setMaster("local[" + noOfProcessors_C + "]");
		JavaSparkContext sc = new JavaSparkContext(configuration);
		
		
		/*
		
		In Spark, the program creating the SparkContext is called 'the driver'. 
		It's sufficient that the jar file with your job is available to the local 
		file system of the driver in order for it to pick it up and ship it 
		to the master/workers.

		In concrete, your config will look like:

		//favor using Spark Conf to configure your Spark Context
		val conf = new SparkConf()
		             .setMaster("spark://mymaster:7077")
		             .setAppName("SimpleApp")
		             .set("spark.local.ip", "172.17.0.1")
		             .setJars(Array("/local/dir/SimpleApp.jar"))

		val sc = new SparkContext(conf)
		
		Under the hood, the driver will start a server where the workers will 
		download the jar file(s) from the driver. It's therefore important 
		(and often an issue) that the workers have network access to the driver.
		This can often be ensured by setting 'spark.local.ip' on the driver 
		in a network that's accessible/routable from the workers.
		
		*/		
		
		//sqlContext can be used to convert rdds to dataframe
		SQLContext sqlContext = new SQLContext(sc);
		
		//-1 is numFeatures. For non-positive value it is determined from input data.
		JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(sc.sc(),
				inputFilePath,
				-1, requiredPartitionCount).toJavaRDD();
		
		//System.out.println("Real # features: " + data.first().features().size());
		
		//count the data beforehand to avoid duplicated counting
		long allDataCount = data.count();
		
		//if the number of instances is less than 1,000,000
		//we will shuffle them randomly to get rid of under-fitting in PRM
		//otherwise everything would be OK
		if(allDataCount < 1000000)
		{
			System.out.println();
			//System.out.println(allDataCount);
			System.out.println("Shuffling Data...");
			DatasetShuffler shuffler = new DatasetShuffler(data.collect());
			shuffler.shuffle();
			data.unpersist(); // remove the previous instances from rdd
			data = sc.parallelize(shuffler.getInstances(), requiredPartitionCount);
			//System.out.println(data.count());
		} // if
		
		//only some % of data would be used for parameter selection
		double[] splitVectorForParamSelection
					= getSplitVectorForCVParameterSelection(numFeatures, allDataCount);
		
		//split the data to parameter optimization data and normal cv data
		JavaRDD<LabeledPoint>[] cvSplits = data.randomSplit(splitVectorForParamSelection);
		
		//guarantee call to make partition count as was before
		cvSplits[1].coalesce(requiredPartitionCount);
		
		
		
		//=================== START OF PARAMETER SELECTION USING CV =========================
		
		// get instances object from some % of data
		Instances paramSelectionInstances 
			= getInstancesFrom(cvSplits[0].collect());
		
		System.out.println("Parameter Selection for PRM started...");
		globalResultString += "Parameter Selection for PRM started...\n";
		
		//this will be used inside map function
		final CVParameterSelection ps = new CVParameterSelection();
		
		//perform parameter selection on chosen base learner
		switch (baseLearner)
		{
		case LOGISTIC: 
			{
				globalResultString 
					+= logisticParamSelection(noOfFolds, paramSelectionInstances, ps);
				break;
			} // case LOGISTIC
		case SMO: 
			{
				globalResultString 
					+= smoParamSelection(noOfFolds, paramSelectionInstances, ps);
				break;
			} // SMO
		default:
			{
				globalResultString 
					+=	logisticParamSelection(noOfFolds, paramSelectionInstances, ps);
			} // default

		} // switch
	    
	    //System.exit(0);
		
	    //=================== END OF PARAMETER SELECTION USING CV =========================
	    
		
		
	    //make note of start point of CV
	    System.out.println("CV for PRM started...");
		globalResultString += "CV for PRM started...\n";
		
	    System.out.println("Data partitions: " + cvSplits[1].partitions().size());
	    
	    //System.exit(0);
	    
		//getSplitVector for 10-fold cross validation
		double[] splitVector = getSplitVector(noOfFolds);
		//System.out.println(Arrays.toString(splitVector));
		

	    // Split initial RDD into N, then join to get [90% training data, 10% testing data].
	    JavaRDD<LabeledPoint>[] splits = cvSplits[1].randomSplit(splitVector);
	    							//= data.randomSplit(splitVector);
	    
	    //we will report average test accuracy
	    double sumOfAccuracies = 0;
	    
	    //represents total runtime of the algorithm
	    double totalRuntime = 0;
	    
	    //perform n-fold cross validation to find average accuracy
	    //and average runtime of the Parallel Radon Machine Algorithm
	  	for (int j = 0; j < noOfFolds; j ++)
	  	{
	  		//cross validation started here, get nanotime
		    long prmFoldStartTime = System.nanoTime();
		    
		    //initialize resultString
			String resultString = "\n****** FOLD #" + (j + 1) + " ******\n";
	  		
	  		JavaRDD<LabeledPoint> emptyRDD = sc.emptyRDD();
	  		List<JavaRDD<LabeledPoint>> rddList = new ArrayList<JavaRDD<LabeledPoint>>();
	  		
	  		for (int index = 0; index < noOfFolds; index ++)
	  		{
	  			//j-th split as a test data, and union all other rdds for training
	  			if (j != index)
	  			{
	  				//aggragate rdds in the list
	  				rddList.add(splits[index]);
	  			} // if
	  		} // for
	  		
	  		//now do single union operation
	  		JavaRDD<LabeledPoint> trainingData = sc.union(emptyRDD, rddList)
	  				.coalesce(requiredPartitionCount);
	  				//.cache();
	  		
	  		//here we will have trainingData ready with 90% of whole dataset
	  		//and testData will be just 10%
	  		JavaRDD<LabeledPoint> testData = splits[j];
	  		
	  		//System.out.println("Training Data paritions: " + trainingData.partitions().size());
		
	  		//mapping each partition to model Vector
	  		JavaRDD<Vector> models = trainingData.mapPartitions(
				new FlatMapFunction<Iterator<LabeledPoint>, Vector>() 
				{
					private static final long serialVersionUID = -5019579095325624912L;

					@Override
					public Iterable<Vector> call(Iterator<LabeledPoint> iterator)
							throws Exception 
					{
						ArrayList<LabeledPoint> partition = new ArrayList<LabeledPoint>();

						//iteratively add labeledPoints to a list
						while (iterator.hasNext()) {
							partition.add(iterator.next());
						} // while

						//call helper method to return a model
						//from serial classification algorithm
						double[] weights = classify(partition);
						
						ArrayList<Vector> oneVector = new ArrayList<Vector>();
						
						if (weights != null)
						{
							//create a dense vector from array
							Vector weightsVector = Vectors.dense(weights);
							//System.out.println("Vector size: " + weightsVector.size());
							oneVector.add(weightsVector);
						} // if
						
						return oneVector;
						
					} // call
					
					//helper method to run the serial algorithm on a parition
					private double[] classify(ArrayList<LabeledPoint> partition)
					{	
						//get Instances
						Instances instances = buildInstances(partition);
						
						//create classifiers on chosen base learner
						switch (baseLearner)
						{
						case LOGISTIC: 
							{
								// create Logistic regression classifier with SGD
								Logistic cModel = new Logistic();
								setBestParamsForLogistic(cModel);

								// now build the classifier from instances
								try 
								{
									//from instances
									cModel.buildClassifier(instances);

									//prints out details of obtained model
									//System.out.println(cModel);

									// get weights from cModel
									double[] weights = transformToWeights(cModel);
									
									//return weights now
									return weights;
								} // try
								catch (Exception e) {
									e.printStackTrace();
								} // catch
								
								break;
							} // case LOGISTIC
						case SMO: 
							{
								//create SMO classifier with support vector
								SMO cModel = new SMO();
								 //disable normalization and standardization
								cModel.setFilterType(new SelectedTag(SMO.FILTER_NONE, 
										SMO.TAGS_FILTER));
								setBestParamsForSMO(cModel);
								
								// now build the classifier from instances
								try 
								{
									//from instances
									cModel.buildClassifier(instances);

									//prints out details of obtained model
									//System.out.println(cModel);

									// get weights from cModel
									double[] weights = transformToWeights(cModel);
									
									//return weights now
									return weights;
								} // try
								catch (Exception e) {
									e.printStackTrace();
								} // catch
								
								break;
							} // SMO
						default:     // FOR DEFAULT USE LOGISTIC
							{
								// create Logistic regression classifier with SGD
								Logistic cModel = new Logistic();
								setBestParamsForLogistic(cModel);

								// now build the classifier from instances
								try 
								{
									//from instances
									cModel.buildClassifier(instances);

									//prints out details of obtained model
									//System.out.println(cModel);

									// get weights from cModel
									double[] weights = transformToWeights(cModel);
									
									//return weights now
									return weights;
								} // try
								catch (Exception e) {
									e.printStackTrace();
								} // catch
							} // default

						} // switch
						 
						 	/*
						  	//FOR LIBSVM - support vector
							String a = cModel.getWeights();
							String[] weightsString = a.split(" ");
							double[] weights = new double[a.length()];
							for(int i =0; i< a.length(); i++)
							    weights[i] = Double.valueOf(weightsString[i]);
							System.out.println("===Weights: " + Arrays.toString(weights));
							System.out.println("===Coef0: " + cModel.weightsTipText());
							Debug.saveToFile(outputDirectory + "svmModel", cModel);
							*/
						
						return null;
					} // classify
					
					
					// helper method for default options in Logistics
					private String[] logisticParamSelectionDefaultOptions() throws Exception 
					{
						// default options
						String defaultOptions = "-R 1.0E-8 -M -1";

						return Utils.splitOptions(defaultOptions);
					} // logisticParamSelectionDefaultOptions
					
					
					//helper method for default options in SMO 
					private String[] smoParamSelectionDefaultOptions() throws Exception
					{	
						// default options
						String defaultOptions = "-C 1 -N 2 -L 1.0e-3 -P 1.0e-12 -V -1 -W 1";
						
						return Utils.splitOptions(defaultOptions);
					} // smoParamSelectionDefaultOptions
					
					
					//helper method for setting param options for Logistic
					private void setBestParamsForLogistic(Logistic cModel)
					{
						//set best parameter selection
						try 
						{
							String[] bestOptions = ps.getBestClassifierOptions();
							if (bestOptions != null)
							{			
								cModel.setOptions(bestOptions);
							} // if
							else
							{
								cModel.setOptions(logisticParamSelectionDefaultOptions());
							} // else
						} // try 
						catch (Exception ex) 
						{
							ex.printStackTrace();
						} // catch
						
					} // setBestParamsForLogistic
					
					
					//helper method for setting param options for SMO
					private void setBestParamsForSMO(SMO cModel)
					{
						//set best parameter selection
						try 
						{
							String[] bestOptions = ps.getBestClassifierOptions();
							if (bestOptions != null)
							{
								//make standardization and normalization disabled
								String tempOps = Utils.joinOptions(bestOptions);
								if(tempOps.contains("-N 0"))
								{
									tempOps = tempOps.replace("-N 0", "-N 2");
									bestOptions = Utils.splitOptions(tempOps);
								} // if
								
								cModel.setOptions(bestOptions);
							} // if
							else
							{
								cModel.setOptions(smoParamSelectionDefaultOptions());
							} // else
						} // try 
						catch (Exception ex) 
						{
							ex.printStackTrace();
						} // catch
						
					} // setBestParamsForSMO
					
					
					//helper method to transform SMO cModel to weights
					private double[] transformToWeights(SMO cModel)
					{
						//FOR SMO - support vector
						double[][][] weights = cModel.sparseWeights();
						double[][] biases = cModel.bias();
						//int[][][] indices = cModel.sparseIndices();
						
						//weights are always in [0][1], + 1 for bias
						double[] weightsToBeReturned = new double[weights[0][1].length + 1];
						
						/*//"[0][0] - 0.0", "[0][1] - bias here",
						//"[1][0] - 0.0", "[1][1] - 0.0"
						for (double[] bias : biases)
						{
							System.out.println("bias: " + Arrays.toString(bias));
						} // for
						
						//"[0][0] - null", "[0][1] - weights here",
						//"[1][0] - null", "[1][1] - null"
						for (int i = 0; i < weights.length; i ++)
						{
							for(int j = 0; j < weights[i].length; j ++)
							{
								System.out.println(Arrays.toString(weights[i][j]));
							} // for
						}
						
						System.out.println("\n");
						for (int i = 0; i < indices.length; i ++)
						{
							for(int j = 0; j < indices[i].length; j ++)
							{
								System.out.println("Indices: " + Arrays.toString(indices[i][j]));
							} // for
						} // for
						*/
						
						
						//"[0][0] - 0.0", "[0][1] - bias here",
						//"[1][0] - 0.0", "[1][1] - 0.0"
						for(int i = 0; i < biases.length - 1; i++)
						{				
							for(int j = 1; j < biases[i].length; j ++)
							{
								//System.out.println(biases[i][j]);
								
								//index 0 will hold bias in weights array
								weightsToBeReturned[0] = biases[i][j];
							} // for
						} // for
						
						//"[0][0] - null", "[0][1] - weights here",
						//"[1][0] - null", "[1][1] - null"
						for (int i = 0; i < weights.length - 1; i ++)
						{
							for(int j = 1; j < weights[i].length; j ++)
							{
								//System.out.println(Arrays.toString(weights[i][j]));
								
								//write the sparse weights to weights array
								for(int k = 0; k < weights[i][j].length; k ++)
								{
									weightsToBeReturned[k + 1] = weights[i][j][k];
								} // for
							} // for
						} // for
						
						//System.out.println(Arrays.toString(weightsToBeReturned));
						
						return weightsToBeReturned;
					} // transformToWeights
					
					//helper method to transform Logistic cModel to weights
					private double[] transformToWeights(Logistic cModel)
					{
						//FOR LOGISTIC
						double[][] coefficients = cModel.coefficients();
						
						//coefficients length is one more along with intercept
						double[] weightsToBeReturned = new double[coefficients.length];							
						//index 0 is intercept
						for (int i = 0; i < coefficients.length; i ++)
						{
							//coefficients[i] length is always one
							for (int j = 0; j < coefficients[i].length; j ++)
							{
								//System.out.println("c[" + i + "] length: " 
										//+ coefficients[i].length);
								//System.out.println("c[" + i + "][" + j + "] = "
										//+ coefficients[i][j]);
								
								//get along with intercept
								weightsToBeReturned[i] = coefficients[i][j];
							} // for
						} // for
						
						return weightsToBeReturned;
					} // transformToWeights
					
					//transform List<LabeledPoint> to Instances
					private Instances buildInstances(ArrayList<LabeledPoint> partition)
					{
						//create an empty arraylist first
						ArrayList<Attribute> attributes = new ArrayList<Attribute>();
						
						// for one labeledPoint in a partition
						// do get feature vector and create attributes from them
						LabeledPoint labeledPoint = partition.get(0);
						double[] features = labeledPoint.features().toArray();

						for (int i = 1; i <= features.length; i++) 
						{
							// attribute name will be x1, x2, x3 etc...
							Attribute attribute = new Attribute("x" + i);
							attributes.add(attribute);
						} // for
						 
						 // Declare the class attribute along with its values
						 FastVector fvClassVal = new FastVector(2);
						 fvClassVal.addElement("1.0");
						 fvClassVal.addElement("0.0");
						 Attribute label = new Attribute("label", fvClassVal);
						 
						 // Declare the feature vector, first add class label attribute
						 FastVector fvWekaAttributes = new FastVector(4);
						 fvWekaAttributes.addElement(label);
						 //then for each attribute in an attributes add them to wekaAttributes
						 for (Attribute attribute : attributes)
						 {
							 fvWekaAttributes.addElement(attribute);
						 } // for
						 
						 // Create an empty training set
						 Instances trainingSet = new Instances("partition", fvWekaAttributes, 10);
						 // Set class index
						 trainingSet.setClassIndex(0);
						 
						 //for each labeledPoint in partition, create an instance
						 //from that labeled point
						 for (LabeledPoint lbldPoint : partition)
						 {
							// Create the instance, number of attrbiutes will be #features + label
							 Instance instance = new Instance(attributes.size() + 1);
							 
							 //class label of labeled point
							 double lbl = lbldPoint.label();
							 
							 //first set class label for the attribute
							 instance.setValue((Attribute)fvWekaAttributes.elementAt(0), "" + lbl);
							 
							 double[] attrs = lbldPoint.features().toArray();

							 for (int index = 0; index < attrs.length; index++) 
							 {
								 instance.setValue((Attribute) fvWekaAttributes
										 .elementAt(index + 1), attrs[index]);
							 } // for
								
							 // add the instance
							 trainingSet.add(instance);
						 } // for
						 
						 /*instance.setValue((Attribute)fvWekaAttributes.elementAt(0), 1.0);
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(1), 0.5);
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(2), "gray");
						 instance.setValue((Attribute)fvWekaAttributes.elementAt(3), "positive");*/
						
						return trainingSet;
					} // buildInstances
					
				}); // mapPartitions
		
	  	
		//HERE basically partition size of models rdd is r^h
		List<Vector> collectedResult = models.collect();
		System.out.println();
		System.out.println("Number of models: " + collectedResult.size());
		
		/*for (Vector vector : collectedResult)
		{
			System.out.println(Arrays.toString(vector.toArray()));
		} // for */
		
		//System.exit(0);
		
		//h has to be at least 2 to make r^2 element to subsets of size r
		//so iteration has to start from 1.
		//we will also find radon point of last radonNumber_R models too,
		//that's why ===== iteration <= noOfIterations_H =======
		for (int iteration = 1; iteration <= noOfIterations_H; iteration ++)
		{
			//partition S into subset s1,...,s_|s|/r of size r
			JavaRDD<Vector> tempModels = sc.parallelize(collectedResult, 
					(int) Math.pow(radonNumber_R, noOfIterations_H - iteration));
			
			System.out.println("Model RDD parition count: " 
					+ tempModels.partitions().size()
					+ " each having " + radonNumber_R + " models");
			
			JavaRDD<Vector> finalModels = tempModels.mapPartitionsWithIndex(
					new Function2<Integer, Iterator<Vector>, Iterator<Vector>>() 
					{

						/**
						 * 
						 */
						private static final long serialVersionUID = 2415057376495159056L;

						@Override
						public Iterator<Vector> call(Integer index,
								Iterator<Vector> iterator) throws Exception 
						{
							ArrayList<Vector> vectorsInThisPartition = new ArrayList<Vector>();
							
							//iteratively add vectors to the list
							while(iterator.hasNext())
							{
								vectorsInThisPartition.add(iterator.next());
							} // while
							
							//System.out.println("Vectors count in partition => " + index + ": " 
									//+ vectorsInThisPartition.size());
							
							//matrix utils for matrix tasks
							MatrixUtils matrixUtils = new MatrixUtils();
							
							//Maybe radonNumberR will always be vector.toArray().length + 2
							//Basically if weight vector dimension increases by 1,
							//radon number will always increase by 1.
							double[][] A = new double[radonNumber_R][radonNumber_R];
							
							//new radonNumber_R is 10 + 1 = 11 (because of intercept)
							double[] b = matrixUtils.getVectorWithSingleOneAtTheEnd(radonNumber_R);
							
							//vector has a dimension of 8 + 1 = 9 (because of intercept)
							//#vectors per partition is 10 + 1 = 11 (because of intercept)						
							for (int row = 0; row < radonNumber_R; row ++)
							{
									A[row] = vectorsInThisPartition.get(row).toArray();
							} // for
							
							//matrixUtils.printMatrixRep(A);
							
							//convert our row matrix to column matrix and add two more rows
							//first would be all ones
							//second would be single one and all zeros
							double[][] tempA = matrixUtils.toColumnMatrix(A);
							
							//tempA has radonNumber_R - 2 rows
							for (int row = 0; row < radonNumber_R - 2; row ++)
							{
									A[row] = tempA[row];
							} // for
							
							A[radonNumber_R - 2] = matrixUtils.getVectorWithOnes(radonNumber_R);
							A[radonNumber_R - 1] = matrixUtils
									.getVectorWithSingleOne(radonNumber_R);
							
							//matrixUtils.printMatrixRep(A);
							
							//get set of multipliers X
							LinearEquationSolver solver = new LinearEquationSolver();
							double[] X = solver.solve(radonNumber_R, A, b);
							
							//create a vector from it and return that vector.
							//System.out.println(Arrays.toString(X));
							
							/*String toPrint = "";
							for (int i = 0; i < X.length; i ++)
							{
								toPrint += String.format("%.2f ", X[i]);
							} // for
							System.out.println(toPrint);*/
							
							Vector radonPoint = matrixUtils.calculateRadonPoint(X, 
									vectorsInThisPartition);
							
							//System.out.println("Radon point:"
									//+ Arrays.toString(radonPoint.toArray()));
							
							//System.exit(0);
							
							//by default
							//Vector radonPoint = Vectors.dense(1, 2, 3, 4, 5 , 6, 7, 8);
							ArrayList<Vector> oneVect = new ArrayList<Vector>();
							oneVect.add(radonPoint);
							
							return oneVect.iterator();
						} // call
					}, false);
			
			collectedResult = finalModels.collect();
			System.out.println("Number of models: " + collectedResult.size());
		} // for		
		
		//transform rdd to Instances to be processed by WEKA
  		Instances testSet = getInstancesFrom(testData.collect());
  		
  		//now test the model obtained from Parallel Radon Machine in Weka
  		//for stringent setup.
  		//HERE collectedResult contains just one model
  		//but for the sake of generality we get this model randomly.
  		Vector chosenModel = getRandomModel(collectedResult);
		double[] randomModelWeights = chosenModel.toArray();
		
		// also input models generated by parallel radon machine
		resultString += "\nModel by Parallel Radon Machine:\n\n";
						
		for (Vector vector : collectedResult) 
		{
			resultString += vector + "\n";
		} // for
		
		//show chosen model, NOT NEEDED there is ONLY ONE model
		//resultString += "\n====== CHOSEN MODEL =======\n" + chosenModel + "\n";
		
		
		
		// ========= EVALUATION START ==========
		
		//perform evaluation using test set on chosen base learner
		switch (baseLearner)
		{
		case LOGISTIC: 
			{
				//transform the array to Weka Logistic acceptable format
				//second parameter will always one, it is basically numClasses - 1
				double[][] wekaCoefficients = new double[randomModelWeights.length][1];							
				
				//index 0 is intercept
				for (int i = 0; i < wekaCoefficients.length; i ++)
				{
					//coefficients[i] length is always one
					for (int k = 0; k < wekaCoefficients[i].length; k ++)
					{
						wekaCoefficients[i][k] = randomModelWeights[i];
					} // for
				} // for
				
				//create template classifier
				Logistic lrClassifier = new Logistic();
				
				try 
				{
					//build it with predefined coefficients
					lrClassifier.buildClassifier(wekaCoefficients, testSet); // API modified
					// evaluate classifier and print some statistics
					Evaluation eval = new Evaluation(testSet);
					eval.evaluateModel(lrClassifier, testSet);
					//System.out.println(eval.toSummaryString("\nResults\n======\n", false));
					//System.out.println(eval.toMatrixString("Confusion Matrix"));
					
					//also append to resultString
					resultString += eval.toSummaryString("\nResults\n======\n", false) + "\n";
					resultString += eval.toMatrixString("Confusion Matrix");
					
					double accuracy = eval.pctCorrect();
					resultString += "\n\n====== Accuracy on test set: " + accuracy + " =======\n";
					sumOfAccuracies += accuracy;

				} catch (Exception e) 
				{
					e.printStackTrace();
				} // catch
				
				break;
			} // case LOGISTIC
		case SMO: 
			{
				//create template classifier
				SMO smoClassifier = new SMO();
				//disable attribute normalization and standardization
				smoClassifier.setFilterType(new SelectedTag(SMO.FILTER_NONE, 
						SMO.TAGS_FILTER));
				
				//bias is at index 0
				double bias = randomModelWeights[0];
				
				//generate weight vector for SMO
				double[] smoSparseWeights = new double[randomModelWeights.length - 1];
				for(int index = 1; index < randomModelWeights.length; index ++)
				{
					smoSparseWeights[index - 1] = randomModelWeights[index];
				} // for
				
				try 
				{
					//build it with predefined coefficients
					smoClassifier.buildClassifier(bias, smoSparseWeights, testSet); // API modified
					// evaluate classifier and print some statistics
					Evaluation eval = new Evaluation(testSet);
					eval.evaluateModel(smoClassifier, testSet);
					//System.out.println(eval.toSummaryString("\nResults\n======\n", false));
					//System.out.println(eval.toMatrixString("Confusion Matrix"));
					
					//also append to resultString
					resultString += eval.toSummaryString("\nResults\n======\n", false) + "\n";
					resultString += eval.toMatrixString("Confusion Matrix");
					
					double accuracy = eval.pctCorrect();
					resultString += "\n\n====== Accuracy on test set: " + accuracy + " =======\n";
					sumOfAccuracies += accuracy;

				} catch (Exception e) 
				{
					e.printStackTrace();
				} // catch
				
				break;
			} // SMO
		default:        // DEFAULT USE LOGISTIC
			{
				//transform the array to Weka Logistic acceptable format
				//second parameter will always one, it is basically numClasses - 1
				double[][] wekaCoefficients = new double[randomModelWeights.length][1];							
				
				//index 0 is intercept
				for (int i = 0; i < wekaCoefficients.length; i ++)
				{
					//coefficients[i] length is always one
					for (int k = 0; k < wekaCoefficients[i].length; k ++)
					{
						wekaCoefficients[i][k] = randomModelWeights[i];
					} // for
				} // for
				
				//create template classifier
				Logistic lrClassifier = new Logistic();
				
				try 
				{
					//build it with predefined coefficients
					lrClassifier.buildClassifier(wekaCoefficients, testSet); // API modified
					// evaluate classifier and print some statistics
					Evaluation eval = new Evaluation(testSet);
					eval.evaluateModel(lrClassifier, testSet);
					//System.out.println(eval.toSummaryString("\nResults\n======\n", false));
					//System.out.println(eval.toMatrixString("Confusion Matrix"));
					
					//also append to resultString
					resultString += eval.toSummaryString("\nResults\n======\n", false) + "\n";
					resultString += eval.toMatrixString("Confusion Matrix");
					
					double accuracy = eval.pctCorrect();
					resultString += "\n\n====== Accuracy on test set: " + accuracy + " =======\n";
					sumOfAccuracies += accuracy;

				} catch (Exception e) 
				{
					e.printStackTrace();
				} // catch
				
			} // default

		} // switch
		
		// ========= EVALUATION END ==========
		
		
		
		
		//get the end time of the fold
	  	long prmFoldEndTime = System.nanoTime();
	  	
	  	//show time in millisseconds scale
	  	double runTimeInMillis = (prmFoldEndTime - prmFoldStartTime) / (double) 1000000;
	  	resultString += "====== PRM Fold Run Time: " + runTimeInMillis + " =======\n\n";
	  	
	  	//print and also write to a file
	  	System.out.println(resultString);
	  	globalResultString += resultString;
	  	
	  	totalRuntime += runTimeInMillis;
		
	  	} // for (N-FOLD CROSS VALIDATION)
	  	
	  	
	  	//also append average accuracy to global result string
	  	double averageAccuracy = sumOfAccuracies / noOfFolds;
	  	System.out.println("------------------------------------------------------------------");
	  	System.out.println("PRM Average Cross-Validation Test Accuracy: " + averageAccuracy);
	  	
	  	globalResultString += "\n------------------------------------------------------------------\n";
	  	globalResultString += "PRM Average Cross-Validation Test Accuracy: " + averageAccuracy + "\n";
	  	
	  	//get the average runtime
	  	double averageRunTime = totalRuntime / noOfFolds;
	  	System.out.println("PRM Average Cross-Validation Run Time: " + averageRunTime 
	  			+ " milliseconds");
	  	System.out.println("------------------------------------------------------------------");
	  	
	  	globalResultString += "PRM Average Cross-Validation Run Time: " + averageRunTime 
	  			+ " milliseconds\n";
	  	globalResultString += "------------------------------------------------------------------\n";
	  	
	  	//write models to a file for each fold of cross validation
	  	writeModelsToFile(outputDirectory + PRM_OUTPUT_FILE,
	  			//("C:\\Users\\Rufat Babayev\\Desktop\\codrna\\PRM_Models.txt",
	  			globalResultString);
	  	
	  	//System.out.println("Data partitions: " + data.partitions().size());
	  	//System.out.println("Data count: " + data.count());
	  	
	  	//unpersist previous rdd, and make it ready for GC
	  	data.unpersist();
	  	data = null;
	  	
	  	//System.exit(0);
	  	
	  	//now do the same thing for spark
	  	crossValidateForSparkLearner(inputFilePath, outputDirectory, 
	  								noOfFolds, sc, sqlContext,
	  								radonNumber_R, sparkLearner);
		
	  	//stop and close sparkContext
		sc.stop();
		sc.close();
	} // main
	
	/*
	 * @param radonNumber_R Radon number to get #features
	 * @param numFolds number of folds in Cross Validation
	 * @param sc JavaSparkContext object reference
	 * @param sqlContext SQLContext
	 * @param sparkLearner chosen sparkLearner
	 * */
	private static void crossValidateForSparkLearner(String inputFilePath, 
			String outputDirectory, int numFolds, 
			JavaSparkContext sc, SQLContext sqlContext, 
			int radonNumber_R, SparkLearner sparkLearner)
	{	
		//get file name here
		File file = new File(inputFilePath);
		String datasetName = file.getName();
		
		System.out.println("\n============================================================");
		System.out.println("Dataset: " + datasetName);
		
		 //global string to hold results for spark learners
	    String globalResultString = "";
		
		//do the same for global resultstring
		globalResultString += "\n============================================================\n";
		globalResultString += "Dataset: " + datasetName + "\n";
		
		System.out.println("Chosen spark-learner: " + sparkLearner);
		System.out.println("============================================================");
		
		globalResultString += "Chosen spark-learner: " + sparkLearner + "\n";
		globalResultString += "============================================================\n";
		
		JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(sc.sc(),
				inputFilePath).toJavaRDD();
				//inputFilePath,
				//-1, (int) Math.pow(radonNumber_R, noOfIterations_H)).toJavaRDD();
		
		//we will coalesce unified rdd to this
		int initialPartitionCount = data.partitions().size();
		
		//number of features of one data instance
		//d = #features + 1 (including intercept), r = d + 2
		int numFeatures = radonNumber_R - 2 - 1;
		
		System.out.println();
		//System.out.println("CV Spark initial data partitions: " + initialPartitionCount);
		
		//only some % of data would be used for parameter selection
		double[] splitVectorForParamSelection
							= getSplitVectorForCVParameterSelection
								(numFeatures, data.count());
				
		JavaRDD<LabeledPoint>[] cvSplits = data.randomSplit(splitVectorForParamSelection);
				
		//guarantee call to make partition count as was before
		cvSplits[1].coalesce(initialPartitionCount);
				
		//get instances object from some % of data
		//Instances paramSelectionInstances = getInstancesFrom(cvSplits[0].collect());
				
		System.out.println("Parameter Selection for Spark Learner started...");
		globalResultString += "Parameter Selection for Spark Learner started...\n";
		
		
		
		//=================== START OF PARAMETER SELECTION USING CV =========================
		
		//getSplitVector for 10-fold cross validation
		double[] cvModelSelectionSplitVector = getSplitVector(numFolds);		

		// Split initial RDD into N, then join to get [90% training data, 10% testing data].
		JavaRDD<LabeledPoint>[] modelSelectionSplits 
			= cvSplits[0].randomSplit(cvModelSelectionSplitVector);
		
		//default value for best validation accuracy
		//will be used for both LRWS and SWS
		double bestValidationAccuracy = Double.MIN_VALUE;
		
		//denotes the number of processed param group, starts from 1
		int paramGroup = 1;
		
		//initialize BestParams for LRWS and SWS
		BestLRWSParams bestLRWSParams = new BestLRWSParams();
		BestSWSParams bestSWSParams = new BestSWSParams();
		
		// perform parameter selection on chosen spark learner
		switch (sparkLearner) 
		{
		case LOGISTIC_REGRESSION_WITH_SGD: 
		{
			//FOR QUICK TESTING PURPOSES
			/*int[] numIters = new int[] {200};
			double[] stepSizes = new double[] {40.0};
			double[] miniBatchFractions = new double[] {0.2};*/
			
			
			// ===== FOR LOGISTIC_REGRESSION_WITH_SGD =====
			int[] numIters = new int[] {150, 200};
			double[] stepSizes = new double[] {20.0, 40.0, 80.0};
			double[] miniBatchFractions = new double[] {0.1, 0.2};
			
			//for each parameter do CV and find best model at the end
			for (int numIter : numIters)
			{
				for (double stepSize : stepSizes)
				{
					for (double miniBatchFraction : miniBatchFractions)
					{
						//initially 0
						double sumOfAccuracies = 0;
						
						//initially 0
						double totalRuntime = 0;
						
						// initialize resultString with fold
						String resultString = "\n====== PARAM GROUP #" + paramGroup
								+ " ======\n";
						
						for (int j = 0; j < numFolds; j++) 
						{
							// cross validation started here, get nanotime
							long startTime = System.nanoTime();

							JavaRDD<LabeledPoint> emptyRDD = sc.emptyRDD();
							List<JavaRDD<LabeledPoint>> rddList 
								= new ArrayList<JavaRDD<LabeledPoint>>();

							for (int index = 0; index < numFolds; index++) {
								// j-th split as a test data, and union all other
								// rdds for training
								if (j != index) {
									// aggragate rdds in the list
									rddList.add(modelSelectionSplits[index]);
								} // if
							} // for

							// now do single union operation
							JavaRDD<LabeledPoint> trainingData 
								= sc.union(emptyRDD, rddList)
									.coalesce(initialPartitionCount); // coalesce to
																		// data's
																		// partition
																		// count
							
							//System.out.println(j + 1 + ") " +
									//"Training Data Count: " + trainingData.count());
							
							// here we will have trainingData ready with 90% of
							// whole dataset
							// and testData will be just 10%
							JavaRDD<LabeledPoint> testData = modelSelectionSplits[j];

							LogisticRegressionModel sparkModel =

							LogisticRegressionWithSGD
							.train(trainingData.rdd(), numIter, stepSize, miniBatchFraction);
							//.setIntercept(true);

							// Compute raw scores on the test set.
							JavaRDD<Tuple2<Object, Object>> predictionAndLabels = testData
									.map(new Function<LabeledPoint, Tuple2<Object, Object>>() {
										
										private static final long serialVersionUID 
											= -5957705839524278600L;

										public Tuple2<Object, Object> call(
												LabeledPoint p) {
											Double prediction = sparkModel
													.predict(p.features());
											return new Tuple2<Object, Object>(
													prediction, p.label());
										}
									});

							// Get evaluation metrics.
							MulticlassMetrics metrics = new MulticlassMetrics(
									predictionAndLabels.rdd());

							// Confusion matrix
							Matrix confusion = metrics.confusionMatrix();

							// create evaluator object from confusion matrix
							Evaluator eval = new Evaluator(confusion);

							//get test accuracy
							double validationAccuracy = eval.accuracy();
							
							//sum the the accuracies up
							sumOfAccuracies += validationAccuracy;

							// show time in milliseconds scale
							double runTimeInMillis = (System.nanoTime() - startTime)
									/ (double) 1000000;
							
							//add to total runtime
							totalRuntime += runTimeInMillis;
							
							// free up memory, slows down performance
							//trainingData.unpersist();

							// System.exit(0);

						} // for (CROSS VALIDATION MODEL SELECTION)
						
						resultString += "\nParam Group trained with ("
								+ "numIter = "
								+ numIter
								+ " stepSize = "
								+ stepSize
								+ " miniBatchFraction = "
								+ miniBatchFraction
								+ ") in "
								+ totalRuntime / numFolds
								+ " millisiseconds.\n";
						
						//calculate average accuracy after numFolds folds
						double avgAccuracy = sumOfAccuracies / numFolds;
						
						resultString += "Param Group has avg. accuracy of "
								+ avgAccuracy
								+ " on validation.\n";
						
						globalResultString += resultString;
						System.out.println(resultString);
						
						if (avgAccuracy > bestValidationAccuracy) 
						{
							//set best params
					        bestLRWSParams.setBestStepSize(stepSize);
					        bestLRWSParams.setBestMiniBatchFraction(miniBatchFraction);
					        bestLRWSParams.setBestNumIter(numIter);
					        
					        bestValidationAccuracy = avgAccuracy;
					      } // if
						
						//increment paramGroup
						paramGroup ++;
						
					} // for miniBatchFractions
				} // for stepSizes
			} // for numIters
			
			System.out.println("====== RESULT =====\n");
			System.out.println("bestNumIter = "
					+ bestLRWSParams.getBestNumIter()
					+ " bestStepSize = "
					+ bestLRWSParams.getBestStepSize()
					+ " bestMiniBatchFraction = "
					+ bestLRWSParams.getBestMiniBatchFraction());
			
			//Also add to global string
			globalResultString += "====== RESULT =====\n\n";
			globalResultString += "bestNumIter = "
					+ bestLRWSParams.getBestNumIter()
					+ " bestStepSize = "
					+ bestLRWSParams.getBestStepSize()
					+ " bestMiniBatchFraction = "
					+ bestLRWSParams.getBestMiniBatchFraction() + "\n";
			
			break;
		} // case LOGISTIC
		case SVM_WITH_SGD: 
		{
			//FOR QUICK TESTING PURPOSES
			/*int[] numIters = new int[] {200};
			double[] stepSizes = new double[] {80.0};
			double[] regParams = new double[] {1.0/8192.0};*/
			
			// ===== FOR SVM_WITH_SGD =====
			double[] stepSizes = new double[] {20.0, 40.0, 80.0};
			int[] numIters = new int[]{150, 200};
			double[] regParams = new double[]{1.0/8192.0, 1.0/4096.0};
			
			//for each parameter do CV and find best model at the end
			for (int numIter : numIters)
			{
				for (double stepSize : stepSizes)
				{
					for (double regParam : regParams)
					{
						//initially 0
						double sumOfAccuracies = 0;
						
						//initially 0
						double totalRuntime = 0;
						
						// initialize resultString with fold
						String resultString = "\n====== PARAM GROUP #" + paramGroup
								+ " ======\n";
						
						for (int j = 0; j < numFolds; j++) 
						{
							// cross validation started here, get nanotime
							long startTime = System.nanoTime();

							JavaRDD<LabeledPoint> emptyRDD = sc.emptyRDD();
							List<JavaRDD<LabeledPoint>> rddList 
								= new ArrayList<JavaRDD<LabeledPoint>>();

							for (int index = 0; index < numFolds; index++) {
								// j-th split as a test data, and union all other
								// rdds for training
								if (j != index) {
									// aggragate rdds in the list
									rddList.add(modelSelectionSplits[index]);
								} // if
							} // for

							// now do single union operation
							JavaRDD<LabeledPoint> trainingData 
								= sc.union(emptyRDD, rddList)
									.coalesce(initialPartitionCount); // coalesce to
																		// data's
																		// partition
																		// count
							
							//System.out.println(j + 1 + ") " +
									//"Training Data Count: " + trainingData.count());
							
							// here we will have trainingData ready with 90% of
							// whole dataset
							// and testData will be just 10%
							JavaRDD<LabeledPoint> testData = modelSelectionSplits[j];

							//train with predefined numIter, stepSize and regParam
							SVMModel sparkModel 
								= SVMWithSGD
								.train(trainingData.rdd(), numIter, stepSize, regParam);
							//.setIntercept(true);

							// Compute raw scores on the test set.
							JavaRDD<Tuple2<Object, Object>> predictionAndLabels = testData
									.map(new Function<LabeledPoint, Tuple2<Object, Object>>() {
										
										private static final long serialVersionUID 
											= -5957705839524278600L;

										public Tuple2<Object, Object> call(
												LabeledPoint p) {
											Double prediction = sparkModel
													.predict(p.features());
											return new Tuple2<Object, Object>(
													prediction, p.label());
										}
									});

							// Get evaluation metrics.
							MulticlassMetrics metrics = new MulticlassMetrics(
									predictionAndLabels.rdd());

							// Confusion matrix
							Matrix confusion = metrics.confusionMatrix();

							// create evaluator object from confusion matrix
							Evaluator eval = new Evaluator(confusion);

							//get test accuracy
							double validationAccuracy = eval.accuracy();
							
							//sum the the accuracies up
							sumOfAccuracies += validationAccuracy;

							// show time in milliseconds scale
							double runTimeInMillis = (System.nanoTime() - startTime)
									/ (double) 1000000;
							
							//add to total runtime
							totalRuntime += runTimeInMillis;
							
							// free up memory, slows down performance
							//trainingData.unpersist();

							// System.exit(0);

						} // for (CROSS VALIDATION MODEL SELECTION)
						
						resultString += "\nParam Group trained with ("
								+ "numIter = "
								+ numIter
								+ " stepSize = "
								+ stepSize
								+ " regParam = "
								+ regParam
								+ ") in "
								+ totalRuntime / numFolds
								+ " millisiseconds.\n";
						
						//calculate average accuracy after numFolds folds
						double avgAccuracy = sumOfAccuracies / numFolds;
						
						resultString += "Param Group has avg. accuracy of "
								+ avgAccuracy
								+ " on validation.\n";
						
						globalResultString += resultString;
						System.out.println(resultString);
						
						//if avgAccuracy is greater then we found better params
						if (avgAccuracy > bestValidationAccuracy) 
						{
							//set best params for SVM
					        bestSWSParams.setBestStepSize(stepSize);
					        bestSWSParams.setBestRegParam(regParam);
					        bestSWSParams.setBestNumIter(numIter);
					        
					        //change best validation accuracy
					        bestValidationAccuracy = avgAccuracy;
					      } // if
						
						//increment paramGroup
						paramGroup ++;
						
					} // for regParams
				} // for stepSizes
			} // for numIters
			
			System.out.println("====== RESULT =====\n");
			System.out.println("bestNumIter = "
					+ bestSWSParams.getBestNumIter()
					+ " bestStepSize = "
					+ bestSWSParams.getBestStepSize()
					+ " bestRegParam = "
					+ bestSWSParams.getBestRegParam());
			
			//Also add to global string
			globalResultString += "====== RESULT =====\n\n";
			globalResultString += "bestNumIter = "
					+ bestSWSParams.getBestNumIter()
					+ " bestStepSize = "
					+ bestSWSParams.getBestStepSize()
					+ " bestRegParam = "
					+ bestSWSParams.getBestRegParam() + "\n";
			
			break;
		} // SVM_WITH_SGD
		default: 			// FOR DEFAULT CASE USE LOGISTIC_REGRESSION_WITH_SGD
		{
			// ===== FOR LOGISTIC_REGRESSION_WITH_SGD =====
			int[] numIters = new int[] {150, 200};
			double[] stepSizes = new double[] {20.0, 40.0, 80.0};
			double[] miniBatchFractions = new double[] {0.1, 0.2};
			
			
			//for each parameter do CV and find best model at the end
			for (int numIter : numIters)
			{
				for (double stepSize : stepSizes)
				{
					for (double miniBatchFraction : miniBatchFractions)
					{
						//initially 0
						double sumOfAccuracies = 0;
						
						//initially 0
						double totalRuntime = 0;
						
						// initialize resultString with fold
						String resultString = "\n====== PARAM GROUP #" + paramGroup
								+ " ======\n";
						
						for (int j = 0; j < numFolds; j++) 
						{
							// cross validation started here, get nanotime
							long startTime = System.nanoTime();

							JavaRDD<LabeledPoint> emptyRDD = sc.emptyRDD();
							List<JavaRDD<LabeledPoint>> rddList 
								= new ArrayList<JavaRDD<LabeledPoint>>();

							for (int index = 0; index < numFolds; index++) {
								// j-th split as a test data, and union all other
								// rdds for training
								if (j != index) {
									// aggragate rdds in the list
									rddList.add(modelSelectionSplits[index]);
								} // if
							} // for

							// now do single union operation
							JavaRDD<LabeledPoint> trainingData 
								= sc.union(emptyRDD, rddList)
									.coalesce(initialPartitionCount); // coalesce to
																		// data's
																		// partition
																		// count
							
							//System.out.println(j + 1 + ") " +
									//"Training Data Count: " + trainingData.count());
							
							// here we will have trainingData ready with 90% of
							// whole dataset
							// and testData will be just 10%
							JavaRDD<LabeledPoint> testData = modelSelectionSplits[j];
							
							//train with predefined numIter, stepSize and miniBatchFraction
							LogisticRegressionModel sparkModel =

							LogisticRegressionWithSGD
							.train(trainingData.rdd(), numIter, stepSize, miniBatchFraction);
							//.setIntercept(true);

							// Compute raw scores on the test set.
							JavaRDD<Tuple2<Object, Object>> predictionAndLabels = testData
									.map(new Function<LabeledPoint, Tuple2<Object, Object>>() {
										
										private static final long serialVersionUID 
											= -5957705839524278600L;

										public Tuple2<Object, Object> call(
												LabeledPoint p) {
											Double prediction = sparkModel
													.predict(p.features());
											return new Tuple2<Object, Object>(
													prediction, p.label());
										}
									});

							// Get evaluation metrics.
							MulticlassMetrics metrics = new MulticlassMetrics(
									predictionAndLabels.rdd());

							// Confusion matrix
							Matrix confusion = metrics.confusionMatrix();

							// create evaluator object from confusion matrix
							Evaluator eval = new Evaluator(confusion);

							//get test accuracy
							double validationAccuracy = eval.accuracy();
							
							//sum the the accuracies up
							sumOfAccuracies += validationAccuracy;

							// show time in milliseconds scale
							double runTimeInMillis = (System.nanoTime() - startTime)
									/ (double) 1000000;
							
							//add to total runtime
							totalRuntime += runTimeInMillis;
							
							// free up memory, slows down performance
							//trainingData.unpersist();

							// System.exit(0);

						} // for (CROSS VALIDATION MODEL SELECTION)
						
						resultString += "\nParam Group trained with ("
								+ "numIter = "
								+ numIter
								+ " stepSize = "
								+ stepSize
								+ " miniBatchFraction = "
								+ miniBatchFraction
								+ ") in "
								+ totalRuntime / numFolds
								+ " millisiseconds.\n";
						
						//calculate average accuracy after numFolds folds
						double avgAccuracy = sumOfAccuracies / numFolds;
						
						resultString += "Param Group has avg. accuracy of "
								+ avgAccuracy
								+ " on validation.\n";
						
						globalResultString += resultString;
						System.out.println(resultString);
						
						if (avgAccuracy > bestValidationAccuracy) 
						{
							//set best params
					        bestLRWSParams.setBestStepSize(stepSize);
					        bestLRWSParams.setBestMiniBatchFraction(miniBatchFraction);
					        bestLRWSParams.setBestNumIter(numIter);
					        
					        bestValidationAccuracy = avgAccuracy;
					      } // if
						
						//increment paramGroup
						paramGroup ++;
						
					} // for miniBatchFractions
				} // for stepSizes
			} // for numIters
			
			System.out.println("====== RESULT =====\n");
			System.out.println("bestNumIter = "
					+ bestLRWSParams.getBestNumIter()
					+ " bestStepSize = "
					+ bestLRWSParams.getBestStepSize()
					+ " bestMiniBatchFraction = "
					+ bestLRWSParams.getBestMiniBatchFraction());
			
			//Also add to global string
			globalResultString += "====== RESULT =====\n\n";
			globalResultString += "bestNumIter = "
					+ bestLRWSParams.getBestNumIter()
					+ " bestStepSize = "
					+ bestLRWSParams.getBestStepSize()
					+ " bestMiniBatchFraction = "
					+ bestLRWSParams.getBestMiniBatchFraction() + "\n";
			
		} // default

		} // switch
		
		//System.exit(0);
		
		//=================== END OF PARAMETER SELECTION USING CV =========================
		
		
		
		System.out.println("\nNormal CV started....");
		System.out.println("Data partitions: " + cvSplits[1].partitions().size());
		
		//also add to global string
		globalResultString += "\nNormal CV started....\n";
		globalResultString += "Data partitions: " + cvSplits[1].partitions().size() + "\n";
			    
		//System.exit(0);
		
		//getSplitVector for 10-fold cross validation
		double[] splitVector = getSplitVector(numFolds);
		//System.out.println(Arrays.toString(splitVector));
		

	    // Split initial RDD into N, then join to get [90% training data, 10% testing data].
	    JavaRDD<LabeledPoint>[] splits = cvSplits[1].randomSplit(splitVector);
	    
	    
	    //we will report average test 
	    double sumOfAccuracies = 0;
	    
	    //represents total runtime of the of all folds in cross validation
	    double totalRuntime = 0;
	    
	    //perform n-fold cross validation to find average accuracy
	    //and average runtime of the Spark Parallel Algorithm
	  	for (int j = 0; j < numFolds; j ++)
	  	{
	  		//cross validation started here, get nanotime
		    long sparkFoldStartTime = System.nanoTime();
		    
		    //initialize resultString with fold
		    String resultString = "\n****** FOLD #" + (j + 1) + " ******\n";
	  		
	  		JavaRDD<LabeledPoint> emptyRDD = sc.emptyRDD();
	  		List<JavaRDD<LabeledPoint>> rddList = new ArrayList<JavaRDD<LabeledPoint>>();
	  		
	  		for (int index = 0; index < numFolds; index ++)
	  		{
	  			//j-th split as a test data, and union all other rdds for training
	  			if (j != index)
	  			{
	  				//aggragate rdds in the list
	  				rddList.add(splits[index]);
	  			} // if
	  		} // for
	  		
	  		//now do single union operation
	  		JavaRDD<LabeledPoint> trainingData = sc.union(emptyRDD, rddList)
	  				.coalesce(initialPartitionCount); // coalesce to data's partition count
	  		
	  		//here we will have trainingData ready with 90% of whole dataset
	  		//and testData will be just 10%
	  		JavaRDD<LabeledPoint> testData = splits[j];
			
	  		
	  		//will be used both for SVMModel and LogisticRegressionModel
	  		JavaRDD<Tuple2<Object, Object>> predictionAndLabels;
	  		
	  		//perform normal learning on chosen spark learner
			switch (sparkLearner)
			{
			case LOGISTIC_REGRESSION_WITH_SGD: 
				{
					//train and build sparkModel from best parameters
					LogisticRegressionModel sparkModel 
						=	LogisticRegressionWithSGD
							.train(trainingData.rdd(), bestLRWSParams.getBestNumIter(), 
									bestLRWSParams.getBestStepSize(),
									bestLRWSParams.getBestMiniBatchFraction());
									//.setIntercept(true);
					
					//get obtained weights
					Vector weights = sparkModel.weights();
					
					// also input models generated by parallel radon machine
					resultString += "\n===== Model by Spark =====\n\n" + sparkModel.intercept() + ", " + weights + "\n";

					// Compute raw scores on the test set.
					predictionAndLabels = testData
							.map(new Function<LabeledPoint, Tuple2<Object, Object>>() 
							{
								/**
								 * 
								 */
								private static final long serialVersionUID = -5957705839524278600L;

								public Tuple2<Object, Object> call(LabeledPoint p) {
									Double prediction = sparkModel.predict(p.features());
									return new Tuple2<Object, Object>(prediction, p.label());
								}
							}); // map
					
					break;
				} // case LOGISTIC_REGRESSION_WITH_SGD
			case SVM_WITH_SGD: 
				{
					//train and build sparkModel from best parameters
			  		SVMModel sparkModel 
			  			= SVMWithSGD
			  				.train(trainingData.rdd(), bestSWSParams.getBestNumIter(),
			  					   bestSWSParams.getBestStepSize(),
			  					   bestSWSParams.getBestRegParam());
			  					   //.setIntercept(true);
					
					//get obtained weights
					Vector weights = sparkModel.weights();
					
					// also input models generated by parallel radon machine
					resultString += "\n===== Model by Spark =====\n\n" + sparkModel.intercept() + ", " + weights + "\n";

					// Compute raw scores on the test set.
					predictionAndLabels = testData
							.map(new Function<LabeledPoint, Tuple2<Object, Object>>() 
							{
								/**
								 * 
								 */
								private static final long serialVersionUID = -5957705839524278600L;

								public Tuple2<Object, Object> call(LabeledPoint p) {
									Double prediction = sparkModel.predict(p.features());
									return new Tuple2<Object, Object>(prediction, p.label());
								}
							}); // map
					break;
				} // SVM_WITH_SGD
			default:				//FOR DEFAULT USE LOGISTIC_REGRESSION_WITH_SGD
				{
					//train and build sparkModel from best parameters
					LogisticRegressionModel sparkModel 
						=	LogisticRegressionWithSGD
							.train(trainingData.rdd(), bestLRWSParams.getBestNumIter(), 
									bestLRWSParams.getBestStepSize(),
									bestLRWSParams.getBestMiniBatchFraction());
									//.setIntercept(true);
					
					//get obtained weights
					Vector weights = sparkModel.weights();
					
					// also input models generated by parallel radon machine
					resultString += "\n===== Model by Spark =====\n\n" + sparkModel.intercept() + ", " + weights + "\n";

					// Compute raw scores on the test set.
					predictionAndLabels = testData
							.map(new Function<LabeledPoint, Tuple2<Object, Object>>() 
							{
								/**
								 * 
								 */
								private static final long serialVersionUID = -5957705839524278600L;

								public Tuple2<Object, Object> call(LabeledPoint p) {
									Double prediction = sparkModel.predict(p.features());
									return new Tuple2<Object, Object>(prediction, p.label());
								}
							}); // map
				} // default

			} // switch

			// Get evaluation metrics.
			MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels.rdd());
			
			//output data instance labels
			//resultString += "\nLabels: " + Arrays.toString(metrics.labels()) + "\n";
			
			// Confusion matrix
			Matrix confusion = metrics.confusionMatrix();
			resultString += "\nConfusion matrix: \n\n" + confusion + "\n";
			
			//create evaluator object from confusion matrix
			Evaluator eval = new Evaluator(confusion);
			
			// Overall statistics
			//there is a problem with metrics.recall, metrics.precision, and metrics.fMeasure
			//that's why I used my own method
			resultString += "\nAccuracy = " + eval.accuracy() + "\n";
			resultString += "Precision = " + eval.precision() + "\n";
			resultString += "Recall = " + eval.recall() + "\n";
			resultString += "F1 measure = " + eval.fMeasure() + "\n";
			
			double accuracy = eval.accuracy();
			resultString += "\n\n====== Accuracy on test set: " + accuracy + " ======\n";
			sumOfAccuracies += accuracy;
			
			//get the end time of the fold
		  	long sparkFoldEndTime = System.nanoTime();
		  	
		  	//show time in milliseconds scale
		  	double runTimeInMillis = (sparkFoldEndTime - sparkFoldStartTime) / (double) 1000000;
		  	resultString += "====== Spark Fold Run Time: " + runTimeInMillis + " =======\n\n";
		  	
		  	//print and also write to a file
		  	System.out.println(resultString);
		  	globalResultString += resultString;
		  	
		  	totalRuntime += runTimeInMillis;
		  	
		  	//System.exit(0);
			
	  	} // for (N-FOLD CROSS VALIDATION)
	    
	  	//also append average accuracy to global result string
	  	double averageAccuracy = sumOfAccuracies / numFolds;
	  	System.out.println("------------------------------------------------------------------");
	  	System.out.println("Spark Average Cross-Validation Test Accuracy: " + averageAccuracy);
	  	
	  	globalResultString += "\n------------------------------------------------------------------\n";
	  	globalResultString += "Spark Average Cross-Validation Test Accuracy: " + averageAccuracy + "\n";
	  	
	  	//get the average runtime
	  	double averageRunTime = totalRuntime / numFolds;
	  	System.out.println("Spark Average Cross-Validation Run Time: " + averageRunTime + " milliseconds");
	  	System.out.println("------------------------------------------------------------------");
	  	
	  	globalResultString += "Spark Average Cross-Validation Run Time: " + averageRunTime
	  			+ " milliseconds\n";
	  	globalResultString += "------------------------------------------------------------------\n";
	  	
	  	//write models to a file for each fold of cross validation
	  	writeModelsToFile(outputDirectory + SPARK_OUTPUT_FILE,
	  			//("C:\\Users\\Rufat Babayev\\Desktop\\codrna\\Spark_Models.txt",
	  			globalResultString);
	  	
	} // crossValidateForSparkLearner

	//helper method to choose one model randomly from the list of models 
	private static Vector getRandomModel(List<Vector> models) 
	{
	    int randIndex = new Random().nextInt(models.size());
	    return models.get(randIndex);
	} // getRandomModel
	
	//helper method to get splitting array for n-fold cross validation
	private static double[] getSplitVector(int n)
	{
		double[] splitter = new double[n];
		
		//iteratively assign values of vector
		for (int index = 0; index < n; index ++)
		{
			splitter[index] = (double) n / (double) 100;
		} // for
		
		return splitter;
	} // getSplitVector
	
	//helper method to get a split vector for CV parameter selection
	//fraction would be used to get the percentage of data for parameter selection
	private static double[] getSplitVectorForCVParameterSelection
				(int numFeatures,
				long allDataCount)
	{
		double[] splitter = new double[2];
		
		//fraction of data to be used for hyperparameter optimization
		double fraction;
		
		//if all data count is smaller than 100000 then make fraction
		//smaller
		if(allDataCount < 100000)
		{
			fraction = 0.01; // fraction is 1%
			
			//check whether 1% of data is bigger than numFeatures * 20
			if(fraction * allDataCount < numFeatures * 20)
			{
				fraction = numFeatures * 20 / (double) allDataCount;
			} // if
			
		} // if
		else
		{
			fraction = 10000 / (double) allDataCount;
			
			//check whether chosen % of data is bigger numFeatures * 20
			if(fraction * allDataCount < numFeatures * 20)
			{
				fraction = numFeatures * 20 / (double) allDataCount;
			} // if
			
		} // else
		
		splitter[0] = fraction;
		splitter[1] = 1 - splitter[0]; // rest of the data would be used for cross validation
		
		return splitter;
	} // getSplitVectorForCVParameterSelection
	
	/*//helper method to generate split vector with a = (numFeatures x 20) / allDataCount
	//and 1 - a, where it is {a, 1 - a}
	private static double[] getSplitVectorForCVParameterSelection(
			int numFeatures, long allDataCount)
	{
		double[] splitter = new double[2];
		
		splitter[0] = numFeatures * 20 / (double) allDataCount;
		splitter[1] = 1 - splitter[0]; // rest of the data would be used for cross validation
		
		return splitter;
	} // getSplitVectorForCVParameterSelection
	*/
	
	/*//helper method to get a fraction of data in one initial parition
	private static double[] getSplitVectorForCVParameterSelection(
			int numFeatures, long allDataCount, int requiredPartitionCount)
	{
		double[] splitter = new double[2];
		
		//get the fraction of data for one initial partition
		double fraction = requiredPartitionCount / (double) allDataCount;
		
		//if fraction is smaller than numFeatures * 20 / (double) allDataCount
		if (fraction < numFeatures * 20 / (double) allDataCount)
		{
			fraction =  numFeatures * 20 / (double) allDataCount;
		} // if
		
		splitter[0] = fraction;
		splitter[1] = 1 - splitter[0]; // rest of the data would be used for cross validation
		
		return splitter;
	} // getSplitVectorForCVParameterSelection
	*/
	
	//helper method to transform LabeledPoint to Instance
	private static Instances getInstancesFrom(List<LabeledPoint> data)
	{
		System.out.println();
		System.out.println("Transformation: 'LabeledPoint => Instance' started...");
		
		//create an empty arraylist first
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		
		// for one labeledPoint in a partition
		// do get feature vector and create attributes from them
		LabeledPoint labeledPoint = data.get(0);
		double[] features = labeledPoint.features().toArray();

		for (int i = 1; i <= features.length; i++) 
		{
			// attribute name will be x1, x2, x3 etc...
			Attribute attribute = new Attribute("x" + i);
			attributes.add(attribute);
		} // for
		 
		 // Declare the class attribute along with its values
		 FastVector fvClassVal = new FastVector(2);
		 fvClassVal.addElement("1.0");
		 fvClassVal.addElement("0.0");
		 Attribute label = new Attribute("label", fvClassVal);
		 
		 // Declare the feature vector, first add class label attribute
		 FastVector fvWekaAttributes = new FastVector(4);
		 fvWekaAttributes.addElement(label);
		 //then for each attribute in an attributes add them to wekaAttributes
		 for (Attribute attribute : attributes)
		 {
			 fvWekaAttributes.addElement(attribute);
		 } // for
		 
		 // Create an empty training set
		 Instances dataset = new Instances("partition", fvWekaAttributes, 10);
		 // Set class index
		 dataset.setClassIndex(0);
		 
		 //for each labeledPoint in partition, create an instance
		 //from that labeled point
		 for (LabeledPoint lbldPoint : data)
		 {
			 // Create the instance, number of attrbiutes will be #features + label
			 Instance instance = new Instance(attributes.size() + 1);
			 
			 //class label of labeled point
			 double lbl = lbldPoint.label();
			 
			 //first set class label for the attribute
			 instance.setValue((Attribute)fvWekaAttributes.elementAt(0), "" + lbl);
			 
			 double[] attrs = lbldPoint.features().toArray();

			 for (int index = 0; index < attrs.length; index++) 
			 {
				 instance.setValue((Attribute) fvWekaAttributes
						 .elementAt(index + 1), attrs[index]);
			 } // for
				
			 // add the instance
			 dataset.add(instance);
		 } // for
		 
		 /*instance.setValue((Attribute)fvWekaAttributes.elementAt(0), 1.0);
		 instance.setValue((Attribute)fvWekaAttributes.elementAt(1), 0.5);
		 instance.setValue((Attribute)fvWekaAttributes.elementAt(2), "gray");
		 instance.setValue((Attribute)fvWekaAttributes.elementAt(3), "positive");*/
		
		 System.out.println("Transformation: 'LabeledPoint => Instance' finished...");
		 
		return dataset;
	} // getInstancesFrom
	
	//helper method to write models to a file
	private static void writeModelsToFile(String filePath, String toWrite)
	{
		// use printwriter
		PrintWriter writer = null;

		try {

			writer = new PrintWriter(
					new File(filePath));
			writer.write(toWrite);

		} // try
		catch (Exception e) {
			e.printStackTrace();
		} // catch
		finally {
			if (writer != null)
				writer.close();

		} // finally
	} // writeModelstToFile
	
	//helper method to detect number of features of one data instance
	//in an arbitrary datatset
	private static Integer getNumFeatures(String inputFilePath)
	{
		//Libsvm file format is the following
		//<label> <index1>:<value1> <index2>:<value2> ...
		BufferedReader reader = null;
		Integer numFeatures = null;
		
		try
		{
			reader = new BufferedReader(new FileReader(inputFilePath));
			String firstLine = reader.readLine().trim();
			// Stop. we only need the first line.
			
			//get an input stream from a string
			InputStream stream 
				= new ByteArrayInputStream(firstLine.getBytes(StandardCharsets.UTF_8));
			
			//validate the libsvm format and determine number of features
			try 
			{
				LibSVMLoader svmLoader = new LibSVMLoader();
				svmLoader.setSource(stream);
				Instances svmData = svmLoader.getStructure();
				
				//svmData.numClasses() returns the number of class labels.
				//our program works only with one class label
				if(svmData.numClasses() > 1)
				{
					throw new Exception("Number of class-labels can only be one...");
				} // if
				
				//#attributes - #class_labels
				numFeatures = svmData.numAttributes() - svmData.numClasses();
			}
			catch(NumberFormatException ex)
			{
				System.out.println("\n" + ex.getMessage());
			} // catch
			catch (IOException ex) 
			{
				System.out.println("\n" + ex.getMessage());
			} // catch
			catch(Exception ex)
			{
				System.out.println("\n" + ex.getMessage());
			} // catch
			
			//split
			//String[] strArray = firstLine.split(" ");
			
			/*int count = 1;
			for (String str : strArray)
			{
				System.out.println(count + ". " + str);
				count ++;
			} // for
			*/
			
			//System.out.println("#Features: " + (strArray.length - 1));
			System.out.println("Number of features of data instance: " + numFeatures);
			
		} // try
		catch(Exception e)
		{
			e.printStackTrace();
		} // catch
		finally
		{
			try
			{
				if (reader != null)
					reader.close();
				
			} // try
			catch(IOException e)
			{
					e.printStackTrace();
			} // catch
			
		} // finally
		
		return numFeatures;
		
	} // getNumFeatures
	
	
	/*
	 * helper method for param selection for Logistic class
	 * only ps will be affected
	 * where we will see this effect in main method
	 */
	private static String logisticParamSelection(int noOfFolds, Instances paramSelectionInstances,
			CVParameterSelection ps) 
	{
		// FOR LOGISTIC
		// setup classifier
		ps.setClassifier(new Logistic());
		String result = "";

		/*
		 * -C 
		 * The confidence parameter C.
		 * -R <ridge> 
		 * Set the ridge in the log-likelihood.
		 * -M <number> 
		 * Set the maximum number of iterations (default -1, until convergence).
		 */
		
		//long start = System.nanoTime();
		
		try 
		{
			ps.setNumFolds(noOfFolds); // using N-fold CV
			// This will test the confidence parameter from 0.001 to 0.01
			// with step size 0.001 (= 10 steps)
			ps.addCVParameter("C 0.001 0.01 10"); //("C 0.01 0.05 5");
			ps.addCVParameter("R 1.0E-14 1.0E-8 7"); // test ridge param from
														// 1.0E-14 1.0E-8 in 7 steps

			// "-1 until convergence is always best"
			// ps.addCVParameter("M 10 40 4"); // test max iters from 10 to 40

			// build and output best options
			ps.buildClassifier(paramSelectionInstances);
			System.out.println("Best Params: "
					+ Utils.joinOptions(ps.getBestClassifierOptions()));
			// output could be: -C 0.1 -R 1.0E-8 -M -1
			result += "Best Params for PRM: "
					+ Utils.joinOptions(ps.getBestClassifierOptions()) + "\n";
		} // try
		catch (Exception ex) 
		{
			ex.printStackTrace();
		} // catch
		
		//System.out.println("Milliseconds: "
				//+ ((System.nanoTime() - start) / (double) 1000000));
		
		return result;
	} // logisticParamSelection
	
	/*
	 * helper method for param selection for SMO class
	 * only ps will be affected
	 * where we will see this effect in main method
	 */
	private static String smoParamSelection(int noOfFolds, Instances paramSelectionInstances,
			CVParameterSelection ps) 
	{
		// FOR SMO - support vector
		// setup classifier
		SMO smo = new SMO();
		// smo.setFilterType(new SelectedTag(SMO.FILTER_NONE,
		// SMO.TAGS_FILTER));
		ps.setClassifier(smo);
		String result = "";

		/*
		 * -C <double> 
		 * The complexity constant C. (default 1) 
		 * -N 
		 * Whether to 0=normalize/1=standardize/2=neither. (default 0=normalize) 
		 * -L <double> 
		 * The tolerance parameter. (default 1.0e-3) 
		 * -P <double>
		 * The epsilon for round-off error. (default 1.0e-12)
		 * -M 
		 * Fit logistic models to SVM outputs. 
		 * -V <double> 
		 * The number of folds for the internal cross-validation. (default -1, use training data) 
		 * -W <double> 
		 * The random number seed. (default 1)
		 */

		//long start = System.nanoTime();

		try 
		{
			ps.setNumFolds(noOfFolds); // using N-fold CV

			// This will test the complexity constant from 2.0 to 3.0
			// with step size 1.0 (= 2 steps)
			ps.addCVParameter("C 2.0 3.0 2");
			// ps.addCVParameter("C 0.01 1.0 10");

			// test tolerance param from 1.0E-3 1.0E-2
			ps.addCVParameter("L 1.0e-3 1.0e-2 2");
			// ps.addCVParameter("L 1.0e-4 1.0e-3 2");

			// test epsilon param from 1.0E-13 1.0E-12
			ps.addCVParameter("P 1.0e-13 1.0e-12 2");
			// ps.addCVParameter("P 1.0e-13 1.0e-12 2");

			// build and output best options
			ps.buildClassifier(paramSelectionInstances);
			System.out.println("Best Params: "
					+ Utils.joinOptions(ps.getBestClassifierOptions()));
			// output could be: -C 1 -N 2 -L 1.0e-3 -P 1.0e-12 ....
			result += "Best Params for PRM: "
					+ Utils.joinOptions(ps.getBestClassifierOptions()) + "\n";
		} // try
		catch (Exception ex) {
			ex.printStackTrace();
		} // catch

		//System.out.println("Milliseconds: "
				//+ ((System.nanoTime() - start) / (double) 1000000));
		
		return result;
	} // smoParamSelection
	
} // class ParallelRadonMachine
