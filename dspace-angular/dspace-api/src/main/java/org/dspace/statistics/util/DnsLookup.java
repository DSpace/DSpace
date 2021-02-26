/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.io.IOException;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

/**
 * XBill DNS resolver to retrieve host names for client IP addresses.
 * TODO: deal with IPv6 addresses.
 *
 * @author kevinvandevelde at atmire.com
 * @author ben at atmire.com
 */
public class DnsLookup {

    /**
     * Default constructor
     */
    private DnsLookup() { }

    /**
     * Resolve an IP address to a host name.
     *
     * @param hostIp dotted decimal IPv4 address.
     * @return name if resolved, or the address.
     * @throws IOException from infrastructure.
     */
    public static String reverseDns(String hostIp) throws IOException {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        Resolver res = new ExtendedResolver();

        // set the timeout, defaults to 200 milliseconds
        int timeout = configurationService.getIntProperty("usage-statistics.resolver.timeout", 200);
        res.setTimeout(0, timeout);

        Name name = ReverseMap.fromAddress(hostIp);
        int type = Type.PTR;
        int dclass = DClass.IN;
        Record rec = Record.newRecord(name, type, dclass);
        Message query = Message.newQuery(rec);
        Message response = res.send(query);

        Record[] answers = response.getSectionArray(Section.ANSWER);
        if (answers.length == 0) {
            return hostIp;
        } else {
            return answers[0].rdataToString();
        }
    }

    /**
     * Resolve a host name to an IPv4 address.
     *
     * @param hostname hostname to resolve to IP
     * @return IPv4 address
     * @throws IOException from infrastructure or no resolution.
     */
    public static String forward(String hostname)
        throws IOException {
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        Resolver res = new ExtendedResolver();
        int timeout = configurationService.getIntProperty("usage-statistics.resolver.timeout", 200);
        res.setTimeout(0, timeout);

        Name name = Name.fromString(hostname, Name.root);
        Record rec = Record.newRecord(name, Type.A, DClass.IN);
        Message query = Message.newQuery(rec);
        Message response = res.send(query);

        Record[] answers = response.getSectionArray(Section.ANSWER);
        if (answers.length == 0) {
            throw new IOException("Unresolvable host name (empty response)");
        }

        String resolution = null;
        for (Record answer : answers) {
            if (answer.getType() == Type.A) {
                resolution = answer.rdataToString();
                break;
            }
        }

        if (null == resolution) {
            throw new IOException("Unresolvable host name (no A record)");
        }

        return resolution;
    }
}
