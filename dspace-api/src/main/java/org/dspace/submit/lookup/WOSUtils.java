/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

public final class WOSUtils {

    /**
     * Default constructor
     */
    private WOSUtils() { }

    public static Record convertWosDomToRecord(Element rec) {
        MutableRecord record = new SubmissionLookupPublication("");

        String wosid = XMLUtils.getElementValue(rec, "UID");
        if (StringUtils.isNotBlank(wosid)) {
            record.addValue("isiId", new StringValue(wosid));
        }
        Element dynamicData = XMLUtils.getSingleElement(rec, "dynamic_data");
        Element clusterRelated = XMLUtils.getSingleElement(dynamicData, "cluster_related");
        Element identifiers = XMLUtils.getSingleElement(clusterRelated, "identifiers");

        List<Element> identifierList = XMLUtils.getElementList(identifiers, "identifier");
        for (Element current : identifierList) {
            if ("doi".equals(current.getAttribute("type"))) {
                String doi = current.getAttribute("value");
                if (StringUtils.isNotBlank(doi)) {
                    record.addValue("doi", new StringValue(doi));
                }
            }
            if ("issn".equals(current.getAttribute("type"))) {
                String issn = current.getAttribute("value");
                if (StringUtils.isNotBlank(issn)) {
                    record.addValue("issn", new StringValue(issn));
                }
            }
        }

        Element staticData = XMLUtils.getSingleElement(rec, "static_data");
        Element summary = XMLUtils.getSingleElement(staticData, "summary");
        Element titles = XMLUtils.getSingleElement(summary, "titles");

        List<Element> titleList = XMLUtils.getElementList(titles, "title");
        for (Element current : titleList) {
            if ("item".equals(current.getAttribute("type"))) {
                String title = current.getTextContent();
                if (StringUtils.isNotBlank(title)) {
                    record.addValue("title", new StringValue(title));
                }
            }
            if ("source".equals(current.getAttribute("type"))) {
                String journalTitle = current.getTextContent();
                if (StringUtils.isNotBlank(journalTitle)) {
                    record.addValue("journalTitle", new StringValue(journalTitle));
                }
            }
        }

        Element pubInfo = XMLUtils.getSingleElement(summary, "pub_info");
        String year = pubInfo.getAttribute("pubyear");
        if (StringUtils.isNotBlank(year)) {
            record.addValue("year", new StringValue(year));
        }

        String volume = pubInfo.getAttribute("vol");
        if (StringUtils.isNotBlank(volume)) {
            record.addValue("volume", new StringValue(volume));
        }

        String issue = pubInfo.getAttribute("issue");
        if (StringUtils.isNotBlank(issue)) {
            record.addValue("issue", new StringValue(issue));
        }

        Element page = XMLUtils.getSingleElement(pubInfo, "page");
        String startPage = page.getAttribute("begin");
        if (StringUtils.isNotBlank(startPage)) {
            record.addValue("startPage", new StringValue(startPage));
        }

        String endPage = page.getAttribute("end");
        if (StringUtils.isNotBlank(endPage)) {
            record.addValue("endPage", new StringValue(endPage));
        }

        Element names = XMLUtils.getSingleElement(summary, "names");
        List<Element> namesList = XMLUtils.getElementList(names, "name");
        List<Value> authors = new ArrayList<Value>();
        for (Element current : namesList) {
            if ("author".equals(current.getAttribute("role"))) {
                Element authorName = XMLUtils.getSingleElement(current, "wos_standard");
                String standardAuthorName = authorName.getTextContent();
                if (StringUtils.isNotBlank(standardAuthorName)) {
                    authors.add(new StringValue(standardAuthorName));
                }
            }
        }
        if (authors.size() > 0) {
            record.addField("authors", authors);
        }

        Element citationRelated = XMLUtils.getSingleElement(dynamicData, "citation_related");
        Element tcList = XMLUtils.getSingleElement(citationRelated, "tc_list");
        Element siloTc = XMLUtils.getSingleElement(tcList, "silo_tc");
        String citationCount = siloTc.getAttribute("local_count");
        if (StringUtils.isNotBlank(citationCount)) {
            record.addValue("citationCount", new StringValue(citationCount));
        }

        Element recordMetadata = XMLUtils.getSingleElement(staticData, "fullrecord_metadata");
        Element keywords = XMLUtils.getSingleElement(recordMetadata, "keywords");
        List<String> keywordList = XMLUtils.getElementValueList(keywords, "keyword");
        if (keywordList != null && keywordList.size() > 0) {
            List<Value> keyVals = new LinkedList<Value>();
            for (String key : keywordList) {
                keyVals.add(new StringValue(key));
            }
            record.addField("keywords", keyVals);
        }

        Element languages = XMLUtils.getSingleElement(recordMetadata, "languages");
        List<Element> languageList = XMLUtils.getElementList(languages, "language");
        for (Element current : languageList) {
            if ("primary".equals(current.getAttribute("type"))) {
                String language = current.getTextContent();
                if (StringUtils.isNotBlank(language)) {
                    record.addValue("language", new StringValue(language));
                }
            }
        }

        Element types = XMLUtils.getSingleElement(recordMetadata, "normalized_doctypes");
        List<Element> typeList = XMLUtils.getElementList(types, "doctype");
        for (Element current : typeList) {
            // if ("primary".equals(current.getAttribute("type"))) {
            String type = current.getTextContent();
            if (StringUtils.isNotBlank(type)) {
                record.addValue("itemType", new StringValue(type));
                record.addValue("wosType", new StringValue(type));
            }
            break;
            // }
        }

        Element abstractsElement = XMLUtils.getSingleElement(recordMetadata, "abstracts");
        List<Element> abstractList = XMLUtils.getElementList(abstractsElement, "abstract");
        if (abstractList != null && abstractList.size() > 0) {
            List<Value> abstracts = new LinkedList<Value>();
            for (Element current : abstractList) {
                Element absText = XMLUtils.getSingleElement(current, "abstract_text");
                String lang = null;
                if (abstractList.size() == 1) {
                    lang = record.getValues("language").get(0).getAsString();
                    switch (lang) {
                        case "English":
                            record.addValue("abstracteng", new StringValue(absText.getTextContent()));
                            break;
                        default:
                            abstracts.add(new StringValue(absText.getTextContent()));
                            break;
                    }
                } else {
                    abstracts.add(new StringValue(absText.getTextContent()));
                }
            }
            record.addField("abstracts", abstracts);
        }

        Element publishers = XMLUtils.getSingleElement(summary, "publishers");
        Element publisher = XMLUtils.getSingleElement(publishers, "publisher");
        Element publisherNames = XMLUtils.getSingleElement(publisher, "names");
        List<Element> nameList = XMLUtils.getElementList(publisherNames, "name");
        for (Element current : nameList) {
            if ("publisher".equals(current.getAttribute("role"))) {
                Element nameElement = XMLUtils.getSingleElement(current, "full_name");
                String name = nameElement.getTextContent();
                if (StringUtils.isNotBlank(name)) {
                    record.addValue("publisherName", new StringValue(name));
                }
                Element addressSpec = XMLUtils.getSingleElement(publisher, "address_spec");
                Element addrElement = XMLUtils.getSingleElement(addressSpec, "full_address");
                String place = addrElement.getTextContent();
                if (StringUtils.isNotBlank(place)) {
                    record.addValue("publisherPlace", new StringValue(place));
                }
                Element cityElement = XMLUtils.getSingleElement(addressSpec, "city");
                String city = cityElement.getTextContent();
                if (StringUtils.isNotBlank(city)) {
                    record.addValue("publisherCountry", new StringValue(city));
                }
                break;
            }
        }

        return record;
    }

}
