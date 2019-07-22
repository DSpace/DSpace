/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.HashMap;
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

public class SciValUtils {

    /**
     * Default constructor
     */
    private SciValUtils() { }

    public static Record convertScopusDomToRecord(Element article) {
        MutableRecord record = new SubmissionLookupPublication("");

        String eid = XMLUtils.getElementValue(article, "eid");
        if (StringUtils.isNotBlank(eid)) {
            record.addValue("eid", new StringValue(eid));
        }

        String title = XMLUtils.getElementValue(article, "articleTitle");
        if (StringUtils.isNotBlank(title)) {
            record.addValue("title", new StringValue(title));
        }

        String year = XMLUtils.getElementValue(article, "publicationDate");
        if (StringUtils.isNotBlank(year)) {
            record.addValue("year", new StringValue(year));
        }

        String issn = XMLUtils.getElementValue(article, "issn");

        String journalTitle = XMLUtils.getElementValue(article, "sourceTitle");
        if (StringUtils.isNotBlank(journalTitle)) {
            record.addValue("journalTitle", new StringValue(journalTitle));
        }

        String volume = XMLUtils.getElementValue(article, "volume");
        if (StringUtils.isNotBlank(volume)) {
            record.addValue("volume", new StringValue(volume));
        }

        String issue = XMLUtils.getElementValue(article, "issue");
        if (StringUtils.isNotBlank(issue)) {
            record.addValue("issue", new StringValue(issue));
        }

        String startPage = XMLUtils.getElementValue(article, "page_start");
        if (StringUtils.isNotBlank(startPage)) {
            record.addValue("startPage", new StringValue(startPage));
        }

        String endPage = XMLUtils.getElementValue(article, "page_end");
        if (StringUtils.isNotBlank(endPage)) {
            record.addValue("endPage", new StringValue(endPage));
        }

        String doi = XMLUtils.getElementValue(article, "doi");
        if (StringUtils.isNotBlank(doi)) {
            record.addValue("doi", new StringValue(doi));
        }

        String articleNumber = XMLUtils.getElementValue(article, "articleNumber");
        if (StringUtils.isNotBlank(articleNumber)) {
            record.addValue("articleNumber", new StringValue(articleNumber));
        }

        String displayUrl = XMLUtils.getElementValue(article, "displayURL");
        if (StringUtils.isNotBlank(displayUrl)) {
            record.addValue("displayUrl", new StringValue(displayUrl));
        }

        String citationUrl = XMLUtils.getElementValue(article, "citedByURL");
        if (StringUtils.isNotBlank(citationUrl)) {
            record.addValue("citationUrl", new StringValue(citationUrl));
        }

        String citationCount = XMLUtils.getElementValue(article, "citedByCount");
        if (StringUtils.isNotBlank(citationCount)) {
            record.addValue("citationCount", new StringValue(citationCount));
        }

        String eissn;
        if (StringUtils.contains(issn, "|")) {
            eissn = StringUtils.split(issn, "\\|")[0];
            if (StringUtils.isNotBlank(eissn)) {
                record.addValue("eissn", new StringValue(eissn));
            }

            issn = StringUtils.split(issn, "\\|")[1];
            if (StringUtils.isNotBlank(issn)) {
                record.addValue("issn", new StringValue(issn));
            }
        }

        Element authElement = XMLUtils.getSingleElement(article, "authors");
        Element affElement = XMLUtils.getSingleElement(article, "affiliations");
        List<Element> authItemsElement = XMLUtils.getElementList(authElement, "item");
        List<Element> affItemsElement = XMLUtils.getElementList(affElement, "item");

        List<String> authors = new LinkedList<String>();
        for (int idx = 0; idx < authItemsElement.size(); idx++) {
            String tmpauth = XMLUtils.getElementValue(authItemsElement.get(idx), "authorName");
            authors.add(tmpauth);
        }

        List<String> affiliations = new LinkedList<String>();
        for (int idx = 0; idx < affItemsElement.size(); idx++) {
            String tmpaff = XMLUtils.getElementValue(affItemsElement.get(idx), "affiliationName");
            affiliations.add(tmpaff);
        }

        List<String> authorsWithAffiliations = new LinkedList<String>();
        if (authors.size() == affiliations.size()) {
            for (int idx = 0; idx < authors.size(); idx++) {
                String aff = StringUtils.isNotBlank(affiliations.get(idx)) ? "; " + affiliations.get(idx) : "";
                authorsWithAffiliations.add(authors.get(idx) + aff);
            }
        } else if (authors.size() > affiliations.size() && authors.size() > 2 && affiliations.size() > 0) {
            String multiAff = StringUtils.join(affiliations.iterator(), "|||");
            authorsWithAffiliations.add(authors.get(0) + "; " + affiliations.get(0));
            for (int idx = 1; idx < authors.size() - 1; idx++) {
                authorsWithAffiliations.add(authors.get(idx) + "; " + multiAff);
            }
            int lastIdx = affiliations.size() - 1;
            authorsWithAffiliations.add(authors.get(lastIdx) + "; " + affiliations.get(lastIdx));
        } else {
            String multiAff = StringUtils.join(affiliations.iterator(), "|||");
            for (int idx = 0; idx < authors.size(); idx++) {
                authorsWithAffiliations.add(authors.get(idx) + "; " + multiAff);
            }
        }

        List<Value> authorsValue = new LinkedList<Value>();
        for (String auth : authors) {
            authorsValue.add(new StringValue(auth));
        }
        if (authors != null && authors.size() > 0) {
            record.addField("authors", authorsValue);
        }

        List<Value> authorsAffValue = new LinkedList<Value>();
        for (String auth : authorsWithAffiliations) {
            authorsAffValue.add(new StringValue(auth));
        }
        if (authorsWithAffiliations != null && authorsWithAffiliations.size() > 0) {
            record.addField("authorsWithAffiliations", authorsAffValue);
        }

        return record;
    }

