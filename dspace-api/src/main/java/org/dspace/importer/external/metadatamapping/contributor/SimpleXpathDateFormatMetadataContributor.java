/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * This contributor can be used when parsing an XML file,
 * particularly to extract a date and convert it to a specific format.
 * In the variable dateFormatFrom the read format should be configured,
 * instead in the variable dateFormatTo the format you want to obtain.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SimpleXpathDateFormatMetadataContributor extends SimpleXpathMetadatumContributor {

    private DateFormat dateFormatFrom;
    private DateFormat dateFormatTo;

    public void setDateFormatFrom(String dateFormatFrom) {
        this.dateFormatFrom = new SimpleDateFormat(dateFormatFrom);
    }

    public void setDateFormatTo(String dateFormatTo) {
        this.dateFormatTo = new SimpleDateFormat(dateFormatTo);
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<Namespace> namespaces = new ArrayList<Namespace>();
        for (String ns : prefixToNamespaceMapping.keySet()) {
            namespaces.add(Namespace.getNamespace(prefixToNamespaceMapping.get(ns), ns));
        }
        XPathExpression<Object> xpath = XPathFactory.instance()
                          .compile(query,Filters.fpassthrough(), null, namespaces);
        List<Object> nodes = xpath.evaluate(element);
        for (Object el : nodes) {
            if (el instanceof Element) {
                values.add(getMetadatum(field, ((Element) el).getText()));
            } else if (el instanceof Attribute) {
                values.add(getMetadatum(field, ((Attribute) el).getValue()));
            } else if (el instanceof String) {
                values.add(getMetadatum(field, (String) el));
            } else if (el instanceof Text) {
                values.add(metadataFieldMapping.toDCValue(field, ((Text) el).getText()));
            } else {
                System.err.println("node of type: " + el.getClass());
            }
        }
        return values;
    }

    private MetadatumDTO getMetadatum(MetadataFieldConfig field, String value) {
        MetadatumDTO dcValue = new MetadatumDTO();
        if (field == null) {
            return null;
        }
        try {
            dcValue.setValue(dateFormatTo.format(dateFormatFrom.parse(value)));
        } catch (ParseException e) {
            dcValue.setValue(value);
        }
        dcValue.setElement(field.getElement());
        dcValue.setQualifier(field.getQualifier());
        dcValue.setSchema(field.getSchema());
        return dcValue;
    }

}