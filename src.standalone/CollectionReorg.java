
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


public class CollectionReorg
{
  public static Connection connection = null;

  public static String
  escape(String x)
  {
    if (x == null) {
      return "";
    }

    // escape for sql
    int i = x.indexOf('\'');
    while (i > -1) {
      x =
        x.substring(0,i) +
        "\\" +
        x.substring(i)
        ;
      i = x.indexOf('\'', i+2);
    }
    return x;
  }


  public static boolean
  getSubcomm(Community subcomm)
  {
    String subcommName = escape(subcomm.getMetadata("name"));
    
    try {
      // Get metadata from the dev site
      if (connection == null) {
        DriverManager.registerDriver(new org.postgresql.Driver());
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:8021/dspace", "dspace", null);
      }

      Statement st = connection.createStatement();
      String strQuery = 
        "SELECT * " +
        "FROM community " +
        "WHERE " +
        "    name = '" + subcommName + "'"
        ;
      
      ResultSet rs = st.executeQuery(strQuery);
      if (rs.next()) {
        subcomm.setMetadata("short_description", escape(rs.getString("short_description")));
        subcomm.setMetadata("introductory_text", escape(rs.getString("introductory_text")));
      } else {
        System.out.println("Error: no subocommunity match in dev");
        System.exit(1);
      }

      // Close the ResultSet and Statement
      rs.close();
      st.close();
 
    }
    catch (SQLException e) {
      System.out.println("Error: SQL exception handling " + subcommName + ":\n" + e.getMessage());
      System.exit(1);
    }
      
    return true;
  }


  public static boolean
  getColl(Collection coll)
  {
    String collName = escape(coll.getMetadata("name"));
    
    try {
      // Get metadata from the dev site
      if (connection == null) {
        DriverManager.registerDriver(new org.postgresql.Driver());
        connection = DriverManager.getConnection("jdbc:postgresql://localhost:8021/dspace", "dspace", null);
      }

      Statement st = connection.createStatement();
      String strQuery = 
        "SELECT * " +
        "FROM collection " +
        "WHERE " +
        "    name='" + collName + "'"
        ;
      
      ResultSet rs = st.executeQuery(strQuery);
      if (rs.next()) {
        coll.setMetadata("short_description", escape(rs.getString("short_description")));
        coll.setMetadata("introductory_text", escape(rs.getString("introductory_text")));
      } else {
        System.out.println("Error: no collection match in dev");
        System.exit(1);
      }

      // Close the ResultSet and Statement
      rs.close();
      st.close();
 
    }
    catch (SQLException e) {
      System.out.println("Error: SQL exception handling " + collName + ":\n" + e.getMessage());
      System.exit(1);
    }
      
    return true;
  }


  public static void
  main(String[] args)
    throws Exception
    {
      PropertyConfigurator.configure("log4j.properties");

      // Setup the context
      Context context = new Context();
      context.setIgnoreAuthorization(true);

      // Change the name of the community
      {
        Community comms[] = Community.findAll(context);
        for (int j=0; j < comms.length; j++) {
          Community comm = comms[j];
          if (comm.getMetadata("name").equals("School of Architecture, Planning, & Preservation")) {
            comm.setMetadata("name", "College of Architecture, Planning, & Preservation");
            comm.update();
          }
        }
      }
      context.commit();

      // Get Collection list
      Collection colls[] = Collection.findAll(context);
      for (int i=0; i < colls.length; i++) {
        Collection coll = colls[i];
        String collName = coll.getMetadata("name");
        System.out.println("Collection: " + collName);

        // Renaming
        if (collName.equals("Materials Science & Engineering")) {
          collName = "Materials & Science Engineering";
        } else if (collName.equals("Biological Resources Engineering")) {
          collName = "Biological Resources & Engineering";
        } else if (collName.equals("Natural Resource Sciences & Landscape Architecture")) {
          collName = "Natural Resources & Engineering";
        } else if (collName.equals("Nutrition & Food Science")) {
          collName = "Nutrition & Food Sciences";
        } else if (collName.equals("Communication")) {
          collName = "Communications";
        } else if (collName.equals("Languages, Literatures & Cultures")) {
          collName = "Languages, Literature & Culture";
        } else if (collName.equals("University Libraries (faculty)")) {
          collName = "Library Faculty";
        } else if (collName.equals("Logistics, Business, & Public Policy")) {
          collName = "Logistics, Business & Public Policy";
        }

        String subcommName = collName;
        if (collName.equals("UMIACS Technical Reports")) {
          collName = "Technical Reports from UMIACS";
        } else if (! collName.equals("UM Theses and Dissertations") &&
                   ! collName.equals("Computer Science Department Technical Reports")) {
          collName += " Research Works";
        }

        // Get the community
        Community comms[] = coll.getCommunities();
        if (comms.length != 1) {
          System.out.println("Error: collection has <> 1 communities");
          continue;
        }
        Community comm = comms[0];
        String commName = comm.getMetadata("name");

        // Create the new subcommunity
        Community subcomm = comm.createSubcommunity();
        subcomm.setMetadata("name", subcommName);
        subcomm.setMetadata("group_id", comm.getIntMetadata("group_id"));
        subcomm.update();
        

        // Get subcommunity metadata from dev
        if (!getSubcomm(subcomm)) {
          System.out.println("Error: Unable to get metadata for " + subcommName);
          continue;
        }
        subcomm.update();

        // Move the collection to the subcommunity
        subcomm.addCollection(coll);
        comm.removeCollection(coll);
        
        // Rename the collection
        coll.setMetadata("name", collName);
        coll.update();
        
	// Get collection metadata from dev
	if (!getColl(coll)) {
	  System.out.println("Error: Unable to get metadata for " + collName);
	  continue;
	}
	coll.update();

        context.commit();
      }

      context.complete();

    }

}



