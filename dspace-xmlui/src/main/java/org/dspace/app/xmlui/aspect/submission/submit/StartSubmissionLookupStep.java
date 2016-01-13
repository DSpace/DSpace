/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.submission.submit;

import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by jonas - jonas@atmire.com on 06/11/15.
 */
public class StartSubmissionLookupStep extends AbstractSubmissionStep {

    protected static final Message T_title =
            message("xmlui.Submission.submit.StartSubmissionLookupStep.title");
    protected static final Message T_lookup_help =
            message("xmlui.Submission.submit.StartSubmissionLookupStep.lookup_help");
    protected static final Message T_submit_lookup =
            message("xmlui.Submission.submit.StartSubmissionLookupStep.submit_lookup");
    protected static final Message T_submit_publication_item=
            message("xmlui.Submission.submit.StartSubmissionLookupStep.submit_publication_item");
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    @Override
    public List addReviewSection(List reviewList) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        return null;
    }

    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException
    {

        pageMeta.addMetadata("title").addContent(T_submission_title);
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_submission_trail);

        pageMeta.addMetadata("javascript", null, "handlebars", true).addContent("../../static/handlebars/handlebars.js");
        pageMeta.addMetadata("javascript", null, "submission-lookup", true).addContent("../../static/js/submission-lookup.js");
        pageMeta.addMetadata("stylesheet", "screen", "datatables", true).addContent("../../static/Datatables/DataTables-1.8.0/media/css/datatables.css");
        pageMeta.addMetadata("javascript", "static", "datatables", true).addContent("static/Datatables/DataTables-1.8.0/media/js/jquery.dataTables.min.js");
    }
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        Division div = body.addInteractiveDivision("StartSubmissionLookupStep",actionURL,Division.METHOD_POST,"primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);

        List form = div.addList("submit-lookup",List.TYPE_FORM);

        form.setHead(T_title);

        form.addItem().addContent(T_lookup_help);

        Item item = form.addItem("lookup-group","input-group");

        item.addText("search");
        item.addButton("lookup").setValue(T_submit_lookup);

        org.dspace.content.Item submissionItem = submission.getItem();

        java.util.List<MetadataValue> pubmedId = itemService.getMetadata(submissionItem, "dc", "identifier", "other", org.dspace.content.Item.ANY);

        if(pubmedId.size()>0){

            form.addItem("publication-header","page-header").addContent(T_submit_publication_item);

            java.util.List<MetadataValue> titles = itemService.getMetadata(submissionItem,"dc","title",null,org.dspace.content.Item.ANY);

            if(titles.size()>0){
                form.addItem("publication-title", "bold").addContent(titles.get(0).getValue());
            }

            java.util.List<MetadataValue> authors = itemService.getMetadata(submissionItem,"dc", "contributor", "author", org.dspace.content.Item.ANY);

            if(authors.size()>0){
                StringBuilder builder = new StringBuilder();

                for (int i = 0;i<authors.size();i++) {
                    builder.append(authors.get(i).getValue());

                    if(i+1<authors.size()){
                        builder.append(", ");
                    }
                }

                if(builder.length()>150){
                    builder.setLength(147);
                    builder.append("...");
                }
                form.addItem().addContent(builder.toString());
            }
        }


        div.addDivision("lookup-modal");

        div.addHidden("publication_id");

        addControlButtons(form);
    }
}
