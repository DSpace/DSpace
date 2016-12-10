/*
 */
package org.datadryad.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.JournalUtils;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author1 Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Package {
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private AuthorsList authors = new AuthorsList();
    private CorrespondingAuthor correspondingAuthor = new CorrespondingAuthor();
    private Integer itemID;

    @JsonIgnore
    private DryadJournalConcept journalConcept;

    @JsonIgnore
    private DryadDataPackage dataPackage;


    @JsonIgnore
    private static final Logger log = Logger.getLogger(Package.class);

    static {
    }

    public Package() {} // JAXB needs this

    public Package(DryadDataPackage dataPackage) {
        this.dataPackage = dataPackage;
        this.itemID = dataPackage.getItem().getID();
    }

    public String getPublicationDOI() {
        String publicationDOI = null;
        try {
            publicationDOI = dataPackage.getPublicationDOI();
        } catch (SQLException e) {
            log.error("couldn't find publication DOI for item " + itemID);
        }
        if (publicationDOI == null) {
            publicationDOI = "";
        }
        return publicationDOI;
    }

    public Date getPublicationDate() {
        return dataPackage.getDateAccessioned();
    }

    public AuthorsList getAuthors() {
        List<Author> authors = getAuthorList();
        AuthorsList authorsList = new AuthorsList();
        for (Author a : authors) {
            authorsList.author.add(a);
        }
        return authorsList;
    }

    @JsonIgnore
    private List<Author> getAuthorList() {
        ArrayList<Author> authors = new ArrayList<Author>();
        try {
            List<String> authorStrings = dataPackage.getAuthors();
            for (String a : authorStrings) {
                Author author = new Author(a);
                authors.add(author);
            }
        } catch (SQLException e) {
            log.error("couldn't find authors for item " + itemID);
        }
        return authors;
    }

    public List<String> getKeywords() {
        try {
            return dataPackage.getKeywords();
        } catch (SQLException e) {
            log.error("couldn't find keywords for item " + itemID);
        }
        return null;
    }

    public String getDryadDOI() {
        return dataPackage.getDryadDOI();
    }

    @JsonIgnore
    public DryadJournalConcept getJournalConcept() {
        DryadJournalConcept journalConcept = null;
        try {
            journalConcept = JournalUtils.getJournalConceptByJournalName(dataPackage.getPublicationName());
        } catch (SQLException e) {
            log.error("couldn't find journal concept for item " + itemID);
        }
        return journalConcept;
    }

    public String getTitle() {
        try {
            return dataPackage.getTitle();
        } catch (SQLException e) {
            log.error("couldn't find title for item " + itemID);
        }
        return null;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "";
        }
    }
}
