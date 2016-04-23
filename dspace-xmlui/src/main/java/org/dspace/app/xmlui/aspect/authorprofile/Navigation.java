/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.AuthorProfile;
import org.dspace.content.Item;
import org.dspace.discovery.AuthorProfileUtil;
import org.dspace.discovery.SearchServiceException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Navigation extends AbstractDSpaceTransformer {

    private static final Message T_author_profile = message("xmlui.authorprofile.administrative.Navigation.create_author_profile");

    private static final Message T_head_author_profile=message("xmlui.ArtifactBrowser.Navigationauthor_profile.head");
    private static final Message T_browse_by_author_profile=message("xmlui.ArtifactBrowser.Navigation.browse_by_author_profile");

    private static final Logger log = Logger.getLogger(Navigation.class);


    @Override
    public void addOptions(Options options) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        List browse = options.addList("browse");

        try {
            if(AuthorProfileUtil.countAuthorProfiles(context) > 0) {
                List ap = browse.addList("authorprofile");
                ap.setHead(T_head_author_profile);
                ap.addItemXref(contextPath+"/browse-by",T_browse_by_author_profile);
            }
        } catch (SearchServiceException e) {
            log.error(e.getMessage(), e);
        }

        options.addList("account");
        options.addList("context");
        if(AuthorizeManager.isAdmin(this.context))
        {
            List admin = options.addList("administrative");
            admin.addItemXref(contextPath+"/admin/authorprofile", T_author_profile);
        }


    }
}
