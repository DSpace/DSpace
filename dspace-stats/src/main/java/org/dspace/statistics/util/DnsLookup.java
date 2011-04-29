/**
 * $Id: DnsLookup.java 4410 2009-10-07 16:17:40Z benbosman $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/util/DnsLookup.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.util;

import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * XBill DNS resolver to retrieve hostnames for client IP addresses.
 * 
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class DnsLookup {

    public static String reverseDns(String hostIp) throws IOException {
         Record opt = null;
         Resolver res = new ExtendedResolver();
         res.setTimeout(0, 20);

         Name name = ReverseMap.fromAddress(hostIp);
         int type = Type.PTR;
         int dclass = DClass.IN;
         Record rec = Record.newRecord(name, type, dclass);
         Message query = Message.newQuery(rec);
         Message response = res.send(query);

         Record[] answers = response.getSectionArray(Section.ANSWER);
         if (answers.length == 0)
            return hostIp;
         else
            return answers[0].rdataToString();
   }
}
