/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.content.AuthorProfile;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Required;

import java.util.regex.Pattern;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class RegexValidator implements Validator {
    private String validationRegex = null;
    private Pattern validationPattern = null;

    public String getValidationRegex() {
        return validationRegex;
    }
    @Required
    public void setValidationRegex(String validationRegex) {

        this.validationRegex=validationRegex;
        validationPattern=Pattern.compile(validationRegex);
    }

    @Override
    public boolean hasClientComponent() {
        return true;
    }

    @Override
    public String getClientComponent() {
        return validationRegex;
    }

    public String getClientComponentClass(){
        return "regex";
    }

    public boolean validate(AuthorProfile ap,Context context,String input){
        return validationPattern == null || validationPattern.matcher(input).matches();
    }

    @Override
    public Message getErrorMessage(String prefix,String suffix) {
        return new Message(AbstractDSpaceTransformer.getDefaultMessageCatalogue(),prefix+suffix+".regexFailed");
    }
}
