package org.orcid.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by jonas - jonas@atmire.com on 13/04/2018.
 */
public class ReleaseNameUtils {
    private static String releaseName;

    static {
        releaseName = readReleaseNameFromFile();
        if (StringUtils.isBlank(releaseName)) {
            releaseName = DateUtils.convertToXMLGregorianCalendar(new Date()).toXMLFormat();
        }
    }

    private static String readReleaseNameFromFile() {
        try (InputStream is = ReleaseNameUtils.class.getResourceAsStream("/release_name.txt")) {
            if (is != null) {
                String input = IOUtils.toString(is);
                return input.trim();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading release name file", e);
        }
        return null;
    }

    public static String getReleaseName() {
        return releaseName;
    }
}
