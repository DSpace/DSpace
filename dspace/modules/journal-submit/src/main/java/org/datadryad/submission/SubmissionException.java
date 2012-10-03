package org.datadryad.submission;

import java.text.MessageFormat;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin S. Clarke <ksclarke@gmail.com>
 */
@SuppressWarnings("serial")
public class SubmissionException extends ServletException {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SubmissionException.class);

	/**
	 * @param aMessage
	 */
	public SubmissionException(String aMessage) {
		super(aMessage);

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(aMessage);
		}
	}

	public SubmissionException(String aTemplate, String aDetailMsg) {
		super(MessageFormat.format(aTemplate, aDetailMsg));

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(MessageFormat.format(aTemplate, aDetailMsg));
		}
	}
	
	public SubmissionException(String aTemplate, Object[] aDetailArray) {
		super(MessageFormat.format(aTemplate, aDetailArray));

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(MessageFormat.format(aTemplate, aDetailArray));
		}
	}

	public SubmissionException(String aTemplate, String aDetailMsg,
			Throwable aThrowable) {
		super(MessageFormat.format(aTemplate, aDetailMsg), aThrowable);

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(MessageFormat.format(aTemplate, aDetailMsg),
					aThrowable);
		}
	}

	public SubmissionException(String aTemplate, Object[] aDetailArray,
			Throwable aThrowable) {
		super(MessageFormat.format(aTemplate, aDetailArray), aThrowable);

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(MessageFormat.format(aTemplate, aDetailArray),
					aThrowable);
		}
	}
	
	/**
	 * @param aRootCause
	 */
	public SubmissionException(Throwable aRootCause) {
		super(aRootCause);

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(aRootCause.getMessage(), aRootCause);
		}
	}

	/**
	 * @param aMessage An exception message
	 * @param aRootCause The underlying cause of the exception
	 */
	public SubmissionException(String aMessage, Throwable aRootCause) {
		super(aMessage, aRootCause);

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(aMessage, aRootCause);
		}
	}

}
