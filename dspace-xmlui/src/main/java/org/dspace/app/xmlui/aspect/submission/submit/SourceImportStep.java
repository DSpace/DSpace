/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.avalon.framework.parameters.*;
import org.apache.cocoon.*;
import org.apache.cocoon.environment.*;
import org.apache.commons.lang.*;
import org.dspace.app.util.*;
import org.dspace.app.xmlui.aspect.submission.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.metadatamapping.*;
import org.dspace.importer.external.elsevier.util.*;
import org.dspace.importer.external.service.*;
import org.dspace.utils.*;
import org.xml.sax.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 01/10/15
 * Time: 15:49
 */
public class SourceImportStep extends AbstractSubmissionStep {

    protected static final Message T_title =
            message("xmlui.Submission.submit.SourceImportStep.title");
    protected static final Message T_lookup_help =
            message("xmlui.Submission.submit.SourceImportStep.lookup_help");
    protected static final Message T_lookup_help2 =
            message("xmlui.Submission.submit.SourceImportStep.lookup_help2");
    protected static final Message T_submit_lookup =
            message("xmlui.Submission.submit.SourceImportStep.submit_lookup");
    protected static final Message T_submit_import_item =
            message("xmlui.Submission.submit.SourceImportStep.submit_import_item");
    protected static final Message T_submit_results =
            message("xmlui.Submission.submit.SourceImportStep.submit_results");
    protected static final Message T_records_found =
            message("xmlui.Submission.submit.SourceImportStep.records-found");
    protected static final Message T_no_records =
            message("xmlui.Submission.submit.SourceImportStep.no-records");
    protected static final Message T_missing_import_source =
            message("xmlui.Submission.submit.SourceImportStep..missing_import_source");


    private LiveImportUtils liveImportUtils = new DSpace().getServiceManager().getServiceByName("LiveImportUtils", LiveImportUtils.class);
    private Map<String, AbstractImportMetadataSourceService> sources = new DSpace().getServiceManager().getServiceByName("ImportServices", HashMap.class);

    public static final String PAGINATION_NEXT_BUTTON = "submit_pagination_next";
    public static final String PAGINATION_PREVIOUS_BUTTON = "submit_pagination_previous";

    private static final int rpp = 10;
    private static final int author_string_max_length = 80;

    private Request request;
    private String buttonPressed;
    private int total = 0;

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        request = ObjectModelHelper.getRequest(objectModel);

