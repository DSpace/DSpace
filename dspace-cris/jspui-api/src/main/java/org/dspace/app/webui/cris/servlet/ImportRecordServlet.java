package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.ItemImportMainOA;
import org.dspace.app.cris.batch.bte.ImpRecordItem;
import org.dspace.app.cris.batch.dao.ImpRecordDAO;
import org.dspace.app.cris.batch.dao.ImpRecordDAOFactory;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.utils.DSpace;

import it.cilea.osd.common.constants.Constants;

public class ImportRecordServlet extends DSpaceServlet
{
    
    private Logger log = Logger.getLogger(ImportRecordServlet.class);

    private DSpace dspace = new DSpace();
    
    private ApplicationService applicationService = dspace.getServiceManager()
            .getServiceByName("applicationService",
                    ApplicationService.class);
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        Integer epersonID = null;
        String crisID = context.getCrisID();
        EPerson currentUser = context.getCurrentUser();
        String requestAnotherCrisID = null;
        if(AuthorizeManager.isAdmin(context)) {
            requestAnotherCrisID = request.getParameter("crisid");
        }
        
        if(StringUtils.isNotBlank(requestAnotherCrisID)) {
            ResearcherPage rp = applicationService.getEntityByCrisId(requestAnotherCrisID, ResearcherPage.class);
            epersonID = rp.getEpersonID();
        }
        else {
            epersonID = currentUser.getID();
        }
        String sourceRef = request.getParameter("sourceref");
        
        ImpRecordDAO impRecordDAO = ImpRecordDAOFactory.getInstance(context);
        List<ImpRecordItem> results = impRecordDAO.findByEPersonIDAndSourceRefAndLastModifiedInNull(epersonID, sourceRef);
        request.setAttribute("results", results);
        request.setAttribute("crisid", crisID);
        request.setAttribute("sourceref", sourceRef);
        JSPManager.showJSP(request, response, "/tools/imprecord-list.jsp");
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        
        final String submitButton = UIUtil.getSubmitButton(request,
                "submit_cancel");

        String crisID = context.getCrisID();
        if (!"submit_cancel".equals(submitButton))
        {
            String sourceRef = request.getParameter("sourceRef");
            String message = null;
            int[] selectedIds = UIUtil.getIntParameters(request, "selectedId");
            int failures = 0;
            int successes = 0;
            for (int selectedId : selectedIds)
            {
                String selectedIdentifier = request
                        .getParameter("identifier_" + selectedId);
                if(StringUtils.isNotBlank(selectedIdentifier)) {
                    String[] identifierArray = selectedIdentifier.split("_");
                    String recordId = identifierArray[0];
                    String recordRef = identifierArray[1];
                    if ("submit_import".equals(submitButton)) {
                        try
                        {
                            ItemImportMainOA.main(new String[]{"-q","SELECT * FROM imp_record WHERE last_modified is NULL AND imp_record_id = '"+recordId+"' AND imp_sourceref = '"+recordRef+"' order by imp_id ASC"});
                            successes++;
                        }
                        catch (Exception e)
                        {
                            failures++;
                            log.error(e.getMessage(), e);
                        }
                    }
                    else {
                        DatabaseManager.updateQuery(context,
                                "UPDATE imp_record " + "SET last_modified = LOCALTIMESTAMP, operation = 'ignored'"
                                        + " WHERE imp_record_id = ? AND imp_sourceref = ?", recordId, recordRef);
                        context.commit();
                    }
                }
            }
            if(failures>0) {
                message = I18nUtil.getMessage("jsp.dspace.imprecord-list.failure." + sourceRef, new Object[] {successes, failures}, context.getCurrentLocale(), false);
            }
            else {
                if(successes > 0) {
                    message = I18nUtil.getMessage("jsp.dspace.imprecord-list.success." + sourceRef, new Object[] {successes}, context.getCurrentLocale(), false);
                }
                else {
                    message = I18nUtil.getMessage("jsp.dspace.imprecord-list.reject." + sourceRef, new Object[] {selectedIds.length}, context.getCurrentLocale(), false);
                }
            }
            if(StringUtils.isNotBlank(message)) {
                request.getSession().setAttribute(Constants.MESSAGES_KEY, Arrays.asList(message));
            }
        }
        
        response.sendRedirect(request.getContextPath() + "/cris/rp/" + crisID);
        context.restoreAuthSystemState();
        
    }

}
