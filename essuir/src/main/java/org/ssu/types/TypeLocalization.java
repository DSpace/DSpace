package org.ssu.types;

import org.apache.log4j.Logger;
import org.ssu.LocalizedInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TypeLocalization {
    private static Logger logger = Logger.getLogger(TypeLocalization.class);

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
