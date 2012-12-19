package ar.edu.unlp.sedici.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.dspace.core.ConfigurationManager;

public class ConfigurationUtil {

	
	/**
	 * Reads properties beginning with the given prefix, followed by some key text:
	 * {prefix}.{key1}=.....
	 * {prefix}.{key2}=.....
	 * Generates a Map with the property's keys as map's keys
	 * 
	 * @param propertyPrefix
	 * @return
	 */
	public static Map<String, String> readMultipleProperties(String module, String propertyPrefix) {
		Properties props = ConfigurationManager.getProperties(module);
		return readMultipleProperties(props, propertyPrefix);
	}

	/**
	 * Reads properties beginning with the given prefix, followed by some key text:
	 * {prefix}.{key1}=.....
	 * {prefix}.{key2}=.....
	 * Generates a Map with the property's keys as map's keys
	 * 
	 * @param propertyPrefix
	 * @return
	 */
	public static Map<String, String> readMultipleProperties(String propertyPrefix) {
		Properties props = ConfigurationManager.getProperties();
		return readMultipleProperties(props, propertyPrefix);
	}
	
	/**
	 * Reads properties beginning with the given prefix, followed by some key text:
	 * {prefix}.{key1}=.....
	 * {prefix}.{key2}=.....
	 * Generates a Map with the property's keys as map's keys
	 * 
	 * @param propertyPrefix
	 * @return
	 */
	public static Map<String, String> readMultipleProperties(Properties properties, String propertyPrefix) {
		Map<String,String> resultMap = new HashMap<String, String>();
		Iterator propertyIterator = properties.keySet().iterator();
		while(propertyIterator.hasNext()) {
			String currentKey = (String) propertyIterator.next();
			if(currentKey.startsWith(propertyPrefix)) {
				System.out.println("KEY: "+currentKey+" - EXTRACT: "+currentKey.substring(propertyPrefix.length())+" - VALUE: "+properties.getProperty(currentKey));
				resultMap.put(currentKey.substring(propertyPrefix.length()+1), properties.getProperty(currentKey));
			}
		}
		return resultMap;
	}

	/**
	 * Reads properties beginning with the given prefix, followed by a ordering number
	 * {prefix}.1=.....
	 * {prefix}.2=.....
	 * Generates a Map with the number as map's keys
	 * 
	 * @param propertyPrefix
	 * @return
	 */
	public static Map<String, String> readOrderedProperties(Properties properties, String propertyPrefix) {
		Map<String,String> resultMap = new HashMap<String, String>();
		int currentProp = 1;
		String propertyValue = null;
		while((propertyValue = properties.getProperty(propertyPrefix+"."+currentProp)) != null) {
			resultMap.put(propertyPrefix+"."+currentProp, propertyValue);
		}
		return resultMap;
	}

}
