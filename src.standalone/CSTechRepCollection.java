
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


public class CSTechRepCollection
{

  public static void
  main(String[] args)
    throws Exception
    {
      PropertyConfigurator.configure("log4j.properties");

      // Setup the context
      Context context = new Context();
      context.setIgnoreAuthorization(true);

      // Get the community
      Community comm = Community.find(context, 37); // Community: Computer Science

      // Get Collection 
      Collection coll = Collection.find(context, 3); // Collection: Computer Science Department Technical Reports

      // Add coll to comm
      comm.addCollection(coll);

      // Get Collection 
      coll = Collection.find(context, 4); // Collection: Technical Reports from UMIACS

      // Add coll to comm
      comm.addCollection(coll);

      comm.update();
      context.commit();
      context.complete();
    }

}



