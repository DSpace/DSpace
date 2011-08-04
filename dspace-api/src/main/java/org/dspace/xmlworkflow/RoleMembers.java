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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

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

    private ArrayList<Group> groups;
    private ArrayList<EPerson> epersons;

    public RoleMembers(){
        this.groups = new ArrayList<Group>();
        this.epersons = new ArrayList<EPerson>();
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
    public void removeEperson(int toRemoveID){
        for(EPerson eperson: epersons){
            if(eperson.getID()==toRemoveID)
                epersons.remove(eperson);
        }
    }
    public ArrayList<EPerson> getAllUniqueMembers(Context context) throws SQLException {
        HashMap<Integer, EPerson> epersonsMap = new HashMap<Integer, EPerson>();
        for(EPerson eperson: epersons){
            epersonsMap.put(eperson.getID(), eperson);
        }
        for(Group group: groups){
            for(EPerson eperson: Group.allMembers(context, group)){
                epersonsMap.put(eperson.getID(), eperson);
            }
        }
        return new ArrayList<EPerson>(epersonsMap.values());
    }
}
