/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * 
 */
package org.dspace.submit.lookup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class ScopusUtils
{

	public final static String PLACEHOLER_NO_DATA="#NODATA#";
	
    public static Record convertScopusDomToRecord(Element article)
    {
        MutableRecord record = new SubmissionLookupPublication("");
        
        String url = XMLUtils.getElementValue(article,
                "prism:url");
        if (url != null){
            record.addValue("url", new StringValue(url));
        }
        String eid = XMLUtils.getElementValue(article,
                "eid");
        if (eid != null){
            record.addValue("eid", new StringValue(eid));
        }
        String title = XMLUtils.getElementValue(article,
                "dc:title");
        if (title != null){
            record.addValue("title", new StringValue(title));
        }
        String aggregationType = XMLUtils.getElementValue(article,
                "prism:aggregationType");
        if (aggregationType != null){
            record.addValue("aggregationtype", new StringValue(aggregationType));
        }
        String subType = XMLUtils.getElementValue(article,
                "subtype");
        if (subType != null){
            record.addValue("itemType", new StringValue(subType));
            record.addValue("scopusType", new StringValue(subType));
        }
        String sourceTitle = XMLUtils.getElementValue(article,
                "prism:publicationName");
        if (sourceTitle != null){
            record.addValue("sourceTitle", new StringValue(sourceTitle));
        }
        String isbn = XMLUtils.getElementValue(article,
                "prism:isbn");
        if (isbn != null){
            record.addValue("isbn", new StringValue(isbn));
        }        
        String issn = XMLUtils.getElementValue(article,
                "prism:issn");
        if (issn != null){
            record.addValue("issn", new StringValue(issn));
        }        
        String eissn = XMLUtils.getElementValue(article,
                "prism:eIssn");
        if (eissn != null){
            record.addValue("eissn", new StringValue(eissn));
        }        
        String volume = XMLUtils.getElementValue(article,
                "prism:volume");
        if (volume != null){
            record.addValue("volume", new StringValue(volume));
        }
        String issue = XMLUtils.getElementValue(article,
                "prism:issueIdentifier");
        if (issue != null){
            record.addValue("issue", new StringValue(issue));
        }
        String pageRange = XMLUtils.getElementValue(article,
                "prism:pageRange");
        if (pageRange != null){
        	String[] pages = StringUtils.split(pageRange, "-");
        	record.addValue("spage", new StringValue(pages[0]));
        	if(pages.length>1){
        		record.addValue("epage", new StringValue(pages[1]));
        	}
        }
        String issued = XMLUtils.getElementValue(article,
                "prism:coverDate");
        if (issued != null){
            record.addValue("issued", new StringValue(issued));
        }
        String doi = XMLUtils.getElementValue(article,
                "prism:doi");
        if (doi != null){
            record.addValue("doi", new StringValue(doi));
        }
        String pmid = XMLUtils.getElementValue(article,
                "pubmed-id");
        if (pmid != null){
            record.addValue("pmid", new StringValue(pmid));
        }
        String pubID = XMLUtils.getElementValue(article,
                "pii");
        if (pubID != null){
            record.addValue("pii", new StringValue(pubID));
        }
        String abs = XMLUtils.getElementValue(article,
                "dc:description");
        if (abs != null){
            record.addValue("abstract", new StringValue(abs));
        }
        String articleNumber = XMLUtils.getElementValue(article,
                "article-number");
        if (articleNumber != null){
            record.addValue("articlenumber", new StringValue(articleNumber));
        }
        String keywords = XMLUtils.getElementValue(article,
                "authkeywords");
        if (keywords != null){
            record.addValue("scopusKeywords", new StringValue(keywords));
        }
        
        List<Element> authors = XMLUtils.getElementList(article,
                    "author");
        LinkedList<Value> authNames = new LinkedList<Value>();
        LinkedList<Value> authUrl = new LinkedList<Value>();
        LinkedList<Value> authScopusID = new LinkedList<Value>();
        LinkedList<Value> authOrcid = new LinkedList<Value>();
        List<String> sequenceAuthors = new LinkedList<String>(); 
        //TODO: Manage Author Affiliation
        authors : for(Element author: authors){
            
            //check sequence number
            String sequenceAuthor = author.getAttribute("seq");
            if(StringUtils.isNotBlank(sequenceAuthor)) {
                if(sequenceAuthors.contains(sequenceAuthor)) {
                    //author already managed, skip it
                    continue authors;
                }
                else {
                    //manage new author
                    sequenceAuthors.add(sequenceAuthor);
                }
            }
            String givenname = XMLUtils.getElementValue(author,
                    "given-name");
            String surname = XMLUtils.getElementValue(author,
                    "surname");
            String authname = XMLUtils.getElementValue(author,
                    "authname");
            if (givenname != null && surname != null) {
                authNames.add(new StringValue(surname+", "+givenname));
            } else if (authname != null){
                authNames.add(new StringValue(authname));
            }

            String auUrl = XMLUtils.getElementValue(author,
                    "author-url");
            if (auUrl != null){
                authUrl.add(new StringValue(auUrl));
            }else{
            	authUrl.add(new StringValue(PLACEHOLER_NO_DATA));
            }

            String scopusID = XMLUtils.getElementValue(author,
                    "authid");
            if (scopusID != null){
                authScopusID.add(new StringValue(scopusID));
            }else{
            	authScopusID.add(new StringValue(PLACEHOLER_NO_DATA));
            }
            
            String orcid = XMLUtils.getElementValue(author,
                    "orcid");
            if (orcid != null){
                authOrcid.add(new StringValue(orcid));
            }else{
            	authOrcid.add(new StringValue(PLACEHOLER_NO_DATA));
            }
        }
        record.addField("authors", authNames);
        record.addField("authorUrl", authUrl);
        record.addField("authorScopusid", authScopusID);
        record.addField("orcid", authOrcid);

        return record;
    }
}
