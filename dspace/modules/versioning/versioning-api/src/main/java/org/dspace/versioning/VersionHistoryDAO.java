package org.dspace.versioning;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 30, 2011
 * Time: 12:37:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class VersionHistoryDAO {

    protected final static String TABLE_NAME="versionhistory";
    protected final static String VERSION_HISTORY_ID = "versionhistory_id";


    public VersionHistoryImpl create(Context context) {
        try {
            TableRow row = DatabaseManager.create(context, TABLE_NAME);
            VersionHistoryImpl vh = new VersionHistoryImpl(context, row);

            //TODO Do I have to manage the event?
            //context.addEvent(new Event(Event.CREATE, Constants.EPERSON, e.getID(), null));

            return vh;

        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }



    public VersionHistoryImpl find(Context context, int itemID, VersionDAO versionDAO) {
        try {

            Version version = versionDAO.findByItemId(context, itemID);

            if(version==null) return null;

            //VersionHistoryImpl fromCache = (VersionHistoryImpl) context.fromCache(VersionHistoryImpl.class, version.getVersionHistoryID());
            //if (fromCache != null) return fromCache;

            TableRow row = DatabaseManager.find(context, TABLE_NAME, version.getVersionHistoryID());

            VersionHistoryImpl vh = new VersionHistoryImpl(context, row);
            List<Version> versions= versionDAO.findByVersionHistory(context, vh.getVersionHistoryId());
            vh.setVersions(versions);
            return vh;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    public VersionHistoryImpl findById(Context context, int id, VersionDAO versionDAO) {

        try {
            TableRow row = DatabaseManager.find(context, TABLE_NAME, id);

            if (row == null) return null;

            VersionHistoryImpl fromCache = (VersionHistoryImpl) context.fromCache(VersionHistoryImpl.class, row.getIntColumn(VERSION_HISTORY_ID));

            if (fromCache != null) return fromCache;

            VersionHistoryImpl versionHistoryImpl = new VersionHistoryImpl(context, row);

            List<Version> versions= versionDAO.findByVersionHistory(context, versionHistoryImpl.getVersionHistoryId());
            versionHistoryImpl.setVersions(versions);
            return versionHistoryImpl;

        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public void delete(Context c, int versionHistoryID, VersionDAO versionDAO) {
        try {
            VersionHistoryImpl history = findById(c, versionHistoryID, versionDAO);
            DatabaseManager.delete(c, history.getMyRow());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


}
