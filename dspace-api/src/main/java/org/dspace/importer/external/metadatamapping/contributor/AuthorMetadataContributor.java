/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.CrisConstants;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;

/**
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4science dot it)
 */
public class AuthorMetadataContributor extends SimpleXpathMetadatumContributor {

    private MetadataFieldConfig authname;
    private MetadataFieldConfig orcid;
    private MetadataFieldConfig scopusId;
    private MetadataFieldConfig affiliation;

    private Map<String, String> affId2affName = new HashMap<String, String>();

    @Override
    public Collection<MetadatumDTO> contributeMetadata(OMElement t) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<MetadatumDTO> metadatums = null;
        fillAffillation(t);
        try {
            AXIOMXPath xpath2author = new AXIOMXPath("ns:author");
            addNameSpace(xpath2author);
            List<Object> nodes = xpath2author.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    metadatums = getMetadataOfAuthors((OMElement) el);
                } else {
                    System.err.println("node of type: " + el.getClass());
                }
                if (Objects.nonNull(metadatums)) {
                    for (MetadatumDTO metadatum : metadatums) {
                        values.add(metadatum);
                    }
                }
            }
        } catch (JaxenException e) {
            throw new RuntimeException(e);
        }
        return values;
    }

    private List<MetadatumDTO> getMetadataOfAuthors(OMElement el) throws JaxenException {
        List<MetadatumDTO> metadatums = new ArrayList<MetadatumDTO>();
        AXIOMXPath authnameXPath = new AXIOMXPath("ns:authname");
        AXIOMXPath scopusIdXPath = new AXIOMXPath("ns:authid");
        AXIOMXPath orcidXPath = new AXIOMXPath("ns:orcid");
        AXIOMXPath afidXPath = new AXIOMXPath("ns:afid");
        addNameSpace(authnameXPath);
        addNameSpace(scopusIdXPath);
        addNameSpace(orcidXPath);
        addNameSpace(afidXPath);
        String afid = getValue(el, afidXPath);
        metadatums.add(getMetadata(getValue(el, authnameXPath), this.authname));
        metadatums.add(getMetadata(getValue(el, scopusIdXPath), this.scopusId));
        metadatums.add(getMetadata(getValue(el, orcidXPath), this.orcid));
        metadatums.add(getMetadata(StringUtils.isNotBlank(afid)
                                   ? this.affId2affName.get(afid) : null, this.affiliation));
        return metadatums;
    }

    private MetadatumDTO getMetadata(String value, MetadataFieldConfig metadaConfig) {
        MetadatumDTO metadata = new MetadatumDTO();
        if (StringUtils.isNotBlank(value)) {
            metadata.setValue(value);
        } else {
            metadata.setValue(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
        }
        metadata.setElement(metadaConfig.getElement());
        metadata.setQualifier(metadaConfig.getQualifier());
        metadata.setSchema(metadaConfig.getSchema());
        return metadata;
    }

    private String getValue(OMElement el, AXIOMXPath xPath) throws JaxenException {
        List<OMElement> elements = (List<OMElement>) xPath.selectNodes(el);
        return elements.size() > 0 ? elements.get(0).getText() : null;
    }

    private void fillAffillation(OMElement t) {
        try {
            AXIOMXPath xpath2affilation = new AXIOMXPath("ns:affiliation");
            addNameSpace(xpath2affilation);
            List<Object> nodes = xpath2affilation.selectNodes(t);
            for (Object el : nodes) {
                if (el instanceof OMElement) {
                    fillAffiliation2Name((OMElement) el);
                } else {
                    System.err.println("node of type: " + el.getClass());
                }
            }
        } catch (JaxenException e) {
            throw new RuntimeException(e);
        }
    }

    private void addNameSpace(AXIOMXPath xpath) throws JaxenException {
        for (String ns : prefixToNamespaceMapping.keySet()) {
            xpath.addNamespace(prefixToNamespaceMapping.get(ns), ns);
        }
    }

    private void fillAffiliation2Name(OMElement value) throws JaxenException {
        AXIOMXPath xpath2affilationName = new AXIOMXPath("ns:affilname");
        AXIOMXPath xpath2affilationId = new AXIOMXPath("ns:afid");
        addNameSpace(xpath2affilationName);
        addNameSpace(xpath2affilationId);
        String affilname = getValue(value, xpath2affilationName);
        String afid = getValue(value, xpath2affilationId);
        if (StringUtils.isNotBlank(afid) | StringUtils.isNotBlank(affilname)) {
            affId2affName.put(afid, affilname);
        }
    }

    public MetadataFieldConfig getAuthname() {
        return authname;
    }

    public void setAuthname(MetadataFieldConfig authname) {
        this.authname = authname;
    }

    public MetadataFieldConfig getOrcid() {
        return orcid;
    }

    public void setOrcid(MetadataFieldConfig orcid) {
        this.orcid = orcid;
    }

    public MetadataFieldConfig getScopusId() {
        return scopusId;
    }

    public void setScopusId(MetadataFieldConfig scopusId) {
        this.scopusId = scopusId;
    }

    public MetadataFieldConfig getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(MetadataFieldConfig affiliation) {
        this.affiliation = affiliation;
    }

}