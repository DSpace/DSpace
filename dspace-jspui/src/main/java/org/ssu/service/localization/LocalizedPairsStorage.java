package org.ssu.service.localization;

import org.dspace.app.util.DCInputsReaderException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocalizedPairsStorage {
    private Map<Locale, LocalizedPairsForLocale> storage = new HashMap<>();

    class LocalizedPairsForLocale {
        private Map<String, String> localizedPairs = new HashMap<>();

        public LocalizedPairsForLocale(Map<String, String> localizedPairs) {
            this.localizedPairs = localizedPairs;
        }

        public String getLocalizedItem(String item) {
            return localizedPairs.getOrDefault(item, item);
        }
    }

    public LocalizedPairsStorage(String pairsName) {
        storage.put(Locale.ENGLISH, updateLocalizationTable(Locale.ENGLISH, pairsName));
        storage.put(Locale.forLanguageTag("uk"), updateLocalizationTable(Locale.forLanguageTag("uk"), pairsName));
        storage.put(Locale.forLanguageTag("ru"), updateLocalizationTable(Locale.forLanguageTag("ru"), pairsName));
    }

    private LocalizedPairsForLocale updateLocalizationTable(Locale locale, String pairsName) {
        Map<String, String> localizedPairs = new HashMap<>();
        List<String> xmlData = null;
        try {
            xmlData = new LocalizedInputsReader().getInputsReader(locale.getLanguage()).getPairs(pairsName);
        } catch (DCInputsReaderException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < xmlData.size(); i += 2)
            localizedPairs.put(xmlData.get(i + 1), xmlData.get(i));
        return new LocalizedPairsForLocale(localizedPairs);
    }

    public String getItem(String name, Locale locale) {
        return storage.get(locale).getLocalizedItem(name);
    }
}
