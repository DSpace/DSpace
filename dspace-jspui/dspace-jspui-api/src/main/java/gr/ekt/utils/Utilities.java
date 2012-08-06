/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package gr.ekt.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.browse.BrowseIndex;

public class Utilities {

	private static Logger log = Logger.getLogger(Utilities.class);
	
	public static HashMap<String, Integer> calculateMapFreqOfItems(String element) throws SQLException, AuthorizeException, BrowseException{
		Long count;

		Context c = new Context();
		// find the EPerson, assign to context
		EPerson myEPerson = null;

		//@ sign, must be an email
		myEPerson = EPerson.findByEmail(c, "kstamatis@ekt.gr");

		//set current user
		c.setCurrentUser(myEPerson);

		c.setIgnoreAuthorization(true);

		//int browseIndexID = getBrowseIndicesId(element);
		//String disTable = "bi_"+Integer.toString(browseIndexID)+"_dis";
		//String dmapTable = "bi_"+Integer.toString(browseIndexID)+"_dmap";

		BrowseIndex bindex = BrowseIndex.getBrowseIndex(element);

		String disTable = bindex.getDistinctTableName();
		String dmapTable = bindex.getMapTableName();

		String findAll = "SELECT "+disTable+".value as a, count(*) as frequency FROM "+disTable+","+dmapTable+" WHERE "+dmapTable+".distinct_id="+disTable+".id GROUP BY "+disTable+".value";

		TableRowIterator iter = DatabaseManager.query(c, findAll);

		HashMap<String, Integer> tmp = new HashMap<String, Integer>();

		while (iter.hasNext()){
			TableRow row = iter.next();
			String value = row.getStringColumn("a");
			long counter = row.getLongColumn("frequency");

			tmp.put(value, new Integer(Integer.parseInt(Long.toString(counter))));
		}
		c.complete();

		log.info("Result Size = " + tmp.size());
		return tmp;
	}
}