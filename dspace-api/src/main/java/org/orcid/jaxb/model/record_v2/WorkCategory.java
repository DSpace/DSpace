/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.jaxb.model.record_v2;

import javax.xml.bind.annotation.XmlEnumValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum WorkCategory {
    @XmlEnumValue("publication")
    PUBLICATION("publication", WorkType.BOOK_CHAPTER, WorkType.BOOK_REVIEW, WorkType.BOOK, WorkType.DICTIONARY_ENTRY, WorkType.DISSERTATION, WorkType.EDITED_BOOK,
            WorkType.ENCYCLOPEDIA_ENTRY, WorkType.JOURNAL_ARTICLE, WorkType.JOURNAL_ISSUE, WorkType.MAGAZINE_ARTICLE, WorkType.MANUAL, WorkType.NEWSLETTER_ARTICLE,
            WorkType.NEWSPAPER_ARTICLE, WorkType.ONLINE_RESOURCE, WorkType.REPORT, WorkType.RESEARCH_TOOL, WorkType.SUPERVISED_STUDENT_PUBLICATION, WorkType.TEST,
            WorkType.TRANSLATION, WorkType.WEBSITE, WorkType.WORKING_PAPER), @XmlEnumValue("conference")
    CONFERENCE("conference", WorkType.CONFERENCE_ABSTRACT, WorkType.CONFERENCE_PAPER, WorkType.CONFERENCE_POSTER), @XmlEnumValue("intellectual_property")
    INTELLECTUAL_PROPERTY("intellectual_property", WorkType.DISCLOSURE, WorkType.LICENSE, WorkType.PATENT, WorkType.REGISTERED_COPYRIGHT, WorkType.TRADEMARK), @XmlEnumValue("other_output")
    OTHER_OUTPUT("other_output", WorkType.ARTISTIC_PERFORMANCE, WorkType.DATA_SET, WorkType.INVENTION, WorkType.LECTURE_SPEECH, WorkType.OTHER,
            WorkType.RESEARCH_TECHNIQUE, WorkType.SPIN_OFF_COMPANY, WorkType.STANDARDS_AND_POLICY, WorkType.TECHNICAL_STANDARD, WorkType.UNDEFINED);

    private List<WorkType> types = new ArrayList<WorkType>();
    private String value;

    private WorkCategory(String value, WorkType... types) {
        this.value = value;
        for (WorkType subType : types) {
            this.types.add(subType);
        }
    }

    public String value() {
        return value;
    }

    public List<WorkType> getSubTypes() {
        return Collections.unmodifiableList(types);
    }

    public static WorkCategory fromValue(String v) {
        for (WorkCategory c : WorkCategory.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static WorkCategory fromWorkType(WorkType type) {
        if (PUBLICATION.getSubTypes().contains(type))
            return PUBLICATION;
        else if (CONFERENCE.getSubTypes().contains(type))
            return CONFERENCE;
        else if (INTELLECTUAL_PROPERTY.getSubTypes().contains(type))
            return INTELLECTUAL_PROPERTY;
        else if (OTHER_OUTPUT.getSubTypes().contains(type))
            return OTHER_OUTPUT;
        else 
            throw new IllegalArgumentException("Invalid work type provided: " + type.name());
    }
}
