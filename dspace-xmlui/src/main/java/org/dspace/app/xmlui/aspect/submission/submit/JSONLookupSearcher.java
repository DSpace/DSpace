/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Collection;
import java.util.*;
import javax.xml.*;
import javax.xml.parsers.*;
import org.apache.avalon.framework.parameters.*;
import org.apache.cocoon.*;
import org.apache.cocoon.environment.*;
import org.apache.cocoon.generation.*;
import org.apache.cocoon.xml.dom.*;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.metadatamapping.*;
import org.dspace.importer.external.service.*;
import org.dspace.utils.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Created by jonas - jonas@atmire.com on 06/11/15.
 */
public class JSONLookupSearcher extends AbstractGenerator {
    private Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);


    private ImportService importService;

    private String lookupURI = null;

    private static Logger log = Logger.getLogger(JSONLookupSearcher.class);
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private AbstractImportMetadataSourceService source;

    private Request request;
    private Context context;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters par) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, par);
        request = ObjectModelHelper.getRequest(objectModel);
        source = sources.get(request.getParameter("source"));
        try {
            context = ContextUtil.obtainContext(objectModel);
        } catch (SQLException e) {
            log.error(e.getMessage(),e);
        }
        importService = new DSpace().getServiceManager().getServiceByName("importService", ImportService.class);
    }

    @Override
    public void generate() throws IOException, SAXException, ProcessingException {

        String query = "";
        Map<String, String> fields = source.getImportFields();
        for (String field : fields.keySet()) {
            String queryString = request.getParameter(field);

            if(StringUtils.isNotBlank(queryString)){
                if(StringUtils.isNotBlank(query.toString())) {
                    query += " AND ";
                }
                query += fields.get(field);
                if (StringUtils.isNotBlank(fields.get(field))) {
                    query += ("(" + queryString + ")");
                } else {
                    query += queryString ;
                }
            }
                }

        try {
            query = URLEncoder.encode(query, "UTF-8");
            query = query.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }

        int start = 0;
        String startString = request.getParameter("start");
        if(StringUtils.isNotBlank(startString)){
            int parsedStart = Integer.parseInt(startString);
            if(parsedStart>=0){
                start = parsedStart;
            }
        }

        try {
            int total = importService.getNbRecords(source.getImportSource(), query);
            Collection<ImportRecord> records = importService.getRecords(source.getImportSource(), query, start, 20);

            MetadataFieldConfig importIdField = new MetadataFieldConfig(source.getIdField());
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document document = docBuilder.newDocument();

            Element rootnode = document.createElement("root");
            document.appendChild(rootnode);
            rootnode.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:i18n", WingConstants.I18N.URI);

            Element totalNode = document.createElement("total");
            totalNode.setTextContent(String.valueOf(total));
            rootnode.appendChild(totalNode);

            Element startNode = document.createElement("start");
            startNode.setTextContent(String.valueOf(start));
            rootnode.appendChild(startNode);

            Element idFieldNode = document.createElement("identifier");
            idFieldNode.setTextContent(importIdField.getField());
            rootnode.appendChild(idFieldNode);

            Element recordsNode = document.createElement("records");
            recordsNode.setAttribute("array", "true");
            rootnode.appendChild(recordsNode);
            recordsNode.setAttribute("array", "true");

            for (ImportRecord record : records) {
                Element recordWrapperNode = document.createElement("recordWrapper");
                recordWrapperNode.setAttribute("object", "true");
                recordsNode.appendChild(recordWrapperNode);

                Element recordNode = document.createElement("record");
                recordNode.setAttribute("namedObject", "true");

                HashMap<String,Element> metadatumValueNodes = new HashMap();

                for (MetadatumDTO metadatum : record.getValueList()) {
                    if (StringUtils.isNotBlank(metadatum.getValue())) {
                        if (!metadatumValueNodes.containsKey(metadatum.getField())) {
                            Element metadatumNode = document.createElement(metadatum.getField());
                        metadatumNode.setAttribute("array", "true");
                            metadatumValueNodes.put(metadatum.getField(), metadatumNode);

                            if (metadatum.getField().equals(importIdField.getField())) {
                                Iterator<Item> iterator = itemService.findByMetadataField(context, importIdField.getSchema(), importIdField.getElement(), importIdField.getQualifier(), metadatum.getValue());

                            if(iterator.hasNext()){
                                Element existsInDSpaceNode = document.createElement("imported");
                                existsInDSpaceNode.setTextContent("true");
                                recordNode.appendChild(existsInDSpaceNode);
                            }
                        }
                    }

                    Element metadatumValueNode = document.createElement("metadatumValue");
                        String value = metadatum.getValue();
                        if(value.startsWith("0")) {
                            value = "\"" + value + "\"";
                        }
                        metadatumValueNode.setTextContent(value);

                        metadatumValueNodes.get(metadatum.getField()).appendChild(metadatumValueNode);
                    }
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
}
