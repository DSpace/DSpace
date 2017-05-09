/*
 */
package org.datadryad.rest.models;

import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.JournalUtils;
import org.dspace.identifier.DOIIdentifierProvider;
import org.datadryad.rest.legacymodels.LegacyManuscript;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.content.authority.Choices;

/**
 *
 * @author1 Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Manuscript {
    // article-specific data:
    public static final String JOURNAL_CODE = "Journal_Code";
    public static final String JOURNAL = "Journal";
    public static final String ABSTRACT = "Abstract";
    public static final String AUTHORS = "Authors";
    public static final String ARTICLE_STATUS = "Article_Status";
    public static final String SKIP_REVIEW = "Skip_Review";
    public static final String MANUSCRIPT = "Manuscript_ID";
    public static final String ARTICLE_TITLE = "Article_Title";
    public static final String CORRESPONDING_AUTHOR = "Corresponding_Author";
    public static final String CORRESPONDING_AUTHOR_ORCID = "Corresponding_Author_ORCID";
    public static final String EMAIL = "Email";
    public static final String ADDRESS_LINE_1 = "Address_Line_1";
    public static final String ADDRESS_LINE_2 = "Address_Line_2";
    public static final String ADDRESS_LINE_3 = "Address_Line_3";
    public static final String CITY = "City";
    public static final String STATE = "State";
    public static final String COUNTRY = "Country";
    public static final String ZIP = "Zip";
    public static final String CLASSIFICATION = "Classification";
    public static final String DRYAD_DOI = "Dryad_DOI";
    public static final String PUBLICATION_DOI = "Publication_DOI";
    public static final String FULL_CITATION = "Full_Citation";
    public static final String CITATION_TITLE = "Citation_Title";
    public static final String JOURNAL_VOLUME = "Journal_Volume";
    public static final String JOURNAL_NUMBER = "Journal_Number";
    public static final String PUBLICATION_DATE = "Publication_Date";
    public static final String TAXONOMIC_NAMES = "Taxonomic_Names";
    public static final String COVERAGE_SPATIAL = "Coverage_Spatial";
    public static final String COVERAGE_TEMPORAL = "Coverage_Temporal";
    public static final String TYPE_REGULAR = "Regular";
    public static final String TYPE_GR_NOTE = "GR Note";

    // journal-specific data:
    public static final String ISSN = "ISSN";
    public static final String JOURNAL_PUBLISHER = "Journal_Publisher";

    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_NEEDS_REVISION = "needs revision";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_INVALID = "invalid";

    public static final List<String> VALID_STATUSES = Arrays.asList(
            STATUS_SUBMITTED, STATUS_ACCEPTED, STATUS_REJECTED, STATUS_NEEDS_REVISION, STATUS_PUBLISHED, STATUS_INVALID
    );

    public static final List<String> SUBMITTED_STATUSES = Arrays.asList(
            STATUS_SUBMITTED,
            STATUS_NEEDS_REVISION,
            "revision in review",
            "revision under review",
            "in review"
    );

    public static final List<String> ACCEPTED_STATUSES = Arrays.asList(
            STATUS_ACCEPTED
    );

    public static final List<String> REJECTED_STATUSES = Arrays.asList(
            STATUS_REJECTED,
            "transferred",
            "rejected w/o review"
    );

    public static final List<String> PUBLISHED_STATUSES = Arrays.asList(
            STATUS_PUBLISHED
    );

    public static final List<String> NEEDS_REVISION_STATUSES = Arrays.asList(
            STATUS_NEEDS_REVISION
    );

    @JsonIgnore
    private static Properties journalMetadata;

    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    @XmlElement(name="abstract")
    @JsonProperty("abstract")
    private String manuscript_abstract = "";
    private AuthorsList authors = new AuthorsList();
    private CorrespondingAuthor correspondingAuthor = new CorrespondingAuthor();
    private String dryadDataDOI;
    private String manuscriptId = "";
    private String status = "";
    private String title = "";
    private String publicationDOI = "";
    private Date publicationDate;
    private String dataReviewURL = "";
    private String dataAvailabilityStatement = "";
    public Map<String, String> optionalProperties = new LinkedHashMap<String, String>();
    @JsonIgnore
    private Integer internalID = 0;

    // indicates whether the metadata for this publication was obtained directly from the journal
    @JsonIgnore
    private boolean metadataFromJournal = true;

    @JsonIgnore
    private DryadJournalConcept journalConcept;

    private List<String> keywords = new ArrayList<String>();
    // from PublicationBean
    private String journalVolume = "";
    private String journalNumber = "";
    private String publisher = "";
    private String fullCitation = "";
    private String pages = "";
    @JsonIgnore
    private String message = "";

    private List<String> taxonomicNames = new ArrayList<String>();
    private List<String> coverageSpatial = new ArrayList<String>();
    private List<String> coverageTemporal = new ArrayList<String>();
    @JsonIgnore
    private String articleType = Manuscript.TYPE_REGULAR; // Default type is regular
    @JsonIgnore
    private String citationTitle = "";
    @JsonIgnore
    private String citationAuthors = "";

    @JsonIgnore
    private static final Logger log = Logger.getLogger(Manuscript.class);

    static {
        journalMetadata = new Properties();

        journalMetadata.setProperty(Manuscript.JOURNAL, "prism.publicationName");
        journalMetadata.setProperty(Manuscript.JOURNAL_VOLUME, "dc.relation.ispartofseries");
        journalMetadata.setProperty(Manuscript.FULL_CITATION, "dc.identifier.citation");
        journalMetadata.setProperty(Manuscript.ARTICLE_TITLE, "dc.title");
        journalMetadata.setProperty(Manuscript.ABSTRACT, "dc.description");
        journalMetadata.setProperty(Manuscript.CORRESPONDING_AUTHOR, "dc.contributor.correspondingAuthor");
        journalMetadata.setProperty(Manuscript.PUBLICATION_DOI, "dc.relation.isreferencedby");
        journalMetadata.setProperty(Manuscript.AUTHORS, "dc.contributor.author");
        journalMetadata.setProperty(Manuscript.CLASSIFICATION, "dc.subject");
        journalMetadata.setProperty(Manuscript.TAXONOMIC_NAMES, "dwc.ScientificName");
        journalMetadata.setProperty(Manuscript.COVERAGE_SPATIAL, "dc.coverage.spatial");
        journalMetadata.setProperty(Manuscript.COVERAGE_TEMPORAL, "dc.coverage.temporal");
        journalMetadata.setProperty(Manuscript.SKIP_REVIEW, "workflow.submit.skipReviewStage");
        journalMetadata.setProperty(Manuscript.MANUSCRIPT, "dc.identifier.manuscriptNumber");
        journalMetadata.setProperty(Manuscript.CITATION_TITLE, "dryad.citationTitle");
        journalMetadata.setProperty(Manuscript.PUBLICATION_DATE, "dc.date.issued");
        journalMetadata.setProperty(Manuscript.ISSN, "dc.identifier.issn");
        journalMetadata.setProperty(Manuscript.JOURNAL_PUBLISHER, "dc.publisher");
    }

    public Manuscript() {} // JAXB needs this

    public Manuscript(DryadJournalConcept journalConcept) {
        this.journalConcept = journalConcept;
    }

    public Manuscript(LegacyManuscript legacyManuscript) {
        DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(legacyManuscript.Journal_Code);
        this.setJournalConcept(journalConcept);
        // Required fields are: manuscriptID, status, authors (though author identifiers are optional), and title. All other fields are optional.
        this.manuscriptId = legacyManuscript.Submission_Metadata.Manuscript;
        this.title = legacyManuscript.Submission_Metadata.Article_Title;
        this.setStatus(legacyManuscript.Article_Status);
        for (String author : legacyManuscript.Authors.Author) {
            this.authors.author.add(new Author(author));
        }
        if (legacyManuscript.Abstract != null) {
            this.manuscript_abstract = legacyManuscript.Abstract;
        }
        if (legacyManuscript.Corresponding_Author != null) {
            this.correspondingAuthor = new CorrespondingAuthor();
            this.correspondingAuthor.author = new Author(legacyManuscript.Corresponding_Author);
            if (legacyManuscript.Email != null) {
                this.correspondingAuthor.email = legacyManuscript.Email;
            }
            if (legacyManuscript.Address_Line_1 != null) {
                this.correspondingAuthor.address.addressLine1 = legacyManuscript.Address_Line_1;
            }
            if (legacyManuscript.Address_Line_2 != null) {
                this.correspondingAuthor.address.addressLine2 = legacyManuscript.Address_Line_2;
            }
            if (legacyManuscript.Address_Line_3 != null) {
                this.correspondingAuthor.address.addressLine3 = legacyManuscript.Address_Line_3;
            }
            if (legacyManuscript.City != null) {
                this.correspondingAuthor.address.city = legacyManuscript.City;
            }
            if (legacyManuscript.State != null) {
                this.correspondingAuthor.address.state = legacyManuscript.State;
            }
            if (legacyManuscript.Country != null) {
                this.correspondingAuthor.address.country = legacyManuscript.Country;
            }
            if (legacyManuscript.Zip != null) {
                this.correspondingAuthor.address.zip = legacyManuscript.Zip;
            }
        }
        if (legacyManuscript.Classification != null) {
            this.keywords = new ArrayList<String>();
            for (String keyword : legacyManuscript.Classification.keyword) {
                this.keywords.add(keyword);
            }
        }
    }

    public String getManuscriptId() {
        if (manuscriptId == null) {
            manuscriptId = "";
        }
        return manuscriptId;
    }

    public void setInternalID(Integer internalID) {
        this.internalID = internalID;
    }

    public Integer getInternalID() {
        return this.internalID;
    }

    public void setManuscriptId(String manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public String getAbstract() {
        return manuscript_abstract;
    }

    public void setAbstract(String msAbstract) {
        this.manuscript_abstract = msAbstract;
    }

    public CorrespondingAuthor getCorrespondingAuthor() {
        return correspondingAuthor;
    }

    @JsonIgnore
    public String getCorrespondingAuthorFullName() {
        String fullname = null;
        if (this.correspondingAuthor != null && this.correspondingAuthor.author != null) {
            fullname = this.correspondingAuthor.author.getHTMLFullName();
        }
        return fullname;
    }

    public void setCorrespondingAuthor(CorrespondingAuthor author) {
        this.correspondingAuthor = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublicationDOI() {
        if (publicationDOI == null) {
            publicationDOI = "";
        }
        return publicationDOI;
    }

    public void setPublicationDOI(String doi) {
        if (doi == null || "".equals(doi)) {
            this.publicationDOI = "";
        } else if (doi.startsWith("doi:")) {
            this.publicationDOI = doi;
        } else {
            this.publicationDOI = "doi:" + doi;
        }
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date date) {
        this.publicationDate = date;
    }

    public String getDataReviewURL() {
        return dataReviewURL;
    }

    public void setDataReviewURL(String url) {
        this.dataReviewURL = url;
    }

    public String getDataAvailabilityStatement() {
        return dataAvailabilityStatement;
    }

    public void setDataAvailabilityStatement(String dataAvailabilityStatement) {
        this.dataAvailabilityStatement = dataAvailabilityStatement;
    }

    public AuthorsList getAuthors() {
        return authors;
    }

    @JsonIgnore
    public List<Author> getAuthorList() {
        return this.authors.author;
    }

    public void setAuthors(AuthorsList authorsList) {
        this.authors = authorsList;
    }

    @JsonIgnore
    public void setAuthorsFromList(List<Author> authorList) {
        if (this.authors == null) {
            this.authors = new AuthorsList();
        }
        this.authors.author.addAll(authorList);
    }

    @JsonIgnore
    public void addAuthor(Author author) {
        if (this.authors == null) {
            this.authors = new AuthorsList();
        }
        this.authors.author.add(author);
    }

    public String getDryadDataDOI() {
        // find the first instance of a Dryad DOI in these three fields (in this order of priority)
        return findDryadDOI(dryadDataDOI + "," + dataReviewURL + "," + dataAvailabilityStatement);
    }

    public void setDryadDataDOI(String newDOI) {
        this.dryadDataDOI = newDOI;
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
        // Required fields are: status, authors (though author identifiers are optional), and title. All other fields are optional.

        // Updated 04/28/16: Manuscripts can come from CrossRef API, which means that they don't have a manuscript ID.
//        if ((manuscriptId == null) || (manuscriptId.length() == 0)) {
//            log.debug("Manuscript is invalid: Manuscript ID not available");
//            return false;
//        }

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

    @JsonIgnore
    public static Boolean statusIsValid(String status) {
        if (status == null) {
            return false;
        }
        return VALID_STATUSES.contains(status);
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if(message != null) {
            this.message = message.trim();
        }
    }

    public List<String> getTaxonomicNames() {
        return taxonomicNames;
    }

    public void setTaxonomicNames(List<String> taxonomicNames) {
        this.taxonomicNames = taxonomicNames;
    }

    public List<String> getCoverageSpatial() {
        return coverageSpatial;
    }

    public void setCoverageSpatial(List<String> coverageSpatial) {
        this.coverageSpatial = coverageSpatial;
    }

    public List<String> getCoverageTemporal() {
        return coverageTemporal;
    }

    public void setCoverageTemporal(List<String> coverageTemporal) {
        this.coverageTemporal = coverageTemporal;
    }

    @JsonIgnore
    public String getJournalName() {
        return journalConcept.getFullName();
    }

    @JsonIgnore
    public String getJournalISSN() {
        return journalConcept.getISSN();
    }

    public boolean isMetadataFromJournal() {
        return metadataFromJournal;
    }

    public void setMetadataFromJournal(boolean metadataFromJournal) {
        this.metadataFromJournal = metadataFromJournal;
    }

    public String getJournalVolume() {
        return journalVolume;
    }

    public void setJournalVolume(String journalVolume) {
        if(journalVolume != null) {
            this.journalVolume = journalVolume;
        }
    }

    @JsonIgnore
    public DryadJournalConcept getJournalConcept() {
        return journalConcept;
    }

    @JsonIgnore
    public String getJournalID() {
        return journalConcept.getJournalID();
    }

    @JsonIgnore
    public void setJournalConcept(DryadJournalConcept journalConcept) {
        this.journalConcept = journalConcept;
    }

    public String getJournalNumber() {
        return journalNumber;
    }

    public void setJournalNumber(String journalNumber) {
        if(journalNumber != null) {
            this.journalNumber = journalNumber.trim();
        }
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        if(publisher != null) {
            this.publisher = publisher.trim();
        }
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getPages() {
        return this.pages;
    }

    public String getFullCitation() {
        if ("".equals(fullCitation)) {
            // there won't be a full citation to make if there is no year of publication.
            if (publicationDate == null) {
                return "";
            }
            // Authors (Year) Title. Journal Volume: Pages. URL.
            StringBuilder citation = new StringBuilder();
            // prepare the authors
            ArrayList<String> authorStrings = new ArrayList<String>();
            for (Author a : authors.author) {
                StringBuilder authorString = new StringBuilder();
                String authorFamilyName = a.getHTMLFamilyName();
                if (authorFamilyName == null) {
                    authorFamilyName = "";
                }
                authorString.append(authorFamilyName);
                authorString.append(" ");
                String authorGivenName = a.getHTMLGivenNames();
                if (authorGivenName == null) {
                    authorGivenName = "";
                }
                for (String givenName : StringUtils.split(authorGivenName, " ")) {
                    authorString.append(StringUtils.left(givenName,1));
                }
                authorStrings.add(authorString.toString());
            }
            citation.append(StringUtils.join(authorStrings.toArray(),", "));

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");
            String year = dateFormat.format(getPublicationDate());

            citation.append(" (");
            citation.append(year);
            citation.append(") ");
            citation.append(getTitle().trim());
            citation.append(". ");
            citation.append(getJournalName().trim());
            if (!"".equals(getJournalVolume())) {
                citation.append(" ");
                citation.append(getJournalVolume());
                if (!"".equals(getJournalNumber())) {
                    citation.append("(");
                    citation.append(getJournalNumber());
                    citation.append(")");
                }
                citation.append(": ");
                citation.append(getPages());
                citation.append(".");
            } else {
                citation.append(", online in advance of print.");
            }
            fullCitation = StringEscapeUtils.unescapeHtml(citation.toString());
        }
        return fullCitation;
    }

    public boolean isSkipReviewStep() {
        if (isSubmitted() || isNeedsRevision()) {
            return false;
        }
        return true;
    }

    public String getArticleType() {
        return articleType;
    }

    public void setArticleType(String articleType) {
        if(articleType != null) {
            this.articleType = articleType;
        }
    }

    public String getCitationTitle() {
        return citationTitle;
    }

    public void setCitationTitle(String citationTitle) {
        if(citationTitle != null) {
            this.citationTitle = citationTitle;
        }
    }

    public String getCitationAuthors() {
        return citationAuthors;
    }

    public void setCitationAuthors(String citationAuthors) {
        if(citationAuthors != null) {
            this.citationAuthors = citationAuthors;
        }
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String>keywords) {
        this.keywords = keywords;
    }

    /**
     Import metadata from the journal settings into the data package item.
     **/
    public void propagateMetadataToItem(Context context, Item item) {
        // These values are common to both Article Types
        int journalConfidence = Choices.CF_UNSET;
        if (journalConcept.isAccepted()) {
            journalConfidence = Choices.CF_ACCEPTED;
        }
        addSingleMetadataValueFromJournal(context, item, Manuscript.JOURNAL, journalConcept.getFullName(), journalConcept.getIdentifier(), journalConfidence);
        addSingleMetadataValueFromJournal(context, item, Manuscript.ISSN, journalConcept.getISSN());

        addSingleMetadataValueFromJournal(context, item, Manuscript.JOURNAL_VOLUME, this.getJournalVolume());
        addSingleMetadataValueFromJournal(context, item, Manuscript.ABSTRACT, manuscript_abstract);
        addSingleMetadataValueFromJournal(context, item, Manuscript.CORRESPONDING_AUTHOR, this.getCorrespondingAuthorFullName());
        addSingleMetadataValueFromJournal(context, item, Manuscript.PUBLICATION_DOI, publicationDOI);
        if (publicationDate != null) {
            addSingleMetadataValueFromJournal(context, item, Manuscript.PUBLICATION_DATE, sdf.format(publicationDate));
        }
        addSingleMetadataValueFromJournal(context, item, Manuscript.JOURNAL_PUBLISHER, this.getPublisher());
        addSingleMetadataValueFromJournal(context, item, Manuscript.MANUSCRIPT, this.getManuscriptId());
        addSingleMetadataValueFromJournal(context, item, Manuscript.SKIP_REVIEW, String.valueOf(this.isSkipReviewStep()));
        ArrayList<String> authors = new ArrayList<String>();
        for (Author a : this.authors.author) {
            authors.add(a.getHTMLFullName());
        }
        addMultiMetadataValueFromJournal(context, item, Manuscript.AUTHORS, authors);
        addMultiMetadataValueFromJournal(context, item, Manuscript.CLASSIFICATION, keywords);
        addMultiMetadataValueFromJournal(context, item, Manuscript.TAXONOMIC_NAMES, this.getTaxonomicNames());
        addMultiMetadataValueFromJournal(context, item, Manuscript.COVERAGE_SPATIAL, this.getCoverageSpatial());
        addMultiMetadataValueFromJournal(context, item, Manuscript.COVERAGE_TEMPORAL, this.getCoverageTemporal());

        // These values differ based on the Article Type
        if (this.getArticleType().equals(Manuscript.TYPE_GR_NOTE)) {
            String articleTitle = String.format("\"%s\" in %s", title, this.getCitationTitle());
            addSingleMetadataValueFromJournal(context, item, Manuscript.ARTICLE_TITLE, articleTitle);
            addSingleMetadataValueFromJournal(context, item, Manuscript.CITATION_TITLE, this.getCitationTitle());
            // Citation Authors are not stored in the Item
        } else { // Assume Regular
            addSingleMetadataValueFromJournal(context, item, Manuscript.ARTICLE_TITLE, title);
        }
        log.info("journal_id=" + journalConcept.getJournalID() + ",ms=" + this.getManuscriptId());
    }

    private void addSingleMetadataValueFromJournal(Context ctx, Item publication, String key, String value, String auth_id, int confidence){
        String mdString = journalMetadata.getProperty(key);
        if (mdString == null) {
            log.error("error importing field from journal: Could not retrieve a metadata field for journal getter: " + key);
            return;
        }

        if (!"".equals(value)) {
            publication.addMetadata(mdString, null, value, auth_id, confidence);
        }

    }

    private void addSingleMetadataValueFromJournal(Context ctx, Item publication, String key, String value){
        addSingleMetadataValueFromJournal(ctx, publication, key, value, null, 0);
    }

    private void addMultiMetadataValueFromJournal(Context ctx, Item publication, String key, List<String> values){
        String mdString = journalMetadata.getProperty(key);
        if (mdString == null) {
            log.error("error importing field from journal: Could not retrieve a metadata field for journal getter: " + key);
            return;
        }

        if (values != null && 0 < values.size()) {
            String[] valArray = values.toArray(new String[values.size()]);
            String[] authArray = new String[values.size()];
            int[] confArray = new int[values.size()];
            publication.addMetadata(mdString, null, valArray, authArray, confArray);
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
        Author author1 = new Author("Smith", "John");
        author1.setIdentifier("0000-0000-0000-0000");
        author1.setIdentifierType("orcid");
        localAuthors.add(author1);
        Author author2 = new Author("Jones", "Sally");
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
