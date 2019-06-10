package org.datadryad.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadBitstream;
import org.datadryad.api.DryadDataFile;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.JournalUtils;
import org.dspace.content.Item;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.paymentsystem.ShoppingCart;

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
    private Integer itemID = 0;

    @JsonIgnore
    private DryadDataPackage dataPackage;

    @JsonIgnore
    private static final Logger log = Logger.getLogger(Package.class);

    static {
    }

    public Package() {
        this.dataPackage = new DryadDataPackage();
    } // JAXB needs this

    public Package(DryadDataPackage dataPackage) {
        this.dataPackage = dataPackage;
        if (this.dataPackage.getItem() != null) {
            this.itemID = dataPackage.getItem().getID();
        } else {
            this.itemID = 0;
        }
    }

    public DryadDataPackage getDataPackage() {
        return dataPackage;
    }

    public Integer getItemID() {
        return itemID;
    }

    public String getPublicationDOI() {
        String publicationDOI = dataPackage.getPublicationDOI();
        if (publicationDOI == null) {
            publicationDOI = "";
        }
        return publicationDOI;
    }

    public void setPublicationDOI(String doi) {
        dataPackage.setPublicationDOI(doi);
    }

    public String getPublicationDate() {
        return sdf.format(dataPackage.getDateAccessioned());
    }

    public void setPublicationDate(String date) {
        dataPackage.setPublicationDate(date);
    }

    public void setAuthors(List<Author> authorList) {
        dataPackage.clearAuthors();
        for (Author author : authorList) {
            dataPackage.addAuthor(author);
        }
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
        List<Author> authors = dataPackage.getAuthors();
        return authors;
    }

    public List<String> getKeywords() {
        return dataPackage.getKeywords();
    }

    public void setKeywords(List<String> keywords) {
        dataPackage.setKeywords(keywords);
    }

    public String getDryadDOI() {
        return dataPackage.getDryadDOI();
    }

    public void setDryadDOI(String doi) {
        dataPackage.setIdentifier(doi);
    }

    public String getManuscriptNumber() {
        return dataPackage.getManuscriptNumber();
    }

    public void setManuscriptNumber(String msID) {
        dataPackage.setManuscriptNumber(msID);
    }

    @JsonIgnore
    public DryadJournalConcept getJournalConcept() {
        return JournalUtils.getJournalConceptByJournalName(dataPackage.getPublicationName());
    }

    public String getTitle() {
        return dataPackage.getTitle();
    }

    public void setTitle(String title) {
        dataPackage.setTitle(title);
    }

    public String getAbstract() {
        return dataPackage.getAbstract();
    }

    public void setAbstract(String theAbstract) {
        dataPackage.setAbstract(theAbstract);
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
        public void serialize(Package restPackage, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            String packageDOI = restPackage.getDryadDOI();
            
            jGen.writeStartObject();
            jGen.writeStringField("@context", "http://schema.org/");
            jGen.writeStringField("@type", "Dataset");
            jGen.writeStringField("@id", DOIIdentifierProvider.getFullDOIURL(packageDOI));
            jGen.writeStringField("url", DOIIdentifierProvider.getFullDOIURL(packageDOI));
            jGen.writeStringField("identifier", packageDOI);
            jGen.writeStringField("name", restPackage.getTitle());
            jGen.writeObjectField("author", restPackage.getAuthorList());
            jGen.writeStringField("datePublished", restPackage.getPublicationDate());
            String version = DOIIdentifierProvider.getDOIVersion(restPackage.getDryadDOI());
            if (!"".equals(version)) {
                jGen.writeStringField("version", version);
            }
            jGen.writeStringField("description", restPackage.getAbstract());
            if (restPackage.getKeywords().size() > 0) {
                jGen.writeObjectField("keywords", restPackage.getKeywords());
            }

            // write citation for article:
            jGen.writeObjectFieldStart("citation");
            jGen.writeStringField("@type", "Article");
            jGen.writeStringField("identifier", restPackage.getPublicationDOI());
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
        public void serialize(Package restPackage, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            DryadDataPackage ddp = restPackage.getDataPackage();
            
            jGen.writeStartObject();

            jGen.writeStringField("identifier", ddp.getVersionlessIdentifier());
            jGen.writeStringField("title", restPackage.getTitle());
            jGen.writeStringField("abstract", restPackage.getAbstract());
            jGen.writeObjectField("authors", restPackage.getAuthorList());

            if(ddp.getItem().isArchived()) {
                ShoppingCart sc = ddp.getShoppingCart();
                if(sc == null) {
                    jGen.writeStringField("invoiceId", "classic-dryad:old-item-" + ddp.getItem().getID());
                } else {
                    jGen.writeStringField("invoiceId", "classic-dryad:" + sc.getID());
                }
            }
            
            // keywordsToWrite and spatialToWrite will include both package- and file-level keywords
            Set<String> keywordsToWrite = new HashSet<String>();
            Set<String> spatialToWrite = new HashSet<String>();

            
            try {
                int userId = ddp.getDashUserID();
                log.debug("userId = " + userId);
                if(userId > 0) {
                    jGen.writeStringField("userId", "" + userId);
                } else {
                    log.debug("setting default userId of 1");
                    jGen.writeStringField("userId", "1");
                }
            } catch(Exception e) {
                log.error("unable to get submitter's ID", e);
            }
            
            // Data Files
            // for each file, write the
            // # <h3>File Title
            // # File Description
            // # Filenames:
            // # - File name
            // # - README filename
            try {
                Context context = new Context();
                List<DryadDataFile> ddfs = ddp.getDataFiles(context);
                if(ddfs.size() > 0) {
                    String fileListString = "";
                    for(DryadDataFile dryadFile : ddfs) {
                        log.debug("serializing file " + dryadFile.getIdentifier() + ", " + dryadFile.getDryadDOI());

                        // file-level keywords get saved to export at the package level
                        // temporal coverage, and scientific names are lumped in with keywords for now
                        keywordsToWrite.addAll(dryadFile.getKeywords());
                        keywordsToWrite.addAll(dryadFile.getCoverageTemporal());
                        keywordsToWrite.addAll(dryadFile.getScientificNames());
                        spatialToWrite.addAll(dryadFile.getCoverageSpatial());

                        // file titles, names, descriptions get formatted into the Usage Notes
                        String fileTitle = dryadFile.getTitle();                        
                        fileListString = fileListString + "<div class=\"o-metadata__file-usage-entry\"><h4 class=\"o-heading__level3-file-title\">" + fileTitle + "</h4>";
                        
                        String fileDescription = dryadFile.getDescription();
                        if(fileDescription != null) {
                            fileListString = fileListString + "<div class=\"o-metadata__file-description\">" + fileDescription + "</div>";
                        }
                                                
                        // bitstreams
                        String previousBitstreamFilename = "";
                        for(Bitstream dspaceBitstream : dryadFile.getAllBitstreams()) {
                            fileListString = fileListString + "<div class=\"o-metadata__file-name\">";
                            DryadBitstream dryadBitstream = new DryadBitstream(dspaceBitstream);                    
                            if(dryadBitstream.isReadme()) {
                                dryadBitstream.setReadmeFilename(previousBitstreamFilename);
                                fileListString = fileListString + dryadBitstream.getReadmeFilename() + "</br>";
                            } else {
                                String filename = dspaceBitstream.getName();
                                if(filename.startsWith(fileTitle)) {
                                    // don't record the filename
                                } else {
                                    fileListString = fileListString + filename + "</br>";
                                }
                                previousBitstreamFilename = dspaceBitstream.getName();
                            }
                            fileListString = fileListString + "</div>";
                        }
                        fileListString = fileListString + "</div>";
                    }
                    jGen.writeStringField("usageNotes", fileListString);
                }
            } catch(Exception e) {
                throw new IOException("Unable to serialize data files", e);
            }
            
            // temporal coverage, and scientific names are lumped in with keywords for now
            keywordsToWrite.addAll(ddp.getKeywords());
            keywordsToWrite.addAll(ddp.getCoverageTemporal());
            keywordsToWrite.addAll(ddp.getScientificNames());
            if (keywordsToWrite.size() > 0) {
                jGen.writeObjectField("keywords", keywordsToWrite);
            }

            spatialToWrite.addAll(ddp.getCoverageSpatial());
            if (spatialToWrite.size() > 0) {
                jGen.writeArrayFieldStart("locations");
                for(String spatialItem : spatialToWrite) {
                    jGen.writeStartObject();
                    jGen.writeStringField("place", spatialItem);
                    jGen.writeEndObject();
                }
                jGen.writeEndArray();
            }

            // --- start of related identifiers section
            // write citation for article and other related identifiers:
            jGen.writeArrayFieldStart("relatedWorks");
            
            if(restPackage.getPublicationDOI() != null &&
               restPackage.getPublicationDOI().length() > 0) {
                jGen.writeStartObject();
                jGen.writeStringField("relationship", "issupplementto");
                jGen.writeStringField("identifierType", "DOI");
                jGen.writeStringField("identifier", restPackage.getPublicationDOI());
                jGen.writeEndObject();
            }
            
            // related identifiers TODO
            List<String> externals = ddp.getExternalRelations();
            for(String externalRelation : externals) {
                // parse them into URL and label
                int colonIndex = externalRelation.indexOf(":");
                if(colonIndex > 0 && (colonIndex + 1 < externalRelation.length())){
                    // if good index, split
                    String relSchema = externalRelation.substring(0, colonIndex);
                    String relLocation = externalRelation.substring(colonIndex + 1);
                    if(relLocation.startsWith("http")){
                        //   if second part is URL, mark it as a URL
                        jGen.writeStartObject();
                        jGen.writeStringField("relationship", "issupplementedby");
                        jGen.writeStringField("identifierType", "URL");
                        jGen.writeStringField("identifier", relLocation);
                        jGen.writeEndObject();
                    } else {
                        // else, add as a URN
                        jGen.writeStartObject();
                        jGen.writeStringField("relationship", "issupplementedby");
                        jGen.writeStringField("identifierType", "URN");
                        jGen.writeStringField("identifier", externalRelation);
                        jGen.writeEndObject();
                    }
                } else {
                    // bad index, add as a URN
                    jGen.writeStartObject();
                    jGen.writeStringField("relationship", "issupplementedby");
                    jGen.writeStringField("identifierType", "URN");
                    jGen.writeStringField("identifier", externalRelation);
                    jGen.writeEndObject();
                }
            }
            
            jGen.writeEndArray();
            // --- end of related identifiers section

            // funder
            String funder = ddp.getFundingEntity();
            if(funder !=null && funder.contains("@National Sci")) {
                int nsfIndex = funder.indexOf("@National Sci");
                String award = funder.substring(0, nsfIndex);
                jGen.writeArrayFieldStart("funders");
                jGen.writeStartObject();
                jGen.writeStringField("organization", "National Science Foundation");
                jGen.writeStringField("awardNumber", award);
                jGen.writeEndObject();
                jGen.writeEndArray();
            }
            
            
            // When working with Dryad Classic packages, we want to disable the
            // default DASH validation and interaction with DataCite
            jGen.writeBooleanField("skipDataciteUpdate", true);
            jGen.writeBooleanField("skipEmails", true);
            jGen.writeBooleanField("loosenValidation", true);
            jGen.writeBooleanField("preserveCurationStatus", true);
            
            jGen.writeEndObject();
        }
    }
}
