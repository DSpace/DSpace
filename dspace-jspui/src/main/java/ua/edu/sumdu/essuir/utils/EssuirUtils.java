package ua.edu.sumdu.essuir.utils;


import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.core.ConfigurationManager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Hashtable;


public class EssuirUtils {
	private static Logger logger = Logger.getLogger(EssuirUtils.class);
	
	
	public static Hashtable<String, Long> getTypesCount() {
		Hashtable<String, Long> types = new Hashtable<String, Long>();
		
		try {
	        Connection c = null;
	        try {
	            Class.forName(ConfigurationManager.getProperty("db.driver"));
        
        	    c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
                	                            ConfigurationManager.getProperty("db.username"),
                        	                    ConfigurationManager.getProperty("db.password"));

	            Statement s = c.createStatement();

        	    ResultSet resSet = s.executeQuery(
		        	    "SELECT text_value, COUNT(*) AS cnts FROM metadatavalue" +
			        	"	WHERE metadata_field_id = 66 " +
			        	"		AND resource_id IN (SELECT item_id FROM item WHERE in_archive) " +
			        	"	GROUP BY text_value; ");

	            while (resSet.next()) {
		            types.put(resSet.getString("text_value"), resSet.getLong("cnts"));
        	    }
	
        	    s.close();
	        } finally {
	            if (c != null) 
	                c.close();
	        }
		} catch (Exception e) {
			logger.error(e);
		}
		
		return types;
	}
	private static String prevSessionLocale = "";
	private static java.util.Hashtable<String, String> typesTable = new java.util.Hashtable<String, String>();

	public static String getTypeLocalized(String type, String locale) throws DCInputsReaderException {
		if (!locale.equals(prevSessionLocale)) {
			typesTable.clear();
			StringBuilder fileName = new StringBuilder(ConfigurationManager.getProperty("dspace.dir")
					+ File.separator + "config" + File.separator + DCInputsReader.getFormDefFile());

			if (!locale.equals("en"))
				fileName.insert(fileName.length() - 4, "_" + locale);
			DCInputsReader dci = new DCInputsReader(fileName.toString());

			java.util.List vList = dci.getPairs("common_types");

			for (int i = 0; i < vList.size(); i += 2)
				typesTable.put((String) vList.get(i + 1), (String) vList.get(i));

			prevSessionLocale = locale;
		}

		String result = typesTable.get(type);
		return result == null ? type : result;
	}
}
