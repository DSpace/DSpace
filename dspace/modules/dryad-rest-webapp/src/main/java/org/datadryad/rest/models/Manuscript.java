/*
 */
package org.datadryad.rest.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 *
 * @author1 Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
public class Manuscript {
    public static final String MANUSCRIPT_ID = "manuscriptId";

    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_NEEDS_REVISION = "needs revision";
    public static final String STATUS_PUBLISHED = "published";

    public static final List<String> MANUSCRIPT_STATUSES = Arrays.asList(
            STATUS_SUBMITTED,
            STATUS_ACCEPTED,
            STATUS_REJECTED,
            STATUS_NEEDS_REVISION,
            STATUS_PUBLISHED
    );

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    @XmlElement(name="abstract")
    @JsonProperty("abstract")
    public String manuscript_abstract;
    public AuthorsList authors;
    public CorrespondingAuthor correspondingAuthor = new CorrespondingAuthor();
    public String dryadDataDOI;
    public KeywordsList keywords;
    public String manuscriptId;
    public String status;
    public String title;
    public String publicationDOI;
    public Date publicationDate;
    public String dataReviewURL;
    public String dataAvailabilityStatement;
    public Manuscript() {} // JAXB needs this

    public Manuscript(String manuscriptId, String status) {
        this.manuscriptId = manuscriptId;
        this.status = status;
    }

    @JsonIgnore
    public Organization organization;

    @JsonIgnore
    public Boolean isValid() {
        // TODO: Check other validations
        // if corresponding author present, must be one of the authors
        // Required fields are: manuscriptID, status, authors (though author identifiers are optional), and title. All other fields are optional.

        return (
                (manuscriptId != null && manuscriptId.length() > 0) &&
                (status != null && status.length() > 0) &&
                (authors != null && authors.author != null && authors.author.size() > 0) &&
                (title != null && title.length() > 0) &&
                MANUSCRIPT_STATUSES.contains(status)
                );
    }

    public void configureTestValues() {
        this.manuscript_abstract = "This is the abstract of the article";
        List<Author> localAuthors = new ArrayList<Author>();
        Author author1 = new Author();
        author1.familyName = "Smith";
        author1.givenNames = "John";
        author1.identifier = "0000-0000-0000-0000";
        author1.identifierType = "orcid";
        localAuthors.add(author1);
        Author author2 = new Author();
        author2.familyName = "Jones";
        author2.givenNames = "Sally";
        localAuthors.add(author2);
        AuthorsList localAuthorsList = new AuthorsList();
        localAuthorsList.author = localAuthors;
        this.authors = localAuthorsList;
        CorrespondingAuthor localCorrespondingAuthor = new CorrespondingAuthor();
        Address address = new Address();
        address.addressLine1 = "123 Main St";
        address.addressLine2 = "Box 40560";
        address.addressLine3 = "";
        address.city = "Anytown";
        address.country = "United States";
        address.state = "North Carolina";
        address.zip = "27511";
        localCorrespondingAuthor.address = address;
        localCorrespondingAuthor.author = author1;
        localCorrespondingAuthor.email = "smith@example.com";
        this.correspondingAuthor = localCorrespondingAuthor;
        this.dryadDataDOI = "doi:10.5061/dryad.abc123";
        KeywordsList localKeywords = new KeywordsList();
        localKeywords.keyword.add("Science");
        localKeywords.keyword.add("Data");
        localKeywords.keyword.add("Publishing");
        this.keywords = localKeywords;
        this.manuscriptId = "MS12345";
        this.publicationDOI = "doi:10.12345/987cba";
        try {
            this.publicationDate = sdf.parse("2014-10-17");
        } catch (ParseException ex) {
            System.err.println("Parse exception" + ex);
        }
        this.status = STATUS_SUBMITTED;
        this.title = "Title of article 1";
    }

    @JsonIgnore
    public Boolean isRejected() {
        return STATUS_REJECTED.equals(this.status) || STATUS_NEEDS_REVISION.equals(this.status);
    }

    @JsonIgnore
    public List<String> getKeywords() {
        if(keywords != null && keywords.keyword != null) {
            return keywords.keyword;
        } else {
            return new ArrayList<String>();
        }
    }

}
