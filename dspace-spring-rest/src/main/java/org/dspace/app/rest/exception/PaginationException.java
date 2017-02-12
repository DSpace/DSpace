package org.dspace.app.rest.exception;

public class PaginationException extends RuntimeException {
	long total;

	public PaginationException(long total) {
		this.total = total;
	}

	public long getTotal() {
		return total;
	}
}
