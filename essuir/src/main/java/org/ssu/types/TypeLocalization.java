package org.ssu.types;

import org.apache.log4j.Logger;
import org.ssu.LocalizedInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TypeLocalization {
    private static Logger logger = Logger.getLogger(TypeLocalization.class);


//    public static Hashtable<String, Long> getTypesCount() {
//        Hashtable<String, Long> types = new Hashtable<String, Long>();
//
//        try {
//            Connection c = null;
//            try {
//                Class.forName(ConfigurationManager.getProperty("db.driver"));
//
//                c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
//                        ConfigurationManager.getProperty("db.username"),
//                        ConfigurationManager.getProperty("db.password"));
//
//                Statement s = c.createStatement();
//
//                ResultSet resSet = s.executeQuery(
//                        "SELECT text_value, COUNT(*) AS cnts FROM metadatavalue" +
//                                "	WHERE metadata_field_id = 66 " +
//                                "		AND resource_id IN (SELECT item_id FROM item WHERE in_archive) " +
//                                "	GROUP BY text_value; ");
//
//                while (resSet.next()) {
//                    types.put(resSet.getString("text_value"), resSet.getLong("cnts"));
//                }
//
//                s.close();
//            } finally {
//                if (c != null)
//                    c.close();
//            }
//        } catch (Exception e) {
//            logger.error(e);
//        }
//
//        return types;
//    }


    private Map<String, String> typesTable = new HashMap<>();

    private void updateTypeLocalizationTable(String locale) {
        typesTable.clear();
        List<String> typesList = null;
        try {
            typesList = new LocalizedInputsReader().getInputsReader(locale).getPairs("common_types");
        } catch (DCInputsReaderException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < typesList.size(); i += 2)
            typesTable.put(typesList.get(i + 1), typesList.get(i));
    }

    public String getTypeLocalized(String type, String locale) {
        updateTypeLocalizationTable(locale);
        return typesTable.getOrDefault(type, type);
    }
}
