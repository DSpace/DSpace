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
import java.util.LinkedList;
import java.util.List;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * This Contributor is helpful to avoid the limit of the Live Import Framework.
 * In Live Import, one dc schema/element/qualifier could be associate with one and
 * only one MetadataContributor, because the map they're saved in use dc entity as key.
 * 
 * In fact, in this implementation we use the MetadataFieldConfig present in this MultipleMetadataContributor
 * contributor, but the data (values of the dc metadatum) will be loaded using any of the contributor defined
 * in the List metadatumContributors, by iterating over them.
 * 
 * @see org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping<RecordType>
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class MultipleMetadataContributor<T> implements MetadataContributor<T> {

    private MetadataFieldConfig field;

    private List<MetadataContributor> metadatumContributors;

    /**
     * Empty constructor
     */
    public MultipleMetadataContributor() {
    }

    /**
     * @param field                 {@link org.dspace.importer.external.metadatamapping.MetadataFieldConfig} used in
     *                              mapping
     * @param metadatumContributors A list of MetadataContributor
     */
    public MultipleMetadataContributor(MetadataFieldConfig field, List<MetadataContributor> metadatumContributors) {
        this.field = field;
        this.metadatumContributors = (LinkedList<MetadataContributor>) metadatumContributors;
    }

    /**
     * Set the metadatafieldMapping used in the transforming of a record to actual metadata
     *
     * @param metadataFieldMapping the new mapping.
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping) {
        for (MetadataContributor metadatumContributor : metadatumContributors) {
            metadatumContributor.setMetadataFieldMapping(metadataFieldMapping);
        }
    }


    /**
     * a separate Metadatum object is created for each index of Metadatum returned from the calls to
     * MetadatumContributor.contributeMetadata(t) for each MetadatumContributor in the metadatumContributors list.
     * All of them have as dc schema/element/qualifier the values defined in MetadataFieldConfig.
     * 
     * @param t the object we are trying to translate
     * @return a collection of metadata got from each MetadataContributor
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        Collection<MetadatumDTO> values = new ArrayList<>();
        for (MetadataContributor metadatumContributor : metadatumContributors) {
            Collection<MetadatumDTO> metadata = metadatumContributor.contributeMetadata(t);
            values.addAll(metadata);
        }
        changeDC(values);
        return values;
    }

    /**
     * This method does the trick of this implementation.
     * It changes the DC schema/element/qualifier of the given Metadatum into
     * the ones present in this contributor.
     * In this way, the contributors in metadatumContributors could have any dc values,
     * because this method remap them all.
     * 
     * @param the list of metadata we want to remap
     */
    private void changeDC(Collection<MetadatumDTO> values) {
        for (MetadatumDTO dto : values) {
            dto.setElement(field.getElement());
            dto.setQualifier(field.getQualifier());
            dto.setSchema(field.getSchema());
        }
    }

    /**
     * Return the MetadataFieldConfig used while retrieving MetadatumDTO
     *
     * @return MetadataFieldConfig
     */
    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     *
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Return the List of MetadataContributor objects set to this class
     *
     * @return metadatumContributors, list of MetadataContributor
     */
    public List<MetadataContributor> getMetadatumContributors() {
        return metadatumContributors;
    }

    /**
     * Set the List of MetadataContributor objects set to this class
     *
     * @param metadatumContributors A list of MetadatumContributor classes
     */
    public void setMetadatumContributors(List<MetadataContributor> metadatumContributors) {
        this.metadatumContributors = metadatumContributors;
    }
}
