/*
 */
package org.datadryad.rest.models;

import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.identifier.DOIIdentifierProvider;

/**
 *
 * @author1 Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Manuscript {
    public static final String MANUSCRIPT_ID = "manuscriptId";

    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_NEEDS_REVISION = "needs revision";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_INVALID = "invalid";

    public static final List<String> SUBMITTED_STATUSES = Arrays.asList(
            STATUS_SUBMITTED,
            "revision in review",
            "revision under review",
            "in review"
    );

    public static final List<String> ACCEPTED_STATUSES = Arrays.asList(
            STATUS_ACCEPTED
    );

    public static final List<String> REJECTED_STATUSES = Arrays.asList(
            STATUS_REJECTED,
            STATUS_NEEDS_REVISION,
            "transferred",
            "rejected w/o review"
    );

    public static final List<String> PUBLISHED_STATUSES = Arrays.asList(
            STATUS_PUBLISHED
    );

    public static final List<String> NEEDS_REVISION_STATUSES = Arrays.asList(
            STATUS_NEEDS_REVISION
    );

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    @XmlElement(name="abstract")
    @JsonProperty("abstract")
    public String manuscript_abstract;
    public AuthorsList authors = new AuthorsList();
    public CorrespondingAuthor correspondingAuthor = new CorrespondingAuthor();
    private String dryadDataDOI;
    public List<String> keywords = new ArrayList<String>();
    public String manuscriptId = "";
    private String status = STATUS_ACCEPTED; // STATUS_ACCEPTED is the default
    public String title;
    public String publicationDOI;
    public Date publicationDate;
    public String dataReviewURL;
    public String dataAvailabilityStatement;
    public Map<String, String> optionalProperties;
    public Manuscript() {} // JAXB needs this

    public Manuscript(String manuscriptId, String status) {
        this.manuscriptId = manuscriptId;
        this.status = status;
    }

    @JsonIgnore
    private static final Logger log = Logger.getLogger(Manuscript.class);

    @JsonIgnore
    public Organization organization = new Organization();

    public void setDryadDataDOI(String newDOI) {
        this.dryadDataDOI = newDOI;
    }

    public String getDryadDataDOI() {
        // find the first instance of a Dryad DOI in these three fields (in this order of priority)
        return findDryadDOI(dryadDataDOI + "," + dataReviewURL + "," + dataAvailabilityStatement);
    }

    public void setStatus(String newStatus) {
        if (newStatus != null) {
            this.status = newStatus;
        } else {
            this.status = "";
        }
    }

    public String getStatus() {
        if (isAccepted()) {
            return STATUS_ACCEPTED;
        }
        if (isRejected()) {
            return STATUS_REJECTED;
        }
        if (isSubmitted()) {
            return STATUS_SUBMITTED;
        }
        if (isPublished()) {
            return STATUS_PUBLISHED;
        }

        // default is STATUS_ACCEPTED
        return STATUS_INVALID;
    }

    // return what the status really said:
    @JsonIgnore
    public String getLiteralStatus() {
        return this.status;
    }

    // check the status of a manuscript, regardless of what the literal status is
    @JsonIgnore
    public Boolean isSubmitted() {
        return statusIsSubmitted(status);
    }

    @JsonIgnore
    public Boolean isAccepted() {
        return statusIsAccepted(status);
    }

    @JsonIgnore
    public Boolean isRejected() {
        return statusIsRejected(status);
    }

    @JsonIgnore
    public Boolean isNeedsRevision() {
        return statusIsNeedsRevision(status);
    }

    @JsonIgnore
    public Boolean isPublished() {
        return statusIsPublished(status);
    }

    // Convenience methods to compare status strings anywhere to known statuses.
    @JsonIgnore
    public static Boolean statusIsSubmitted(String status) {
        return SUBMITTED_STATUSES.contains(status);
    }

    @JsonIgnore
    public static Boolean statusIsAccepted(String status) {
        return ACCEPTED_STATUSES.contains(status);
    }

    @JsonIgnore
    public static Boolean statusIsRejected(String status) {
        return REJECTED_STATUSES.contains(status);
    }

    @JsonIgnore
    public static Boolean statusIsNeedsRevision(String status) {
        return NEEDS_REVISION_STATUSES.contains(status);
    }

    @JsonIgnore
    public static Boolean statusIsPublished(String status) {
        return PUBLISHED_STATUSES.contains(status);
    }

    @JsonIgnore
    public Boolean isValid() {
        // Required fields are: manuscriptID, status, authors (though author identifiers are optional), and title. All other fields are optional.
        if ((manuscriptId == null) || (manuscriptId.length() == 0)) {
            log.debug("Manuscript is invalid: Manuscript ID not available");
            return false;
        }

        if ((status == null) || (status.length() == 0)) {
            log.debug("Manuscript is invalid: Article Status not available");
            return false;
        }

        if ((authors == null) || (authors.author == null) || (authors.author.size() == 0)) {
            log.debug("Manuscript is invalid: Authors not available");
            return false;
        }

        if ((title == null) || (title.length() == 0)) {
            log.debug("Manuscript is invalid: Title not available");
            return false;
        }

        if (getStatus().equals(STATUS_INVALID)) {
            log.debug("Manuscript is invalid: Article Status " + status + " does not correspond to an accepted value");
            return false;
        }

        // TODO: if corresponding author present, must be one of the authors
        return true;
    }

    private static String findDryadDOI(String searchString) {
        // we need to look for anything matching the form doi:10.5061/dryad.xxxx as well as dx.doi.org/10.5061/dryad.xxxx
        Matcher manuscriptMatcher = Pattern.compile(Pattern.quote(DOIIdentifierProvider.getDryadDOIPrefix()) + "[a-zA-Z0-9]+").matcher(searchString);
        if (manuscriptMatcher.find()) {
            return "doi:" + manuscriptMatcher.group(0);
        } else {
            return null;
        }
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
        this.keywords.add("Science");
        this.keywords.add("Data");
        this.keywords.add("Publishing");
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
}
