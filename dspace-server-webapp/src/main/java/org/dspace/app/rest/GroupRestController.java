/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.regex.Pattern.compile;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.dspace.app.rest.utils.ContextUtil.obtainContext;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_UUID;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.dspace.app.rest.utils.ContextUtil;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.apache.logging.log4j.Logger;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;

/**
 * This will be the entry point for the api/eperson/groups endpoint with additional paths to it
 */
// /api/eperson/groups
// There is something that has defined this GroupRest.CATEGORY,
// So if i create a new Controller, I will have to create a new Rest for it, just like this one.
@RestController
@RequestMapping("/api/" + GroupRest.CATEGORY + "/" + GroupRest.GROUPS)
public class GroupRestController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemService itemService;


    @Autowired
    Utils utils;

private static Logger log = org.apache.logging.log4j.LogManager.getLogger(GroupRestController.class);


    /**
     * Method to add one or more subgroups to a group.
     * The subgroups to be added should be provided in the request body as a uri-list.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param uuid the uuid of the group to add the subgroups to
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = POST, path = "/{uuid}/subgroups", consumes = {"text/uri-list"})
    public void addChildGroups(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, uuid);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + uuid);
        }

        AuthorizeUtil.authorizeManageGroup(context, parentGroup);

        List<String> groupLinks = utils.getStringListFromRequest(request);

        List<Group> childGroups = new ArrayList<>();
        for (String groupLink : groupLinks) {
            Optional<Group> childGroup = findGroup(context, groupLink);
            if (!childGroup.isPresent() || !canAddGroup(context, parentGroup, childGroup.get())) {
                throw new UnprocessableEntityException("cannot add child group: " + groupLink);
            }
            childGroups.add(childGroup.get());
        }

        for (Group childGroup : childGroups) {
            groupService.addMember(context, parentGroup, childGroup);
        }
        // this is required to trigger the rebuild of the group2group cache
        groupService.update(context, parentGroup);
        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    private Optional<Group> findGroup(Context context, String groupLink) throws SQLException {

        Group group = null;

        Pattern linkPattern = compile("^.*/(" + REGEX_UUID + ")/?$");
        Matcher matcher = linkPattern.matcher(groupLink);
        if (matcher.matches()) {
            group = groupService.find(context, UUID.fromString(matcher.group(1)));
        }

        return Optional.ofNullable(group);
    }

    private boolean canAddGroup(Context context, Group parentGroup, Group childGroup) throws SQLException {

        return !groupService.isParentOf(context, childGroup, parentGroup);
    }

    /**
     * Method to add one or more members to a group.
     * The members to be added should be provided in the request body as a uri-list.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param uuid the uuid of the group to add the members to
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = POST, path = "/{uuid}/epersons", consumes = {"text/uri-list"})
    public void addMembers(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, uuid);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + uuid);
        }

        AuthorizeUtil.authorizeManageGroup(context, parentGroup);

        List<String> memberLinks = utils.getStringListFromRequest(request);

        List<EPerson> members = new ArrayList<>();
        for (String memberLink : memberLinks) {
            Optional<EPerson> member = findEPerson(context, memberLink);
            if (!member.isPresent()) {
                throw new UnprocessableEntityException("cannot add child group: " + memberLink);
            }
            members.add(member.get());
        }

        for (EPerson member : members) {
            groupService.addMember(context, parentGroup, member);
        }

        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    private Optional<EPerson> findEPerson(Context context, String groupLink) throws SQLException {

        EPerson ePerson = null;

        Pattern linkPattern = compile("^.*/(" + REGEX_UUID + ")/?$");
        Matcher matcher = linkPattern.matcher(groupLink);
        if (matcher.matches()) {
            ePerson = ePersonService.find(context, UUID.fromString(matcher.group(1)));
        }

        return Optional.ofNullable(ePerson);
    }

    /**
     * Method to remove a subgroup from a group.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param parentUUID the uuid of the parent group
     * @param childUUID  the uuid of the subgroup which has to be removed
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = DELETE, path = "/{parentUUID}/subgroups/{childUUID}")
    public void removeChildGroup(@PathVariable UUID parentUUID, @PathVariable UUID childUUID,
                                 HttpServletResponse response, HttpServletRequest request)
            throws IOException, SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, parentUUID);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + parentUUID);
        }

        AuthorizeUtil.authorizeManageGroup(context, parentGroup);

        Group childGroup = groupService.find(context, childUUID);
        if (childGroup == null) {
            response.sendError(SC_UNPROCESSABLE_ENTITY);
        }

        groupService.removeMember(context, parentGroup, childGroup);
        // this is required to trigger the rebuild of the group2group cache
        groupService.update(context, parentGroup);
        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    /**
     * Method to remove a member from a group.
     * Note that only the 'AUTHENTICATED' state will be checked in PreAuthorize, a more detailed check will be done by
     * using the 'checkAuthorization' method.
     *
     * @param parentUUID the uuid of the parent group
     * @param memberUUID the uuid of the member which has to be removed
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = DELETE, path = "/{parentUUID}/epersons/{memberUUID}")
    public void removeMember(@PathVariable UUID parentUUID, @PathVariable UUID memberUUID,
                             HttpServletResponse response, HttpServletRequest request)
            throws IOException, SQLException, AuthorizeException {

        Context context = obtainContext(request);

        Group parentGroup = groupService.find(context, parentUUID);
        if (parentGroup == null) {
            throw new ResourceNotFoundException("parent group is not found for uuid: " + parentUUID);
        }

        AuthorizeUtil.authorizeManageGroup(context, parentGroup);

        EPerson childGroup = ePersonService.find(context, memberUUID);
        if (childGroup == null) {
            response.sendError(SC_UNPROCESSABLE_ENTITY);
        }

        groupService.removeMember(context, parentGroup, childGroup);

        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    //  I started with the POST, but I don't think it absolutely 
    //  necessary I swiched to the GET below.  I could also test
    //  the GET with the browser.
    // @RequestMapping(method = POST, value="/subscribe2")
    // //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    // public void subscribeToStats2(HttpServletResponse response, HttpServletRequest request)
    //         throws SQLException, AuthorizeException {    
        
    //     Context context = ContextUtil.obtainContext(request);
        
    //     EPerson currentUser = context.getCurrentUser();
    //     String email = currentUser.getEmail();
 
    //     //I have created this method.
    //     currentUser.DeleteFromIndivStats( context, email);
 
    //     // I have created this method.
    //     currentUser.AddIndivStats ( context, email );

    //     response.setStatus(HttpServletResponse.SC_OK);

    //     return;
    // }

    // This code need further processing to handle if there is an exception, and return
    // A failure Status.  IF there are any problems, just return internal server error.
    @RequestMapping(method = GET, value="/subscribe")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public void subscribeToStats(HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {    
        try {
          Context context = ContextUtil.obtainContext(request);
        
          EPerson currentUser = context.getCurrentUser();
          String email = currentUser.getEmail();
 
          //I have created this method.
          currentUser.DeleteFromIndivStats( context, email);
 
          // I have created this method.
          currentUser.AddIndivStats ( context, email );

          response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return;

    }


    @RequestMapping(method = GET, value="/unsubscribe")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public void unsubscribeToStats(HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {    
        try {
          Context context = ContextUtil.obtainContext(request);
        
          EPerson currentUser = context.getCurrentUser();
          String email = currentUser.getEmail();
 
          //I have created this method.
          currentUser.DeleteFromIndivStats( context, email);

          response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return;

    }

    @RequestMapping(method = GET, value="/issubscribed")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public Boolean issubscribedToStats(HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {     
        try {


          Context context = ContextUtil.obtainContext(request);
          EPerson currentUser = context.getCurrentUser();
          String email = currentUser.getEmail();
 
          //I have created this method.
          Boolean isSubs = currentUser.SendingIndivStats( context, email);
          response.setStatus(HttpServletResponse.SC_OK);

          return isSubs;

        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          return false;
        }

    }




    @RequestMapping(method = GET, value="/issubscribed_admin/{uuid}")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public Boolean issubscribedToAdminStats(@PathVariable String uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {     
        try {

          Context context = ContextUtil.obtainContext(request);
          EPerson currentUser = context.getCurrentUser();
          String email = currentUser.getEmail();
          String collemail = uuid + " : " + email;
          //I have created this method.
          int count = collectionService.IsSubscribedToStats( context, collemail);
          Boolean isSubs = false;

          if ( count > 0 )
          {
            isSubs = true;
          }


          response.setStatus(HttpServletResponse.SC_OK);

          return isSubs;
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          return false;
        }

    }



    @RequestMapping(method = GET, value="/subscribe_admin/{uuid}")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public void subscribeToAdminStats(@PathVariable String uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {  

        try {

          String collectionId = uuid;
          Context context = ContextUtil.obtainContext(request);

          EPerson currentUser = context.getCurrentUser();        

          String email = currentUser.getEmail();       
          String collemail = collectionId + " : " + email;         

          //Mark the DB to do stats
          collectionService.DeleteEmailFromStats(context, collemail);

          //Mark the DB to do stats
          collectionService.InsertEmailFromStats(context, collemail);

          response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return;
    }

    @RequestMapping(method = GET, value="/unsubscribe_admin/{uuid}")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public void unsubscribeToAdminStats(@PathVariable String uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {  

        try {

          String collectionId = uuid;
          Context context = ContextUtil.obtainContext(request);

          EPerson currentUser = context.getCurrentUser();        

          String email = currentUser.getEmail();       
          String collemail = collectionId+ " : " + email;         

          //Mark the DB to do stats
          collectionService.DeleteEmailFromStats(context, collemail);

          response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return;
    }



    @RequestMapping(method = GET, value="/getmonthdatestats")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public String getMonthDateStats(HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {  

        try {

          Context context = ContextUtil.obtainContext(request);

        String colldt = itemService.findMaxCollDtFromStats(context);

        if ( colldt == null )
        {
            colldt = "9999/01";
        }



          response.setStatus(HttpServletResponse.SC_OK);
          return  colldt;
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return "9999/01";
    }



    @RequestMapping(method = GET, value="/getmonthstats/{handle}")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public String getMonthStats(@PathVariable String handle, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {  

        try {

           handle = handle.replace("_", "/");

          //String handle = handle;

          Context context = ContextUtil.obtainContext(request);

        String colldt = itemService.findMaxCollDtFromStats(context);

        if ( colldt == null )
        {
            colldt = "9999/01";
        }

        int count = itemService.getMonthStat(context, handle, colldt, false);

          response.setStatus(HttpServletResponse.SC_OK);
          return  String.valueOf(count);
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return "0";
    }

    @RequestMapping(method = GET, value="/gettotalstats/{handle}")
    //@PreAuthorize("hasPermission(#uuid, 'ITEM', 'ADD')")
    public String getTotalStats(@PathVariable String handle, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {  

        try {

           handle = handle.replace("_", "/");

          //String handle = handle;
          Context context = ContextUtil.obtainContext(request);

        String colldt = itemService.findMaxCollDtFromStats(context);

        if ( colldt == null )
        {
            colldt = "9999/01";
        }

        int count = itemService.getMonthStat(context, handle, "2008/02", true);

    

          response.setStatus(HttpServletResponse.SC_OK);

          return  String.valueOf(count);
        } catch (Exception e) {

          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return "0";
    }




}
