package com.rb.sparkprll;

import java.util.Scanner;

//Main class of the program
public class Launcher {
	
	//private static final String INPUT_FILE 
	//= "C:\\Users\\Rufat Babayev\\Desktop\\susy\\susy.libsvm";
	//= "C:\\Users\\Rufat Babayev\\Desktop\\susy\\susy_shuffled.libsvm";
	//= "C:\\Users\\Rufat Babayev\\Desktop\\higgs\\higgs.libsvm";
	//= "C:\\Users\\Rufat Babayev\\Desktop\\codrna\\codrna_shuffled.libsvm";
	//= "C:\\Users\\Rufat Babayev\\Desktop\\codrna\\codrna.libsvm";
	
	//private static final String OUTPUT_DIR = "C:\\Users\\Rufat Babayev\\Desktop\\codrna\\";
	
	public static void main(String[] args)
	{
		// required vars
		String inputFilePath = null;
		String outputDir = null;
		int numIters_H = 0;
		
		//by default, SMO and SVM_WITH_SGD are chosen
		BaseLearner baseLearner = BaseLearner.SMO;
		SparkLearner sparkLearner = SparkLearner.SVM_WITH_SGD;
		
		
		// retrieve the input file path, output dir and number of iterations H
		// using scanner
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.print("Enter input file path: ");
			inputFilePath = scanner.nextLine();

			// now outputdir
			System.out.print("Enter output directory: ");
			outputDir = scanner.nextLine();

			// now the number of iterations
			System.out.print("Enter number of iterations H: ");
			numIters_H = scanner.nextInt();
			
			//for choosing base learner
			System.out.print("\nChoose base learner [(1) Logistic, (2) SMO]: ");
			switch(scanner.nextInt())
			{
			case 1:
			{
				baseLearner = BaseLearner.LOGISTIC; 
				System.out.println("== Logistic regression with SGD"
						+ " has been choosen as a base learner ==");
				break;
			} // case 1
			case 2:
			{
				baseLearner = BaseLearner.SMO;
				System.out.println("== John Platt's sequential minimal optimization algorithm"
						+ " for training a support vector classifier"
						+ " has been choosen as a base learner ==");
				break;
			} // case 2
			default :
			{
				baseLearner = BaseLearner.LOGISTIC;
				System.out.println("== Logistic regression with SGD"
						+ " has been choosen as a base learner ==");
			} // default
			
			} // switch
			
			
			//for choosing spark learner
			System.out.print("\nChoose spark learner [(1) LogisticRegressionWithSGD,"
					+ " (2) SVMWithSGD]: ");
			switch(scanner.nextInt())
			{
			case 1:
			{
				sparkLearner = SparkLearner.LOGISTIC_REGRESSION_WITH_SGD; 
				System.out.println("== LogisticRegressionWithSGD"
						+ " has been choosen as a spark learner ==");
				break;
			} // case 1
			case 2:
			{
				sparkLearner = SparkLearner.SVM_WITH_SGD;
				System.out.println("== SVMWithSGD"
						+ " has been choosen as a spark learner ==");
				break;
			} // case 2
			default :
			{
				sparkLearner = SparkLearner.LOGISTIC_REGRESSION_WITH_SGD; 
				System.out.println("== LogisticRegressionWithSGD"
						+ " has been choosen as a spark learner ==");
			} // default
			
			} // switch
			
		} // try 
		catch (Exception ex) 
		{
			ex.printStackTrace(System.err);
		} // catch
		
		
		//run the machine
		ParallelRadonMachine.run(inputFilePath, outputDir, 
					numIters_H, baseLearner, sparkLearner);
		
		
		//ParallelRadonMachine.run(INPUT_FILE, OUTPUT_DIR, 3, baseLearner, sparkLearner);
	} // main

} // class Launcher
