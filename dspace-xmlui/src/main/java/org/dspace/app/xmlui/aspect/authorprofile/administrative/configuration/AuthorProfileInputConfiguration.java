/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Adán Román (arvo.es)
 */
public class AuthorProfileInputConfiguration {

    private List<AuthorProfileInput> authorProfileFields;

    public List<AuthorProfileInput> getAuthorProfileFields(Context context,String authorProfileMail) throws SQLException {
	EPerson eperson=context.getCurrentUser();
	if(AuthorizeManager.isAdmin(context)){
	    return authorProfileFields;
	}else if(eperson!=null && authorProfileMail!=null && eperson.getEmail().equalsIgnoreCase(authorProfileMail)){
	    List<AuthorProfileInput> authorWritableFields=new ArrayList<AuthorProfileInput>();
	    CollectionUtils.addAll(authorWritableFields, authorProfileFields.iterator());
	    Iterator<AuthorProfileInput> it=authorWritableFields.iterator();
	    while(it.hasNext()){
		AuthorProfileInput current=it.next();
		if(current.getEditable()!=null && current.getEditable().equals("admin")){
		    it.remove();
		}
	    }
	    return authorWritableFields;
        }else{
    		return new ArrayList<AuthorProfileInput>();
        }
    }
    public List<AuthorProfileInput> getAuthorProfileFields() throws SQLException {
	return authorProfileFields;
    }
    @Autowired
    public void setAuthorProfileFields(List<AuthorProfileInput> authorProfileFields) {
        this.authorProfileFields = authorProfileFields;
    }
}
