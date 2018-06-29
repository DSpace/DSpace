/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.app.cleanup;


import java.util.List;
import java.util.Properties;

// IO
import java.io.BufferedReader;
import java.io.InputStreamReader;

// Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// DSpace
import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

// Lims
import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 Delete items. Input is list of item handles on stdin.

 @author  Ben Wallberg

*********************************************************************/

public class DeleteItems {

  private static Logger log = Logger.getLogger(DeleteItems.class);

  static long lRead = 0;
  static long lDeleted = 0;

  private static final ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

  protected final static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();;

  protected final static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

  protected final static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception 
  {

    Context context = new Context();
    context.turnOffAuthorisationSystem();

    try {

      // Properties
      Properties props     = System.getProperties();

      // dspace dir
      String strDspace     = configurationService.getProperty("dspace.dir");

      // logging (log4j.defaultInitOverride needs to be set or
      // config/log4j.properties will be read and used additionally)
      PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");
      // Open the reader
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

      // Loop through the lines
      String strLine = null;

      while ((strLine = br.readLine()) != null) {
        lRead++;
        String strHandle = strLine.trim();

        Item item = (Item)handleService.resolveToObject(context, strHandle);

        if (item == null) {
          log.info(strHandle + ": not found");
        } else {
          String strTitle = itemService.getMetadataFirstValue(item, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);

          // Remove from all collection; will be deleted when removed from
          // last collection
          List<Collection> cs = item.getCollections();
          for (Collection collection : cs) {
            collectionService.removeItem(context, collection, item);
          }

          context.commit();

          log.info(strHandle + ": deleted: " + strTitle);
          lDeleted++;
        }
      }
    }

    catch (Exception e) {
      log.error("Uncaught exception: " + ErrorHandling.getStackTrace(e));
      System.exit(1);
    }

    finally {
      if (context != null) {
        try { context.complete(); } catch (Exception e) {}
      }

      log.info("=====================================\n" +
	       "Items read:    " + lRead + "\n" +
	       "Items deleted: " + lDeleted + "\n");

    }

    System.exit(0);
  }
}



