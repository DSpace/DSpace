package cz.cuni.mff.ufal.lindat.utilities.interfaces;

/**
 * Interface tracing the functionalities supplied for the underlying
 * architecture.
 * 
 * @author Karel Vandas
 * 
 */
public interface IFunctionalities extends IShibbolethAuthentication, ILicenses, java.io.Closeable {

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

	/**
	 * Function gets the error to be viewed if something goes wrong.
	 * 
	 * @param message
	 */
	String getErrorMessage();
	
	void openSession();

	void closeSession();
	
	/**
	 * Allows IDE to track unclosed resources, implement as this.closeSession();
	 */
	public void close();
}
