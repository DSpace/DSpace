/**
 * 
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;

import gr.ekt.bte.core.AbstractModifier;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

/**
 *
 *
 */
public class MapConverterModifier extends AbstractModifier {

	String filename; //The properties filename

	List<String> fieldKeys;

	Properties mapConfig = null;
	private Map<String, String> regexConfig = new HashMap<String, String>();

	public final String REGEX_PREFIX = "regex.";

	/**
	 * @param name
	 */
	public MapConverterModifier(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see gr.ekt.bte.core.AbstractModifier#modify(gr.ekt.bte.core.MutableRecord)
	 */
	@Override
	public Record modify(MutableRecord record) {
		if (mapConfig != null && fieldKeys != null) {
			for (String key : fieldKeys){
				List<Value> values = record.getValues(key);

				List<Value> newValues = new ArrayList<Value>();

				for (Value value : values){
					String stringValue = value.getAsString();

					String tmp = "";
					if (mapConfig.containsKey(stringValue))
					{
						tmp = mapConfig.getProperty(stringValue, mapConfig
								.getProperty("mapConverter.default"));    
					}
					else
					{
						tmp = mapConfig.getProperty("mapConverter.default");
						for (String regex : regexConfig.keySet())
						{
							if (stringValue != null && stringValue.matches(regex))
							{
								tmp = stringValue.replaceAll(regex, regexConfig.get(regex));
							}
						}
					}

					if ("@@ident@@".equals(tmp))
					{
						newValues.add(new StringValue(stringValue));
					}
					else if (StringUtils.isNotBlank(tmp))
					{
						newValues.add(new StringValue(tmp));
					}
					else
						newValues.add(new StringValue(stringValue));
				}

				record.updateField(key, newValues);
			}
		}

		return record;
	}

	//Init method, must be declared in the Spring XML configuration file
	//Initializes the config map
	public void init(){
		if (filename != null) {
			String configFilePath = ConfigurationManager
					.getProperty("dspace.dir")
					+ File.separator
					+ "config"
					+ File.separator
					+ "crosswalks"
					+ File.separator
					+ filename;

			FileInputStream fis = null;
			try
			{
				fis = new FileInputStream(configFilePath);
				mapConfig = new Properties();
				mapConfig.load(fis);
				fis.close();
				for (Object key : mapConfig.keySet())
				{
					String keyS = (String)key;
					if (keyS.startsWith(REGEX_PREFIX))
					{
						String regex = keyS.substring(REGEX_PREFIX.length());
						String regReplace = mapConfig.getProperty(keyS);
						if (regReplace == null)
						{
							regReplace = "";
						}
						else if (regReplace.equalsIgnoreCase("@ident@"))
						{
							regReplace = "$0";
						}
						regexConfig.put(regex,regReplace);
					}
				}
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException(
						"Impossibile leggere la configurazione per il converter "
								+ getName(), e);
			}
			finally
			{
				if (fis != null)
				{
					try
					{
						fis.close();
					}
					catch (IOException ioe)
					{
						// ...
					}
				}
			}
		}
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setFieldKeys(List<String> fieldKeys) {
		this.fieldKeys = fieldKeys;
	}
}
