package ua.edu.sumdu.essuir.utils;


import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ua.edu.sumdu.essuir.service.DatabaseService;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

@Component
public class EssuirUtils {
    private static final String MINIMAL_YEAR = "1964";


    private static DatabaseService databaseService;

    private static Logger logger = Logger.getLogger(EssuirUtils.class);

    @Autowired
    public void setDatabaseService(DatabaseService databaseService) {
        EssuirUtils.databaseService = databaseService;
    }

    public static Hashtable<String, Long> getTypesCount() {
        Hashtable<String, Long> types = new Hashtable<String, Long>();
        String query = "SELECT text_value, COUNT(*) AS cnts FROM metadatavalue WHERE metadata_field_id = 66 AND resource_id IN (SELECT item_id FROM item WHERE in_archive) GROUP BY text_value;";

        try {
            CachedRowSet resSet = databaseService.executeQuery(query);
            while (resSet.next()) {
                types.put(resSet.getString("text_value"), resSet.getLong("cnts"));
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return types;
    }

    private static String prevSessionLocale = "";
    private static java.util.Hashtable<String, String> typesTable = new java.util.Hashtable<String, String>();

    public static String getTypeLocalized(String type, String locale) {
        if (!locale.equals(prevSessionLocale)) {
            typesTable.clear();
            java.util.List vList = DCInputReader.getInputsReader(locale).getPairs("common_types");

            for (int i = 0; i < vList.size(); i += 2)
                typesTable.put((String) vList.get(i + 1), (String) vList.get(i));

            prevSessionLocale = locale;
        }

        String result = typesTable.get(type);
        return result == null ? type : result;
    }

    public static String getLanguageLocalized(String lang, String locale) {
        java.util.Hashtable<String, String> langTable = new java.util.Hashtable<String, String>();
        java.util.List vList = DCInputReader.getInputsReader(locale).getPairs("common_iso_languages");

        for (int i = 0; i < vList.size(); i += 2)
            langTable.put((String) vList.get(i + 1), (String) vList.get(i));

        String result = langTable.get(lang);
        return result == null ? lang : result;
    }


    public static String getMinimalPaperYear() {
        String query = "SELECT MIN(text_value) AS value FROM metadatavalue LEFT JOIN item ON resource_id = item_id WHERE metadata_field_id = 15 AND in_archive";
        CachedRowSet result = databaseService.executeQuery(query);
        List<String> years = new LinkedList<>();

        try {
            while (result.next()) {
                years.add(result.getString("value"));
            }
        } catch (SQLException e) {
            logger.error(e);
        }
        if (years.isEmpty()) {
            return MINIMAL_YEAR;
        } else {
            return years.get(0);
        }
    }
}
