package org.datadryad.dspace.statistics;

@SuppressWarnings("serial")
public class StatisticsException extends Exception {
	
	public StatisticsException(String aMessage) {
		super(aMessage);
	}

	public StatisticsException(String aMessage, Exception aCause) {
		super(aMessage, aCause);
	}
	
	public StatisticsException(Exception aCause) {
		super(aCause.getMessage(), aCause);
	}
}
