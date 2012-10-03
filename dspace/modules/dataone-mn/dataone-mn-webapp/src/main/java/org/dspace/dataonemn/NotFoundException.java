package org.dspace.dataonemn;

public class NotFoundException extends Exception {

	private static final long serialVersionUID = -9179491259428621939L;

	public NotFoundException() {
		super();
	}

	public NotFoundException(String aMessage) {
		super(aMessage);
	}

	public NotFoundException(Throwable aCause) {
		super(aCause);
	}

	public NotFoundException(String aMessage, Throwable aCause) {
		super(aMessage, aCause);
	}

}
