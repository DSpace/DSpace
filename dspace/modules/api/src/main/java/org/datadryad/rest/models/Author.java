/*
 */
package org.datadryad.rest.models;

import org.apache.commons.lang.StringUtils;
import javax.xml.bind.annotation.XmlRootElement;
import java.lang.String;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@XmlRootElement
public class Author {
    public String familyName;
    public String givenNames;
    public String identifier;
    public String identifierType;
    public Author() {}

    public Author(String familyName, String givenNames) {
        this.familyName = familyName;
        this.givenNames = givenNames;
    }

    public Author(String authorString) {
        // initialize to empty strings, in case there isn't actually anything in the authorString.
        this.givenNames = "";
        this.familyName = "";
        String suffix = "";

        if (authorString != null) {
            authorString = StringUtils.stripToEmpty(authorString);
            // Remove any leading title, like Dr.
            authorString = authorString.replaceAll("^[D|M]+rs*\\.*\\s*","");
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
                    this.givenNames = namepattern.group(2);
                    this.familyName = namepattern.group(1);
                    return;
                }
            }

            // if it's firstname lastname
            namepattern = Pattern.compile("^(.+) +(.*)$").matcher(authorString);
            if (namepattern.find()) {
                this.givenNames = namepattern.group(1);
                this.familyName = namepattern.group(2) + suffix;
            } else {
                // there is only one word in the name: assign it to the familyName?
                this.familyName = authorString;
                this.givenNames = null;
            }
        }
        return;
    }

    public final String fullName() {
        String name = familyName;
        if (givenNames != null) {
            name = familyName + ", " + givenNames;
        }
        return name;
    }
}
