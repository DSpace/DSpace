/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.output.Bibliography;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CSLGenerator} that uses a nested Citation processor
 * to generate the citations.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLNestedGenerator implements CSLGenerator {

    private static final Logger LOGGER = LogManager.getLogger(CSLNestedGenerator.class);

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public CSLResult generate(DSpaceListItemDataProvider itemDataProvider, String style, String format) {
        CSL citeproc = createCitationProcessor(itemDataProvider, style, format);
        if (citeproc == null) {
            return null;
        }
        Bibliography bibliography = citeproc.makeBibliography();
        return CSLResult.fromBibliography(format, bibliography, itemDataProvider.getIds());
    }

    private CSL createCitationProcessor(DSpaceListItemDataProvider itemDataProvider, String style, String format) {
        try {
            CSL citeproc = new CSL(itemDataProvider, getStyle(style), getLanguage(itemDataProvider));
            citeproc.setOutputFormat(format);
            citeproc.registerCitationItems(itemDataProvider.getIds());
            return citeproc;
        } catch (Exception e) {
            LOGGER.warn("Something went wrong for: " + itemDataProvider.getId(), e);
            return null;
        }
    }

    private String getLanguage(DSpaceListItemDataProvider itemDataProvider) {
        String fixedLanguage = configurationService.getProperty("csl.fixedLanguage");
        if (StringUtils.isNotBlank(fixedLanguage)) {
            return fixedLanguage;
        }
        return StringUtils.isNotBlank(itemDataProvider.getCitationLanguage()) ?
                itemDataProvider.getCitationLanguage() : "en-US";
    }

    private String getStyle(String style) throws IOException {
        return CSL.supportsStyle(style) && !(StringUtils.startsWith(style, File.separator) ||
        StringUtils.endsWith(style, ".csl")) ?
                style : readXmlStyleContent(style);
    }

    private String readXmlStyleContent(String style) throws IOException {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File styleFile = new File(parent, style);
        if (!styleFile.exists()) {
            parent = parent + File.separator + "crosswalks" + File.separator + "csl";
            styleFile = new File(parent, style);
            if (!styleFile.exists()) {
                throw new FileNotFoundException("Could not find style " + style);
            }
        }

        try (FileInputStream fis = new FileInputStream(styleFile)) {
            return IOUtils.toString(fis, Charset.defaultCharset());
        }
    }

}
