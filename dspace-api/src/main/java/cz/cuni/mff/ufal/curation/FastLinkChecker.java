/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import cz.cuni.mff.ufal.curation.LinkChecker;

public class FastLinkChecker extends LinkChecker
{

    private static final int MAX_ERRORS = 10;    

    /**
     * Get the URLs to check
     * 
     * @param item
     *            The item to extract URLs from
     * @return An array of URL Strings
     */
    protected List<String> getURLs(Item item)
    {
        // Get URIs from anyschema.anyelement.uri.*
        Metadatum[] urls = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        ArrayList<String> theURLs = new ArrayList<String>();
        for (Metadatum url : urls)
        {
            if ((url.value.startsWith("http://")) || (url.value.startsWith("https://"))
                    && !(url.schema.equals("dc") && url.element.equals("identifier")))
            {                
                theURLs.add(url.value);
            }
        }
        return theURLs;
    }
    
    /**
     * Checks whether we can continue performing tasks 
     * 
     * @return False if no more checking should be done
     */
    protected boolean canContinue()
    {        
        return errors < MAX_ERRORS; 
    }

}
