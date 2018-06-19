/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.metadatamapping.contributor;

import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.springframework.beans.factory.annotation.Required;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Pubmed specific implementation of {@link MetadataContributor}
 * Responsible for generating a set of Date metadata from the retrieved document.
 *
 * @author Philip Vissenaekens (philip at atmire dot com)
 */
public class PubmedDateMetadatumContributor<T> implements MetadataContributor<T> {
    Logger log = Logger.getLogger(PubmedDateMetadatumContributor.class);

    private MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping;

    /* A list of all the dateFormats to attempt, these should be configured to have the most specific first and the more lenient at the back */
    private List<String> dateFormatsToAttempt;


    public List<String> getDateFormatsToAttempt() {
        return dateFormatsToAttempt;
    }
    @Required
    public void setDateFormatsToAttempt(List<String> dateFormatsToAttempt) {
        this.dateFormatsToAttempt = dateFormatsToAttempt;
    }

    private MetadataFieldConfig field;
    private MetadataContributor day;
    private MetadataContributor month;
    private MetadataContributor year;

    /**
     * Set the metadatafieldMapping used in the transforming of a record to actual metadata
     * @param metadataFieldMapping
     */
    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
        day.setMetadataFieldMapping(metadataFieldMapping);
        month.setMetadataFieldMapping(metadataFieldMapping);
        year.setMetadataFieldMapping(metadataFieldMapping);
    }
    /**
     * Initialize an empty PubmedDateMetadatumContributor object
     */
    public PubmedDateMetadatumContributor() {
    }

    /**
     *
     * @param field {@link org.dspace.importer.external.metadatamapping.MetadataFieldConfig} used in mapping
     * @param day a MetadataContributor, representing a day
     * @param month a {@link MetadataContributor}, representing a month
     * @param year a {@link MetadataContributor}, representing a year
     */
    public PubmedDateMetadatumContributor(MetadataFieldConfig field, MetadataContributor day, MetadataContributor month, MetadataContributor year) {
        this.field = field;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    /**
     * Retrieve the metadata associated with the given object.
     * The code will loop over the different dates and attempt to format them using the configured dateFormats to attempt.
     * For each date, once a format is successful, this result is used. Make sure that dateFormatsToAttempt is configured from most restrictive to most lenient to try and get the most precise result
     * @param t A class to retrieve metadata from.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        List<MetadatumDTO> values = new LinkedList<>();


        try {
            LinkedList<MetadatumDTO> yearList = (LinkedList<MetadatumDTO>) year.contributeMetadata(t);
            LinkedList<MetadatumDTO> monthList = (LinkedList<MetadatumDTO>) month.contributeMetadata(t);
            LinkedList<MetadatumDTO> dayList = (LinkedList<MetadatumDTO>) day.contributeMetadata(t);

            for (int i = 0; i < yearList.size(); i++) {
                DCDate dcDate = null;
                String dateString = "";

                if (monthList.size() > i && dayList.size() > i) {
                    dateString = yearList.get(i).getValue() + "-" + monthList.get(i).getValue() + "-" + dayList.get(i).getValue();
                } else if (monthList.size() > i) {
                    dateString = yearList.get(i).getValue() + "-" + monthList.get(i).getValue();
                } else {
                    dateString = yearList.get(i).getValue();
                }

                int j = 0 ;
                // Use the first dcDate that has been formatted (Config should go from most specific to most lenient)
                while (j<dateFormatsToAttempt.size() && dcDate==null){
                    String dateFormat = dateFormatsToAttempt.get(j);
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
                        Date date = formatter.parse(dateString);
                        dcDate = new DCDate(date);
                    } catch (ParseException e) {
                        // Multiple dateformats can be configured, we don't want to print the entire stacktrace every time one of those formats fails.
                        log.info("Failed parsing "+dateString+" using the following format: "+dateFormat+ ", check the configured dataformats in config/spring/api/pubmed-integration.xml");
                    }
                    j++;
                }

                if (dcDate != null) {
                    values.add(metadataFieldMapping.toDCValue(field, dcDate.toString()));
                }
            }
        } catch (Exception e) {
            log.error("Error", e);
        }
        return values;
    }

    /**
     * Return the MetadataFieldConfig used while retrieving MetadatumDTO
     * @return MetadataFieldConfig
     */
    public MetadataFieldConfig getField() {
        return field;
    }

    /**
     * Setting the MetadataFieldConfig
     * @param field MetadataFieldConfig used while retrieving MetadatumDTO
     */
    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    /**
     * Retrieve the day from the object
     * @return {@link MetadataContributor}, representing a day
     */
    public MetadataContributor getDay() {
        return day;
    }

    /**
     * Set a day ({@link MetadataContributor}) to this object
     * @param day a {@link MetadataContributor}, representing a day
     */
    public void setDay(MetadataContributor day) {
        this.day = day;
    }

    /**
     * Retrieve the month from the object
     * @return {@link MetadataContributor}, representing a month
     */
    public MetadataContributor getMonth() {
        return month;
    }

    /**
     * Set a month ({@link MetadataContributor}) to this object
     * @param month a {@link MetadataContributor}, representing a month
     */
    public void setMonth(MetadataContributor month) {
        this.month = month;
    }

    /**
     * Retrieve the year from the object
     * @return {@link MetadataContributor}, representing a year
     */
    public MetadataContributor getYear() {
        return year;
    }

    /**
     * Set a year ({@link MetadataContributor}) to this object
     * @param year a {@link MetadataContributor}, representing a year
     */
    public void setYear(MetadataContributor year) {
        this.year = year;
    }

}