/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.eperson.Group;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Utilty methods for the xml workflow
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowUtils extends Util{
    /** log4j category */
    public static Logger log = Logger.getLogger(WorkflowUtils.class);


    /**
     * Return a string for logging, containing useful information about the
     * current request - the URL, the method and parameters.
     *
     * @param request
     *            the request object.
     * @return a multi-line string containing information about the request.
     */
    public static String getRequestLogInfo(HttpServletRequest request)
    {
        String report;

        report = "-- URL Was: " + getOriginalURL(request) + "\n";
        report = report + "-- Method: " + request.getMethod() + "\n";

        // First write the parameters we had
        report = report + "-- Parameters were:\n";

        Enumeration e = request.getParameterNames();

        while (e.hasMoreElements())
        {
            String name = (String) e.nextElement();

            if (name.equals("login_password"))
            {
                // We don't want to write a clear text password
                // to the log, even if it's wrong!
                report = report + "-- " + name + ": *not logged*\n";
            }
            else
            {
                report = report + "-- " + name + ": \""
                        + request.getParameter(name) + "\"\n";
            }
        }

        return report;
    }



    /**
     * Get the original request URL.
     *
     * @param request
     *            the HTTP request
     *
     * @return the original request URL
     */
    public static String getOriginalURL(HttpServletRequest request)
    {
        // Make sure there's a URL in the attribute
        storeOriginalURL(request);

        return ((String) request.getAttribute("dspace.original.url"));
    }



    /**
     * Put the original request URL into the request object as an attribute for
     * later use. This is necessary because forwarding a request removes this
     * information. The attribute is only written if it hasn't been before; thus
     * it can be called after a forward safely.
     *
     * @param request
     *            the HTTP request
     */
    public static void storeOriginalURL(HttpServletRequest request)
    {
        String orig = (String) request.getAttribute("dspace.original.url");

        if (orig == null)
        {
            String fullURL = request.getRequestURL().toString();

            if (request.getQueryString() != null)
            {
                fullURL = fullURL + "?" + request.getQueryString();
            }

            request.setAttribute("dspace.original.url", fullURL);
        }
    }


    /**
     * Send an alert to the designated "alert recipient" - that is, when a
     * database error or internal error occurs, this person is sent an e-mail
     * with details.
     * <P>
     * The recipient is configured via the "alert.recipient" property in
     * <code>dspace.cfg</code>. If this property is omitted, no alerts are
     * sent.
     * <P>
     * This method "swallows" any exception that might occur - it will just be
     * logged. This is because this method will usually be invoked as part of an
     * error handling routine anyway.
     *
     * @param request
     *            the HTTP request leading to the error
     * @param exception
     *            the exception causing the error, or null
     */
    public static void sendAlert(HttpServletRequest request, Exception exception)
    {
        String logInfo = WorkflowUtils.getRequestLogInfo(request);
        Context c = (Context) request.getAttribute("dspace.context");

        try
        {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (StringUtils.isNotBlank(recipient))
            {
                Email email = Email.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "internal_error"));

                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                        .getProperty("dspace.url"));
                email.addArgument(new Date());
                email.addArgument(request.getSession().getId());
                email.addArgument(logInfo);

                String stackTrace;

                if (exception != null)
                {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                }
                else
                {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                email.send();
            }
        }
        catch (Exception e)
        {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }
    }


    /***************************************
     * WORKFLOW ROLE MANAGEMENT
     **************************************/

    /*
     * Creates a role for a collection by linking a group of epersons to a role ID
     */
    public static void createCollectionWorkflowRole(Context context, int collectionId, String roleId, Group group) throws AuthorizeException, SQLException {
        CollectionRole ass = CollectionRole.create(context);
        ass.setCollectionId(collectionId);
        ass.setRoleId(roleId);
        ass.setGroupId(group);
        ass.update();
    }
    /*
     * Deletes a role group linked to a given role and a collection
     */
    public static void deleteRoleGroup(Context context, Collection collection, String roleID) throws SQLException, IOException, WorkflowConfigurationException {
        Workflow workflow = WorkflowFactory.getWorkflow(collection);
        Role role = workflow.getRoles().get(roleID);
        if(role.getScope() == Role.Scope.COLLECTION){
            CollectionRole ass = CollectionRole.find(context, collection.getID(), roleID);
            ass.delete();
        }
    }


    public static HashMap<String, Role> getCollectionRoles(Collection thisCollection) throws IOException, WorkflowConfigurationException {
        Workflow workflow = WorkflowFactory.getWorkflow(thisCollection);
        LinkedHashMap<String, Role> result = new LinkedHashMap<String, Role>();
        if(workflow != null){
            //Make sure we find one
            HashMap<String, Role> allRoles = workflow.getRoles();
            //We have retrieved all our roles, not get the ones which can be configured by the collection
            for(String roleId : allRoles.keySet()){
                Role role = allRoles.get(roleId);
                // We just require the roles which have a scope of collection
                if(role.getScope() == Role.Scope.COLLECTION && !role.isInternal()){
                    result.put(roleId, role);
                }
            }

        }
        return result;
    }


    public static HashMap<String, Role> getCollectionAndRepositoryRoles(Collection thisCollection) throws IOException, WorkflowConfigurationException {
        Workflow workflow = WorkflowFactory.getWorkflow(thisCollection);
        LinkedHashMap<String, Role> result = new LinkedHashMap<String, Role>();
        if(workflow != null){
            //Make sure we find one
            HashMap<String, Role> allRoles = workflow.getRoles();
            //We have retrieved all our roles, not get the ones which can be configured by the collection
            for(String roleId : allRoles.keySet()){
                Role role = allRoles.get(roleId);
                // We just require the roles which have a scope of collection
                if((role.getScope() == Role.Scope.COLLECTION || role.getScope() == Role.Scope.REPOSITORY) && !role.isInternal()){
                    result.put(roleId, role);
                }
            }

        }
        return result;
    }


    public static HashMap<String, Role> getAllExternalRoles(Collection thisCollection) throws IOException, WorkflowConfigurationException {
        Workflow workflow = WorkflowFactory.getWorkflow(thisCollection);
        LinkedHashMap<String, Role> result = new LinkedHashMap<String, Role>();
        if(workflow != null){
            //Make sure we find one
            HashMap<String, Role> allRoles = workflow.getRoles();
            //We have retrieved all our roles, not get the ones which can be configured by the collection
            for(String roleId : allRoles.keySet()){
                Role role = allRoles.get(roleId);
                // We just require the roles which have a scope of collection
                if(!role.isInternal()){
                    result.put(roleId, role);
                }
            }

        }
        return result;
    }

    public static Group getRoleGroup(Context context, int collectionId, Role role) throws SQLException {
        if(role.getScope() == Role.Scope.REPOSITORY){
            return Group.findByName(context, role.getName());
        }else
        if(role.getScope() == Role.Scope.COLLECTION){
            CollectionRole collectionRole = CollectionRole.find(context, collectionId, role.getId());
        if(collectionRole == null)
            return null;

            return collectionRole.getGroup();
        }else
        if(role.getScope() == Role.Scope.ITEM){

        }
        return null;
    }

//    public static List<String> getAllUsedStepIdentifiers(Context context) throws SQLException {
//        TableRowIterator tri = DatabaseManager.queryTable(context, "cwf_claimtask", "SELECT DISTINCT step_id FROM cwf_pooltask UNION SELECT DISTINCT step_id FROM cwf_claimtask");
//        List<String> result = new ArrayList<String>();
//        while(tri.hasNext()){
//            TableRow row = tri.next();
//            result.add(row.getStringColumn("step_id"));
//        }
//        return result;
//    }
}
