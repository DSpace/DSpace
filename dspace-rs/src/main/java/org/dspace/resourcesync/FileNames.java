/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import java.io.File;

/**
 * @author Richard Jones
 *
 */
public class FileNames
{
    public static String resourceSyncDocument = "sourcedescription.xml";
    public static String resourceSyncDocumentIndex = "sourcedescriptionindex.xml";
    public static String resourceList = "resourcelist.xml";
    public static String resourceDumpZip = "resourcedump.zip";
    public static String resourceDump = "resourcedump.xml";
    public static String capabilityList = "capabilitylist.xml";
    public static String resourceDumpManifest = "manifest.xml";
    public static String changeDumpZip = "changedump.zip";
    public static String changeDumpManifest = "manifest.xml";
    public static String changeListArchive = "changelistindex.xml";
    public static String dumpResourcesDir = "resources";
    
    public static String changeList(String dateString)
    {
        return "changelist_" + dateString + ".xml";

    }
    public static String changeDump(String dateString)
    {
        return "changedump_" + dateString + ".zip";

    }
    public static boolean isChangeList(File file)
    {
        return file.getName().startsWith("changelist_");
    }
    public static boolean isChangeDump(File file)
    {
        return file.getName().startsWith("changedump_");
    }
    public static String changeListDate(File file)
    {
        return FileNames.changeListDate(file.getName());
    }

    public static String changeListDate(String filename)
    {
        int start = "changelist_".length(); // 11
        int end = filename.length() - ".xml".length();
        String dr = filename.substring(start, end);
        return dr;
    }
    public static String changeDumpDate(File file)
    {
        return FileNames.changeDumpDate(file.getName());
    }

    public static String changeDumpDate(String filename)
    {
        int start = "changedump_".length(); // 11
        int end = filename.length() - ".zip".length();
        String dr = filename.substring(start, end);
        return dr;
    }
}
