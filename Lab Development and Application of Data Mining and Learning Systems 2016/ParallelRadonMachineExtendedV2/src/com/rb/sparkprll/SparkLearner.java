package com.rb.sparkprll;

//enum describing Spark learners
public enum SparkLearner
{
	//enum values
	LOGISTIC_REGRESSION_WITH_SGD, SVM_WITH_SGD;
	
	// toString method for enum description
	@Override
	public String toString() {
		switch (this) {
		case LOGISTIC_REGRESSION_WITH_SGD:
			return "LogisticRegressionWithSGD";
		case SVM_WITH_SGD:
			return "SVMWithSGD";
		default:
			throw new IllegalArgumentException();
		} // switch
	} // toString
	
} // enum SparkLearner
