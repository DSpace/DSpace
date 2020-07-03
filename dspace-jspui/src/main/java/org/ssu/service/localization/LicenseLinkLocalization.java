package org.ssu.service.localization;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Locale;

@Service
public class LicenseLinkLocalization {
    private LocalizedPairsStorage storage;

    @PostConstruct
    public void init() {
        storage = new LocalizedPairsStorage("rights_links_localization");
    }

    public String getLicenseLink(String licenseType, Locale locale) {
        return storage.getItem(licenseType, locale);
    }
}
