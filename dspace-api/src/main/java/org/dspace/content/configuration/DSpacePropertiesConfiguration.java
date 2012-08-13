/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.configuration;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Class to read and write configuration files.
 * 
 * @author DSpace @ Lyncode
 */
public class DSpacePropertiesConfiguration {
	private PropertiesConfiguration write;
	private PropertiesConfiguration read;
	
	public DSpacePropertiesConfiguration(File file) throws ConfigurationException {
		write = new PropertiesConfiguration(file);
		write.setAutoSave(true);
		read = new PropertiesConfiguration(file);
		read.setAutoSave(false);
	}
	public DSpacePropertiesConfiguration(String fileName) throws ConfigurationException {
		write = new PropertiesConfiguration(fileName);
		write.setAutoSave(true);
		read = new PropertiesConfiguration(fileName);
		read.setAutoSave(false);
	}
	public DSpacePropertiesConfiguration(URL url) throws ConfigurationException {
		write = new PropertiesConfiguration(url);
		write.setAutoSave(true);
		read = new PropertiesConfiguration(url);
		read.setAutoSave(false);
	}
	
	public void save() throws ConfigurationException {
		write.save();
	}
	public void addProperty(String key, Object value) {
		write.addProperty(key, value);
		read.addProperty(key, value);
	}
	public void setProperty(String key, Object value) {
		write.setProperty(key, value);
		read.setProperty(key, value);
	}
	
	public Properties getProperties () {
		Properties p = new Properties();
		Iterator<String> it = write.getKeys();
		while (it.hasNext()) {
			String k = it.next();
			p.put(k, write.getString(k));
		}
		return p;
	}

	public String getString (String key) {
		return read.getString(key);
	}
	public String getString (String key, String defaultValue) {
		return read.getString(key, defaultValue);
	}
	public List<String> getList (String key) {
		return Arrays.asList(read.getStringArray(key));
	}
	
	public boolean containsKey (String key) {
		return read.containsKey(key);
	}
	
	public Iterator<String> getKeys () {
		return write.getKeys();
	}
	
	public void load (File f) throws ConfigurationException {
		read.load(f);
	}
}
