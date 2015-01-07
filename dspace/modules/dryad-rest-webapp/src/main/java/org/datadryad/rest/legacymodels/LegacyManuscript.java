/*
 */
package org.datadryad.rest.legacymodels;

import javax.xml.bind.annotation.XmlRootElement;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.Manuscript;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement(name="DryadEmailSubmission")
public class LegacyManuscript {
    public String Abstract;
    public String Journal;
    public String Article_Status;
    public LegacyAuthorsList Authors = new LegacyAuthorsList();
    public LegacySubmissionMetadata Submission_Metadata = new LegacySubmissionMetadata();
    public String Corresponding_Author;
    public String Email;
    public String Address_Line_1;
    public String Address_Line_2;
    public String Address_Line_3;
    public String City, State, Country, Zip;
    public LegacyClassifications Classification = new LegacyClassifications();

    public LegacyManuscript() {}
    public LegacyManuscript(Manuscript manuscript, String journalName) {
        this.Journal = journalName;
        this.Submission_Metadata.Manuscript = manuscript.manuscriptId;
        this.Submission_Metadata.Article_Title = manuscript.title;
        this.Article_Status = manuscript.status;
        for(Author author : manuscript.authors.author) {
            this.Authors.Author.add(author.fullName());
        }
        if(manuscript.correspondingAuthor != null) {
            if(manuscript.correspondingAuthor.author != null) {
                this.Corresponding_Author = manuscript.correspondingAuthor.author.fullName();
            }
            this.Email = manuscript.correspondingAuthor.email;
            if(manuscript.correspondingAuthor.address != null) {
                this.Address_Line_1 = manuscript.correspondingAuthor.address.addressLine1;
                this.Address_Line_2 = manuscript.correspondingAuthor.address.addressLine2;
                this.Address_Line_3 = manuscript.correspondingAuthor.address.addressLine3;
                this.City = manuscript.correspondingAuthor.address.city;
                this.State = manuscript.correspondingAuthor.address.state;
                this.Country = manuscript.correspondingAuthor.address.country;
                this.Zip = manuscript.correspondingAuthor.address.zip;
            }
        }
        this.Abstract = manuscript.manuscript_abstract;
        for(String keyword : manuscript.getKeywords()) {
            this.Classification.keyword.add(keyword);
        }
    }

}
