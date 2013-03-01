package ar.edu.unlp.sedici.aspect.collectionViewer;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;


import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

/**
* Display a single collection. This includes a full text search, browse by
* list, community display and a list of recent submissions.
* 
* @author Scott Phillips
* @author Kevin Van de Velde (kevin at atmire dot com)
* @author Mark Diggory (markd at atmire dot com)
* @author Ben Bosman (ben at atmire dot com)
*/
public class CommunityViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{

   /** Cached validity object */
   private SourceValidity validity;
   
   /**
    * Generate the unique caching key.
    * This key must be unique inside the space of this component.
    */
   public Serializable getKey()
   {
       try
       {
           DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

           if (dso == null)
           {
               return "0";
           }
               
           return HashUtil.hash(dso.getHandle());
       }
       catch (SQLException sqle)
       {
           // Ignore all errors and just return that the component is not
           // cachable.
           return "0";
       }
   }

   /**
    * Generate the cache validity object.
    * 
    * The validity object will include the collection being viewed and 
    * all recently submitted items. This does not include the community / collection
    * hierarch, when this changes they will not be reflected in the cache.
    */
   public SourceValidity getValidity()
   {
   	if (this.validity == null)
   	{
           Community community = null;
	        try
	        {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	
	            if (dso == null)
               {
                   return null;
               }
	
	            if (!(dso instanceof Community))
               {
                   return null;
               }
	
	            community = (Community) dso;
	
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            // Add the actual collection;
	            validity.add(community);
	
	            this.validity = validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Just ignore all errors and return an invalid cache.
	        }

   	}
   	return this.validity;
   }
   
   
   /**
    * Agregael mets de la comunidad de la collection
    */
   public void addBody(Body body) throws SAXException, WingException,
           UIException, SQLException, IOException, AuthorizeException
   {
       DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
       if (!(dso instanceof Community))
       {
           return;
       }

       Community[] superCommunities = ((Community) dso).getAllParents();
       int i = 0;
       while(i < superCommunities.length) 
       {
    	   if(superCommunities[i].getLogo() != null) 
    	   {
    	       // Agrego la referencia a la comunidad root
    	   	   Division viewer =  body.addDivision("community-view-root","secondary");
    	       ReferenceSet mainInclude = viewer.addReferenceSet("community-view-root", ReferenceSet.TYPE_DETAIL_LIST);
    	       mainInclude.addReference(superCommunities[i]);
    	       break;
    	   }
    	   i++;
       }
   }
   
   /**
    * Recycle
    */
   public void recycle() 
   {   
       // Clear out our item's cache.
       this.validity = null;
       super.recycle();
   }
}

