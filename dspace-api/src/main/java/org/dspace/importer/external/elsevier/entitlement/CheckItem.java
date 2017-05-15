/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.elsevier.entitlement;

import java.io.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;
import org.apache.commons.lang3.*;
import org.apache.log4j.*;
import org.dspace.authority.rest.*;
import org.dspace.authority.util.XMLUtils;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.core.*;
import org.dspace.fileaccess.factory.*;
import org.dspace.fileaccess.service.*;
import org.dspace.importer.external.elsevier.util.*;
import org.w3c.dom.*;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 02 Oct 2015
 */
public abstract class CheckItem {

    /**
     * log4j logger
     */
    private static final Logger log = Logger.getLogger(CheckItem.class);
    protected Document response = null;
    protected Set<String> trueValues = Collections.singleton("true");
    protected Set<String> falseValues = Collections.singleton("false");
    protected boolean useApiKey = true;

    protected static ItemMetadataService itemMetadataService = FileAccessServiceFactory.getInstance().getItemMetadataService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Sends a rest request to the elsevier api
     *
     * @param item The item to be checked
     * @return  true    when the response element is contained in getTrueValues()
     *          false   when the response element is contained in getFalseValues()
     *          null    when none of the above
     */
    public ArticleAccess check(Item item) {
        ArticleAccess articleAccess = new ArticleAccess();

        String url = ConfigurationManager.getProperty("external-sources.elsevier." + getUrlConfigKey());
        RESTConnector connect = new RESTConnector(url);

        String pii = itemMetadataService.getPII(item);
        String doi = itemMetadataService.getDOI(item);
        String eid = itemMetadataService.getEID(item);
        String scopus_id = itemMetadataService.getScopusID(item);
        String pubmed_ID = itemMetadataService.getPubmedID(item);

        if (StringUtils.isNotBlank(pii)) {
            response = connect.get("hostingpermission/pii/" + pii + getQueryString());
        } else if (StringUtils.isNotBlank(doi) && doi.startsWith("10.1016")) {
            response = connect.get("hostingpermission/doi/" + doi + getQueryString());
        } else if (publisherIsElsevier(item)) {
            if (StringUtils.isNotBlank(scopus_id)) {
                response = connect.get("hostingpermission/scopus_id/" + scopus_id + getQueryString());
            } else if (StringUtils.isNotBlank(pubmed_ID)) {
                response = connect.get("hostingpermission/pubmed_id/" + pubmed_ID + getQueryString());
            }
        }
        else if (StringUtils.isNotBlank(eid)) {
            response = connect.get("hostingpermission/eid/" + eid + getQueryString());
        }

        if (response != null) {
            try {
                Node hostingNode = XMLUtils.getNode(response, getCheckNodeXPath());
                Node hostingAllowedNode;
                if(hostingNode==null){
                    // If the hostingNode == null
                    // -> Log the response and return the initially created articleAcces object since it's no use continuing the processing
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    StringWriter writer = new StringWriter();
                    transformer.transform(new DOMSource(response), new StreamResult(writer));
                    String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
                    log.warn("Error retrieving required nodes from the response, please verify whether your ScienceDirect API key has sufficient permissions: "+output);
                    return articleAccess;
                }else{
                    hostingAllowedNode = XMLUtils.getNode(hostingNode, "hosting-platform[@type='institutional_repository']/document-version[journal_article_version/text()='AM']/hosting-allowed[@audience='Public']");
                }
                if (hostingAllowedNode != null) {
                    NamedNodeMap attributes = hostingAllowedNode.getAttributes();

                    Node audience = attributes.getNamedItem("audience");
                    Node start_date = attributes.getNamedItem("start_date");
                    articleAccess.setAudience(audience.getTextContent());

                    if(start_date !=null){
                       articleAccess.setStartDate(start_date.getTextContent());
                    }
                }
            } catch (XPathExpressionException e) {
                log.error("Error", e);
            } catch (TransformerConfigurationException e) {
                log.error("Error", e);
            } catch (TransformerException e) {
                log.error("Error", e);
            }
        }

        if(StringUtils.isBlank(articleAccess.getAudience())){
            if (StringUtils.isNotBlank(pii)) {
            response = connect.get("pii/" + pii + getQueryString());
                log.error("using the fallback access check implementation for article with pii " + pii);

        } else if (StringUtils.isNotBlank(doi)) {
            response = connect.get("doi/" + doi + getQueryString());
                log.error("using the fallback access check implementation for article with doi " + doi);
        }

        if (response != null) {
            try {
                Node node = XMLUtils.getNode(response, getCheckNodeXPath());

                if (node != null) {
                        articleAccess.setAudience(node.getTextContent());
                }
            } catch (XPathExpressionException e) {
                log.error("Error", e);
            }
        }
        }

        // if no permission can be found, set default to restricted
        if(StringUtils.isBlank(articleAccess.getAudience())){
            articleAccess.setAudience("restricted");

            if (StringUtils.isNotBlank(pii)) {
                log.error("fallback found no permissions for article with pii " + pii);

            } else if (StringUtils.isNotBlank(doi)) {
                log.error("fallback found no permissions for article with doi " + doi);
            }
        }

        return articleAccess;
    }
    private boolean publisherIsElsevier(Item item) {
        List<MetadataValue> publisherMetadata = itemService.getMetadataByMetadataString(item,"dc.publisher");
        boolean publisherIsElsevier = false;
        for(MetadataValue metadatum : publisherMetadata){
            if(metadatum.getValue().matches("^(?i)elsevier.*")){
                publisherIsElsevier =true;
            }
        }
        return publisherIsElsevier;
    }

    /**
     * Initializes when check(Item) is called.
     *
     * @return The link, if any, associated with the last item that has been checked.
     */
    public String getLink() {
        String link = null;
        if (response != null) {
            try {
                Node node = XMLUtils.getNode(response, getLinkNodeXPath());
                if (node != null) {
                    link = node.getTextContent();
                }
            } catch (XPathExpressionException e) {
                log.error("Error", e);
            }
        }
        return link;
    }

    private String getQueryString() {
        String key = "apiKey=" + ConfigurationManager.getProperty("external-sources.elsevier.key");
        String accept = "httpAccept=text/xml";

        String params;
        if (useApiKey) {
            params = GeneralUtils.join("&", key, accept);
        } else {
            params = accept;
        }
        if (StringUtils.isNotBlank(params)) {
            params = "?" + params;
        }

        return params;
    }

    protected Set<String> getFalseValues() {
        return falseValues;
    }

    protected Set<String> getTrueValues(){
        return trueValues;
    }

    public void setFalseValues(Set<String> falseValues) {
        this.falseValues = falseValues;
    }

    public void setTrueValues(Set<String> trueValues) {
        this.trueValues = trueValues;
    }

    public boolean isUseApiKey() {
        return useApiKey;
    }

    public void setUseApiKey(boolean useApiKey) {
        this.useApiKey = useApiKey;
    }

    protected abstract String getLinkNodeXPath();

    protected abstract String getCheckNodeXPath();

    protected abstract String getUrlConfigKey();

}
