/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.importer.external;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.avalon.framework.parameters.*;
import org.apache.cocoon.*;
import org.apache.cocoon.environment.*;
import org.dspace.app.xmlui.cocoon.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.*;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.importer.external.service.*;
import org.dspace.utils.*;
import org.xml.sax.*;

/**
 * @author lotte.hofstede at atmire.com
 */
public class ExternalImportSourceForm extends AbstractDSpaceTransformer {
    private static final Message T_DSPACE_HOME                  = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("xmlui.administrative.importer.external.import-source.trail");
    private static final Message T_head = message("xmlui.administrative.importer.external.import-source.head");
    private static final Message T_title = message("xmlui.administrative.importer.external.import-source.title");
    private static final Message T_submit = message("xmlui.administrative.importer.external.import-source.submit");
    private static final Message T_admin_help =
            message("xmlui.Submission.submit.SourceChoiceStep.admin_help");
    protected static final Message T_select_help =
            message("xmlui.administrative.importer.external.source-import.select_help");

    private Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
        pageMeta.addTrailLink(null, T_trail);
    }

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        Request request = ObjectModelHelper.getRequest(objectModel);

        request.getSession().setAttribute("selected",null);
        request.getSession().setAttribute("currentRecords",null);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {
        
        Division div = body.addInteractiveDivision("import-source", contextPath + "/admin/external-import/search", Division.METHOD_POST, "import-source-division");
        div.setHead(T_head);

        if (authorizeService.isAdmin(context) && sources.size() == 1) {
            Division notice = body.addDivision("general-message", "notice neutral");
            Para p = notice.addPara();
            p.addContent(T_admin_help);
        }

        List form = div.addList("submit-lookup", List.TYPE_FORM);

        form.addItem().addContent(T_select_help);

        Select select = form.addItem().addSelect("source", "ImportSourceSelect");
        for (Map.Entry<String, AbstractImportMetadataSourceService> source : sources.entrySet()) {
            select.addOption(source.getKey(), source.getValue().getName());
        }


        form.addItem().addButton("submit").setValue(T_submit);
    }
}
