/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import org.dspace.core.ConfigurationManager;
import org.xbill.DNS.*;

import java.io.IOException;

/**
 * XBill DNS resolver to retrieve hostnames for client IP addresses.
 * TODO: deal with IPv6 addresses.
 * 
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class DnsLookup {

    /**
     * Resolve an IP address to a host name.
     *
     * @param hostIp dotted decimal IPv4 address.
     * @return name if resolved, or the address.
     * @throws IOException from infrastructure.
     */
    public static String reverseDns(String hostIp) throws IOException {
         Resolver res = new ExtendedResolver();
         
         // set the timeout, defaults to 200 milliseconds
         int timeout = ConfigurationManager.getIntProperty("usage-statistics", "resolver.timeout", 200);
         res.setTimeout(0, timeout);

         Name name = ReverseMap.fromAddress(hostIp);
         int type = Type.PTR;
         int dclass = DClass.IN;
         Record rec = Record.newRecord(name, type, dclass);
         Message query = Message.newQuery(rec);
         Message response = res.send(query);

         Record[] answers = response.getSectionArray(Section.ANSWER);
         if (answers.length == 0)
         {
             return hostIp;
         }
         else
         {
             return answers[0].rdataToString();
         }
   }

    /**
     * Resolve a host name to an IPv4 address.
     * @throws IOException from infrastructure or no resolution.
     */
    public static String forward(String hostname)
            throws IOException
    {
        Resolver res = new ExtendedResolver();
        int timeout = ConfigurationManager.getIntProperty("usage-statistics",
                "resolver.timeout", 200);
        res.setTimeout(0, timeout);

        Name name = Name.fromString(hostname, Name.root);
        Record rec = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(rec);
        Message response = res.send(query);

        Record[] answers = response.getSectionArray(Section.ANSWER);
        if (answers.length == 0)
        {
            throw new IOException("Unresolvable host name (empty response)");
        }

        String resolution = null;
        for (Record answer : answers)
        {
            if (answer.getType() == Type.A)
            {
                resolution = answer.rdataToString();
                break;
            }
        }

        if (null == resolution)
        {
            throw new IOException("Unresolvable host name (no A record)");
        }

        return resolution;
    }
}
