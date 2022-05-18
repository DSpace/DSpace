package edu.umd.lib.dspace.app;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.HashMap;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;

public class GetCollectionId
{

  private final static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
  
  public static void
  main(String[] args)
    throws Exception
    {
      // Setup the context
      Context context = new Context();
      context.turnOffAuthorisationSystem();

      try {
	// Get the list of all collections
	HashMap<String, Collection> m = new HashMap<String, Collection>();
	List<Collection> collections = collectionService.findAll(context);
	for (Collection collection : collections) {
	  String strName = collection.getName();
	  if (m.containsKey(strName)) {
	    System.err.println("Error: duplicate collection names: " + strName);
	    System.exit(1);
	  }
	  
	  m.put(strName, collection);
	}

	// Read through each collection, 1 collection per line
	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	String strLine;
	while ((strLine = br.readLine()) != null) {
	  String strName = strLine.trim();
	  if (m.containsKey(strName)) {
	    System.out.println(strName + "::" +
			       ((Collection)(m.get(strName))).getID());
	  } else {
	    System.out.println(strName + "::not found");
	  }
	}
      }
      finally {
	      context.complete();
      }
    }

}
