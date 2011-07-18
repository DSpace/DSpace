/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.authority.Choices;

/**
 * Simple data structure-like class representing a Dublin Core value. It has an
 * element, qualifier, value and language.
 *
 * @author Robert Tansley
 * @author Martin Hald
 * @version $Revision: 5844 $
 */
@Deprecated
public class DCValue
{
    /** The DC element */
    public String element;

    /** The DC qualifier, or <code>null</code> if unqualified */
    public String qualifier;

    /** The value of the field */
    public String value;

    /** The language of the field, may be <code>null</code> */
    public String language;

    /** The schema name of the metadata element */
    public String schema;

    /** Authority control key */
    public String authority = null;

    /** Authority control confidence  */
    public int confidence = Choices.CF_UNSET;
}
