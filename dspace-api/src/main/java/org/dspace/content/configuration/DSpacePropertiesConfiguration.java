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
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

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
		FileChangedReloadingStrategy stretagy = new FileChangedReloadingStrategy();
		read.setReloadingStrategy(stretagy);
		write.setReloadingStrategy(stretagy);
	}
	public DSpacePropertiesConfiguration(String fileName) throws ConfigurationException {
		write = new PropertiesConfiguration(fileName);
		write.setAutoSave(true);
		read = new PropertiesConfiguration(fileName);
		read.setAutoSave(false);
		FileChangedReloadingStrategy stretagy = new FileChangedReloadingStrategy();
		read.setReloadingStrategy(stretagy);
		write.setReloadingStrategy(stretagy);
	}
	public DSpacePropertiesConfiguration(URL url) throws ConfigurationException {
		write = new PropertiesConfiguration(url);
		write.setAutoSave(true);
		read = new PropertiesConfiguration(url);
		read.setAutoSave(false);
		FileChangedReloadingStrategy stretagy = new FileChangedReloadingStrategy();
		read.setReloadingStrategy(stretagy);
		write.setReloadingStrategy(stretagy);
	}
	
	public void save() throws ConfigurationException {
		write.save();
	}
	public void addProperty(String key, Object value) {
		write.addProperty(key, value);
		read.addProperty(key, value);
	}
	
	public void setPropertyDescription (String key, String description) {
		write.getLayout().setComment(key, description);
		read.getLayout().setComment(key, description);
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
	public Object getProperty (String key) {
		return read.getProperty(key);
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
	
	public void addConfigurationEventListener (ConfigurationListener event) {
		read.addConfigurationListener(event);
	}
	public int getInt(String key, int defaultValue) {
		return read.getInt(key, defaultValue);
	}
	public long getLong(String key, long defaultValue) {
		return read.getLong(key, defaultValue);
	}
	public boolean getBoolean(String key, boolean defaultValue) {
		return read.getBoolean(key, defaultValue);
	}
	public String getDescription(String key) {
		return read.getLayout().getComment(key);
	}
}
