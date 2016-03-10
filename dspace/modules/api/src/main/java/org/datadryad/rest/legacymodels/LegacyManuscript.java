/*
 */
package org.datadryad.rest.legacymodels;

import javax.xml.bind.annotation.XmlRootElement;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.CorrespondingAuthor;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement(name="DryadEmailSubmission")
public class LegacyManuscript {
    public String Journal;
    public String Journal_Code;
    public String Article_Status;
    public LegacyAuthorsList Authors = new LegacyAuthorsList();
    public LegacySubmissionMetadata Submission_Metadata = new LegacySubmissionMetadata();
    public String Corresponding_Author;
    public String Email;
    public String Address_Line_1;
    public String Address_Line_2;
    public String Address_Line_3;
    public String City, State, Country, Zip;
    public String Abstract;
    public LegacyClassifications Classification = new LegacyClassifications();
    public Map<String,String> optionalProperties;

    public LegacyManuscript() {}
    public LegacyManuscript(Manuscript manuscript) {
        this.Journal = manuscript.getOrganization().organizationName;
        this.Journal_Code = manuscript.getOrganization().organizationCode;
        this.Submission_Metadata.Manuscript = manuscript.getManuscriptId();
        this.Submission_Metadata.Article_Title = manuscript.getTitle();
        this.Article_Status = manuscript.getLiteralStatus();
        for (Author author : manuscript.getAuthors().author) {
            this.Authors.Author.add(author.fullName());
        }
        CorrespondingAuthor correspondingAuthor = manuscript.getCorrespondingAuthor();
        if(correspondingAuthor != null) {
            if(correspondingAuthor.author != null) {
                this.Corresponding_Author = correspondingAuthor.author.fullName();
            }
            this.Email = correspondingAuthor.email;
            if(correspondingAuthor.address != null) {
                this.Address_Line_1 = correspondingAuthor.address.addressLine1;
                this.Address_Line_2 = correspondingAuthor.address.addressLine2;
                this.Address_Line_3 = correspondingAuthor.address.addressLine3;
                this.City = correspondingAuthor.address.city;
                this.State = correspondingAuthor.address.state;
                this.Country = correspondingAuthor.address.country;
                this.Zip = correspondingAuthor.address.zip;
            }
        }
        this.Abstract = manuscript.getAbstract();
        this.optionalProperties = manuscript.optionalProperties;
        for(String keyword : manuscript.getKeywords()) {
            this.Classification.keyword.add(keyword);
        }
    }

    public String toString() {
        return Journal_Code + " " + Submission_Metadata.Manuscript + " " + Submission_Metadata.Article_Title;
    }
}