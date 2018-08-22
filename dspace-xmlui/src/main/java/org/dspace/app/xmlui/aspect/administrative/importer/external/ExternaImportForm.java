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
import org.apache.commons.lang3.*;
import org.dspace.app.xmlui.cocoon.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.*;
import org.dspace.importer.external.service.*;
import org.dspace.utils.*;
import org.xml.sax.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 30/09/15
 * Time: 17:26
 */
public class ExternaImportForm extends AbstractDSpaceTransformer {

    private static final Message T_DSPACE_HOME = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("xmlui.administrative.importer.external.external-import.trail");
    private static final Message T_head = message("xmlui.administrative.importer.external.external-import.head");
    private static final Message T_submit = message("xmlui.administrative.importer.external.external-import.submit");
    protected static final Message T_lookup_help = message("xmlui.Submission.submit.SourceImportStep.lookup_help");
    private Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException {
        pageMeta.addMetadata("title").addContent(T_head);

        pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
        pageMeta.addTrailLink(null, T_trail);
    }

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        Request request = ObjectModelHelper.getRequest(objectModel);

        request.getSession().setAttribute("selected", null);
        request.getSession().setAttribute("currentRecords", null);
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        Division div = body.addInteractiveDivision("live-import", contextPath + "/admin/external-import/result", Division.METHOD_POST, "");
        div.setHead(T_head);

        List form = div.addList("submit-liveimport", List.TYPE_FORM);

        String importSourceString = request.getParameter("source");

        if (StringUtils.isBlank(importSourceString)) {

            importSourceString = request.getSession(true).getAttribute("source").toString();

        }

        if (StringUtils.isNotBlank(importSourceString)) {

            form.addItem().addContent(T_lookup_help);
        }

        AbstractImportMetadataSourceService importSource = sources.get(importSourceString);


        if (importSource != null) {

            Map<String, String> fields = importSource.getImportFields();

            for (String field : fields.keySet()) {

                Text text = form.addItem().addText(field);

                text.setLabel(message("xmlui.administrative.importer.external.external-import." + field));

                text.setHelp(message("xmlui.administrative.importer.external.external-import." + field + "_hint"));


                if (StringUtils.isNotBlank(request.getParameter(field))) {

                    text.setValue(request.getParameter(field));

                }

            }

            request.getSession(true).removeAttribute("source");

            request.getSession().setAttribute("source", importSourceString);


        }

        form.addItem().addButton("submit-search").setValue(T_submit);

    }
}
