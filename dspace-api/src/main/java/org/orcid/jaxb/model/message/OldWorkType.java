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
package org.orcid.jaxb.model.message;

import javax.xml.bind.annotation.XmlEnumValue;
import java.io.Serializable;

public enum OldWorkType implements Serializable {

    @XmlEnumValue("advertisement")
    ADVERTISEMENT("advertisement"), @XmlEnumValue("audiovisual")
    AUDIOVISUAL("audiovisual"), @XmlEnumValue("bible")
    BIBLE("bible"), @XmlEnumValue("book")
    BOOK("book"), @XmlEnumValue("brochure")
    BROCHURE("brochure"), @XmlEnumValue("cartoon-comic")
    CARTOON_COMIC("cartoon-comic"), @XmlEnumValue("chapter-anthology")
    CHAPTER_ANTHOLOGY("chapter-anthology"), @XmlEnumValue("components")
    COMPONENTS("components"), @XmlEnumValue("conference-proceedings")
    CONFERENCE_PROCEEDINGS("conference-proceedings"), @XmlEnumValue("congressional-publication")
    CONGRESSIONAL_PUBLICATION("congressional-publication"), @XmlEnumValue("court-case")
    COURT_CASE("court-case"), @XmlEnumValue("database")
    DATABASE("database"), @XmlEnumValue("dictionary-entry")
    DICTIONARY_ENTRY("dictionary-entry"), @XmlEnumValue("digital-image")
    DIGITAL_IMAGE("digital-image"), @XmlEnumValue("dissertation-abstract")
    DISSERTATON_ABSTRACT("dissertation-abstract"), @XmlEnumValue("dissertation")
    DISSERTATION("dissertation"), @XmlEnumValue("e-mail")
    EMAIL("e-mail"), @XmlEnumValue("editorial")
    EDITORIAL("editorial"), @XmlEnumValue("electronic-only")
    ELECTRONIC_ONLY("electronic-only"), @XmlEnumValue("encyclopedia-article")
    ENCYCLOPEDIA_ARTICLE("encyclopedia-article"), @XmlEnumValue("executive-order")
    EXECUTIVE_ORDER("executive-order"), @XmlEnumValue("federal-bill")
    FEDERAL_BILL("federal-bill"), @XmlEnumValue("federal-report")
    FEDERAL_REPORT("federal-report"), @XmlEnumValue("federal-rule")
    FEDERAL_RULE("federal-rule"), @XmlEnumValue("federal-statute")
    FEDERAL_STATUTE("federal-statute"), @XmlEnumValue("federal-testimony")
    FEDERAL_TESTIMONY("federal-testimony"), @XmlEnumValue("film-movie")
    FILM_MOVIE("film-movie"), @XmlEnumValue("government-publication")
    GOVERNMENT_PUBLICATION("government-publication"), @XmlEnumValue("interview")
    INTERVIEW("interview"), @XmlEnumValue("journal-article")
    JOURNAL_ARTICLE("journal-article"), @XmlEnumValue("lecture-speech")
    LECTURE_SPEECH("lecture-speech"), @XmlEnumValue("legal")
    LEGAL("legal"), @XmlEnumValue("letter")
    LETTER("letter"), @XmlEnumValue("live-performance")
    LIVE_PERFORMANCE("live-performance"), @XmlEnumValue("magazine-article")
    MAGAZINE_ARTICLE("magazine-article"), @XmlEnumValue("mailing-list")
    MAILING_LIST("mailing-list"), @XmlEnumValue("manuscript")
    MANUSCRIPT("manuscript"), @XmlEnumValue("map-chart")
    MAP_CHART("map-chart"), @XmlEnumValue("musical-recording")
    MUSICAL_RECORDING("musical-recording"), @XmlEnumValue("newsgroup")
    NEWSGROUP("newsgroup"), @XmlEnumValue("newsletter")
    NEWSLETTER("newsletter"), @XmlEnumValue("newspaper-article")
    NEWSPAPER_ARTICLE("newspaper-article"), @XmlEnumValue("non-periodicals")
    NON_PERIODICALS("non-periodicals"), @XmlEnumValue("other")
    OTHER("other"), @XmlEnumValue("pamphlet")
    PAMPHLET("pamphlet"), @XmlEnumValue("painting")
    PAINTING("painting"), @XmlEnumValue("patent")
    PATENT("patent"), @XmlEnumValue("periodicals")
    PERIODICALS("periodicals"), @XmlEnumValue("photograph")
    PHOTOGRAPH("photograph"), @XmlEnumValue("press-release")
    PRESSRELEASE("press-release"), @XmlEnumValue("raw-data")
    RAW_DATA("raw-data"), @XmlEnumValue("religious-text")
    RELIGIOUS_TEXT("religious-text"), @XmlEnumValue("report")
    REPORT("report"), @XmlEnumValue("reports-working-papers")
    REPORTS_WORKING_PAPERS("reports-working-papers"), @XmlEnumValue("review")
    REVIEW("review"), @XmlEnumValue("scholarly-project")
    SCHOLARLY_PROJECT("scholarly-project"), @XmlEnumValue("software")
    SOFTWARE("software"), @XmlEnumValue("standards")
    STANDARDS("standards"), @XmlEnumValue("television-radio")
    TELEVISION_RADIO("television-radio"), @XmlEnumValue("thesis")
    THESIS("thesis"), @XmlEnumValue("web-site")
    WEBSITE("web-site"), @XmlEnumValue("undefined")
    UNDEFINED("undefined");

    private final String value;

    OldWorkType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OldWorkType fromValue(String v) {
        for (OldWorkType c : OldWorkType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
