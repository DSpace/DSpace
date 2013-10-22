/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class LookupProvidersCheck
{
    private List<String> providersOk = new ArrayList<String>();

    private List<String> providersErr = new ArrayList<String>();

    public List<String> getProvidersOk()
    {
        return providersOk;
    }

    public void setProvidersOk(List<String> providersOk)
    {
        this.providersOk = providersOk;
    }

    public List<String> getProvidersErr()
    {
        return providersErr;
    }

    public void setProvidersErr(List<String> providersErr)
    {
        this.providersErr = providersErr;
    }

}
