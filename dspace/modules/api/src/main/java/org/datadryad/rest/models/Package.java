/*
 */
package org.datadryad.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.JournalUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Package {
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    // can un-ignore itemID for debugging purposes
    @JsonIgnore
    private Integer itemID;

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

    public Integer getItemID() {
        return itemID;
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

    public String getPublicationDate() {
        return sdf.format(dataPackage.getDateAccessioned());
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
        List<Author> authors = new ArrayList<Author>();
        try {
            authors = dataPackage.getAuthors();
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

    @JsonIgnore
    public String getAbstract() {
        try {
            return dataPackage.getAbstract();
        } catch (SQLException e) {
            log.error("couldn't find abstract for item " + itemID);
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

    public static ArrayList<Package> getPackagesForItemSet(Collection<Integer> itemList, Integer limit, Context context) throws SQLException {
        ArrayList<Package> packageList = new ArrayList<Package>();
        for (Integer itemID : itemList) {
            Package dataPackage = new Package(new DryadDataPackage(Item.find(context, itemID)));
            packageList.add(dataPackage);
            if (limit != null && packageList.size() == limit) {
                break;
            }
        }
        return packageList;
    }
    
    public static class SchemaDotOrgSerializer extends JsonSerializer<Package> {
        @Override
        public void serialize(Package dataPackage, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            jGen.writeStartObject();
            jGen.writeStringField("@context", "http://schema.org/");
            jGen.writeStringField("@type", "Dataset");
            jGen.writeStringField("@id", DOIIdentifierProvider.getFullDOIURL(dataPackage.getDryadDOI()));
            jGen.writeStringField("url", DOIIdentifierProvider.getFullDOIURL(dataPackage.getDryadDOI()));
            jGen.writeStringField("identifier", dataPackage.getDryadDOI());
            jGen.writeStringField("name", dataPackage.getTitle());
            jGen.writeObjectField("author", dataPackage.getAuthorList());
            jGen.writeStringField("datePublished", dataPackage.getPublicationDate());
            String version = DOIIdentifierProvider.getDOIVersion(dataPackage.getDryadDOI());
            if (!"".equals(version)) {
                jGen.writeStringField("version", version);
            }
            jGen.writeStringField("description", dataPackage.getAbstract());
            if (dataPackage.getKeywords().size() > 0) {
                jGen.writeObjectField("keywords", dataPackage.getKeywords());
            }

            // write citation for article:
            jGen.writeObjectFieldStart("citation");
            jGen.writeStringField("@type", "Article");
            jGen.writeStringField("identifier", dataPackage.getPublicationDOI());
            jGen.writeEndObject();

            // write Dryad Digital Repository Organization object:
            jGen.writeObjectFieldStart("publisher");
            jGen.writeStringField("@type", "Organization");
            jGen.writeStringField("name", "Dryad Digital Repository");
            jGen.writeStringField("url", "https://datadryad.org");
            jGen.writeEndObject();

            jGen.writeEndObject();
        }
    }

    public static class DashSerializer extends JsonSerializer<Package> {
        @Override
        public void serialize(Package dataPackage, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            jGen.writeStartObject();

            jGen.writeStringField("identifier", dataPackage.getDryadDOI());
            jGen.writeStringField("title", dataPackage.getTitle());
            jGen.writeStringField("abstract", dataPackage.getAbstract());
            jGen.writeObjectField("authors", dataPackage.getAuthorList());

            if (dataPackage.getKeywords().size() > 0) {
                jGen.writeObjectField("keywords", dataPackage.getKeywords());
            }
            
            //TODO: replace this with a real epersonID OR DASH user ID
            jGen.writeStringField("userID", "1");
            
            // write citation for article:
            jGen.writeArrayFieldStart("relatedWorks");
            jGen.writeStartObject();
            jGen.writeStringField("relationship", "iscitedby");
            jGen.writeStringField("identifierType", "DOI");
            jGen.writeStringField("identifier", dataPackage.getPublicationDOI());
            jGen.writeEndObject();
            jGen.writeEndArray();

            // When working with Dryad Classic packages, we want to disable the
            // default DASH validation and interaction with DataCite
            jGen.writeBooleanField("skipDataciteUpdate", true);
            jGen.writeBooleanField("skipEmails", true);
            jGen.writeBooleanField("loosenValidation", true);
            
            jGen.writeEndObject();
        }
    }
}
