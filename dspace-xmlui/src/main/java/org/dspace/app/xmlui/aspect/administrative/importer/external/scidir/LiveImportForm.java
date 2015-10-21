/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.importer.external.scidir;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 30/09/15
 * Time: 17:26
 */
public class LiveImportForm extends AbstractDSpaceTransformer {

    private static final Message T_DSPACE_HOME                  = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("xmlui.scidir.live-import.trail");
    private static final Message T_head = message("xmlui.scidir.live-import.head");
    private static final Message T_hint = message("xmlui.scidir.live-import.hint");
    private static final Message T_submit = message("xmlui.scidir.live-import.submit");

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(T_head);

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

        Division div = body.addInteractiveDivision("live-import", contextPath + "/liveimport/result", Division.METHOD_POST, "");
        div.setHead(T_head);

        div.addPara().addContent(T_hint);

        HashMap<String, String> liveImportFields = new DSpace().getServiceManager().getServiceByName("LiveImportFields", HashMap.class);

        List form = div.addList("submit-liveimport", List.TYPE_FORM);

        for (String field : liveImportFields.keySet()) {
            Text text = form.addItem().addText(field);
            text.setLabel(message("xmlui.scidir.live-import." + field));
            text.setHelp(message("xmlui.scidir.live-import." + field + "_hint"));
        }

        form.addItem().addButton("submit").setValue(T_submit);
    }
}
