/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper class used to accommodate for the possibility of correlations between multiple MetadatumContributor objects
 * @author Philip Vissenaekens (philip at atmire dot com)
 */
public class CombinedMetadatumContributor<T> implements MetadataContributor<T> {
    private MetadataFieldConfig field;

    private LinkedList<MetadataContributor> metadatumContributors;

    private String separator;

    private MetadataFieldMapping<T,MetadataContributor<T>> metadataFieldMapping;

    /**
     * Initialize an empty CombinedMetadatumContributor object
     */
    public CombinedMetadatumContributor() {
    }

    /**
     *
     *
     * @param field {@link org.dspace.importer.external.metadatamapping.MetadataFieldConfig} used in mapping
     * @param metadatumContributors A list of MetadataContributor
     * @param separator A separator used to differentiate between different values
     */
    public CombinedMetadatumContributor(MetadataFieldConfig field, List<MetadataContributor> metadatumContributors, String separator) {
        this.field = field;
        this.metadatumContributors = (LinkedList<MetadataContributor>) metadatumContributors;
        this.separator = separator;
    }

    /**
     * Set the metadatafieldMapping used in the transforming of a record to actual metadata
     *
     * @param metadataFieldMapping the new mapping.
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;

        for (MetadataContributor metadatumContributor : metadatumContributors) {
            metadatumContributor.setMetadataFieldMapping(metadataFieldMapping);
        }
    }

    /**
     * a separate Metadatum object is created for each index of Metadatum returned from the calls to
     * MetadatumContributor.contributeMetadata(t) for each MetadatumContributor in the metadatumContributors list.
     * We assume that each contributor returns the same amount of Metadatum objects
     *
     * @param t the object we are trying to translate
     * @return a collection of metadata composed by each MetadataContributor
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        List<MetadatumDTO> values=new LinkedList<>();

        LinkedList<LinkedList<MetadatumDTO>> metadatumLists = new LinkedList<>();

        for (MetadataContributor metadatumContributor : metadatumContributors) {
            LinkedList<MetadatumDTO> metadatums = (LinkedList<MetadatumDTO>) metadatumContributor.contributeMetadata(t);
            metadatumLists.add(metadatums);
        }

        for (int i = 0; i<metadatumLists.getFirst().size();i++) {

            StringBuilder value = new StringBuilder();

            for (LinkedList<MetadatumDTO> metadatums : metadatumLists) {
                value.append(metadatums.get(i).getValue());

                if (!metadatums.equals(metadatumLists.getLast())) {
                    value.append(separator);
                }
            }
            values.add(metadataFieldMapping.toDCValue(field, value.toString()));
        }

        return values;
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
    public LinkedList<MetadataContributor> getMetadatumContributors() {
        return metadatumContributors;
    }

    /**
     * Set the List of MetadataContributor objects set to this class
     *
     * @param metadatumContributors A list of MetadatumContributor classes
     */
    public void setMetadatumContributors(LinkedList<MetadataContributor> metadatumContributors) {
        this.metadatumContributors = metadatumContributors;
    }

    /**
     * Return the separator used to differentiate between distinct values
     *
     * @return the separator used to differentiate between distinct values
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Set the separator used to differentiate between distinct values
     *
     * @param separator
     *     separator used to differentiate between distinct values
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
