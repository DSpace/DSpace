
import java.sql.*;
import java.util.*;

import net.handle.hdllib.*;
import net.handle.util.StreamTable;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;

import org.dspace.handle.HandleManager;


public class ETDSamples
{

  public static void
  main(String[] args)
    throws Exception
    {
		PropertyConfigurator.configure("log4j.properties");
		  
		// Setup the context
		Context context = new Context();
		context.setIgnoreAuthorization(true);
		
		// Get Collection 
		Collection coll = Collection.find(context, 1); // UM T & D
		
		for (ItemIterator j = coll.getItems(); j.hasNext(); ) {
			 // Get item
		  Item item = j.next();

		  String title = (item.getDC("title", null, Item.ANY))[0].value;
		  if (title.length() > 70) {
			 title = title.substring(0,70);
		  }
		  
		  System.out.print(title);
		  System.out.print("::" + item.getHandle());

		  Bundle bundle[] = item.getBundles();
		  for (int i=0; i < bundle.length; i++) {
				if (bundle[i].getName().equals("ORIGINAL")) {
					 Bitstream bits[] = bundle[i].getBitstreams();

					 for (int k=0; k < bits.length; k++) {
						  String name = bits[k].getName();
						  BitstreamFormat format = bits[k].getFormat();
						  System.out.print("::" + name);
					 }
						  
					 
				}
		  }
		  System.out.println();
		}

		// Loop through the items
      context.complete();
    }

}



