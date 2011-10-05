package org.dspace.versioning;

import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 30, 2011
 * Time: 2:38:42 PM
 * To change this template use File | Settings | File Templates.
 */
public interface VersionHistory {

    public Version getLatestVersion();
    public Version getFirstVersion();
    public List<Version> getVersions();
    public int getVersionHistoryId();
    public Version getPrevious(Version version);
    public Version getNext(Version version);
    public boolean hasNext(Version version);
    public void add(Version version);
    public Version getVersion(org.dspace.content.Item item);
    public boolean hasNext(org.dspace.content.Item item);
    public boolean isFirstVersion(Version version);
    public boolean isLastVersion(Version version);
    public void remove(Version version);
    public boolean isEmpty();
    public int size();
}
