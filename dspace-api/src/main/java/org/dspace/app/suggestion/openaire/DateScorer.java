/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openaire;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.app.suggestion.SuggestionUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.util.MultiFormatDateParser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@see org.dspace.app.suggestion.oaire.EvidenceScorer} which evaluate ImportRecords
 * based on the distance from a date extracted from the ResearcherProfile (birthday / graduation date)
 * 
 * @author Andrea Bollini (andrea.bollini at 4science dot it)
 *
 */
public class DateScorer implements EvidenceScorer {

    private String birthDateMetadata;

    //private String educationDateMetadata;

    private String minDateMetadata;

    private String maxDateMetadata;

    private int birthDateDelta = 20;
    private int birthDateRange = 50;

    private int educationDateDelta = -3;
    private int educationDateRange = 50;

    @Autowired
    private ItemService itemService;

    private String publicationDateMetadata;

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setBirthDateMetadata(String birthDate) {
        this.birthDateMetadata = birthDate;
    }

    public String getBirthDateMetadata() {
        return birthDateMetadata;
    }
    /*
    public void setEducationDateMetadata(String educationDate) {
        this.educationDateMetadata = educationDate;
    }

    public String getEducationDateMetadata() {
        return educationDateMetadata;
    }
    */

    public void setBirthDateDelta(int birthDateDelta) {
        this.birthDateDelta = birthDateDelta;
    }

    public void setBirthDateRange(int birthDateRange) {
        this.birthDateRange = birthDateRange;
    }

    public void setEducationDateDelta(int educationDateDelta) {
        this.educationDateDelta = educationDateDelta;
    }

    public void setEducationDateRange(int educationDateRange) {
        this.educationDateRange = educationDateRange;
    }

    public void setMaxDateMetadata(String maxDateMetadata) {
        this.maxDateMetadata = maxDateMetadata;
    }

    public void setMinDateMetadata(String minDateMetadata) {
        this.minDateMetadata = minDateMetadata;
    }

    public void setPublicationDateMetadata(String publicationDateMetadata) {
        this.publicationDateMetadata = publicationDateMetadata;
    }

    /**
     * Method which is responsible to evaluate ImportRecord based on the publication date.
     * ImportRecords which have a date outside the defined or calculated expected range will be discarded.
     * 
     * @param importRecord the ExternalDataObject to check
     * @param researcher DSpace item
     * @return the generated evidence or null if the record must be discarded
     */
    @Override
    public SuggestionEvidence computeEvidence(Item researcher, ExternalDataObject importRecord) {
        Integer[] range = calculateRange(researcher);
        if (range == null) {
            return new SuggestionEvidence(this.getClass().getSimpleName(),
                    0,
                    "No assumption was possible about the publication year range. "
                    + "Please consider to set a min/max date in the profile, specify the birthday "
                    + "or education achievements");
        } else {
            String optDate = SuggestionUtils.getFirstEntryByMetadatum(importRecord, publicationDateMetadata);
            int year = getYear(optDate);
            if (year > 0) {
                if ((range[0] == null || year >= range[0]) &&
                    (range[1] == null || year <= range[1])) {
                    return new SuggestionEvidence(this.getClass().getSimpleName(),
                            10,
                            "The publication date is within the expected range [" + range[0] + ", "
                                    + range[1] + "]");
                } else {
                    // outside the range, discard the suggestion
                    return null;
                }
            } else {
                return new SuggestionEvidence(this.getClass().getSimpleName(),
                        0,
                        "No assumption was possible as the publication date is " + (optDate != null
                                ? "unprocessable [" + optDate + "]"
                                : "unknown"));
            }
        }
    }

    /**
     * returns min and max year interval in between it's probably that the researcher
     * actually contributed to the suggested item
     * @param researcher
     * @return
     */
    private Integer[] calculateRange(Item researcher) {
        String minDateStr = getSingleValue(researcher, minDateMetadata);
        int minYear = getYear(minDateStr);
        String maxDateStr = getSingleValue(researcher, maxDateMetadata);
        int maxYear = getYear(maxDateStr);
        if (minYear > 0 && maxYear > 0) {
            return new Integer[] { minYear, maxYear };
        } else {
            String birthDateStr = getSingleValue(researcher, birthDateMetadata);
            int birthDateYear = getYear(birthDateStr);
            int educationDateYear = -1;
            /*
            getListMetadataValues(researcher, educationDateMetadata)
              .stream()
              .mapToInt(x -> getYear(x.getValue()))
              .filter(d -> d > 0)
              .min().orElse(-1);
            */
            if (educationDateYear > 0) {
                return new Integer[] {
                    minYear > 0 ? minYear : educationDateYear + educationDateDelta,
                    maxYear > 0 ? maxYear : educationDateYear + educationDateDelta + educationDateRange
                };
            } else if (birthDateYear > 0) {
                return new Integer[] {
                    minYear > 0 ? minYear : birthDateYear + birthDateDelta,
                    maxYear > 0 ? maxYear : birthDateYear + birthDateDelta + birthDateRange
                };
            } else {
                return null;
            }
        }
    }

    /*
    private List<MetadataValue> getListMetadataValues(Item researcher, String metadataKey) {
        if (metadataKey != null) {
            return itemService.getMetadataByMetadataString(researcher, metadataKey);
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    */

    private String getSingleValue(Item researcher, String metadataKey) {
        if (metadataKey != null) {
            return itemService.getMetadata(researcher, metadataKey);
        }
        return null;
    }

    private int getYear(String birthDateStr) {
        int birthDateYear = -1;
        if (birthDateStr != null) {
            Date birthDate = MultiFormatDateParser.parse(birthDateStr);
            if (birthDate != null) {
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(birthDate);
                birthDateYear = calendar.get(Calendar.YEAR);
            }
        }
        return birthDateYear;
    }
}
