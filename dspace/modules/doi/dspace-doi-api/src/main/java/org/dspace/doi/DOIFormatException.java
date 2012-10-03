package org.dspace.doi;

public class DOIFormatException extends RuntimeException {

	/**
	 * A generated <code>serialVersionUID</code>.
	 */
	private static final long serialVersionUID = 741154263322560861L;

	public DOIFormatException() {
		super();
	}

	public DOIFormatException(String aMessage) {
		super(aMessage);
	}

	public DOIFormatException(Throwable aCause) {
		super(aCause);
	}

	public DOIFormatException(String aMessage, Throwable aCause) {
		super(aMessage, aCause);
	}

}
