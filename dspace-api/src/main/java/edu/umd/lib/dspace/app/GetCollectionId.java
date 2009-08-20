package edu.umd.lib.dspace.app;

import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.util.HashMap;

import org.dspace.content.Collection;
import org.dspace.core.Context;

public class GetCollectionId
{
  public static void
  main(String[] args)
    throws Exception
    {
      // Setup the context
      Context context = new Context();
      context.setIgnoreAuthorization(true);

      try {
	// Get the list of all collections
	HashMap m = new HashMap();
	Collection c[] = Collection.findAll(context);
	for (int i=0; i < c.length; i++) {
	  String strName = c[i].getMetadata("name");
	  if (m.containsKey(strName)) {
	    System.err.println("Error: duplicate collection names: " + strName);
	    System.exit(1);
	  }
	  
	  m.put(strName, c[i]);
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
