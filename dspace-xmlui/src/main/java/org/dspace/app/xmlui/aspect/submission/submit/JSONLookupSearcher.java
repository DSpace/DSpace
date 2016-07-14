/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.submission.submit;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.generation.AbstractGenerator;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.core.Context;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.ImportService;
import org.dspace.utils.DSpace;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by jonas - jonas@atmire.com on 06/11/15.
 */
public class JSONLookupSearcher extends AbstractGenerator {
    private ImportService importService;

    private String lookupURI = null;

    private static Logger log = Logger.getLogger(JSONLookupSearcher.class);
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Request request;
    private Context context;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, par);
        request = ObjectModelHelper.getRequest(objectModel);
        try {
            context = ContextUtil.obtainContext(objectModel);
        } catch (SQLException e) {
            log.error(e.getMessage(),e);
        }
        importService = new DSpace().getServiceManager().getServiceByName("importService", ImportService.class);
    }

    @Override
    public void generate() throws IOException, SAXException, ProcessingException {
        String query = request.getParameter("search");

        int start = 0;
        String startString = request.getParameter("start");
        if(StringUtils.isNotBlank(startString)){
            int parsedStart = Integer.parseInt(startString);
            if(parsedStart>=0){
                start = parsedStart;
            }
        }

        try {
            int total = importService.getNbRecords(getLookupURI(), query);
            Collection<ImportRecord> records = importService.getRecords(getLookupURI(), query, start, 20);

            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            org.w3c.dom.Document document = docBuilder.newDocument();

            Element rootnode = document.createElement("root");
            document.appendChild(rootnode);
            rootnode.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:i18n", WingConstants.I18N.URI);

            Element totalNode = document.createElement("total");
            totalNode.setTextContent(String.valueOf(total));
            rootnode.appendChild(totalNode);

            Element startNode = document.createElement("start");
            startNode.setTextContent(String.valueOf(start));
            rootnode.appendChild(startNode);

            Element recordsNode = document.createElement("records");
            recordsNode.setAttribute("array", "true");
            rootnode.appendChild(recordsNode);
            recordsNode.setAttribute("array", "true");

            MetadataFieldConfig importIdField = new DSpace().getServiceManager().getServiceByName("lookupID", MetadataFieldConfig.class);

            for (ImportRecord record : records) {
                Element recordWrapperNode = document.createElement("recordWrapper");
                recordWrapperNode.setAttribute("object", "true");
                recordsNode.appendChild(recordWrapperNode);

                Element recordNode = document.createElement("record");
                recordNode.setAttribute("namedObject", "true");

                HashMap<String,Element> metadatumValueNodes = new HashMap();

                for (MetadatumDTO metadatum : record.getValueList()) {
                    if(!metadatumValueNodes.containsKey(getField(metadatum))) {
                        Element metadatumNode = document.createElement(getField(metadatum));
                        metadatumNode.setAttribute("array", "true");
                        metadatumValueNodes.put(getField(metadatum), metadatumNode);

                        if (getField(metadatum).equals(importIdField.getField())) {
                                Iterator<Item> iterator = itemService.findByMetadataField(context, importIdField.getSchema(), importIdField.getElement(), importIdField.getQualifier(), metadatum.getValue());

                            if(iterator.hasNext()){
                                Element existsInDSpaceNode = document.createElement("imported");
                                existsInDSpaceNode.setTextContent("true");
                                recordNode.appendChild(existsInDSpaceNode);
                            }
                        }
                    }

                    Element metadatumValueNode = document.createElement("metadatumValue");
                    metadatumValueNode.setTextContent(metadatum.getValue());

                    metadatumValueNodes.get(getField(metadatum)).appendChild(metadatumValueNode);
                }

                for (Element element : metadatumValueNodes.values()) {
                    recordNode.appendChild(element);
                }

                recordWrapperNode.appendChild(recordNode);
            }

            DOMStreamer streamer = new DOMStreamer(contentHandler, lexicalHandler);
            streamer.stream(document);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String getField(MetadatumDTO metadatum) {
        return metadatum.getSchema()+"."+metadatum.getElement()+((metadatum.getQualifier()!=null)?"."+metadatum.getQualifier():"");
    }

    public String getLookupURI() {
        if(lookupURI ==null){
            lookupURI = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("publication-lookup.url", "*");
        }
        return lookupURI;
    }

    public void setLookupURI(String lookupURI) {
        this.lookupURI = lookupURI;
    }
}
