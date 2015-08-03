package cz.cuni.mff.ufal.lindat.utilities.units;

import java.io.*;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Variables {

	private static final Logger log = Logger.getLogger(Variables.class);
	private static boolean initialized = false;

	public static String emptyName = "";

	public static String databaseURL;
	public static String databaseUser;
	public static String databasePassword;

	/**
	 * Final values not to be changed.
	 */
	public final static String configurationEnabled = "true";
	public final static int invalidIntValue = -1;
	public static final String loggingProperties = "utilities.logging";
	public static final String logFile = "utilities.status";

	public final static String default_config_file = "modules/lr.cfg";

	/**
	 * Loaded properties from the property file.
	 */
	private static Properties properties = new Properties();
	private static String _errorMessage = "";


	/**
	 * Function returns the information whether the used functionality is
	 * enabled
	 * 
	 * @param functionalityName
	 *            is the functionality name
	 * @return true if the configuration is enabled
	 */
	public static boolean isConfigurationTrue(String functionalityName) {
		if (properties.containsKey(functionalityName))
			return properties.getProperty(functionalityName).equals(
					configurationEnabled);
		return false;
	}

	/**
	 * Function returngs the property value for the key.
	 * 
	 * @param key
	 *            is the key in properties
	 * @return the property value as String
	 */
	public static String get(String key) {
		return properties.getProperty(key);
	}

	/**
	 * Function initializes the Variables class.
	 * 
	 * @param configuration_file
	 *            the property file path.
	 */
  public static void init(String configuration_file) throws IOException {
		if (initialized) {
            return;
        }

        Reader reader = null;
        try {
            if (null == configuration_file) {
                configuration_file = Variables.class.getClassLoader().getResource("./") + File.separator + default_config_file;
                InputStream is = Variables.class.getClassLoader().getResourceAsStream(default_config_file);
                reader = new BufferedReader(new InputStreamReader(is));
            } else {
                reader = new FileReader(configuration_file);
            }

            properties.load(reader);
            databaseURL = get("lr.utilities.db.url");
            databaseUser = get("lr.utilities.db.username");
            databasePassword = get("lr.utilities.db.password");
            initialized = true;

        } catch (IOException e) {
            log.error(e);
            String err_msg = String.format("Failed to find and load lr.cfg from either [%s] or [%s%s] because of [%s]",
                configuration_file,
                Variables.class.getClassLoader().getResource("./"), default_config_file,
                e.toString()
            );
            System.err.println(err_msg);
            throw e;
        }
	}

	/**
	 * Function sets actual error message.
	 * 
	 * @param object
	 */
	public static void setErrorMessage(String message, boolean append_default_msg) {
		_errorMessage = message;
		if(message!=null && !message.equals("")) {
			log.log(Level.ERROR, "Message '" + message + "' has been set up!");
		}
		if (null != message && append_default_msg) {
			_errorMessage += " For more information please contact our Help Desk.";
		}
	}

	public static void setErrorMessage(String message) {
		setErrorMessage( message, true );
	}
	/**
	 * Function gets actual error message.
	 * 
	 * @return the string representation of the error
	 */
	public static String getErrorMessage() {
		if (_errorMessage == null)
			return "";
		return _errorMessage;
	}

}



