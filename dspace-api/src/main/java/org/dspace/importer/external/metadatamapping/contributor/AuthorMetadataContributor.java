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

import org.apache.commons.lang.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Scopus specific implementation of {@link MetadataContributor}
 * Responsible for generating the ScopusID, orcid, author name and affiliationID
 * from the retrieved item.
 *
 * @author Boychuk Mykhaylo (boychuk.mykhaylo at 4science dot it)
 */
public class AuthorMetadataContributor extends SimpleXpathMetadatumContributor {

    private static final Namespace NAMESPACE = Namespace.getNamespace("http://www.w3.org/2005/Atom");

    private MetadataFieldConfig orcid;
    private MetadataFieldConfig scopusId;
    private MetadataFieldConfig authname;
    private MetadataFieldConfig affiliation;

    private Map<String, String> affId2affName = new HashMap<String, String>();

    /**
     * Retrieve the metadata associated with the given object.
     * Depending on the retrieved node (using the query),
     * different types of values will be added to the MetadatumDTO list.
     *
     * @param element    A class to retrieve metadata from.
     * @return           A collection of import records. Only the ScopusID, orcid, author name and affiliation
     *                     of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(Element element) {
        List<MetadatumDTO> values = new LinkedList<>();
        List<MetadatumDTO> metadatums = null;
        fillAffillation(element);
        try {
            List<Element> nodes = element.getChildren("author", NAMESPACE);
            for (Element el : nodes) {
                metadatums = getMetadataOfAuthors(el);
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

    /**
     * Retrieve the the ScopusID, orcid, author name and affiliationID
     * metadata associated with the given element object.
     * If the value retrieved from the element is empty
     * it is set PLACEHOLDER_PARENT_METADATA_VALUE
     * 
     * @param element           A class to retrieve metadata from
     * @throws JaxenException   If Xpath evaluation failed
     */
    private List<MetadatumDTO> getMetadataOfAuthors(Element element) throws JaxenException {
        List<MetadatumDTO> metadatums = new ArrayList<MetadatumDTO>();
        Element authname = element.getChild("authname", NAMESPACE);
        Element scopusId = element.getChild("authid", NAMESPACE);
        Element orcid = element.getChild("orcid", NAMESPACE);
        Element afid = element.getChild("afid", NAMESPACE);

        addMetadatum(metadatums, getMetadata(getElementValue(authname), this.authname));
        addMetadatum(metadatums, getMetadata(getElementValue(scopusId), this.scopusId));
        addMetadatum(metadatums, getMetadata(getElementValue(orcid), this.orcid));
        addMetadatum(metadatums, getMetadata(StringUtils.isNotBlank(afid.getValue())
                                 ? this.affId2affName.get(afid.getValue()) : null, this.affiliation));
        return metadatums;
    }

    private void addMetadatum(List<MetadatumDTO> list, MetadatumDTO metadatum) {
        if (Objects.nonNull(metadatum)) {
            list.add(metadatum);
        }
    }

    private String getElementValue(Element element) {
        if (Objects.nonNull(element)) {
            return element.getValue();
        }
        return StringUtils.EMPTY;
    }

    private MetadatumDTO getMetadata(String value, MetadataFieldConfig metadaConfig) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        MetadatumDTO metadata = new MetadatumDTO();
        metadata.setElement(metadaConfig.getElement());
        metadata.setQualifier(metadaConfig.getQualifier());
        metadata.setSchema(metadaConfig.getSchema());
        metadata.setValue(value);
        return metadata;
    }

    private void fillAffillation(Element element) {
        try {
            List<Element> nodes = element.getChildren("affiliation", NAMESPACE);
            for (Element el : nodes) {
                fillAffiliation2Name(el);
            }
        } catch (JaxenException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillAffiliation2Name(Element element) throws JaxenException {
        Element affilationName = element.getChild("affilname", NAMESPACE);
        Element affilationId = element.getChild("afid", NAMESPACE);
        if (Objects.nonNull(affilationId) && Objects.nonNull(affilationName)) {
            affId2affName.put(affilationId.getValue(), affilationName.getValue());
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