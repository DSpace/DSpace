package cz.cuni.mff.ufal.lindat.utilities.functionalities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import cz.cuni.mff.ufal.lindat.utilities.units.Variables;

public class ShibbolethMailAcceptation {

	/**
	 * The name of the setting for the module a property file.
	 */
	private static final String allowedEmailTemplateField = "lr.shibboleth.name.acceptation.allowedEmailTemplate";
	/**
	 * The name of the setting for the module a property file. If there is more
	 * than one email, it is splitted up by this separator.
	 */
	private static final String allowedEmailTemplateSeparatorField = "lr.shibboleth.name.acceptation.allowedEmailTemplateSeparator";

	/**
	 * Logger.
	 */
	private static Logger log = Logger
			.getLogger(ShibbolethMailAcceptation.class);

	/**
	 * Function returns whether the authentication attempt is accepted or not.
	 * 
	 * @param email
	 *            is email passed to authentication
	 * @return email itself if authentication is correct, null otherwise
	 */
	static public String getEmailAcceptedOrNull(String email) {

		System.out.println("CHECKING THE EMAIL");
		log.log(Level.INFO, "Starting the check of the email!");

		// If the plugin is not enabled, we should not do anything
		//if (!FunctionalityManager.manager
		//		.isFunctionalityEnabled("lr.mail.acceptation")) {
		//	return email;
		//}

		// In case of any problems, we set the error message in forward.
		if (email != null)
			Variables
					.setErrorMessage("Email supplied by your organization '"
							+ email
							+ "' is in incorrect form! The problem is at your Identity provider (Central authentification service of your institution). " +
							"It should provide email at urn:oid:0.9.2342.19200300.100.1.3 attribute via Shibboleth. You should change your email at your " +
							"Central authentication service (usually users are allowed to do it by themselves) and try access once more. If this does not work, " +
							"please provide us contact to your IdP and we solve it for you.");

		// If email is null, return just null
		if (email == null || email.equals("")) {
			// TODO: For testing purposes we set fake email! This has to be
			// removed!
			// email = "charlie.vandas@ufal.mff.cuni.cz";
			return null;
		}

		String allowedEmailTemplate = Variables.get(allowedEmailTemplateField);
		String allowedEmailTemplateSeparator = Variables
				.get(allowedEmailTemplateSeparatorField);

		log.log(Level.INFO, "Using template '" + allowedEmailTemplate
				+ "' and separator of shibboleth attributes '"
				+ allowedEmailTemplateSeparator + "'.");
		Pattern pattern = Pattern.compile(allowedEmailTemplate,
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(email);
		log.log(Level.INFO, "Matching " + email + " against '"
				+ allowedEmailTemplate + "'");
		log.log(Level.DEBUG, "Separator '" + allowedEmailTemplateSeparator
				+ "' used.");

		if (matcher.find()) {
			Variables.setErrorMessage("Null object definition message!");
			String[] mails = email.split(allowedEmailTemplateSeparator);
			log.log(Level.INFO, "Email " + mails[0] + " returned from emails '"
					+ email + "'.");
			return mails[0];
		}
		return null;
	}
}

