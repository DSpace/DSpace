/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class CC4LocaleConverter {

    private static Logger log = Logger.getLogger(CC4LocaleConverter.class);

    private final Document license_doc;

    private CC4LocaleConverter(Document license_doc) {
        this.license_doc =  (Document)license_doc.clone();
    }

    private CC4LocaleConverter(InputStream in) throws JDOMException, IOException {
        SAXBuilder parser = new SAXBuilder();
        license_doc = parser.build(in);
    }

    public static CC4LocaleConverter builder(Document license_doc) {
        return new CC4LocaleConverter(license_doc);
    }

    public static CC4LocaleConverter builder(InputStream in) throws JDOMException, IOException {
        return new CC4LocaleConverter(in);
    }

    public Document build(String locale) {
        try {
            JDOMXPath xp_licenseUri = new JDOMXPath("//result/license-uri");
            Element licenseUri = (Element) xp_licenseUri.selectSingleNode(license_doc);

            if (StringUtils.containsIgnoreCase(licenseUri.getText(), "4.")) {

                JDOMXPath xp_licenseUriAttributes = new JDOMXPath(String.format("(//result//@rdf:* | //result//@href)[.='%s']", licenseUri.getText()));
                xp_licenseUriAttributes.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                @SuppressWarnings("unchecked")
                List<Attribute> licenseAttributes = (List<Attribute>)xp_licenseUriAttributes.selectNodes(license_doc);

                String cc_baseUri = StringUtils.substringBeforeLast(licenseUri.getText(), "/");
                String localeLicenseUri = String.format("%s/deed.%s", cc_baseUri, StringUtils.isEmpty(locale) ? "en" : locale);
                licenseUri.setText(localeLicenseUri);
                for (Attribute attribute : licenseAttributes) {
                    attribute.setValue(localeLicenseUri);
                }
            } else {
                log.warn("Creative Commons license " + licenseUri.getText() +  " is not version 4");
            }
        } catch (JaxenException e) {
            log.warn(e.getMessage());
            return null;
        }
        return this.license_doc;
    }
}