        buttonPressed = Util.getSubmitButton(request, "");
    }

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
    }


    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        org.dspace.content.Collection collection = submission.getCollection();
        String actionURL = contextPath + "/handle/"+collection.getHandle() + "/submit/" + knot.getId() + ".continue";
        Division div = body.addInteractiveDivision("submit-lookup", actionURL, Division.METHOD_POST, "primary submission");
        div.setHead(T_submission_head);
        addSubmissionProgressList(div);

        List form = div.addList("submit-lookup", List.TYPE_FORM);

        String importSourceString = itemService.getMetadata(submission.getItem(), "workflow.import.source");
        if (StringUtils.isNotBlank(importSourceString)) {
            AbstractImportMetadataSourceService importSource = sources.get(importSourceString);
            form.setHead(T_title.parameterize(importSource.getName()));
            form.addItem().addContent(T_lookup_help);

            if (importSource != null) {
                Map<String, String> fields = importSource.getImportFields();
                for (String field : fields.keySet()) {
            Text text = form.addItem().addText(field);
            text.setLabel(message("xmlui.administrative.importer.external.external-import." + field));
            text.setHelp(message("xmlui.administrative.importer.external.external-import." + field + "_hint"));

            if(StringUtils.isNotBlank(request.getParameter(field))){
                text.setValue(request.getParameter(field));
            }
        }
                form.addItem().addHidden("source").setValue(importSourceString);
            }

        Item item = form.addItem();
        item.addButton("submit_lookup").setValue(T_submit_lookup);


            HashMap<String, String> fieldValues = liveImportUtils.getFieldValues(request, importSource);

        // field import_id is only used by mirage2. If the field is blank, show the search results in the page.
        if(fieldValues.size()>0 && StringUtils.isBlank(request.getParameter("import_id"))) {
            form.addItem("results-header","page-header").addContent(T_submit_results);

            total = liveImportUtils.getNbRecords(fieldValues);
            java.util.Collection<ImportRecord> records = liveImportUtils.getRecords(fieldValues, getStart(), rpp);

            if(total>0) {
                form.addItem().addContent(T_records_found.parameterize(total, getStart() + 1, getStart() + records.size()));

            List result = form.addList("result");

            for (ImportRecord record : records) {

                java.util.Collection<MetadatumDTO> eid = record.getValue(importSource.getIdField());

                if(eid.size()>0) {
                    String eidString = eid.iterator().next().getValue();

                    Item leftItem = result.addItem("record-left", "record-left");
                    List rightList = result.addList("record-right");


                    leftItem.addButton("submit-import-" + eidString).setValue("import");

                    java.util.Collection<MetadatumDTO> values = record.getValue("dc", "title", null);

                    if (values.size() > 0) {
                        String title = values.iterator().next().getValue();
                        rightList.addItem("record-title","record-title").addContent(title);
                    }

                    values = record.getValue("dc", "contributor", "author");

                    if (values.size() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (MetadatumDTO value : values) {
                            if (StringUtils.isNotBlank(sb.toString())) {
                                sb.append("; ");
                            }
                            sb.append(value.getValue());
                        }

                        String authorString = sb.toString();

                        if (authorString.length() > author_string_max_length) {
                            authorString = authorString.substring(0, author_string_max_length) + "...";
                        }

                        rightList.addItem().addContent(authorString);
                    }
                }
            }

            addPagination(form);
            } else {
                form.addItem().addContent(T_no_records);
            }
        }

        org.dspace.content.Item submissionItem = submission.getItem();
        MetadataFieldConfig importIdField = new MetadataFieldConfig(importSource.getIdField());
        java.util.List<MetadataValue> importId = itemService.getMetadata(submissionItem, importIdField.getSchema(), importIdField.getElement(), importIdField.getQualifier(), org.dspace.content.Item.ANY);

        if(importId.size() > 0) {
            form.addItem("import-header","page-header").addContent(T_submit_import_item);

            java.util.List<MetadataValue> titles = itemService.getMetadata(submissionItem, "dc", "title", null, org.dspace.content.Item.ANY);

            if(titles.size()>0){
                form.addItem("import-title", "bold").addContent(titles.get(0).getValue());
            }

            java.util.List<MetadataValue> authors = itemService.getMetadata(submissionItem, "dc", "contributor", "author", org.dspace.content.Item.ANY);

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
        else if(fieldValues.size()==0){
            form.addItem().addContent(T_lookup_help2);
        }

        div.addDivision("lookup-modal");

        div.addHidden("import_id");
        } else {
            form.setHead(T_title.parameterize(""));
            form.addItem().addContent(T_missing_import_source);
        }
        addControlButtons(form);
    }

    private int getStart(){
        int start = 0;

        String startPara = request.getParameter("start");

        if(StringUtils.isNotBlank(startPara)){
            start = Integer.parseInt(startPara);
        }

        if(buttonPressed.equals(PAGINATION_PREVIOUS_BUTTON)){
            start -= rpp;

            if(start<0){
                start = 0;
            }
        }

        if(buttonPressed.equals(PAGINATION_NEXT_BUTTON)){
            start+=rpp;
        }

        return start;
    }

    private void addPagination(List form) throws WingException {
        form.addItem().addHidden("start").setValue(getStart());

        Item pagination = form.addItem("pagination", "records-pagination");

        Button previous = pagination.addButton(PAGINATION_PREVIOUS_BUTTON, "pagination-previous");
        previous.setValue("«");

        Button next = pagination.addButton(PAGINATION_NEXT_BUTTON, "pagination-next");
        next.setValue("»");

        if(getStart()==0){
            previous.setDisabled(true);
        }

        if(getStart() + rpp >= total){
            next.setDisabled(true);
        }

    }
}

