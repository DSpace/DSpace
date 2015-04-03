/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.dspace.core.ConfigurationManager;

import cz.cuni.mff.ufal.dspace.AbstractPIDService.HANDLE_FIELDS;

public class PIDService {

    public static final String SERVICE_TYPE_EPIC = "epic";

    public static final String SERVICE_TYPE_EPIC2 = "epic2";

	private static AbstractPIDService pidService = null;
	
	private PIDService() {
		
	}
	
	private static void initialize() throws Exception {
		if(pidService==null) {
			String serviceType = getServiceType();
			String pidServiceClass = "cz.cuni.mff.ufal.dspace.PIDServiceEPICv1";
			if(serviceType.equals(PIDService.SERVICE_TYPE_EPIC)) {
				pidServiceClass = "cz.cuni.mff.ufal.dspace.PIDServiceEPICv1";
			} else if(serviceType.equals(PIDService.SERVICE_TYPE_EPIC2)) {
				pidServiceClass = "cz.cuni.mff.ufal.dspace.PIDServiceEPICv2";
			}
			else {
			    throw new IllegalArgumentException("Illegal pid.service type");
			}
			try {
				pidService = (AbstractPIDService)Class.forName(pidServiceClass).newInstance();
			} catch (Exception e) {
				throw new Exception(e);
			}
		}
	}

	public static String getServiceType() {
	    return ConfigurationManager.getProperty("lr", "lr.pid.service.type");
	}

	public static String resolvePID(String PID) throws Exception {
		initialize();
		return pidService.resolvePID(PID);
	}

	public static String modifyPID(String PID, String URL) throws Exception {		
		initialize();
		Map<String, String> handleFields = new HashMap<String, String>();
		handleFields.put(HANDLE_FIELDS.URL.toString(), URL);		
		return pidService.modifyPID(PID, handleFields);
	}

	public static String createPID(String URL, String prefix) throws Exception {
		initialize();
		Map<String, String> handleFields = new HashMap<String, String>();
		handleFields.put(HANDLE_FIELDS.URL.toString(), URL);				
		return pidService.createPID(handleFields, prefix);
	}

    public static String createCustomPID(String URL, String prefix, String suffix) throws Exception {
        initialize();
        Map<String, String> handleFields = new HashMap<String, String>();
        handleFields.put(HANDLE_FIELDS.URL.toString(), URL);
        return pidService.createCustomPID(handleFields, prefix, suffix);
    }

	public static String findHandle(String URL, String prefix) throws Exception {
		initialize();
		Map<String, String> handleFields = new HashMap<String, String>();
		handleFields.put(HANDLE_FIELDS.URL.toString(), URL);				
		return pidService.findHandle(handleFields, prefix);
	}
	
	public static boolean supportsCustomPIDs() throws Exception {
        initialize();
        return pidService.supportsCustomPIDs();
    }

	public static String who_am_i(String encoding) throws Exception {
		initialize();
		return pidService.whoAmI(encoding);
	}
	
	public static String test_pid(String PID) throws Exception {
		who_am_i(null);
		// 1. search for pid
		// 2. modify it
		resolvePID(PID);
		Random randomGenerator = new Random();
	    int randomInt = randomGenerator.nextInt(10000);
		String url = String.format("http://only.testing.mff.cuni.cz/%d", randomInt);
		modifyPID(PID, url);
		String resolved = resolvePID(PID);
		if ( resolved.equals(url) ) {
			return "testing succesful";
		}else {
			return "testing seemed ok but resolving did not return the expected result";
			
		}
	}
	
    public static void main(String args[]) throws Exception {
        PIDService.findHandle("www.google.com", "11372");
    }

}
