package com.atmire.statistics.export;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Date: 24/04/12
 * Time: 15:02
 *
 * @author: Kevin Van Ransbeeck (kevin van ransbeeck @ atmire dot com)
 */
public class OpenUrlTrackerLogger {
    /**
     * Our context
     */
    private Context myContext;
    /**
     * The row in the table representing this eperson
     */
    private TableRow myRow;
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(OpenUrlTrackerLogger.class);

    public OpenUrlTrackerLogger(Context context, TableRow row) {
        myContext = context;
        myRow = row;
        context.cache(this, row.getIntColumn("tracker_id"));
        try {
            myContext.commit();
        } catch (SQLException ignored) {
        }

    }

    public static OpenUrlTrackerLogger find(Context context, int id) throws SQLException {
        OpenUrlTrackerLogger fromCache = (OpenUrlTrackerLogger) context.fromCache(OpenUrlTrackerLogger.class, id);
        if (fromCache != null) {
            return fromCache;
        }
        TableRow row = DatabaseManager.find(context, "openurltracker", id);
        if (row == null)
            return null;
        else
            return new OpenUrlTrackerLogger(context, row);
    }

    public static OpenUrlTrackerLogger create(Context context) throws SQLException {
        TableRow row = DatabaseManager.create(context, "openurltracker");
        log.info("Created new OpenUrlTrackerLogger");
        return new OpenUrlTrackerLogger(context, row);
    }

    public void delete() throws SQLException {
        myContext.removeCached(this, getID());
        DatabaseManager.delete(myContext, myRow);
        myContext.commit();
    }

    public int getID() {
        return myRow.getIntColumn("tracker_id");
    }

    public void update() throws SQLException {
        DatabaseManager.update(myContext, myRow);
        myContext.commit();
    }

    public void setUrl(String url) {
        myRow.setColumn("tracker_url", url);
    }

    public void setUploaddate(java.util.Date d) {
        myRow.setColumn("uploaddate", d);
    }

    public String getUrl() {
        return myRow.getStringColumn("tracker_url");
    }

    public java.util.Date getUploaddate() {
        return myRow.getDateColumn("uploaddate");
    }

    public static OpenUrlTrackerLogger[] findAll(Context c) throws SQLException {
        TableRowIterator rows = DatabaseManager.queryTable(c, "openurltracker", "SELECT * FROM openurltracker");
        try {
            List<TableRow> trackerList = rows.toList();
            OpenUrlTrackerLogger[] trackers = new OpenUrlTrackerLogger[trackerList.size()];

            for (int i = 0; i < trackerList.size(); i++) {
                TableRow row = trackerList.get(i);
                OpenUrlTrackerLogger fromCache =
                        (OpenUrlTrackerLogger) c.fromCache(OpenUrlTrackerLogger.class,
                                row.getIntColumn("tracker_id"));
                if (fromCache != null) {
                    trackers[i] = fromCache;
                } else {
                    trackers[i] = new OpenUrlTrackerLogger(c, row);
                }
            }
            return trackers;
        } finally {
            if (rows != null) {
                rows.close();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpenUrlTrackerLogger)) return false;

        OpenUrlTrackerLogger that = (OpenUrlTrackerLogger) o;

        return !(myContext != null ? !myContext.equals(that.myContext) : that.myContext != null) &&
                !(myRow != null ? !myRow.equals(that.myRow) : that.myRow != null);

    }

    @Override
    public int hashCode() {
        int result = myContext != null ? myContext.hashCode() : 0;
        result = 31 * result + (myRow != null ? myRow.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OpenUrlTrackerLogger on " + getUploaddate() + " for URL: " + getUrl() + " #" + getID() + "#";
    }
}
