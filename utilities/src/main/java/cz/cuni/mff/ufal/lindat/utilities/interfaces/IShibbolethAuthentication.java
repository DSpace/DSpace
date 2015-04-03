package cz.cuni.mff.ufal.lindat.utilities.interfaces;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.VerificationTokenEperson;

public interface IShibbolethAuthentication {

	/**
	 * Function register user into the system with userID, shibbolethID and
	 * organization.
	 * 
	 * @param userID
	 * @param email
	 * @param organization
	 * @param confirmation
	 * 			the value of this parameter is false if the new user is not yet active
	 * @return the registered user
	 */
	public UserRegistration registerUser(int userID, String email, String organization, boolean confirmation);

	/**
	 * Function verifies the user.
	 * 
	 * @param userID
	 *            is user id
	 * @return the information if the given user has been successfully verified
	 */
	boolean verifyUser(int userID);
	
	
	/**
	 * Function accepts the given email (checks if it is in the correct form).
	 * 
	 * @param email
	 *            is a string representing the email
	 * @return the email or null if the email is incorrect.
	 */
	public String getEmailAcceptedOrNull(String email);

	/**
	 * Function converts the string appropriately according to defined
	 * encoding.
	 * 
	 * @param toConvert
	 *            is the string to be converted
	 * @return converted string
	 */
	public String convert(String toConvert);

	/**
	 * Function returns the affiliation of the input.
	 * 
	 * @param affiliation
	 *            is the email of the person supplied
	 * @return the affiliation TODO: Maybe some database-cooperation might be
	 *         nice (stored affiliations not recognized from email).
	 */
	public String scopeRecognition(String affiliation);


	UserRegistration getRegisteredUser(int userID);


	UserRegistration updateRegisteredUser(int userID, String email,
			String organization, boolean confirmation);
	
	VerificationTokenEperson getVerificationToken(String token);

	VerificationTokenEperson getVerificationTokenByEmail(String email);
	
}

