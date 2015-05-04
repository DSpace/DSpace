package cz.cuni.mff.ufal.lindat.utilities.interfaces;

/**
 * Interface tracing the functionalities supplied for the underlying
 * architecture.
 * 
 * @author Karel Vandas
 * 
 */
public interface IFunctionalities extends IShibbolethAuthentication, IDatabase, ILicenses, IPiwikReport {

	/**
	 * Function returns the information whether the functionality demanded by
	 * underlying architecture is enabled.
	 * 
	 * @param functionalityName
	 * @return the logical value true if the functionality has been set in the
	 *         property file to Variables.configurationEnabled
	 */
	public boolean isFunctionalityEnabled(String functionalityName);

	/**
	 * Function returns the property value for the key.
	 * 
	 * @param key
	 *            is the key in properties
	 * @return the property value as String
	 */
	public String get(String key);


	/**
	 * Function sets the error to be viewed if something goes wrong.
	 * 
	 * @param message
	 */
	void setErrorMessage(String message);
	void setErrorMessage(String message, boolean append_default_msg);

	/**
	 * Function gets the error to be viewed if something goes wrong.
	 * 
	 * @param message
	 */
	String getErrorMessage();

}
