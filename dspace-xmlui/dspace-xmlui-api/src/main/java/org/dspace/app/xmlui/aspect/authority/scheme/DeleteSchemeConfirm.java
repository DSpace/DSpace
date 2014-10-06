/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authority.scheme;

import java.sql.SQLException;
import java.util.ArrayList;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Scheme;

/**
 * Present the user with a list of not-yet-but-soon-to-be-deleted-scheme.
 *
 * @author Alexey Maslov
 */
public class DeleteSchemeConfirm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");

    private static final Message T_scheme_trail =
            message("xmlui.administrative.scheme.general.scheme_trail");

    private static final Message T_title =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.title");

    private static final Message T_trail =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.trail");

    private static final Message T_confirm_head =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.confirm_head");

    private static final Message T_confirm_para =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.confirm_para");

    private static final Message T_head_id =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.head_id");

    private static final Message T_head_name =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.head_name");

    private static final Message T_head_email =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.head_email");

    private static final Message T_submit_confirm =
            message("xmlui.administrative.scheme.DeleteEPeopleConfirm.submit_confirm");

    private static final Message T_submit_cancel =
            message("xmlui.general.cancel");


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/scheme",T_scheme_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

    public void addBody(Body body) throws WingException, SQLException, AuthorizeException
    {
        // Get all our parameters
        String idsString = parameters.getParameter("schemeIds", null);
        ArrayList<Scheme> schemes = new ArrayList<Scheme>();
        for (String id : idsString.split(","))
        {
            Scheme person = Scheme.find(context, Integer.valueOf(id));
            schemes.add(person);
        }

        // DIVISION: scheme-confirm-delete
        Division deleted = body.addInteractiveDivision("scheme-confirm-delete",contextPath+"/admin/scheme",Division.METHOD_POST,"primary administrative scheme");
        deleted.setHead(T_confirm_head);
        deleted.addPara(T_confirm_para);

        Table table = deleted.addTable("scheme-confirm-delete",schemes.size() + 1, 1);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_head_id);
        header.addCell().addContent(T_head_name);
        header.addCell().addContent(T_head_email);

        for (Scheme scheme : schemes)
        {
            Row row = table.addRow();
            row.addCell().addContent(scheme.getID());
            row.addCell().addContent(scheme.getIdentifier());
            row.addCell().addContent(scheme.getStatus());
        }
        Para buttons = deleted.addPara();
        buttons.addButton("submit_confirm").setValue("Confirm");
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        deleted.addHidden("administrative-continue").setValue(knot.getId());
    }
}
