/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.util;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class SubmissionLookupDTO implements Serializable
{
    private static final long serialVersionUID = 1;

    private String uuid;

    private List<ItemSubmissionLookupDTO> items;

    public SubmissionLookupDTO()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public void setItems(List<ItemSubmissionLookupDTO> items)
    {
        this.items = items;
    }

    public ItemSubmissionLookupDTO getLookupItem(String uuidLookup)
    {
        if (items != null)
        {
            for (ItemSubmissionLookupDTO item : items)
            {
                if (item.getUUID().equals(uuidLookup))
                {
                    return item;
                }
            }
        }
        return null;
    }
}
