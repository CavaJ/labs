package com.rb.sparkprll;

//enum for base learners
public enum BaseLearner 
{
	//enum values
	LOGISTIC, SMO;
	
	//toString method for enum description
	@Override
	  public String toString() {
	    switch(this) {
	      case LOGISTIC: return "WEKA [Logistic regression with SGD]";
	      case SMO: return "WEKA [John Platt's sequential minimal optimization algorithm"
						+ " for training a support vector classifier]";
	      default: throw new IllegalArgumentException();
	    } // switch
	  } // toString
	
} // enum BaseLearner
