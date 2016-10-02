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
	      case LOGISTIC: return "WEKA [Logistic regression with quasi-Newton method]";
	      case SMO: return "WEKA [John Platt's sequential minimal optimization algorithm"
						+ " for training a support vector classifier]";
	      default: throw new IllegalArgumentException();
	    } // switch
	  } // toString
	
	//to convert to simple string
	public String toSimpleStirng()
	{
		switch(this) {
	      case LOGISTIC: return "Logistic";
	      case SMO: return "SMO";
	      default: throw new IllegalArgumentException();
	    } // switch
	} // toSimpleStirng
	
} // enum BaseLearner
