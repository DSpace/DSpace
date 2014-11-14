/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.StringHelper;

/**
 * This utility class simply determines the version of a given Solr/Lucene index,
 * so that they can be upgraded to the latest version.
 * <p>
 * You must pass it the full path of the index directory, e.g.
 * [dspace]/solr/statistics/data/index/
 * <p>
 * The response is simply a version number (e.g. 4.4), as this is utilized by
 * the "ant update_solr_indexes" target in [src]/dspace/src/main/config/build.xml
 * 
 * @author tdonohue
 */
public class IndexVersion
{
    public static void main(String[] argv)
            throws IOException
    {
        // Usage checks
        if (argv.length < 1)
        {
            System.out.println("\nRequired Solr/Lucene index directory is missing.");
            System.out.println("Mimimally, pass in the full path of the Solr/Lucene Index directory to analyze");
            System.out.println("Usage: IndexVersion [full-path-to-solr-index] ([version-to-compare])");
            System.out.println("  - [full-path-to-index] is REQUIRED (e.g. [dspace.dir]/solr/statistics/data/index/)");
            System.out.println("  - [version-to-compare] is optional. When specified, this command will return:");
            System.out.println("       -1 if index dir version < version-to-compare");
            System.out.println("        0 if index dir version = version-to-compare");
            System.out.println("        1 if index dir version > version-to-compare");
            System.exit(1);
        }
        
        // First argument is the Index path. Determine its version
        String indexVersion = getIndexVersion(argv[0]);
        
        // Second argumet is an optional version number to compare to
        String compareToVersion = argv.length > 1 ? argv[1] : null;
        
        // If indexVersion comes back as null, then this is likely not a valid directory
        if(indexVersion==null)
        {
            System.out.println("\nRequired Solr/Lucene index directory is invalid.");
            System.out.println("The following path does NOT seem to be a valid index directory:");
            System.out.println(argv[0]);
            System.out.println("Please pass in the full path of the Solr/Lucene Index directory to analyze");
            System.out.println("(e.g. [dspace.dir]/solr/statistics/data/index/)\n");
            System.exit(1);
        }
        // If a compare-to-version was passed in, print the result of this comparison
        else if(compareToVersion!=null && !compareToVersion.isEmpty())
        {
            System.out.println(compareSoftwareVersions(indexVersion,compareToVersion));
        }
        // Otherwise, we'll just print the version of this index directory
        else
        {
            System.out.println(indexVersion);
        }
    }
    
    /**
     * Determine the version of Solr/Lucene which was used to create a given index directory.
     * 
     * @param indexDirPath
     *          Full path of the Solr/Lucene index directory
     * @return version as a string (e.g. "4.4"), or null if directory doesn't exist
     * @throws IOException 
     */
    public static String getIndexVersion(String indexDirPath)
            throws IOException
    {
        String indexVersion = null;
        
        // Make sure this directory exists
        File dir = new File(indexDirPath);
        if(dir.exists() && dir.isDirectory())
        {
            Directory indexDir = FSDirectory.open(dir);

            // Get info on the Lucene segment file(s) in Solr index directory
            SegmentInfos sis = new SegmentInfos();
            try
            {
                sis.read(indexDir);
            }
            catch(Throwable t)
            {
                throw new IOException("Could not read Lucene segments files in " + dir.getAbsolutePath(), t);
            }

            // Loop through our Lucene segment files to locate the OLDEST
            // version. It is possible for individual segment files to be
            // created by different versions of Lucene. So, we just need
            // to find the oldest version of Lucene which created these
            // index segment files. 
            // This logic borrowed from Lucene v.4.9 CheckIndex class:
            // https://github.com/apache/lucene-solr/blob/lucene_solr_4_9/lucene/core/src/java/org/apache/lucene/index/CheckIndex.java#L407
            // WARNING: It MAY require updating whenever we upgrade the 
            // "lucene.version" in our DSpace Parent POM
            String oldest = Integer.toString(Integer.MAX_VALUE);
            String oldSegment = null;
            Comparator<String> versionComparator = StringHelper.getVersionComparator();
            for (SegmentCommitInfo si : sis) 
            {
                // Get the version of Lucene which created this segment file
                String version = si.info.getVersion();
                if(version == null)
                {
                    // If null, then this is a pre-3.1 segment file.
                    // For our purposes, we will just assume it is "3.0", 
                    // This lets us know we will need to upgrade it to 3.5
                    // before upgrading to Solr/Lucene 4.x or above
                    oldSegment = "3.0";
                }
                // else if this segment is older than our oldest thus far
                else if(versionComparator.compare(version, oldest) < 0)
                {
                    // We have a new oldest segment version
                    oldest = version;
                }
            }

            // If we found a really old segment
            if(oldSegment!=null)
            {
                // See if it really is older than any others, just for safety
                if(versionComparator.compare(oldSegment, oldest) < 0)
                {
                    oldest = oldSegment;
                }
            }

            // Verify we only have *one* decimal point. We want sub-minor
            // versions to be dropped, so 4.10.2 becomes simply 4.10
            String[] parts = oldest.split(".");
            if(parts.length>2)
            {
                oldest = parts[0] + "." + parts[1];
            }

            // At this point, we know what version of Lucene created our
            // oldest segment file. We will return this as the Index version
            // as it's the oldest segment we will need to upgrade.
            indexVersion = oldest;
        }
        
        return indexVersion;
    }
    
