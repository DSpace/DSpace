package cz.cuni.mff.ufal.lindat.utilities.units;

import java.io.FileReader;
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
	public final static String testProperties = "testProperties";
	public static final String loggingProperties = "utilities.logging";
	public static final String logFile = "utilities.status";

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
	 * @param path
	 *            is where the property file is stored.
	 */
  public static void init() {
    init(null);
  }
  
  public static void init(String dspace_cfg_path) {
		if (initialized)
			return;
		try { 			
          URL url = null;
          if ( null == dspace_cfg_path ) {
        	url = Variables.class.getClassLoader().getResource("modules/lr.cfg");
        	if(url == null){
        		url = Variables.class.getClassLoader().getResource("config/modules/lr.cfg");
        	}
          }else {
            url = new URL(dspace_cfg_path);
          }
          // last nasty try
          if ( url == null ) {
        	  log.error("Failed to find lr.cfg. The class loader search is from " + Variables.class.getClassLoader().getResource("./"));
        	  System.err.println("Failed to find lr.cfg. The class loader search is from " + Variables.class.getClassLoader().getResource("./"));
              url = Variables.class.getClassLoader().getResource(Variables.class.getName().replace('.', '/') + ".class");
              url = new URL(  new URL(url.getPath().split("utilities-")[0]),
                              "../../../../config/modules/lr.cfg");
          }

			properties.load(new FileReader(url.getPath()));
			databaseURL = get("lr.utilities.db.url");
			databaseUser = get("lr.utilities.db.username");
			databasePassword = get("lr.utilities.db.password");
			initialized = true;
		} catch(Exception e) {
			log.error(e);
		}
	}

	/**
	 * Function sets actual error message.
	 * 
	 * @param object
	 */
	public static void setErrorMessage(String message) {
		_errorMessage = message;
		if(message!=null && !message.equals(""))
			log.log(Level.ERROR,"Message '" + message + "' has been set up!");
		if (null != message) {
			_errorMessage += " For more information please contact our Help Desk.";
		}
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



