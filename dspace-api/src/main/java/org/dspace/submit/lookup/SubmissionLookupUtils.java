/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.crosswalk.IConverter;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.submit.util.SubmissionLookupPublication;

public class SubmissionLookupUtils {
	private static Logger log = Logger.getLogger(SubmissionLookupUtils.class);

	/** Location of config file */
	private static final String configFilePath = ConfigurationManager
			.getProperty("dspace.dir")
			+ File.separator
			+ "config"
			+ File.separator + "crosswalks" + File.separator;

	private static final Map<String, Properties> importerProps = new HashMap<String, Properties>();

	// Patter to extract the converter name if any
	private static final Pattern converterPattern = Pattern
			.compile(".*\\((.*)\\)");

	public static LookupProvidersCheck getProvidersCheck(Context context,
			Item item, String dcSchema, String dcElement, String dcQualifier) {
		try {
			LookupProvidersCheck check = new LookupProvidersCheck();
			MetadataSchema[] schemas = MetadataSchema.findAll(context);
			DCValue[] values = item.getMetadata(dcSchema, dcElement, dcQualifier, Item.ANY);
			
			for (MetadataSchema schema : schemas)
			{
				boolean error = false;
				if (schema.getNamespace().startsWith(SubmissionLookupService.SL_NAMESPACE_PREFIX))
				{
					DCValue[] slCache = item.getMetadata(schema.getName(), dcElement, dcQualifier, Item.ANY);
					if (slCache.length == 0)
						continue;
					
					if (slCache.length != values.length)
					{
						error = true;
					}
					else
					{
						for (int idx = 0; idx < values.length; idx++)
						{
							DCValue v = values[idx];
							DCValue sl = slCache[idx];
							// FIXME gestire authority e possibilita' multiple:
							// match non sicuri, affiliation, etc.
							if (!v.value.equals(sl.value))
							{
								error = true;
								break;
							}
						}
					}
					if (error)
					{
						check.getProvidersErr().add(schema.getName());
					}
					else
					{
						check.getProvidersOk().add(schema.getName());
					}
				}
			}
			return check;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	public static SubmissionLookupPublication convert(String shortName,
			Object bean) throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		SubmissionLookupPublication publication = new SubmissionLookupPublication(
				shortName);
		Field[] fields = bean.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.getType() == String.class) {
				Method getter = bean.getClass().getMethod(
						"get" + field.getName().substring(0, 1).toUpperCase()
								+ field.getName().substring(1));

				String value = (String) getter.invoke(bean);

				addMetadata(shortName, publication, field.getName(), value);

			} else if (field.getType() == List.class) {
				ParameterizedType pt = (ParameterizedType) field
						.getGenericType();

				Method getter = bean.getClass().getMethod(
						"get" + field.getName().substring(0, 1).toUpperCase()
								+ field.getName().substring(1));

				if (pt.getActualTypeArguments()[0] instanceof GenericArrayType) { // nomi
																					// di
																					// persone
					List<String[]> values = (List<String[]>) getter
							.invoke(bean);
					if (values != null) {
						for (String[] nvalue : values) {
							String value = nvalue[1] + ", " + nvalue[0];
							addMetadata(shortName, publication,
									field.getName(), value);
						}
					}
				} else { // metadati ripetibili
					List<String> values = (List<String>) getter.invoke(bean);
					if (values != null) {
						for (String value : values) {
							addMetadata(shortName, publication,
									field.getName(), value);
						}
					}
				}
			}
		}
		return publication;
	}

	private static void addMetadata(String config,
			SubmissionLookupPublication publication, String name, String value) {
		String md = getSubmissionLoookupConfiguration(config).getProperty(name);

		if (StringUtils.isBlank(md)) {
			return;
		} else {
			md = md.trim();
		}

		Matcher converterMatcher = converterPattern.matcher(md);
		String converterName = null;
		if (converterMatcher.matches()) {
			converterName = converterMatcher.group(1);
			md = md.replaceAll("\\(" + converterName + "\\)", "");
		}
		IConverter converter = (IConverter) PluginManager.getNamedPlugin(
				IConverter.class, converterName);

		String nValue;
		if (converter != null) {
			nValue = converter.makeConversion(value);
		} else {
			nValue = value;
		}
		publication.add(md, nValue);
	}

	private static synchronized Properties getSubmissionLoookupConfiguration(
			String config) {
		Properties properties = importerProps.get(config);
		if (properties != null) {
			return properties;
		}
		// Read in configuration
		properties = new Properties();
		importerProps.put(config, properties);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(configFilePath + config
					+ "-submission.properties");
			properties.load(fis);
		} catch (Exception notfound) {
			throw new IllegalArgumentException(
					"Impossibile leggere la configurazione per il SubmissionLookupProvider "
							+ config, notfound);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ioe) {
					log.error(ioe.getMessage(), ioe);
				}
			}
		}

		return properties;
	}

	public static String normalizeDOI(String doi) {
		if (doi != null)
		{
		    return doi.trim().replaceAll("^http://dx.doi.org/", "")
	                .replaceAll("^doi:", "");	
		}
		return null;
		
	}
}
