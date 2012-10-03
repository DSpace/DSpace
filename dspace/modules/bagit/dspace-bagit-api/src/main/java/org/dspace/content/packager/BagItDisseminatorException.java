package org.dspace.content.packager;

import java.io.File;

@SuppressWarnings("serial")
public class BagItDisseminatorException extends Exception {

	private String myWorkplace;

	public BagItDisseminatorException(String aMessage) {
		super(aMessage);
	}

	public BagItDisseminatorException(String aMessage, Throwable aCause) {
		super(aMessage, aCause);
	}

	public BagItDisseminatorException(Throwable aCause) {
		super(aCause.getMessage(), aCause);
	}

	public String getWorkplace() {
		return myWorkplace == null ? "Not relevant to problem" : myWorkplace;
	}

	public BagItDisseminatorException setWorkplace(File aFile) {
		myWorkplace = aFile.getAbsolutePath();
		return this;
	}
}
