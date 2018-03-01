/*
 */
package org.datadryad.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.datadryad.api.DryadJournalConcept;

import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.lang.Object;
import java.lang.Override;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Journal {
    public String fullName = "";
    public String issn = "";
    public String website = "";
    public String description = "";
    protected DryadJournalConcept dryadJournalConcept;

    public Journal() {
    }

    public Journal(DryadJournalConcept dryadJournalConcept) {
        fullName = dryadJournalConcept.getFullName();
        issn = dryadJournalConcept.getISSN();
        website = dryadJournalConcept.getWebsite();
        description = dryadJournalConcept.getDescription();
        this.dryadJournalConcept = dryadJournalConcept;
    }

    @JsonIgnore
    public Boolean isValid() {
        return (fullName != null && fullName.length() > 0 && issn != null && issn.length() > 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            Journal journal = (Journal) o;
            if (this.issn.equals(journal.issn) && this.fullName.equals(journal.fullName)) {
                return true;
            }
        }
        return false;
    }

    public static class JournalLookupSerializer extends JsonSerializer<Journal> {
        @Override
        public void serialize(Journal journal, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            jGen.writeStartObject();
            jGen.writeStringField("issn", journal.issn);
            if (journal.dryadJournalConcept.getAllowEmbargo()) {
                jGen.writeStringField("embargo", "Y");
            } else {
                jGen.writeStringField("embargo", "N");
            }
            if (journal.dryadJournalConcept.getPublicationBlackout()) {
                jGen.writeStringField("hidden", "Y");
            } else {
                jGen.writeStringField("hidden", "N");
            }
            if (journal.dryadJournalConcept.getIntegrated()) {
                jGen.writeStringField("integrated", "Y");
            } else {
                jGen.writeStringField("integrated", "N");
            }
            jGen.writeStringField("title", journal.fullName);
            String sponsor = journal.dryadJournalConcept.getSponsorName();
            if (!"".equals(sponsor)) {
                jGen.writeStringField("sponsor", sponsor);
            } else {
                jGen.writeStringField("sponsor", "$120");
            }
            String submission = "A";
            if (journal.dryadJournalConcept.getAllowReviewWorkflow()) {
                submission = "R";
            }
            // allowDataFirst is a subset of allowReview, so it needs to be checked after.
            if (journal.dryadJournalConcept.getAllowDataFirstWorkflow()) {
                submission = "D";
            }
            jGen.writeStringField("submission", submission);
            jGen.writeEndObject();
        }
    }
}
