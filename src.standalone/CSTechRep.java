
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
import org.dspace.content.DCValue;

import org.dspace.handle.HandleManager;

// for every collection {
//   if the collection's name is 'XXX Research Works' {
//     create a new collection 'XXX Theses and Dissertations'
//     put the new collection in the same subcommunity as 'XXX Research Works'
//     for every item in 'XXX Research Works' {
//       if the item is also in 'UM Theses and Dissertations' {
//         put the item in 'XXX Theses and Dissertations'
//         remove the item from 'XXX Research Works'
//       }
//     }
// }

public class CSTechRep
{
  
  public static void main(String[] args) throws Exception {

    PropertyConfigurator.configure("log4j.properties");

    long lCS = 0;
    long lUmiacs = 0;

    // Setup the context
    Context context = new Context();
    context.setIgnoreAuthorization(true);

    // Get the collections
    Collection collCS = null;
    Collection collUmiacs = null;

    // Get all collections
    Collection colls[] = Collection.findAll(context);
    for (int i=0; i < colls.length; i++) {
      if (colls[i].getMetadata("name").equals("Technical Reports of the Computer Science Department")) {
        collCS = colls[i];
      }
      else if (colls[i].getMetadata("name").equals("Technical Reports from UMIACS")) {
        collUmiacs = colls[i];
      }
    }

    if (collCS == null || collUmiacs == null) {
      throw new Exception("can't find collections");
    }

    // Loop through collCS items
    System.out.println("=== Technical Reports of the Computer Science Department ===");
    for (ItemIterator i = collCS.getItems(); i.hasNext(); ) {
      Item item = i.next();

      String title = (item.getDC("title", null, Item.ANY))[0].value;
      if (title.length() > 70) {
        title = title.substring(0,70);
      }
      System.out.println();
      System.out.println(title);

      DCValue series[] = item.getDC("relation", "ispartofseries", Item.ANY);
      boolean bCS = false;
      boolean bUmiacs = false;
      for (int j=0; j < series.length; j++) {
        if (series[j].value.indexOf("CS-TR") > -1) {
          bCS = true;
        }
        if (series[j].value.indexOf("UMIACS-TR") > -1) {
          bUmiacs = true;
        }
      }
      System.out.println("  CS-TR="+bCS+", UMIACS-TR="+bUmiacs);
     
      // Check collection membership
      if (bUmiacs) {
        colls = item.getCollections();
        boolean b = false;
        for (int k=0; k < colls.length; k++) {
          if (colls[k].equals(collUmiacs)) {
            b = true;
            break;
          }
        }

        if (! b) {
          System.out.println("  Adding to UMIACS");
	  collUmiacs.addItem(item);
          lUmiacs++;
        }
      }
            
    }
    context.commit();

    // Loop through collUmiacs items
    System.out.println("=== Technical Reports from UMIACS ===");
    for (ItemIterator i = collUmiacs.getItems(); i.hasNext(); ) {
      Item item = i.next();

      String title = (item.getDC("title", null, Item.ANY))[0].value;
      if (title.length() > 70) {
        title = title.substring(0,70);
      }
      System.out.println();
      System.out.println(title);

      DCValue series[] = item.getDC("relation", "ispartofseries", Item.ANY);
      boolean bCS = false;
      boolean bUmiacs = false;
      for (int j=0; j < series.length; j++) {
        if (series[j].value.indexOf("CS-TR") > -1) {
          bCS = true;
        }
        if (series[j].value.indexOf("UMIACS-TR") > -1) {
          bUmiacs = true;
        }
      }
      System.out.println("  CS-TR="+bCS+", UMIACS-TR="+bUmiacs);

      // Check collection membership
      if (bCS) {
        colls = item.getCollections();
        boolean b = false;
        for (int k=0; k < colls.length; k++) {
          if (colls[k].equals(collCS)) {
            b = true;
            break;
          }
        }

        if (! b) {
          System.out.println("  Adding to CS");
	  collCS.addItem(item);
	  lCS++;
        }
      }
            
    }
    context.commit();

    context.complete();

    System.out.println();
    System.out.println("Items added to CS:     " + lCS);
    System.out.println("Items added to Umiacs: " + lUmiacs);
  }
}

