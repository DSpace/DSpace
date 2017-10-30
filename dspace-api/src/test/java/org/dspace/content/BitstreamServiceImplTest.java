/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.AbstractDSpaceTest;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BitstreamServiceImplTest extends AbstractUnitTest {
    private static final Logger log = Logger.getLogger(BitstreamServiceImplTest.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    @Before
    public void init(){
        super.init();
    }

    protected Bitstream createBitstream() throws SQLException, IOException, AuthorizeException, IllegalAccessException {
        InputStream stubInputStream =
                IOUtils.toInputStream("some test data for my input stream", "UTF-8");
        return bitstreamService.create(context, stubInputStream);
    }

    @Test
    public void testFindAllAuthorizedDoesntIncludePrivateItem() throws SQLException, IOException, AuthorizeException, IllegalAccessException, ParseException {
        Bitstream bitstream = createBitstream();
        context.restoreAuthSystemState();
        UUID id = bitstream.getID();
        List<Group> anonGroups = groupService.search(context, Group.ANONYMOUS);
        for(Group group: anonGroups){
            ResourcePolicy resourcePolicy = authorizeService.createResourcePolicy(context, bitstream, group, context.getCurrentUser(), Constants.READ, "open-access");
            String dateString = "2020-04-01";
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = formatter.parse(dateString);
            resourcePolicy.setStartDate(startDate);
        }
        Iterator<Bitstream> foundBitstreams = bitstreamService.findAllAuthorized(context, 20, 0);
        List<UUID> uuidList = new LinkedList<>();
        while(foundBitstreams.hasNext()){
            Bitstream nextBitstream = foundBitstreams.next();
            uuidList.add(nextBitstream.getID());
        }
        assertFalse(uuidList.contains(id));
    }

    @Test
    public void testFindAllAuthorizedDoesIncludePublicItems() throws SQLException, IOException, AuthorizeException, IllegalAccessException, ParseException{
        Bitstream bitstream = createBitstream();
        context.restoreAuthSystemState();
        UUID id = bitstream.getID();
        List<Group> anonGroups = groupService.search(context, Group.ANONYMOUS);
        for(Group group: anonGroups){
            ResourcePolicy resourcePolicy = authorizeService.createResourcePolicy(context, bitstream, group, null, Constants.READ, ResourcePolicy.TYPE_CUSTOM);
        }
        Iterator<Bitstream> foundBitstreams = bitstreamService.findAllAuthorized(context, 20, 0);
        List<UUID> uuidList = new LinkedList<>();
        while(foundBitstreams.hasNext()){
            Bitstream nextBitstream = foundBitstreams.next();
            uuidList.add(nextBitstream.getID());
        }
        assertTrue(uuidList.contains(id));
    }

    @Test
    public void testFindAllAuthorizedDoesIncludeItemsForWhichEmbargoDateHasPassed() throws SQLException, IOException, AuthorizeException, IllegalAccessException, ParseException{
        Bitstream bitstream = createBitstream();
        context.restoreAuthSystemState();
        UUID id = bitstream.getID();
        List<Group> anonGroups = groupService.search(context, Group.ANONYMOUS);
        for(Group group: anonGroups){
            ResourcePolicy resourcePolicy = authorizeService.createResourcePolicy(context, bitstream, group, context.getCurrentUser(), Constants.READ, "open-access");
            String dateString = "2000-04-01";
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = formatter.parse(dateString);
            resourcePolicy.setStartDate(startDate);
        }
        Iterator<Bitstream> foundBitstreams = bitstreamService.findAllAuthorized(context, 20, 0);
        List<UUID> uuidList = new LinkedList<>();
        while(foundBitstreams.hasNext()){
            Bitstream nextBitstream = foundBitstreams.next();
            uuidList.add(nextBitstream.getID());
        }
        assertTrue(uuidList.contains(id));
    }
}
