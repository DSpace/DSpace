/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface EPersonCRISIntegration
{
    public String getResearcher(Integer epersonID);
    
    public List<Choices> getMatches(Context context, HttpServletRequest request, EPerson eperson);
}
