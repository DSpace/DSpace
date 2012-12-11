package org.dspace.submit.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all data about a publication.
 *
 * @author Amol Bapat
 * @author Ryan Scherle
 *
 */

public class PublicationBean extends DataBean
{
	private static final long serialVersionUID = 1L;

	// indicates whether the metadata for this publication was obtained directly from the journal
	private boolean metadataFromJournal = true;

	private String manuscriptNumber = "";
    private String journalID = "";
    private String journalName = "";
	private String journalISSN = "";
	private String journalVolume = "";
	private String journalNumber = "";
	private String publisher = "";
	private String fullCitation = "";
	private String message = "";
	private String title = "";
	private String pubAbstract = "";
	private String publicationDate = "";
	private String correspondingAuthor = "";
	private String doi = "";
    private String email = "";
    private List<String> authors = new ArrayList<String>();
    private List<String> subjectKeywords = new ArrayList<String>();
    private List<String> taxonomicNames = new ArrayList<String>();
	private List<String> coverageSpatial = new ArrayList<String>();
	private List<String> coverageTemporal = new ArrayList<String>();
	private List<String> datasetBeans = new ArrayList<String>();
	private List<String> datasetHandles = new ArrayList<String>();
	private String publicationDir = ""; //directory where this publication's files are stored, relative to the base directory
    private boolean skipReviewStep = true;

    private String status=null;

    public static final String TYPE_REGULAR = "Regular";
    public static final String TYPE_GR_NOTE = "GR Note";

    private String articleType = TYPE_REGULAR; // Default type is regular
    private String citationTitle = "";
    private String citationAuthors = "";

    public static String STATUS_SUBMITTED="submitted";
    public static String STATUS_IN_REVIEW="in review";
    public static String STATUS_UNDER_REVIEW="under review";
    public static String STATUS_REVISION_IN_REVIEW="revision in review";
    public static String STATUS_REVISION_UNDER_REVIEW="revision under review";
    public static String STATUS_ACCEPTED="accepted";
    public static String STATUS_REJECTED="rejected";
    public static String STATUS_NEEDS_REVISION="needs revision";






	public PublicationBean()
	{
	}

    public String getJournalID() {
	return journalID;
    }

    public void setJournalID(String journalID) {
	if(journalID != null) {
	    this.journalID = journalID.trim();
	}
    }

	public String getJournalName() {
		return journalName;
	}

	public void setJournalName(String journalName) {
		if(journalName != null) {
			this.journalName = journalName.trim();
		}
	}

	public String getManuscriptNumber() {
		return manuscriptNumber;
	}

	public void setManuscriptNumber(String manuscriptNumber) {
		if(manuscriptNumber != null) {
			this.manuscriptNumber = manuscriptNumber.trim();
		}
	}

	public void setDatasetBeans(List<String> datasetBeans) {
			this.datasetBeans = datasetBeans;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		if(message != null) {
			this.message = message.trim();
		}
	}

	public String getCorrespondingAuthor() {
		return correspondingAuthor;
	}

	public void setCorrespondingAuthor(String correspondingAuthor) {
		if(correspondingAuthor != null) {
			this.correspondingAuthor = correspondingAuthor.trim();
		}
	}

	public List<String> getAuthors() {
		return authors;
	}

	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}

	public List<String> getSubjectKeywords() {
		return subjectKeywords;
	}

	public void setSubjectKeywords(List<String> subjectKeywords) {
		this.subjectKeywords = subjectKeywords;
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

	public List<String> getDatasetBeans() {
		return datasetBeans;
	}

	public String getAbstract() {
		return pubAbstract;
	}

	public void setAbstract(String pubAbstract) {
		if(pubAbstract != null) {
			this.pubAbstract = pubAbstract.trim();
		}
	}

	public String getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(String publicationDate) {
		if(publicationDate != null) {
			this.publicationDate = publicationDate.trim();
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if(title != null) {
			this.title = title.trim();
		}
	}

	public String getJournalISSN() {
		return journalISSN;
	}

	public void setJournalISSN(String journalISSN) {
		if(journalISSN != null) {
			this.journalISSN = journalISSN.trim();
		}
	}

	public boolean isMetadataFromJournal() {
		return metadataFromJournal;
	}

	public void setMetadataFromJournal(boolean metadataFromJournal) {
		this.metadataFromJournal = metadataFromJournal;
	}

	public String getDOI() {
		return doi;
	}

	public void setDOI(String doi) {
		if(doi != null) {
			this.doi = doi.trim();
		}
	}

	public String getJournalVolume() {
		return journalVolume;
	}

	public void setJournalVolume(String journalVolume) {
		if(journalVolume != null) {
			this.journalVolume = journalVolume;
		}
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

	public String getFullCitation() {
		return fullCitation;
	}

	public void setFullCitation(String fullCitation) {
		if(fullCitation != null) {
			this.fullCitation = fullCitation.trim();
		}
	}

	public String getPublicationDir() {
		return publicationDir;
	}

	public void setPublicationDir(String publicationDir) {
		this.publicationDir = publicationDir;
	}

	public List<String> getDatasetHandles() {
		return datasetHandles;
	}

	public void setDatasetHandles(List<String> datasetHandles) {
		this.datasetHandles = datasetHandles;
	}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSkipReviewStep() {
        return skipReviewStep;
    }

    public void setSkipReviewStep(boolean skipReviewStep) {
        this.skipReviewStep = skipReviewStep;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
