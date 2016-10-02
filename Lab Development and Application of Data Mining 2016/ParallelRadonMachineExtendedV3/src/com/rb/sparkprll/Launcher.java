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
	
	private static String HDFS_DATA_DIR 
		= "hdfs://str24.kdlan.iais.fraunhofer.de/user/mkamp/data/";
	private static String LOCAL_DATA_DIR 
		= "/home/IAIS/mkamp/java/ParallelRadonMachineExtendedV3/data/";
	
	private static final String HIGGS = "higgs.libsvm";
	private static final String SUSY = "susy.libsvm";
	private static final String CODRNA = "codrna_shuffled.libsvm";
	private static final String POKER = "poker.libsvm";
	private static final String SEA_50 = "sea_50.libsvm";
	
	//full path in HDFS
	private static String HDFS_HIGGS 
		= HDFS_DATA_DIR + HIGGS;
	private static String HDFS_SUSY 
		= HDFS_DATA_DIR + SUSY;
	private static String HDFS_CODRNA
		= HDFS_DATA_DIR + CODRNA;
	private static String HDFS_POKER
		= HDFS_DATA_DIR + POKER;
	private static String HDFS_SEA_50
		= HDFS_DATA_DIR + SEA_50;
	
	
	//full path in local file system
	private static String LOCAL_HIGGS 
		= LOCAL_DATA_DIR + HIGGS;
	private static String LOCAL_SUSY 
		= LOCAL_DATA_DIR + SUSY;
	private static String LOCAL_CODRNA
		= LOCAL_DATA_DIR + CODRNA;
	private static String LOCAL_POKER
		= LOCAL_DATA_DIR + POKER;
	private static String LOCAL_SEA_50
		= LOCAL_DATA_DIR + SEA_50;
	
	private static String OUTPUT_DIR = "/home/IAIS/mkamp/java/ParallelRadonMachineExtendedV3/output/";
	private static String MASTER = "spark://str24.kdlan.iais.fraunhofer.de:7077";
	private static String HADOOP_HOME_DIR = "C:\\winutils\\";
	
	public static void main(String[] args)
	{
		boolean isLocalMode = false;
		boolean discard = false;
		Scanner scanner = new Scanner(System.in);
		
		System.out.print("Do you want to run the experiments in a local mode [(yes) (no)]: ");
		String mode = scanner.nextLine();
		
		if(mode.equalsIgnoreCase("no"))
		{
			isLocalMode = false;
		} // if
		else if(mode.equalsIgnoreCase("yes"))
		{
			isLocalMode = true;
			
			System.out.println("Default data dir: " + LOCAL_DATA_DIR);
			System.out.print("\nCopy paste above dir or type path for new data dir [do not forget / at the end]: " );
			String dataDir = scanner.nextLine();
			
			// adjust full paths
			HDFS_HIGGS = dataDir + HIGGS;
			HDFS_SUSY = dataDir + SUSY;
			HDFS_CODRNA = dataDir + CODRNA;
			HDFS_POKER = dataDir + POKER;
			HDFS_SEA_50 = dataDir + SEA_50;
			
			// adjust full paths
			LOCAL_HIGGS = dataDir + HIGGS;
			LOCAL_SUSY = dataDir + SUSY;
			LOCAL_CODRNA = dataDir + CODRNA;
			LOCAL_POKER = dataDir + POKER;
			LOCAL_SEA_50 = dataDir + SEA_50;
			
			//adjust output dir
			System.out
			.println("\noutput .txt files will be created by driver node "
					+ "using java FileWriter.");
			System.out.println("\nDefault output dir: " + OUTPUT_DIR);
			System.out
			.print("\nCopy paste above dir or type path for new output dir [do not forget / at the end]: ");
			OUTPUT_DIR = scanner.nextLine();
			
			System.out
			.println("\nThere is a line in a code >> System.setProperty(\"hadoop.home.dir\", \"C:\\winutils\\\"); <<");
			System.out
			.print("Do you want to discard that line [(yes) (no)]: ");
			String choice = scanner.nextLine();

			if (choice.equalsIgnoreCase("no")) 
			{
				System.out.println("\nDefault HADOOP_HOME_DIR: "
						+ HADOOP_HOME_DIR);
				System.out
					.print("\nCopy paste above dir or type new hadoop home dir path: ");
				HADOOP_HOME_DIR = scanner.nextLine();
				discard = false;
			} // if
			else if (choice.equalsIgnoreCase("yes")) 
			{
				discard = true;
			} // else if
		} // else if
		else
		{
			System.out.println("Enter yes or no !!!");
			System.exit(0);
		} // else
		
		
		//if not a local mode, then...
		if (!isLocalMode) {
			System.out
					.println("Make sure all .libsvm files are ready in HDFS and in LOCAL FILE system\n");
			// Scanner for input prompt
			try {
				System.out.println("Default HDFS data dir: " + HDFS_DATA_DIR);
				System.out
						.print("\nCopy paste above dir or type path for new HDFS data dir [do not forget / at the end]: ");
				HDFS_DATA_DIR = scanner.nextLine();

				// adjust full paths
				HDFS_HIGGS = HDFS_DATA_DIR + HIGGS;
				HDFS_SUSY = HDFS_DATA_DIR + SUSY;
				HDFS_CODRNA = HDFS_DATA_DIR + CODRNA;
				HDFS_POKER = HDFS_DATA_DIR + POKER;
				HDFS_SEA_50 = HDFS_DATA_DIR + SEA_50;

				System.out.println("\nDefault LOCAL data dir: "
						+ LOCAL_DATA_DIR);
				System.out
						.print("\nCopy paste above dir or type path for new LOCAL data dir [do not forget / at the end]: ");
				LOCAL_DATA_DIR = scanner.nextLine();

				// adjust full paths
				LOCAL_HIGGS = LOCAL_DATA_DIR + HIGGS;
				LOCAL_SUSY = LOCAL_DATA_DIR + SUSY;
				LOCAL_CODRNA = LOCAL_DATA_DIR + CODRNA;
				LOCAL_POKER = LOCAL_DATA_DIR + POKER;
				LOCAL_SEA_50 = LOCAL_DATA_DIR + SEA_50;

				System.out
						.println("\noutput .txt files will be created by driver node "
								+ "using java FileWriter.");
				System.out.println("\nDefault output dir: " + OUTPUT_DIR);
				System.out
						.print("\nCopy paste above dir or type path for new output dir [do not forget / at the end]: ");
				OUTPUT_DIR = scanner.nextLine();

				System.out.println("\nDefault MASTER: " + MASTER);
				System.out
						.print("\nCopy paste above master or type new url:port combination for master: ");
				MASTER = scanner.nextLine();

				System.out
						.println("\nThere is a line in a code >> System.setProperty(\"hadoop.home.dir\", \"C:\\winutils\\\"); <<");
				System.out
						.print("Do you want to discard that line [(yes) (no)]: ");
				String choice = scanner.nextLine();

				if (choice.equalsIgnoreCase("no")) 
				{
					System.out.println("\nDefault HADOOP_HOME_DIR: "
							+ HADOOP_HOME_DIR);
					System.out
							.print("\nCopy paste above dir or type new hadoop home dir path: ");
					HADOOP_HOME_DIR = scanner.nextLine();
					discard = false;
				} // if
				else if (choice.equalsIgnoreCase("yes")) 
				{
					discard = true;
				} // else if
				else
				{
					System.out.println("Enter yes or no !!!");
					System.exit(0);
				} // else
				
			} // try
			catch (Exception ex) 
			{
				ex.printStackTrace(System.err);
			} // catch

		} // if not a local mode
		
		//close scanner
		scanner.close();
		
		//========================= RUN THE EXPERIMENTS =========================
		
		//FOR POKER
		//Logistic regression SGD
		ParallelRadonMachine.run(MASTER, HDFS_POKER, LOCAL_POKER,
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_POKER, LOCAL_POKER, 
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_POKER, LOCAL_POKER, 
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		//Logistic regression LBFGS
		ParallelRadonMachine.run(MASTER, HDFS_POKER, LOCAL_POKER,
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_POKER, LOCAL_POKER, 
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_POKER, LOCAL_POKER, 
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		//FOR SEA_50
		//Logistic regression SGD
		ParallelRadonMachine.run(MASTER, HDFS_SEA_50, LOCAL_SEA_50,
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SEA_50, LOCAL_SEA_50, 
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SEA_50, LOCAL_SEA_50, 
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		//Logistic regression LBFGS
		ParallelRadonMachine.run(MASTER, HDFS_SEA_50, LOCAL_SEA_50,
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SEA_50, LOCAL_SEA_50, 
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SEA_50, LOCAL_SEA_50, 
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		
		//FOR CODRNA
		//Logistic regression SGD
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA,
					OUTPUT_DIR, 1, 
					BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
					discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA, 
					OUTPUT_DIR, 2, 
					BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
					discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA, 
					OUTPUT_DIR, 3, 
					BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
					discard, HADOOP_HOME_DIR, isLocalMode);
		
		//Logistic regression LBFGS
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA,
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA, 
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA, 
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		
		//SVM
		/*ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA, 
				OUTPUT_DIR, 1 
				BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA,
				OUTPUT_DIR, 2, 
				BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_CODRNA, LOCAL_CODRNA,
				OUTPUT_DIR, 3, 
				BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		*/
		
		
		//FOR SUSY 
		//Logistic regression SGD
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY, 
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY, 
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY,  
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		//Logistic regression LBFGS
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY, 
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY, 
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY,  
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		
		//SVM
		/*ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY,
				OUTPUT_DIR, 1, 
				BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY,
				OUTPUT_DIR, 2, 
				BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_SUSY, LOCAL_SUSY, 
				OUTPUT_DIR, 3, 
				BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);*/
		
		
		//HIGGS can run on cluster mode
		if(!isLocalMode)
		{
		//FOR HIGSS
		
		// Logistic regression SGD
		ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS, 
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS,
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS,
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_SGD,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		// Logistic regression LBFGS
		ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS, 
				OUTPUT_DIR, 1, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS,
				OUTPUT_DIR, 2, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS,
				OUTPUT_DIR, 3, 
				BaseLearner.LOGISTIC, SparkLearner.LOGISTIC_REGRESSION_WITH_LBFGS,
				discard, HADOOP_HOME_DIR, isLocalMode);
		
		
				// SVM
				/*ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS,
						OUTPUT_DIR, 1, 
						BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
						discard, HADOOP_HOME_DIR, isLocalMode);
				ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS,
						OUTPUT_DIR, 2, 
						BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
						discard, HADOOP_HOME_DIR, isLocalMode);
				ParallelRadonMachine.run(MASTER, HDFS_HIGGS, LOCAL_HIGGS,
						OUTPUT_DIR, 3, 
						BaseLearner.SMO, SparkLearner.SVM_WITH_SGD,
						discard, HADOOP_HOME_DIR, isLocalMode);*/
		} // if
		
	} // main

} // class Launcher
