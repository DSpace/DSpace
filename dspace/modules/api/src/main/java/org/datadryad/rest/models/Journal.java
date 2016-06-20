/*
 */
package org.datadryad.rest.models;

import org.datadryad.api.DryadJournalConcept;

import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.lang.Object;
import java.lang.Override;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Journal {
    public static final String JOURNAL_CODE = "organizationCode";
    public Integer conceptID;
    public String organizationCode = "";
    public String organizationName = "";
    public String organizationISSN = "";

    public Journal() {
    }

    public Journal(DryadJournalConcept dryadJournalConcept) {
        conceptID = new Integer(dryadJournalConcept.getConceptID());
        organizationCode = dryadJournalConcept.getJournalID();
        organizationName = dryadJournalConcept.getFullName();
        organizationISSN = dryadJournalConcept.getISSN();
    }

    public Boolean isValid() {
        return (organizationCode != null && organizationCode.length() > 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            Journal org = (Journal) o;
            if (this.organizationCode.equals(org.organizationCode) && this.organizationName.equals(org.organizationName) && this.organizationISSN.equals(org.organizationISSN)) {
                return true;
            }
        }
        return false;
    }
}
