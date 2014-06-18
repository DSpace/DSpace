/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.util.List;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
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
