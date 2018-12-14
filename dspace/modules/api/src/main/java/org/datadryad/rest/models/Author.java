/*
 */
package org.datadryad.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.DCValue;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.lang.String;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Author {
    private String familyName;
    private String givenNames;
    private String identifier;
    private String identifierType;

    public static final String ORCID_TYPE = "ORCID";
    public Author() {}

    public Author(String familyName, String givenNames) {
        setFamilyName(familyName);
        setGivenNames(givenNames);
    }

    public Author(DCValue authorMetadata) {
        this(authorMetadata.value);
        if (authorMetadata.authority != null) {
            Matcher orcidpattern = Pattern.compile("orcid:(.+)$").matcher(authorMetadata.authority);
            if (orcidpattern.find()) {
                setIdentifier(orcidpattern.group(1));
                setIdentifierType(ORCID_TYPE);
            }
        }
    }

    public Author(String authorString) {
        // initialize to empty strings, in case there isn't actually anything in the authorString.
        setGivenNames("");
        setFamilyName("");

        if (authorString != null) {
            setFullName(authorString);
        }
        return;
    }

    // compare last names and first initial of given name


    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            Author author = (Author) obj;
            if (author.getNormalizedFamilyName().equalsIgnoreCase(this.getNormalizedFamilyName())) {
                return author.getFirstInitial().equalsIgnoreCase(this.getFirstInitial());
            }
        }
        return false;
    }

    public void setFullName(String authorString) {
        if ("".equals(familyName) || "".equals(givenNames)) {
            String suffix = "";
            authorString = StringUtils.stripToEmpty(authorString);
            // Remove any leading title, like Dr.
            authorString = authorString.replaceAll("^[D|M]+rs*\\.*\\s+","");
            // is there a comma in the name?
            // it could either be lastname, firstname, or firstname lastname, title
            Matcher namepattern = Pattern.compile("^(.+),\\s*(.*)$").matcher(authorString);
            if (namepattern.find()) {
                if (namepattern.group(2).matches("(Jr\\.*|Sr\\.*|III)")) {
                    // if it's a suffix situation, then group 2 will say something like "Jr"
                    // if this is the case, it's actually a firstname lastname situation.
                    // the last name will be the last word in group 1 + ", " + suffix.
                    suffix = ", " + namepattern.group(2);
                    authorString = namepattern.group(1);
                } else if (namepattern.group(2).matches("(Ph|J)\\.*D\\.*|M\\.*[DAS]c*\\.*")) {
                    // if it's a title situation, group 2 will say something like "PhD" or "MD"
                    // there are probably more titles that might happen here, but we can't deal with that.
                    // throw this away.
                    authorString = namepattern.group(1);
                } else {
                    setGivenNames(namepattern.group(2));
                    setFamilyName(namepattern.group(1));
                    return;
                }
            }

            // if it's firstname lastname
            namepattern = Pattern.compile("^(.+) +(.*)$").matcher(authorString);
            if (namepattern.find()) {
                setGivenNames(namepattern.group(1));
                setFamilyName(namepattern.group(2) + suffix);
            } else {
                // there is only one word in the name: assign it to the familyName?
                setGivenNames("");
                setFamilyName(authorString);
            }
        }
        return;
    }

    public void setFamilyName(String familyName) {
        this.familyName = StringEscapeUtils.escapeHtml(familyName);
    }

    public void setGivenNames(String givenNames) {
        this.givenNames = StringEscapeUtils.escapeHtml(givenNames);
    }

    @JsonIgnore
    public final String getUnicodeFullName() {
        String name = getUnicodeFamilyName();
        if (!"".equals(getUnicodeGivenNames())) {
            name = getUnicodeFamilyName() + ", " + getUnicodeGivenNames();
        }
        return name;
    }

    @JsonIgnore
    public String getUnicodeFamilyName() {
        return StringEscapeUtils.unescapeHtml(familyName);
    }

    @JsonIgnore
    public String getUnicodeGivenNames() {
        return StringEscapeUtils.unescapeHtml(givenNames);
    }

    public String getFullName() {
        return getUnicodeFullName();
    }

    public String getFamilyName() {
        return getUnicodeFamilyName();
    }

    public String getGivenNames() {
        return getUnicodeGivenNames();
    }


    @JsonIgnore
    public final String getHTMLFullName() {
        String name = getHTMLFamilyName();
        if (!"".equals(getHTMLGivenNames())) {
            name = getHTMLFamilyName() + ", " + getHTMLGivenNames();
        }
        return name;
    }

    @JsonIgnore
    public String getHTMLFamilyName() {
        return StringEscapeUtils.unescapeHtml(familyName);
    }

    @JsonIgnore
    public String getHTMLGivenNames() {
        return StringEscapeUtils.unescapeHtml(givenNames);
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @JsonIgnore
    public String getNormalizedFamilyName() {
        return Normalizer.normalize(getUnicodeFamilyName(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    @JsonIgnore
    public String getNormalizedGivenNames() {
        return Normalizer.normalize(getUnicodeGivenNames(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    @JsonIgnore
    public String getFirstInitial() {
        return getNormalizedFamilyName().substring(0,1);
    }

    @JsonIgnore
    public String getNormalizedFullName() {
        return Normalizer.normalize(getUnicodeFullName(), Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getIdentifierType() {
        return this.identifierType;
    }

    public static String normalizeName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public static class SchemaDotOrgSerializer extends JsonSerializer<Author> {
        @Override
        public void serialize(Author author, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            jGen.writeStartObject();
            jGen.writeStringField("@type", "Person");
            if (author.getIdentifierType() != null && author.getIdentifierType().equals(ORCID_TYPE)) {
                jGen.writeStringField("@id", "http://orcid.org/"+author.getIdentifier());
            }
            jGen.writeStringField("givenName", author.getGivenNames());
            jGen.writeStringField("familyName", author.getFamilyName());
            jGen.writeEndObject();
        }
    }

    public static class DashSerializer extends JsonSerializer<Author> {
        @Override
        public void serialize(Author author, JsonGenerator jGen, SerializerProvider provider) throws IOException {
            jGen.writeStartObject();
            jGen.writeStringField("firstName", author.getGivenNames());
            jGen.writeStringField("lastName", author.getFamilyName());
            if (author.getIdentifierType() != null && author.getIdentifierType().equals(ORCID_TYPE)) {
                jGen.writeStringField("orcid", author.getIdentifier());
            }
            jGen.writeEndObject();
        }
    }

}
