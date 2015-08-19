/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * The members from a role, can either
 * contains a list of epersons or groups
 * 
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class RoleMembers {

    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    private ArrayList<Group> groups;
    private ArrayList<EPerson> epersons;

    public RoleMembers(){
        this.groups = new ArrayList<>();
        this.epersons = new ArrayList<>();
    }

    public ArrayList<Group> getGroups(){
        return groups;
    }
    public ArrayList<EPerson> getEPersons(){
        return epersons;
    }

    public void addGroup(Group group){
        groups.add(group);
    }
    public void addEPerson(EPerson eperson){
        epersons.add(eperson);
    }
    public void removeEperson(EPerson epersonToRemove){
        for(EPerson eperson: epersons){
            if(eperson.equals(epersonToRemove))
                epersons.remove(eperson);
        }
    }
    public ArrayList<EPerson> getAllUniqueMembers(Context context) throws SQLException {
        HashMap<UUID, EPerson> epersonsMap = new HashMap();
        for(EPerson eperson: epersons){
            epersonsMap.put(eperson.getID(), eperson);
        }
        for(Group group: groups){
            for(EPerson eperson: groupService.allMembers(context, group)){
                epersonsMap.put(eperson.getID(), eperson);
            }
        }
        return new ArrayList<>(epersonsMap.values());
    }
}
