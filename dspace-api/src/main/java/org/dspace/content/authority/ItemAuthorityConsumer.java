/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

/**
 * Sample authority to link a dspace item with another (i.e a publication with
 * the corresponding dataset or viceversa)
 *
 * @author Andrea Bollini
 * @version $Revision $
 */
public class ItemAuthorityConsumer implements Consumer
{
    private static final Logger log = Logger.getLogger(ItemAuthorityConsumer.class);
    
    private Map<String, String> reciprocalMetadata = new ConcurrentHashMap<String, String>();
    
    private transient Set<String> processedHandles = new HashSet<String>();
    
	public ItemAuthorityConsumer() {
		for (Object confObj : ConfigurationManager.getProperties().keySet()) {
			String conf = (String) confObj;
			if (conf.startsWith("ItemAuthority.reciprocalMetadata.")) {
				reciprocalMetadata.put(conf.substring("ItemAuthority.reciprocalMetadata.".length()),
						ConfigurationManager.getProperty(conf));
				reciprocalMetadata.put(ConfigurationManager.getProperty(conf),
						conf.substring("ItemAuthority.reciprocalMetadata.".length()));
			}
		}
	}
    
    public void initialize()
        throws Exception
    {
       
    }

    public void consume(Context ctx, Event event)
        throws Exception
    {
    	try
    	{
	    	ctx.turnOffAuthorisationSystem();
	    	Item item = (Item) event.getSubject(ctx);
	    	if (item == null || !item.isArchived()) return;
	    	if (processedHandles.contains(item.getHandle())) {
	    		return;
	    	}
	    	else {
	    		processedHandles.add(item.getHandle());
	    	}
	        boolean needCommit = false;
	        if (reciprocalMetadata != null) {
	        	for (String m : reciprocalMetadata.keySet()) {
	        		needCommit = needCommit || checkItemRefs(ctx, item, m);
	        	}
	        }
	        
	    	if (needCommit) {
	    		// reciprocal link need to be wrote to the DB, so we have to commit.
	    		// use directly the db connection to avoid multiple roundtrip in this consumer
	    		ctx.getDBConnection().commit();
	    	}
    	}
    	finally {
    		ctx.restoreAuthSystemState();
    	}
    }

	private boolean checkItemRefs(Context ctx, Item item, String m) throws SQLException {
		boolean needCommit = false;
		Metadatum[] meta = item.getMetadataByMetadataString(m);
		if (meta != null) {
			for (Metadatum md : meta) {
				if (md.authority != null) {
					DSpaceObject dso = HandleManager.resolveToObject(ctx, md.authority);
		    		if (dso != null && dso instanceof Item) {
		    			Item target = (Item) dso;
		    			needCommit = needCommit || assureReciprocalLink(target, reciprocalMetadata.get(m), item.getName(), item.getHandle());
		    		}			
				}
			}
		}
		return needCommit;
	}

    private boolean assureReciprocalLink(Item target, String mdString, String name, String handle) {
    	Metadatum[] meta = target.getMetadataByMetadataString(mdString);
    	String[] mdSplit = mdString.split("\\.");
    	target.clearMetadata(mdSplit[0], mdSplit[1], mdSplit.length>2?mdSplit[2]:null, Item.ANY);
    	boolean added = false;
		if (meta != null) {
			for (Metadatum md : meta) {
				if (StringUtils.equals(md.authority, handle)) {
					if (!StringUtils.equals(md.value, name)) {
						md.value = name;
						added = true;
					}
					else {
						return false;
					}
				}
				target.addMetadata(mdSplit[0], mdSplit[1], mdSplit.length>2?mdSplit[2]:null, null, md.value, md.authority, md.confidence);
			}
		}
		if (!added) {
			target.addMetadata(mdSplit[0], mdSplit[1], mdSplit.length>2?mdSplit[2]:null, null, name, handle, Choices.CF_ACCEPTED);
		}
    	try {
    		target.updateMetadata();
		} catch (SQLException | AuthorizeException e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	public void end(Context ctx)
        throws Exception
    {
    	// nothing
		processedHandles.clear();
    }
    
    public void finish(Context ctx) 
    {
    	// nothing
    }

}