    /**
     * Compare two software version numbers to see which is greater. ONLY does
     * a comparison of *major* and *minor* versions (any sub-minor versions are
     * stripped and ignored).
     * <P>
     * This method returns -1 if firstVersion is less than secondVersion,
     * 1 if firstVersion is greater than secondVersion, and 0 if equal.
     * <P>
     * However, since we ignore sub-minor versions, versions "4.2.1" and "4.2.5"
     * will be seen as EQUAL (as "4.2" = "4.2").
     * <P>
     * NOTE: In case it is not obvious, software version numbering does NOT 
     * behave like normal decimal numbers. For example, in software versions
     * the following statement is TRUE: 4.1 < 4.4 < 4.5 < 4.10 < 4.21 < 4.51
     * 
     * @param firstVersion
     *          First version to compare, as a String
     * @param secondVersion
     *          Second version to compare as a String
     * @return -1 if first < second, 1 if first > second, 0 if equal
     * @throws IOException 
     */
    public static int compareSoftwareVersions(String firstVersion, String secondVersion)
            throws IOException
    {
        // Constants which represent our various return values for this comparison
        int GREATER_THAN = 1;
        int EQUAL = 0;
        int LESS_THAN = -1;
        
        // "null" is less than anything
        if(firstVersion==null)
            return LESS_THAN;
        // Anything is newer than "null"
        if(secondVersion==null)
            return GREATER_THAN;
            
        //Split the first version into it's parts (i.e. major & minor versions)
        String[] firstParts = firstVersion.split("\\.");
        String[] secondParts = secondVersion.split("\\.");
        
        // Get major / minor version numbers. Default to "0" if unspecified
        // NOTE: We are specifically IGNORING any sub-minor version numbers
        int firstMajor = firstParts.length>=1 ? Integer.parseInt(firstParts[0]) : 0;
        int firstMinor = firstParts.length>=2 ? Integer.parseInt(firstParts[1]) : 0;
        int secondMajor = secondParts.length>=1 ? Integer.parseInt(secondParts[0]) : 0;
        int secondMinor = secondParts.length>=2 ? Integer.parseInt(secondParts[1]) : 0;
        
        // Check for equality
        if(firstMajor==secondMajor && firstMinor==secondMinor)
        {
            return EQUAL;
        }
        // If first major version is greater than second
        else if(firstMajor > secondMajor)
        {
            return GREATER_THAN;
        }
        // If first major version is less than second
        else if(firstMajor < secondMajor)
        {
            return LESS_THAN;
        }
        // If we get here, major versions must be EQUAL. Now, time to check our minor versions
        else if(firstMinor > secondMinor)
        {
            return GREATER_THAN;
        }
        else if(firstMinor < secondMinor)
        {
            return LESS_THAN;
        }
        else
        {
            // This is an impossible scenario. 
            // This 'else' should never be triggered since we've checked for equality above already
            return EQUAL;
        }
    }
}
