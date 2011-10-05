package org.datadryad.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class SubmissionRuntimeException extends RuntimeException {

	private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionRuntimeException.class);
	
	public SubmissionRuntimeException(Throwable aThrowable) {
		this(aThrowable.getMessage());
		initCause(aThrowable);
		
		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(aThrowable.getMessage(), aThrowable);
		}
	}
	
	public SubmissionRuntimeException(String aMessage) {
		super(aMessage);
		
		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(aMessage, this);
		}
	}
}
