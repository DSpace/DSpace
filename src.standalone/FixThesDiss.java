
import java.sql.*;
import java.util.*;

import net.handle.hdllib.*;
import net.handle.util.StreamTable;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;

import org.dspace.handle.HandleManager;

// for every collection {
//   if the collection's name is 'XXX Research Works' {
//     create a new collection 'XXX Theses and Dissertations'
//     put the new collection in the same subcommunity as 'XXX Research Works'
//     for every item in 'XXX Research Works' {
// 	 if the item is also in 'UM Theses and Dissertations' {
// 	   put the item in 'XXX Theses and Dissertations'
// 	   remove the item from 'XXX Research Works'
// 	 }
//     }
// }

public class FixThesDiss
{
  
  public static void
  main(String[] args)
    throws Exception
    {
      PropertyConfigurator.configure("log4j.properties");

      // Setup the context
      Context context = new Context();
      context.setIgnoreAuthorization(true);

      // Get the id of the UM Theses and Dissertations collection
      int id = 0;
      { 
	// Get all collections
	Collection colls[] = Collection.findAll(context);
	for (int i=0; i < colls.length; i++) {
	  if (colls[i].getMetadata("name").equals("UM Theses and Dissertations")) {
	    id = colls[i].getID();
	  }
	}
      }
      
      // Get all collections
      Collection colls[] = Collection.findAll(context);
      for (int i=0; i < colls.length; i++) {

	Collection collOld = colls[i];
	String collOldName = collOld.getMetadata("name").trim();

	System.out.println("\n" + collOldName);

	// Handle Research Works
	if (collOldName.endsWith("Research Works")) {
	  
	  // Create the new collection
	  String collNewName = collOldName.replaceFirst("Research Works", "Theses and Dissertations");

	  Community comms[] = collOld.getParents();
	  Collection collNew = comms[0].createCollection();
	  collNew.setMetadata("name", collNewName);
	  collNew.update();

	  for (int a=1; a < comms.length; a++) {
	    comms[a].addCollection(collNew);
	  }

	  System.out.println("  created: " + collNewName);

	  // Get all items in the old collection
	  for (ItemIterator j = collOld.getItems(); j.hasNext(); ) {
	    Item item = j.next();

	    String title = (item.getDC("title", null, Item.ANY))[0].value;
	    if (title.length() > 70) {
	      title = title.substring(0,70);
	    }

	    System.out.println("  item: " + title);

	    // Get all collections the item is in
	    Collection itemcolls[] = item.getCollections();
	    for (int k=0; k < itemcolls.length; k++) {

	      // If it's in UM Theses and Dissertations
	      if (itemcolls[k].getID() == id) {
	        // Move to the new collection
		collOld.removeItem(item);
		collNew.addItem(item);
		System.out.println("    moved");
	      }
	    }
	  }
	}
      }

      context.commit();
      context.complete();
    }

//	// Change the name of the community
//	{
//	  Community comms[] = Community.findAll(context);
//	  for (int j=0; j < comms.length; j++) {
//	    Community comm = comms[j];
//	    if (comm.getMetadata("name").equals("School of Architecture, Planning, & Preservation")) {
//	      comm.setMetadata("name", "College of Architecture, Planning, & Preservation");
//	      comm.update();
//	    }
//	  }
//	}
//	context.commit();
//
//	// Get Collection list
//	Collection colls[] = Collection.findAll(context);
//	for (int i=0; i < colls.length; i++) {
//	  Collection coll = colls[i];
//	  String collName = coll.getMetadata("name");
//	  System.out.println("Collection: " + collName);
//
//	  // Renaming
//	  if (collName.equals("Materials Science & Engineering")) {
//	    collName = "Materials & Science Engineering";
//	  } else if (collName.equals("Biological Resources Engineering")) {
//	    collName = "Biological Resources & Engineering";
//	  } else if (collName.equals("Natural Resource Sciences & Landscape Architecture")) {
//	    collName = "Natural Resource & Engineering";
//	  } else if (collName.equals("Nutrition & Food Science")) {
//	    collName = "Nutrition & Food Sciences";
//	  } else if (collName.equals("Communication")) {
//	    collName = "Communications";
//	  } else if (collName.equals("Languages, Literatures & Cultures")) {
//	    collName = "Languages, Literatures & Culture";
//	  }
//
//	  String subcommName = collName;
//	  if (collName.equals("UMIACS Technical Reports")) {
//	    collName = "Technical Reports from UMIACS";
//	  } else if (! collName.equals("UM Theses and Dissertations") &&
//		     ! collName.equals("Computer Science Department Technical Reports")) {
//	    collName += " Research Works";
//	  }
//
//	  // Get the community
//	  Community comms[] = coll.getCommunities();
//	  if (comms.length != 1) {
//	    System.out.println("Error: collection has <> 1 communities");
//	    continue;
//	  }
//	  Community comm = comms[0];
//	  String commName = comm.getMetadata("name");
//	  System.out.println("  Community: " + commName);
//
//	  // Create the new subcommunity
//	  Community subcomm = comm.createSubcommunity();
//	  subcomm.setMetadata("name", subcommName);
//	  subcomm.setMetadata("group_id", comm.getIntMetadata("group_id"));
//	  subcomm.update();
//
//	  // Move the collection to the subcommunity
//	  subcomm.addCollection(coll);
//	  comm.removeCollection(coll);
//
//	  // Rename the collection
//	  coll.setMetadata("name", collName);
//	  coll.update();
//
//	  context.commit();
//	}
//
//	context.complete();
//
//    }

}



