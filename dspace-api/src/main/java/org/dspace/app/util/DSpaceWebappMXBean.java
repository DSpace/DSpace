/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.util;

/**
 * MBean type for discovering DSpace web applications.
 *
 * @author mwood
 */
public interface DSpaceWebappMXBean
{
    /** 
     * Is this webapp a user interface?  False if machine interface such as SWORD.
     * @return true/false  
     */
    public boolean isUI();

    /** 
     * What kind of webapp?  XMLUI, OAI, etc.
     * @return kind of webapp
     */
    public String getKind();

    /** 
     * What is the base URL of this application?
     * @return base url
     */
    public String getURL();

    /** 
     * When did this application start?
     * @return start time
     */
    public String getStarted();
}
