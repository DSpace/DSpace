package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.TableRow;


import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 29, 2011
 * Time: 3:11:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class VersionImpl implements Version {

    private int versionId;
    private EPerson eperson;
    private int itemID=-1;

    private Date versionDate;
    private int versionNumber;
    private String summary;
    private int versionHistoryID;
    private Context myContext;
    private TableRow myRow;

    protected VersionImpl(Context c, TableRow row){
        myContext = c;
        myRow = row;

        c.cache(this, row.getIntColumn(VersionDAO.VERSION_ID));
    }


    public int getVersionId() {
        return myRow.getIntColumn(VersionDAO.VERSION_ID);
    }

    private void setVersionId(int versionId) {
        this.versionId = versionId;
    }

    public EPerson getEperson(){
        try {
            if (eperson == null)
                return EPerson.find(myContext, myRow.getIntColumn(VersionDAO.EPERSON_ID));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return eperson;
    }

    public void setEperson(EPerson ePerson) {
        this.eperson = ePerson;
        myRow.setColumn(VersionDAO.EPERSON_ID, ePerson.getID());
    }

    public int getItemID() {
        return myRow.getIntColumn(VersionDAO.ITEM_ID);
    }


    public Item getItem(){
        try{
            if(getItemID()==-1) return null;

            return Item.find(myContext, getItemID());
            
        }catch(SQLException e){
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
        myRow.setColumn(VersionDAO.ITEM_ID, itemID);
    }

    public Date getVersionDate() {
        return myRow.getDateColumn(VersionDAO.VERSION_DATE);
    }

    public void setVersionDate(Date versionDate) {
        this.versionDate = versionDate;
        myRow.setColumn(VersionDAO.VERSION_DATE, versionDate);
    }

    public int getVersionNumber() {
        return myRow.getIntColumn(VersionDAO.VERSION_NUMBER);
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
        myRow.setColumn(VersionDAO.VERSION_NUMBER, versionNumber);
    }

    public String getSummary() {
        return myRow.getStringColumn(VersionDAO.VERSION_SUMMARY);
    }

    public void setSummary(String summary) {
        this.summary = summary;
        myRow.setColumn(VersionDAO.VERSION_SUMMARY, summary);
    }


    public int getVersionHistoryID() {
        return myRow.getIntColumn(VersionDAO.HISTORY_ID);
    }

    public void setVersionHistory(int versionHistoryID) {
        this.versionHistoryID = versionHistoryID;
        myRow.setColumn(VersionDAO.HISTORY_ID, versionHistoryID);
    }


    public Context getMyContext(){
        return myContext;
    }

    protected TableRow getMyRow(){
        return myRow;
    }
      
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionImpl version = (VersionImpl) o;

        if (getVersionId() != version.getVersionId()) return false;

        return true;
    }

    @Override
    public int hashCode() {
         int hash=7;
        hash=79*hash+(int) (this.getVersionId() ^ (this.getVersionId() >>> 32));
        return hash;
    }
}
