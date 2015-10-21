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
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.scidir.util.LiveImportUtils;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 01/10/15
 * Time: 10:34
 */
public class LiveImportResult extends AbstractDSpaceTransformer {

    private static final Message T_DSPACE_HOME = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("xmlui.scidir.live-import.trail");
    private static final Message T_head = message("xmlui.scidir.live-import.head");
    private static final Message T_submit = message("xmlui.scidir.live-import-result.submit");
    private static final Message T_submit_next = message("xmlui.scidir.live-import-result.submit-next");
    private static final Message T_no_fields_error = message("xmlui.scidir.live-import-result.no-fields-error");
    private static final Message T_records_found = message("xmlui.scidir.live-import-result.records-found");
    private static final Message T_no_records_found = message("xmlui.scidir.live-import-result.no-records-found");
    private static final Message T_records_selected = message("xmlui.scidir.live-import-result.records-selected");

    private static final int rpp = 10;
    private static final int author_string_max_length = 80;
    public static final String PAGINATION_NEXT_BUTTON = "submit_pagination_next";
    public static final String PAGINATION_PREVIOUS_BUTTON = "submit_pagination_previous";
    public static final String NEXT_BUTTON = "submit_next";
    public static final String BACK_BUTTON = "submit_back";

    private HashMap<String, String> liveImportFields = new DSpace().getServiceManager().getServiceByName("LiveImportFields", HashMap.class);
    private LiveImportUtils liveImportUtils = new DSpace().getServiceManager().getServiceByName("LiveImportUtils", LiveImportUtils.class);

    private Request request;
    private String buttonPressed;
    private int total = 0;

    private HashMap<String,SessionRecord> selected;
    private HashMap<String,SessionRecord> currentRecords;

    Logger log = Logger.getLogger(LiveImportResult.class);

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

        request = ObjectModelHelper.getRequest(objectModel);
        buttonPressed = Util.getSubmitButton(request, "");

        selected = new HashMap<>();
        if(request.getSession().getAttribute("selected")!=null) {
            selected = (HashMap<String,SessionRecord>) request.getSession().getAttribute("selected");
        }

        currentRecords = new HashMap<>();
        if(request.getSession().getAttribute("currentRecords")!=null) {
            currentRecords = (HashMap<String,SessionRecord>) request.getSession().getAttribute("currentRecords");
        }
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        if(buttonPressed.equals(BACK_BUTTON)){

            ((HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT)).sendRedirect(request.getContextPath() + "/liveimport");
        }

        if(buttonPressed.equals(NEXT_BUTTON) || buttonPressed.equals(PAGINATION_NEXT_BUTTON) || buttonPressed.equals(PAGINATION_PREVIOUS_BUTTON)){
            Enumeration parameterNames = request.getParameterNames();

            while(parameterNames.hasMoreElements()){
                String parameter = (String) parameterNames.nextElement();

                if(parameter.startsWith("record-eid-")){
                    SessionRecord record = new SessionRecord();

                    String eidString = parameter.substring("record-eid-".length());
                    record.setEid(eidString);

                    String title = request.getParameter(eidString + "-record-title");
                    record.setTitle(title);

                    String authors = request.getParameter(eidString + "-record-authors");
                    record.setAuthors(authors);

                    selected.put(eidString,record);
                }
            }

            for (String eid : currentRecords.keySet()) {
                if(selected.containsKey(eid) && StringUtils.isBlank(request.getParameter("record-eid-" + eid))){
                    selected.remove(eid);
                }
            }

            currentRecords = new HashMap<>();

            request.getSession().setAttribute("selected", selected);
        }

        if(buttonPressed.equals(NEXT_BUTTON)){
            ((HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT)).sendRedirect(request.getContextPath() + "/liveimport/selected");
        }

        Division div = body.addInteractiveDivision("live-import-result", contextPath + "/liveimport/result", Division.METHOD_POST, "");
        div.setHead(T_head);

        HashMap<String, String> fieldValues = liveImportUtils.getFieldValues(request);

        for (String field : fieldValues.keySet()) {
            div.addHidden(field).setValue(fieldValues.get(field));
        }



        for (String field : liveImportFields.keySet()) {
            String value = request.getParameter(field);

            if(StringUtils.isNotBlank(value)){
                fieldValues.put(liveImportFields.get(field),value);
                div.addHidden(field).setValue(value);
            }
        }

        if(fieldValues.size()==0){
            div.addPara().addContent(T_no_fields_error);
        }
        else {
            total = liveImportUtils.getNbRecords(fieldValues);
            renderRecords(div, fieldValues);
        }

        Para para = div.addDivision("navigation-buttons").addPara();
        para.addButton(BACK_BUTTON).setValue(T_submit);
        para.addButton(NEXT_BUTTON).setValue(T_submit_next);
    }

    private void renderRecords(Division div, HashMap<String, String> fieldValues) throws WingException, SQLException {
        Collection<ImportRecord> records = liveImportUtils.getRecords(fieldValues, getStart(), rpp);

        if (records.size() > 0) {
            div.addPara().addContent(T_records_found.parameterize(total,getStart() + 1, getStart() + records.size()));

            for (ImportRecord record : records) {
                SessionRecord currentRecord = new SessionRecord();
                Division result = div.addDivision("result", "row import-record");
                Collection<MetadatumDTO> eid = record.getValue("elsevier", "identifier", "eid");

                Division leftDiv = result.addDivision("record-left", "record-leftdiv");
                Division rightDiv = result.addDivision("record-right", "record-rightdiv");

                String eidString = eid.iterator().next().getValue();
                currentRecord.setEid(eidString);

                boolean isSelected = selected.containsKey(eidString);

                leftDiv.addPara().addCheckBox("record-eid-" + eidString, "record-checkbox").addOption(isSelected,"record-checkbox-" + eid.iterator().next().getValue());

                Collection<MetadatumDTO> values = record.getValue("dc", "title", null);

                if (values.size() > 0) {
                    String title = values.iterator().next().getValue();
                    Para para = rightDiv.addPara("", "record-title");
                    para.addContent(title);
                    para.addHidden(eidString + "-record-title").setValue(title);
                    currentRecord.setTitle(title);
                }

                values = record.getValue("dc", "contributor", "author");

                if (values.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (MetadatumDTO value : values) {
                        if(StringUtils.isNotBlank(sb.toString())){
                            sb.append(", ");
                        }
                        sb.append(value.getValue());
                    }

                    String authorString = sb.toString();

                    if(authorString.length()> author_string_max_length){
                        authorString = authorString.substring(0,author_string_max_length) + "...";
                    }

                    Para para = rightDiv.addPara("", "record-authors");
                    para.addContent(authorString);
                    para.addHidden(eidString + "-record-authors").setValue(authorString);
                    currentRecord.setAuthors(authorString);
                }

                currentRecords.put(eidString, currentRecord);
            }

            request.getSession().setAttribute("currentRecords", currentRecords);

            addPagination(div);

            div.addPara("records-selected","records-selected").addContent(T_records_selected.parameterize(selected.size()));
        }
        else {
            div.addPara().addContent(T_no_records_found);
        }
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

    private void addPagination(Division div) throws WingException {
        Division paginationDiv = div.addDivision("pagination", "record-pagination");

        paginationDiv.addHidden("start").setValue(getStart());

        Para pagination = paginationDiv.addPara("pagination", "records-pagination");

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
