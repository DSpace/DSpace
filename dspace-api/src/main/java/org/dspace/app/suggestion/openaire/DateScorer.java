/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openaire;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.app.suggestion.SuggestionUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
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

    /**
     * if available it should contains the metadata field key in the form (schema.element[.qualifier]) that contains
     * the birth date of the researcher
     */
    private String birthDateMetadata;

    /**
     * if available it should contains the metadata field key in the form (schema.element[.qualifier]) that contains
     * the date of graduation of the researcher. If the metadata has multiple values the min will be used
     */
    private String educationDateMetadata;

    /**
     * The minimal age that is expected for a researcher to be a potential author of a scholarly contribution
     * (i.e. the minimum delta from the publication date and the birth date)
     */
    private int birthDateDelta = 20;

    /**
     * The maximum age that is expected for a researcher to be a potential author of a scholarly contribution
     * (i.e. the maximum delta from the publication date and the birth date)
     */
    private int birthDateRange = 50;

    /**
     * The number of year from/before the graduation that is expected for a researcher to be a potential
     * author of a scholarly contribution (i.e. the minimum delta from the publication date and the first
     * graduation date)
     */
    private int educationDateDelta = -3;

    /**
     * The maximum scientific longevity that is expected for a researcher from its graduation to be a potential
     * author of a scholarly contribution (i.e. the maximum delta from the publication date and the first
     * graduation date)
     */
    private int educationDateRange = 50;

    @Autowired
    private ItemService itemService;

    /**
     * the metadata used in the publication to track the publication date (i.e. dc.date.issued)
     */
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

    public void setEducationDateMetadata(String educationDate) {
        this.educationDateMetadata = educationDate;
    }

    public String getEducationDateMetadata() {
        return educationDateMetadata;
    }

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

    public void setPublicationDateMetadata(String publicationDateMetadata) {
        this.publicationDateMetadata = publicationDateMetadata;
    }

    /**
     * Method which is responsible to evaluate ImportRecord based on the publication date.
     * ImportRecords which have a date outside the defined or calculated expected range will be discarded.
     * {@link DateScorer#birthDateMetadata}, {@link DateScorer#educationDateMetadata}
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
                    + "Please consider setting your birthday in your profile.");
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
        String birthDateStr = getSingleValue(researcher, birthDateMetadata);
        int birthDateYear = getYear(birthDateStr);
        int educationDateYear = getListMetadataValues(researcher, educationDateMetadata).stream()
                .mapToInt(x -> getYear(x.getValue())).filter(d -> d > 0).min().orElse(-1);
        if (educationDateYear > 0) {
            return new Integer[] {
                educationDateYear + educationDateDelta,
                educationDateYear + educationDateDelta + educationDateRange
            };
        } else if (birthDateYear > 0) {
            return new Integer[] {
                birthDateYear + birthDateDelta,
                birthDateYear + birthDateDelta + birthDateRange
            };
        } else {
            return null;
        }
    }

    private List<MetadataValue> getListMetadataValues(Item researcher, String metadataKey) {
        if (metadataKey != null) {
            return itemService.getMetadataByMetadataString(researcher, metadataKey);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

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
