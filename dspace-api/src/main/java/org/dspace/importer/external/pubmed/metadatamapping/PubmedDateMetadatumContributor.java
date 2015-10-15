/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.pubmed.metadatamapping;

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
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 06/07/15
 * Time: 13:48
 */
public class PubmedDateMetadatumContributor<T> implements MetadataContributor<T> {
    Logger log = Logger.getLogger(PubmedDateMetadatumContributor.class);

    private MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping;

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

    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> metadataFieldMapping) {
        this.metadataFieldMapping = metadataFieldMapping;
        day.setMetadataFieldMapping(metadataFieldMapping);
        month.setMetadataFieldMapping(metadataFieldMapping);
        year.setMetadataFieldMapping(metadataFieldMapping);
    }

    public PubmedDateMetadatumContributor() {
    }

    public PubmedDateMetadatumContributor(MetadataFieldConfig field, MetadataContributor day, MetadataContributor month, MetadataContributor year) {
        this.field = field;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        List<MetadatumDTO> values = new LinkedList<MetadatumDTO>();


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


                for (String dateFormat : dateFormatsToAttempt) {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
                        Date date = formatter.parse(dateString);
                        dcDate = new DCDate(date);
                    } catch (ParseException e) {
                        log.error(e.getMessage(), e);
                    }
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

    public MetadataFieldConfig getField() {
        return field;
    }

    public void setField(MetadataFieldConfig field) {
        this.field = field;
    }

    public MetadataContributor getDay() {
        return day;
    }

    public void setDay(MetadataContributor day) {
        this.day = day;
    }

    public MetadataContributor getMonth() {
        return month;
    }

    public void setMonth(MetadataContributor month) {
        this.month = month;
    }

    public MetadataContributor getYear() {
        return year;
    }

    public void setYear(MetadataContributor year) {
        this.year = year;
    }

}