    public static Record convertFullScopusDomToRecord(Element article) {
        MutableRecord record = new SubmissionLookupPublication("");

        Element xocsMeta = XMLUtils.getSingleElement(article, "xocs:meta");
        String eid = XMLUtils.getElementValue(xocsMeta, "xocs:eid");
        if (StringUtils.isNotBlank(eid)) {
            record.addValue("eid", new StringValue(eid));
        }

        String issn = XMLUtils.getElementValue(xocsMeta, "xocs:issn");

        // online = XMLUtils.getElementValue(xocsMeta, "xocs:onlinestatus");
        String volume = XMLUtils.getElementValue(xocsMeta, "xocs:volume");
        if (StringUtils.isNotBlank(volume)) {
            record.addValue("volume", new StringValue(volume));
        }
        String startPage = XMLUtils.getElementValue(xocsMeta, "xocs:firstpage");
        if (StringUtils.isNotBlank(startPage)) {
            record.addValue("startPage", new StringValue(startPage));
        }
        String endPage = XMLUtils.getElementValue(xocsMeta, "xocs:lastpage");
        if (StringUtils.isNotBlank(endPage)) {
            record.addValue("endPage", new StringValue(endPage));
        }
        String year = XMLUtils.getElementValue(xocsMeta, "xocs:pub-year");
        if (StringUtils.isNotBlank(year)) {
            record.addValue("year", new StringValue(year));
        }
        String itemType = XMLUtils.getElementValue(xocsMeta, "cto:doctype");
        if (StringUtils.isNotBlank(itemType)) {
            record.addValue("itemType", new StringValue(itemType));
        }

        Element xocsItem = XMLUtils.getSingleElement(article, "xocs:item");
        Element item = XMLUtils.getSingleElement(xocsItem, "item");

        Element aitInfo = XMLUtils.getSingleElement(item, "ait:process-info");
        String issueDate = XMLUtils.getElementAttribute(aitInfo, "ait:date-sort", "year") + "-"
            + XMLUtils.getElementAttribute(aitInfo, "ait:date-sort", "month") + "-"
            + XMLUtils.getElementAttribute(aitInfo, "ait:date-sort", "day");

        Element bibRecord = XMLUtils.getSingleElement(item, "bibrecord");

        Element itemInfo = XMLUtils.getSingleElement(bibRecord, "item-info");

        Element itemidList = XMLUtils.getSingleElement(itemInfo, "itemidlist");
        String doi = XMLUtils.getElementValue(itemidList, "ce:doi");
        if (StringUtils.isNotBlank(doi)) {
            record.addValue("doi", new StringValue(doi));
        }

        List<Element> itemIds = XMLUtils.getElementList(itemidList, "itemid");
        for (Element itemId : itemIds) {
            String idType = itemId.getAttribute("idtype");
            if (StringUtils.equals(idType, "SCP")) {
                String scpId = itemId.getTextContent();
                if (StringUtils.isNotBlank(scpId)) {
                    record.addValue("scpid", new StringValue(scpId));
                }
            } else if (StringUtils.equals(idType, "MEDL")) {
                String medlineId = itemId.getTextContent();
                if (StringUtils.isNotBlank(medlineId)) {
                    record.addValue("medlineid", new StringValue(medlineId));
                }
            }
        }

        Element head = XMLUtils.getSingleElement(bibRecord, "head");
        // CITATION-INFO
        Element citInfo = XMLUtils.getSingleElement(head, "citation-info");

        String language = XMLUtils.getElementAttribute(citInfo, "citation-language", "xml:lang");
        if (StringUtils.isNotBlank(language)) {
            record.addValue("language", new StringValue(language));
        }

        Element keys = XMLUtils.getSingleElement(citInfo, "author-keywords");

        List<String> keywords = XMLUtils.getElementValueList(keys, "author-keyword");
        if (keywords != null && keywords.size() > 0) {
            List<Value> keyVals = new LinkedList<Value>();
            for (String key : keywords) {
                keyVals.add(new StringValue(key));
            }
            record.addField("keywords", keyVals);
        }

        String medium = XMLUtils.getElementValue(citInfo, "medium");
        if (StringUtils.isNotBlank(medium)) {
            record.addValue("medium", new StringValue(medium));
        }
        // END CITATION-INFO

        // CITATION-TITLE
        Element citTitle = XMLUtils.getSingleElement(head, "citation-title");
        List<Element> titleList = XMLUtils.getElementList(citTitle, "titletext");
        List<Value> titleAlternative = new LinkedList<Value>();
        String title = "";
        for (Element tit : titleList) {
            String original = tit.getAttribute("original");
            if (StringUtils.equals(original, "y")) {
                title = tit.getTextContent();
                if (StringUtils.isNotBlank(title)) {
                    record.addValue("title", new StringValue(title));
                }
            } else {
                titleAlternative.add(new StringValue(tit.getTextContent()));
            }
        }
        if (titleAlternative != null && titleAlternative.size() > 0) {
            record.addField("titleAlternative", titleAlternative);
        }

        // END CITATION-TITLE
        // CORRESPONDENCE AUTHOR
        String corAuthorInitials = "";
        String corAuthorLastName = "";
        String corAuthorIndexName = "";
        String corAuthorEmail = "";
        Element correspondence = XMLUtils.getSingleElement(head, "correspondence");
        if (correspondence != null) {
            Element corAuthor = XMLUtils.getSingleElement(correspondence, "person");
            if (corAuthor != null) {
                corAuthorInitials = XMLUtils.getElementValue(corAuthor, "initials");
                corAuthorLastName = XMLUtils.getElementValue(corAuthor, "ce:surname");
                corAuthorIndexName = XMLUtils.getElementValue(corAuthor, "ce:indexed-name");
                corAuthorEmail = XMLUtils.getElementValue(correspondence, "ce:e-address");
            }
        }
        // AUTHOR-GROUP
        //
        List<Element> authorList = XMLUtils.getElementList(head, "author-group");
        HashMap<Integer, Value> authSM = new HashMap<Integer, Value>();
        List<Value> affiliations = new LinkedList<Value>();

        boolean international = false;
        boolean correspondeceFound = false;
        for (int x = 0; x < authorList.size(); x++) {
            Element affElement = XMLUtils.getSingleElement(authorList.get(x), "affiliation");
            String affiliation = "";
            if (affElement != null) {
                List<String> affValues = XMLUtils.getElementValueList(affElement, "organization");

                for (int y = 0; y < affValues.size(); y++) {
                    affiliation += "//" + affValues.get(y);
                }
                String affCountryIso = XMLUtils.getElementAttribute(authorList.get(x), "affiliation", "country");
                String affCountry = XMLUtils.getElementValue(affElement, "country");
                affiliation += StringUtils.isNotBlank(affCountry) ? "//" + affCountry : "";
                affiliations.add(new StringValue(affiliation));

                String affCity = XMLUtils.getElementValue(affElement, "city-group");
                affiliation += StringUtils.isNotBlank(affCity) ? "//" + affCity : "";
                if (!international && !StringUtils.equalsIgnoreCase(affCountryIso, "ita")) {
                    international = true;
                }
            }

            List<Element> authTmp = XMLUtils.getElementList(authorList.get(x), "author");
            for (int y = 0; y < authTmp.size(); y++) {

                String authSequence = authTmp.get(y).getAttribute("seq");

                String authSurname = XMLUtils.getElementValue(authTmp.get(y), "ce:surname");
                String authGivenName = XMLUtils.getElementValue(authTmp.get(y), "ce:given-name");
                String authInitials = XMLUtils.getElementValue(authTmp.get(y), "ce:initials");
                String authIndexedName = XMLUtils.getElementValue(authTmp.get(y), "ce:indexed-name");
                String authEmail = XMLUtils.getElementValue(authTmp.get(y), "ce:e-address");
                // CHECK IF THIS AUTHOR IS THE CORRESPONDING ONE
                String cor = "";
                if (!correspondeceFound
                    && (StringUtils.equals(authEmail, corAuthorEmail)
                    || StringUtils.equals(authIndexedName, corAuthorIndexName) || (StringUtils.equals(
                    authSurname, corAuthorLastName) && StringUtils.equals(authInitials, corAuthorInitials)))) {

                    correspondeceFound = true;
                    cor = "*";
                }
                String authName = StringUtils.isNotBlank(authGivenName) ? authGivenName : authInitials;
                authSM.put(Integer.parseInt(authSequence), new StringValue(authSurname + ", " + authName + cor));
            }
        }
        List<Value> authors = new LinkedList<Value>();
        for (int i = 1; i <= authSM.keySet().size(); i++) {
            authors.add(authSM.get(i));
        }
        if (authors != null && authors.size() > 0) {
            record.addField("authors", authors);
        }
        String internationalAuthor = international ? "yes" : "no";
        if (StringUtils.isNotBlank(internationalAuthor)) {
            record.addValue("internationalAuthor", new StringValue(internationalAuthor));
        }
        // END AUTHOR-GROUP
        // ABSTRACTS
        Element abstractsElement = XMLUtils.getSingleElement(head, "abstracts");
        List<Value> abstracts = new LinkedList<Value>();
        if (abstractsElement != null) {
            List<Element> absList = XMLUtils.getElementList(abstractsElement, "abstract");
            for (int x = 0; x < absList.size(); x++) {

                String abstractLanguage = absList.get(x).getAttribute("xml:lang");
                String abs = "";
                List<String> absValues = XMLUtils.getElementValueList(absList.get(x), "ce:para");
                for (int y = 0; y < absValues.size(); y++) {
                    abs += absValues.get(y);
                }

                if (StringUtils.equals(abstractLanguage, "ita")) {
                    String abstractita = abs;
                    record.addValue("abstractita", new StringValue(abstractita));
                } else if (StringUtils.equals(abstractLanguage, "eng")) {
                    String abstracteng = abs;
                    record.addValue("abstracteng", new StringValue(abstracteng));
                } else if (StringUtils.equals(abstractLanguage, "fre")) {
                    String abstractfre = abs;
                    record.addValue("abstractfre", new StringValue(abstractfre));
                } else if (StringUtils.equals(abstractLanguage, "ger")) {
                    String abstractger = abs;
                    record.addValue("abstractger", new StringValue(abstractger));
                } else if (StringUtils.equals(abstractLanguage, "esp")) {
                    String abstractesp = abs;
                    record.addValue("abstractesp", new StringValue(abstractesp));
                } else {
                    abstracts.add(new StringValue(abs));
                }
            }
        }
        if (abstracts != null && abstracts.size() > 0) {
            record.addField("abstracts", abstracts);
        }
        // END ABSTRACTS
        // SOURCE
        Element source = XMLUtils.getSingleElement(head, "source");
        String sourcePlace = XMLUtils.getElementAttribute(head, "source", "country");
        String sourceType = XMLUtils.getElementAttribute(head, "source", "type");
        String bookTitle = "";
        if (StringUtils.equals(sourceType, "j")) {
            String journalTitle = XMLUtils.getElementValue(source, "sourcetitle");
            if (StringUtils.isNotBlank(journalTitle)) {
                record.addValue("sourceTitle", new StringValue(journalTitle));
            }
        } else if (StringUtils.equals(sourceType, "k")) {

            bookTitle = XMLUtils.getElementValue(source, "sourcetitle");
            String volTitle = XMLUtils.getElementValue(source, "volumetitle");
            if (StringUtils.isNotBlank(volTitle)) {
                StringValue volTit = new StringValue(volTitle);
                record.removeField("title");
                record.addValue("title", volTit);

            }

        } else if (StringUtils.equals(sourceType, "b") || StringUtils.equals(sourceType, "p")) {
            bookTitle = XMLUtils.getElementValue(source, "sourcetitle");
        }
        if (StringUtils.isNotBlank(bookTitle)) {
            record.addValue("bookTitle", new StringValue(bookTitle));
        }
        String issueTitle = XMLUtils.getElementValue(source, "issuetitle");
        if (StringUtils.isNotBlank(issueTitle)) {
            record.addValue("issueTitle", new StringValue(issueTitle));
        }

        List<Element> issnList = XMLUtils.getElementList(source, "issn");

        for (int x = 0; x < issnList.size(); x++) {
            String issnAttr = issnList.get(x).getAttribute("type");
            String val = issnList.get(x).getTextContent();
            if (StringUtils.equals(issnAttr, "electronic")) {
                String eissn = val;
                eissn = StringUtils.substring(eissn, 0, 4) + "-" + StringUtils.substring(eissn, 4);
                record.addValue("eissn", new StringValue(eissn));
            } else if (StringUtils.equals(issnAttr, "print")) {
                issn = val;
            }
        }

        if (StringUtils.isNotBlank(issn)) {
            issn = StringUtils.substring(issn, 0, 4) + "-" + StringUtils.substring(issn, 4);
            record.addValue("issn", new StringValue(issn));
        }

        String isbn = XMLUtils.getElementValue(source, "isbn");
        if (StringUtils.isNotBlank(isbn)) {
            record.addValue("isbn", new StringValue(isbn));
        }
        // CONTRIBUTORS-GROUP
        //
        List<Element> contGroupList = XMLUtils.getElementList(source, "contributor-group");
        List<Value> sourceAuthor = new LinkedList<Value>();
        List<Value> sourceEditor = new LinkedList<Value>();
        List<Value> sourceTranslator = new LinkedList<Value>();

        for (int x = 0; x < contGroupList.size(); x++) {
            Element affElement = XMLUtils.getSingleElement(contGroupList.get(x), "affiliation");
            String affiliation = "";
            if (affElement != null) {
                List<String> affValues = XMLUtils.getElementValueList(affElement, "organization");
                if (affValues != null) {
                    for (int y = 0; y < affValues.size(); y++) {
                        affiliation += " ," + affValues.get(y);
                    }
                }
                String affCity = XMLUtils.getElementValue(affElement, "city-group");
                affiliation += StringUtils.isNotBlank(affCity) ? ", " + affCity : "";
                String affCountry = XMLUtils.getElementValue(affElement, "country");
                affiliation += StringUtils.isNotBlank(affCountry) ? ", " + affCountry : "";
            }
            List<Element> contTmp = XMLUtils.getElementList(contGroupList.get(x), "contributor");

            for (int y = 0; y < contTmp.size(); y++) {
                String contRole = contTmp.get(y).getAttribute("role");

                String contSurname = XMLUtils.getElementValue(contTmp.get(y), "ce:surname");
                String contGivenName = XMLUtils.getElementValue(contTmp.get(y), "ce:given-name");
                if (StringUtils.equals(contRole, "auth")) {
                    sourceAuthor.add(new StringValue(contGivenName + " , " + contSurname + " , " + affiliation));
                } else if (StringUtils.equals(contRole, "edit")) {
                    sourceEditor.add(new StringValue(contGivenName + " , " + contSurname + " , " + affiliation));
                } else if (StringUtils.equals(contRole, "tran")) {
                    sourceTranslator.add(new StringValue(contGivenName + " , " + contSurname + " , " + affiliation));
                }
            }
        }
        if (sourceAuthor != null && sourceAuthor.size() > 0) {
            record.addField("sourceAuthor", sourceAuthor);
        }
        if (sourceEditor != null && sourceEditor.size() > 0) {
            record.addField("sourceEditor", sourceEditor);
        }
        if (sourceTranslator != null && sourceTranslator.size() > 0) {
            record.addField("sourceTransaltor", sourceTranslator);
        }
        // END CONTRIBUTORS
        String edition = XMLUtils.getElementValue(source, "edition");
        if (StringUtils.isNotBlank(edition)) {
            record.addValue("edition", new StringValue(edition));
        }
        String part = XMLUtils.getElementValue(source, "part");
        if (StringUtils.isNotBlank(part)) {
            record.addValue("part", new StringValue(part));
        }

        String articleNumber = XMLUtils.getElementValue(source, "article-number");
        if (StringUtils.isNotBlank(articleNumber)) {
            record.addValue("articleNumber", new StringValue(articleNumber));
        }

        // year = XMLUtils.getElementAttribute(source, "publicationyear",
        // "first");

        List<Element> websites = XMLUtils.getElementList(source, "website");

        for (Element website : websites) {
            String websiteType = website.getAttribute("type");
            if (!StringUtils.equals(websiteType, "source")) {
                String url = XMLUtils.getElementValue(website, "ce:e-address");
                if (StringUtils.isNotBlank(url)) {
                    record.addValue("url", new StringValue(url));
                }
            }
        }

        Element volumes = XMLUtils.getSingleElement(source, "volisspag");
        if (volumes != null) {
            String issue = XMLUtils.getElementAttribute(volumes, "voliss", "issue");
            if (StringUtils.isNotBlank(issue)) {
                record.addValue("issue", new StringValue(issue));
            }
            String supplement = XMLUtils.getElementValue(volumes, "supplement");
            if (StringUtils.isNotBlank(supplement)) {
                record.addValue("supplement", new StringValue(supplement));
            }
        }

        Element publisher = XMLUtils.getSingleElement(source, "publisher");

        if (publisher != null) {
            String publisherPlace = "";
            String publisherName = XMLUtils.getElementValue(publisher, "publishername");
            if (StringUtils.isNotBlank(publisherName)) {
                record.addValue("publisherName", new StringValue(publisherName));
            }
            String publisherAddress = XMLUtils.getElementValue(publisher, "publisheraddress");
            if (StringUtils.isNotBlank(publisherAddress)) {
                publisherPlace = publisherAddress;

            } else if (XMLUtils.getElementValue(publisher, "affiliation") != null) {
                Element affElement = XMLUtils.getSingleElement(publisher, "affiliation");
                List<String> affValues = XMLUtils.getElementValueList(affElement, "organization");

                for (int y = 0; y < affValues.size(); y++) {
                    publisherPlace += affValues.get(y) + " ; ";
                }
                String affCity = XMLUtils.getElementValue(affElement, "city-group");
                publisherPlace += StringUtils.isNotBlank(affCity) ? ";" + affCity : "";

                String affAddress = XMLUtils.getElementValue(affElement, "address-part");
                publisherPlace += StringUtils.isNotBlank(affAddress) ? ";" + affAddress : "";

                String affCountry = XMLUtils.getElementAttribute(publisher, "affiliation", "country");
                if (StringUtils.isNotBlank(affCountry)) {
                    record.addValue("publisherCountry", new StringValue(affCountry));
                }
            }
            if (StringUtils.isNotBlank(publisherPlace)) {
                record.addValue("publisherPlace", new StringValue(publisherPlace));
            }
        }


        Element addInfo = XMLUtils.getSingleElement(source, "additional-srcinfo");
        if (addInfo != null) {
            Element confInfo = XMLUtils.getSingleElement(addInfo, "conferenceinfo");
            if (confInfo != null) {
                Element confEvent = XMLUtils.getSingleElement(confInfo, "confevent");
                if (confEvent != null) {
                    String conferenceName = XMLUtils.getElementValue(confEvent, "confname");
                    if (StringUtils.isNotBlank(conferenceName)) {
                        record.addValue("conferenceName", new StringValue(conferenceName));
                    }

                    String conferenceNumber = XMLUtils.getElementValue(confEvent, "confnumber");
                    if (StringUtils.isNotBlank(conferenceNumber)) {
                        record.addValue("conferenceNumber", new StringValue(conferenceNumber));
                    }

                    Element confSponsors = XMLUtils.getSingleElement(confEvent, "confsponsors");
                    List<String> conferenceSponsor = XMLUtils.getElementValueList(confSponsors, "confsponsor");

                    if (conferenceSponsor != null && conferenceSponsor.size() > 0) {
                        List<Value> sponsorValues = new LinkedList<Value>();
                        for (String sponsor : conferenceSponsor) {
                            sponsorValues.add(new StringValue(sponsor));
                        }
                        record.addField("conferenceSponsor", sponsorValues);
                    }

                    String conferencePlace = "";
                    Element confLocation = XMLUtils.getSingleElement(confEvent, "conflocation");
                    String nationIso = XMLUtils.getElementAttribute(confEvent, "conflocation", "country");

                    String venue = XMLUtils.getElementValue(confLocation, "venue");
                    conferencePlace += StringUtils.isNotBlank(venue) ? venue + ", " : "";
                    String address = XMLUtils.getElementValue(confLocation, "address-part");
                    conferencePlace += StringUtils.isNotBlank(address) ? address + ", " : "";
                    String confCity = XMLUtils.getElementValue(confLocation, "city-group");
                    conferencePlace += StringUtils.isNotBlank(confCity) ? confCity + ", " : "";
                    String postalCode = XMLUtils.getElementValue(confLocation, "postal-code");
                    conferencePlace += StringUtils.isNotBlank(postalCode) ? postalCode + ", " : "";
                    conferencePlace += nationIso;
                    if (StringUtils.isNotBlank(conferencePlace)) {
                        record.addValue("conferencePlace", new StringValue(conferencePlace));
                    }
                    String conferenceTarget = StringUtils.equalsIgnoreCase(nationIso, "ita") ? "Convegno nazionale"
                        : "Convegno internazionale";
                    if (StringUtils.isNotBlank(conferenceTarget)) {
                        record.addValue("conferenceTarget", new StringValue(conferenceTarget));
                    }

                    Element confDate = XMLUtils.getSingleElement(confEvent, "confdate");
                    String conferenceYear = XMLUtils.getElementAttribute(confDate, "startdate", "year");
                    if (StringUtils.isNotBlank(conferenceYear)) {
                        record.addValue("conferenceYear", new StringValue(conferenceYear));
                    }
                }
                Element confPublication = XMLUtils.getSingleElement(confInfo, "confpublication");
                if (confPublication != null) {
                    Element confEditors = XMLUtils.getSingleElement(confPublication, "confeditors");
                    if (confEditors != null) {
                        List<Value> conferenceEditors = new LinkedList<Value>();
                        Element editorsElement = XMLUtils.getSingleElement(confEditors, "editors");
                        List<Element> editorList = XMLUtils.getElementList(editorsElement, "editor");
                        // LE AFFILIATION DEGLI EDITOR NON SONO IN EDITOR
                        // DOBBIAMO
                        // USARLE?
                        for (Element ed : editorList) {
                            String editorGivenName = XMLUtils.getElementValue(ed, "ce:given-name");
                            String editorLastName = XMLUtils.getElementValue(ed, "ce:surname");
                            conferenceEditors.add(new StringValue(editorGivenName + " , " + editorLastName));
                        }
                        record.addField("chairs", conferenceEditors);
                    }
                }
            }
        }
        // END SOURCE
        // ENHANCEMENT
        Element enhancement = XMLUtils.getSingleElement(head, "enhancement");

        if (enhancement != null) {
            List<Value> classificationASJC = new LinkedList<>();
            Element classificationGroup = XMLUtils.getSingleElement(enhancement, "classificationgroup");
            for (Element classifications : XMLUtils.getElementList(classificationGroup, "classifications")) {
                String cltype = classifications.getAttribute("type");
                if (StringUtils.equals(cltype, "ASJC")) {
                    for (Element cl : XMLUtils.getElementList(classifications, "classification")) {
                        classificationASJC.add(new StringValue(cl.getTextContent()));
                    }
                    break;
                }

            }
            if (classificationASJC != null && classificationASJC.size() > 0) {
                record.addField("classificationASJC", classificationASJC);
            }
        }
        // END ENHANCEMENT
        return record;
    }

}
