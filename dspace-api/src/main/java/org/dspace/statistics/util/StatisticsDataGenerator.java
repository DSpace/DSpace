/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.apache.commons.cli.*;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.ConfigurationManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.eperson.EPerson;
import org.dspace.statistics.SolrLogger;

import java.util.Date;
import java.text.SimpleDateFormat;

import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.Location;

/**
 * Test class to generate random statistics data.
 * Used for load testing of searches. Inputs are slow
 * due to inefficient randomizer.
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class StatisticsDataGenerator {
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();

		Options options = new Options();
		options.addOption("n", "nrlogs", true,
				"type: nr of logs to be generated");
		options.addOption("s", "startDate", true,
				"type: the start date from which we start generating our logs");
		options.addOption("e", "endDate", true,
				"type: the end date from which we start generating our logs");
		options.addOption("a", "cms", true, "The starting id of our community");
		options.addOption("b", "cme", true, "The end id of our community");
		options
				.addOption("c", "cls", true,
						"The starting id of our collection");
		options.addOption("d", "cle", true, "The end if of our collection");
		options.addOption("f", "is", true, "The starting id of our item");
		options.addOption("g", "ie", true, "The end id of our item");
		options.addOption("h", "bs", true, "The starting id of our bitstream");
		options.addOption("i", "be", true, "The end id of our bitstream");
		options.addOption("j", "ps", true, "The starting id of our epersons");
		options.addOption("k", "pe", true, "The end id of our epersons");

		CommandLine line = parser.parse(options, args);

		int nrLogs;
		long startDate;
		long endDate;
		long commStartId;
		long commEndId;
		long collStartId;
		long collEndId;
		long itemStartId;
		long itemEndId;
		long bitStartId;
		long bitEndId;
		long epersonStartId;
		long epersonEndId;

		if (line.hasOption("n"))
        {
            nrLogs = Integer.parseInt(line.getOptionValue("n"));
        }
		else {
			System.out
					.println("We need to know how many logs we need to create");
			return;
		}
		if (line.hasOption("s")) {
			startDate = getDateInMiliseconds(line.getOptionValue("s"));
		} else
        {
            startDate = getDateInMiliseconds("01/01/2006");
        }
		if (line.hasOption("e")) {
			endDate = getDateInMiliseconds(line.getOptionValue("e"));
		} else
        {
            endDate = new Date().getTime();
        }

		if (line.hasOption("a"))
        {
            commStartId = Long.parseLong(line.getOptionValue("a"));
        }
		else
        {
            return;
        }

		if (line.hasOption("b"))
        {
            commEndId = Long.parseLong(line.getOptionValue("b"));
        }
		else
        {
            return;
        }
		if (line.hasOption("c"))
        {
            collStartId = Long.parseLong(line.getOptionValue("c"));
        }
		else
        {
            return;
        }
		if (line.hasOption("d"))
        {
            collEndId = Long.parseLong(line.getOptionValue("d"));
        }
		else
        {
            return;
        }
		if (line.hasOption("f"))
        {
            itemStartId = Long.parseLong(line.getOptionValue("f"));
        }
		else
        {
            return;
        }
		if (line.hasOption("g"))
        {
            itemEndId = Long.parseLong(line.getOptionValue("g"));
        }
		else
        {
            return;
        }
		if (line.hasOption("h"))
        {
            bitStartId = Long.parseLong(line.getOptionValue("h"));
        }
		else
        {
            return;
        }
		if (line.hasOption("i"))
        {
            bitEndId = Long.parseLong(line.getOptionValue("i"));
        }
		else
        {
            return;
        }
		if (line.hasOption("j"))
        {
            epersonStartId = Long.parseLong(line.getOptionValue("j"));
        }
		else
        {
            return;
        }
		if (line.hasOption("k"))
        {
            epersonEndId = Long.parseLong(line.getOptionValue("k"));
        }
		else
        {
            return;
        }

		// Get the max id range
		long maxIdTotal = Math.max(commEndId, collEndId);
		maxIdTotal = Math.max(maxIdTotal, itemEndId);
		maxIdTotal = Math.max(maxIdTotal, bitEndId);

		// We got 3/4 chance the person visting the dso is not logged in
		epersonEndId *= 4;

		// We got all our parameters now get the rest
		Context context = new Context();
		// Find our solr server
		HttpSolrServer solr = new HttpSolrServer(
				ConfigurationManager.getProperty("solr-statistics", "server"));
		solr.deleteByQuery("*:*");
		solr.commit();

		String prevIp = null;
		String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
		LookupService cl = new LookupService(dbfile,
				LookupService.GEOIP_STANDARD);
		int countryErrors = 0;
		for (int i = 0; i < nrLogs; i++) {
			String ip = "";
			Date time;
			String continent;
			String countryCode;
			float longitude;
			float latitude;
			String city;

			// 1. Generate an ip for our user
            StringBuilder ipBuilder = new StringBuilder();
			for (int j = 0; j < 4; j++) {
				ipBuilder.append(getRandomNumberInRange(0, 254));
				if (j != 3)
                {
                    ipBuilder.append(".");
                }
			}
            ip = ipBuilder.toString();
            
			// 2 Depending on our ip get all the location info
			Location location;
			try {
				location = cl.getLocation(ip);
			} catch (Exception e) {
				location = null;
			}
			if (location == null) {
				// If we haven't got a prev ip this is pretty useless so move on
				// to the next one
				if (prevIp == null)
                {
                    continue;
                }
				ip = prevIp;
				location = cl.getLocation(ip);
			}

			city = location.city;
			countryCode = location.countryCode;
			longitude = location.longitude;
			latitude = location.latitude;
			try {
				continent = LocationUtils.getContinentCode(countryCode);
			} catch (Exception e) {
				// We could get an error if our country == Europa this doesn't
				// matter for generating statistics so ignore it
				System.out.println("COUNTRY ERROR: " + countryCode);
				countryErrors++;
				continue;
			}

			// 3. Generate a date that the object was visited
			time = new Date(getRandomNumberInRange(startDate, endDate));

			// 4. Get our dspaceobject we are supposed to be working on
			// We got mostly item views so lets say we got 1/2 chance that we
			// got an item view
			// What type have we got (PS: I know we haven't got 5 as a dso type
			// we can log but it is used so our item gets move traffic)
			int type = (int) getRandomNumberInRange(0, 8);
			if (type == Constants.BUNDLE || type >= 5)
            {
                type = Constants.ITEM;
            }

			int dsoId = -1;
			// Now we need to find a valid id
			switch (type) {
			case Constants.COMMUNITY:
				dsoId = (int) getRandomNumberInRange(commStartId, commEndId);
				break;
			case Constants.COLLECTION:
				dsoId = (int) getRandomNumberInRange(collStartId, collEndId);
				break;
			case Constants.ITEM:
				dsoId = (int) getRandomNumberInRange(itemStartId, itemEndId);
				break;
			case Constants.BITSTREAM:
				dsoId = (int) getRandomNumberInRange(bitStartId, bitEndId);
				break;
			}
			// Now find our dso
			DSpaceObject dso = DSpaceObject.find(context, type, dsoId);
			if (dso instanceof Bitstream) {
				Bitstream bit = (Bitstream) dso;
				if (bit.getFormat().isInternal()) {
					dso = null;
				}
			}
			// Make sure we got a dso
			boolean substract = false;
			while (dso == null) {
				// If our dsoId gets higher then our maxIdtotal we need to lower
				// to find a valid id
				if (dsoId == maxIdTotal)
                {
                    substract = true;
                }

				if (substract)
                {
                    dsoId--;
                }
				else
                {
                    dsoId++;
                }

				dso = DSpaceObject.find(context, type, dsoId);
				if (dso instanceof Bitstream) {
					Bitstream bit = (Bitstream) dso;
					if (bit.getFormat().isInternal()) {
						dso = null;
					}
				}
				// System.out.println("REFIND");
			}
			// Find the person who is visting us
			int epersonId = (int) getRandomNumberInRange(epersonStartId, epersonEndId);
			EPerson eperson = EPerson.find(context, epersonId);
			if (eperson == null)
            {
                epersonId = -1;
            }

			// System.out.println(ip);
			// System.out.println(country + " " +
			// LocationUtils.getCountryName(countryCode));

			// Resolve the dns
			String dns = null;
			try {
				dns = DnsLookup.reverseDns(ip);
			} catch (Exception e) {

			}

			System.out.println(ip);
			System.out.println(dns);

			// Save it in our server
			SolrInputDocument doc1 = new SolrInputDocument();
			doc1.addField("ip", ip);
			doc1.addField("type", dso.getType());
			doc1.addField("id", dso.getID());
			doc1.addField("time", DateFormatUtils.format(time,
					SolrLogger.DATE_FORMAT_8601));
			doc1.addField("continent", continent);
			// doc1.addField("country", country);
			doc1.addField("countryCode", countryCode);
			doc1.addField("city", city);
			doc1.addField("latitude", latitude);
			doc1.addField("longitude", longitude);
			if (epersonId > 0)
            {
                doc1.addField("epersonid", epersonId);
            }
			if (dns != null)
            {
                doc1.addField("dns", dns.toLowerCase());
            }

			SolrLogger.storeParents(doc1, dso);

			solr.add(doc1);

			// Make sure we have a previous ip
			prevIp = ip;
		}
		System.out.println("Nr of countryErrors: " + countryErrors);
		// Commit at the end cause it takes a while
		solr.commit();
	}

	/**
	 * Method returns a random integer between the given int
	 * 
	 * @param min
	 *            the random number must be greater or equal to this
	 * @param max
	 *            the random number must be smaller or equal to this
	 * @return a random in
	 */
	private static long getRandomNumberInRange(long min, long max) {
		return min + (long) (Math.random() * ((max - min) + 1));
	}

	/**
	 * Method to get the miliseconds from a datestring
	 * 
	 * @param dateString
	 *            the string containing our date in a string
	 * @return the nr of miliseconds in the given datestring
	 * @throws java.text.ParseException
	 *             should not happen
	 */
	private static long getDateInMiliseconds(String dateString)
			throws java.text.ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		return formatter.parse(dateString).getTime();
	}

}
