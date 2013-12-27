/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.identifier.doi.DOIConnector;
import org.dspace.identifier.doi.DOIIdentifierException;

/**
 *
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class MockDOIConnector
extends MockUp<DOIConnector>
implements org.dspace.identifier.doi.DOIConnector
{

    public Map<String, Integer> reserved;
    public Map<String, Integer> registered;
    
    public MockDOIConnector()
    {
        reserved = new HashMap<String, Integer>();
        registered = new HashMap<String, Integer>();
    }
    
    public void reset()
    {
        reserved.clear();
        registered.clear();
    }
            
    @Override
    @Mock
    public boolean isDOIReserved(Context context, String doi)
            throws DOIIdentifierException
    {
        return reserved.containsKey(doi);
    }

    @Override
    @Mock
    public boolean isDOIReserved(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        if (null == doi)
        {
            throw new NullPointerException();
        }
        Integer itemId = reserved.get(doi);
        return (itemId != null && itemId.intValue() == dso.getID()) ? true : false;
    }

    @Override
    @Mock
    public boolean isDOIRegistered(Context context, String doi)
            throws DOIIdentifierException
    {
        return registered.containsKey(doi);
    }

    @Override
    @Mock
    public boolean isDOIRegistered(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        if (null == doi)
        {
            throw new NullPointerException();
        }
        Integer itemId = registered.get(doi);
        return (itemId != null && itemId.intValue() == dso.getID()) ? true : false;
    }

    @Override
    @Mock
    public void deleteDOI(Context context, String doi)
            throws DOIIdentifierException
    {
        if (reserved.remove(doi) == null)
        {
            throw new DOIIdentifierException("Trying to delete a DOI that was "
                    + "never reserved!", DOIIdentifierException.DOI_DOES_NOT_EXIST);
        }
        registered.remove(doi);
    }

    @Override
    @Mock
    public void reserveDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        Integer itemId = reserved.get(doi);
        if (null != itemId)
        {
            if (dso.getID() == itemId.intValue())
            {
                return;
            }
            else
            {
                throw new DOIIdentifierException("Trying to reserve a DOI that "
                        + "is reserved for another object.",
                        DOIIdentifierException.MISMATCH);
            }
        }
        reserved.put(doi, new Integer(dso.getID()));
    }

    @Override
    @Mock
    public void registerDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        if (!reserved.containsKey(doi))
        {
            throw new DOIIdentifierException("Trying to register an unreserverd "
                    + "DOI.", DOIIdentifierException.RESERVE_FIRST);
        }
        
        if (reserved.get(doi).intValue() != dso.getID())
        {
            throw new DOIIdentifierException("Trying to register a DOI that is"
                    + " reserved for another item.", DOIIdentifierException.MISMATCH);
        }
        
        if (registered.containsKey(doi))
        {
            if (registered.get(doi).intValue() == dso.getID())
            {
                return;
            }
            else
            {
                throw new DOIIdentifierException("Trying to register a DOI that "
                        + "is registered for another item.",
                        DOIIdentifierException.MISMATCH);
            }
        }
        
        registered.put(doi, new Integer(dso.getID()));
    }

    @Override
    @Mock
    public void updateMetadata(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException
    {
        if (!reserved.containsKey(doi))
        {
            throw new DOIIdentifierException("Trying to update a DOI that is not "
                    + "registered!", DOIIdentifierException.DOI_DOES_NOT_EXIST);
        }
        if (reserved.get(doi).intValue() != dso.getID())
        {
            throw new DOIIdentifierException("Trying to update metadata of an "
                    + "unreserved DOI.", DOIIdentifierException.DOI_DOES_NOT_EXIST);
        }
    }
    
}