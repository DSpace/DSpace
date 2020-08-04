/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.net.Inet6Address;
import java.net.UnknownHostException;

import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Quickly tests whether a given IP address matches an IP range. An
 * {@code IPMatcher} is initialized with a particular IP range specification.
 * Calls to {@link IPMatcher#match(String) match} method will then quickly
 * determine whether a given IP falls within that range.
 * </p>
 * <p>
 * Supported range specifications are:
 * </p>
 * <ul>
 * <li>Full IPv4 address, e.g. {@code 12.34.56.78}</li>
 * <li>Full IPv6 address, e.g. {@code 2001:18e8:3:171:218:8bff:fe2a:56a4}</li>
 * <li>Partial IPv4 address, e.g. {@code 12.34} (which matches any IP starting
 * {@code 12.34})</li>
 * <li>IPv4 network/netmask, e.g. {@code 18.25.0.0/255.255.0.0}</li>
 * <li>IPv4 or IPv6 CIDR slash notation, e.g. {@code 18.25.0.0/16},
 * {@code 2001:18e8:3:171::/64}</li>
 * </ul>
 *
 * @author Robert Tansley
 * @author Ben Bosman
 * @author Roeland Dillen
 * @version $Revision$
 */
public class IPMatcher {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(IPMatcher.class);

    /**
     * Network to match
     */
    private byte[] network;

    /**
     * Network mask
     */
    private byte[] netmask;

    /**
     * Construct an IPMatcher that will test for the given IP specification
     *
     * @param ipSpec IP specification (full or partial address, network/netmask,
     *               network/cidr)
     * @throws IPMatcherException if there is an error parsing the specification (i.e. it is
     *                            somehow malformed)
     */
    public IPMatcher(String ipSpec) throws IPMatcherException {
        // Boil all specs down to network + mask

        String ipPart = ipSpec;
        String[] parts = ipSpec.split("/");

        if (parts[0].indexOf(':') >= 0) { // looks like IPv6
            try {
                network = Inet6Address.getByName(parts[0]).getAddress();
            } catch (UnknownHostException e) {
                throw new IPMatcherException(
                    "Malformed IP range specification " + ipSpec, e);
            }

            netmask = new byte[16];
            switch (parts.length) {
                case 2: // CIDR notation:  calculate the mask
                    int maskBits;
                    try {
                        maskBits = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException nfe) {
                        throw new IPMatcherException(
                            "Malformed IP range specification " + ipSpec, nfe);
                    }
                    if (maskBits < 0 || maskBits > 128) {
                        throw new IPMatcherException("Mask bits out of range 0-128 "
                                                         + ipSpec);
                    }

                    for (int i = 0; i < netmask.length; i++) {
                        if (maskBits <= 0) {
                            netmask[i] = 0;
                        } else if (maskBits > 8) {
                            netmask[i] = (byte) 0Xff;
                        } else {
                            netmask[i] = (byte) ((byte) 0Xff << 8 - maskBits);
                        }

                        maskBits = maskBits - 8;
                    }
                    break;
                case 1: // No explicit mask:  fill the mask with 1s
                    for (int i = 0; i < netmask.length; i++) {
                        netmask[i] = (byte) 0Xff;
                    }
                    break;
                default:
                    throw new IPMatcherException("Malformed IP range specification "
                                                     + ipSpec);
            }
        } else {   // assume IPv4
            // Allow partial IP
            boolean mustHave4 = false;

            network = new byte[4];
            netmask = new byte[4];
            switch (parts.length) {
                case 2:
                    // Some kind of slash notation -- we'll need a full network IP
                    ipPart = parts[0];
                    mustHave4 = true;

                    String[] maskParts = parts[1].split("\\.");
                    if (maskParts.length == 1) {
                        // CIDR slash notation
                        int x;

                        try {
                            x = Integer.parseInt(maskParts[0]);
                        } catch (NumberFormatException nfe) {
                            throw new IPMatcherException(
                                "Malformed IP range specification " + ipSpec, nfe);
                        }

                        if (x < 0 || x > 32) {
                            throw new IPMatcherException();
                        }

                        int fullMask = -1 << (32 - x);
                        netmask[0] = (byte) ((fullMask & 0xFF000000) >>> 24);
                        netmask[1] = (byte) ((fullMask & 0x00FF0000) >>> 16);
                        netmask[2] = (byte) ((fullMask & 0x0000FF00) >>> 8);
                        netmask[3] = (byte) (fullMask & 0x000000FF);
                        ipToBytes(ipPart, network, mustHave4);
                        if (log.isDebugEnabled()) {
                            log.debug("fullMask: " + fullMask);
                            for (int i = 0; i < network.length; i++) {
                                log.debug("network[" + i + "]: " + network[i]);
                            }
                            for (int i = 0; i < netmask.length; i++) {
                                log.debug("netmask[" + i + "]: " + netmask[i]);
                            }
                        }
                    } else {
                        // full netmask specified
                        ipToBytes(parts[0], network, true);
                        ipToBytes(parts[1], netmask, true);
                    }

                    break;

                case 1:
                    // Get IP
                    for (int i = 0; i < netmask.length; i++) {
                        netmask[i] = -1;
                    }
                    int partCount = ipToBytes(ipPart, network, mustHave4);

                    // If partial IP, set mask for remaining bytes
                    for (int i = 3; i >= partCount; i--) {
                        netmask[i] = 0;
                    }

                    break;

                default:
                    throw new IPMatcherException("Malformed IP range specification "
                                                     + ipSpec);
            }
            network = ip4ToIp6(network);
            netmask = ip4MaskToIp6(netmask);
            if (log.isDebugEnabled()) {
                for (int i = 0; i < network.length; i++) {
                    log.debug("network[" + i + "]: " + network[i]);
                }
                for (int i = 0; i < netmask.length; i++) {
                    log.debug("netmask[" + i + "]: " + netmask[i]);
                }
            }
        }
    }

    /**
     * Fill out a given four-byte array with the IPv4 address specified in the
     * given String
     *
     * @param ip        IPv4 address as a dot-delimited String
     * @param bytes     4-byte array to fill out
     * @param mustHave4 if true, will require that the given IP string specify all
     *                  four bytes
     * @return the number of actual IP bytes found in the given IP address
     * String
     * @throws IPMatcherException if there is a problem parsing the IP string -- e.g. number
     *                            outside of range 0-255, too many numbers, less than 4 numbers
     *                            if {@code mustHave4} is true
     */
    private static int ipToBytes(String ip, byte[] bytes, boolean mustHave4)
        throws IPMatcherException {
        String[] parts = ip.split("\\.");

        if (parts.length > 4 || mustHave4 && parts.length != 4) {
            throw new IPMatcherException("Malformed IP specification " + ip);
        }

        try {

            for (int i = 0; i < parts.length; i++) {
                int p = Integer.parseInt(parts[i]);
                if (p < 0 || p > 255) {
                    throw new IPMatcherException("Malformed IP specification "
                                                     + ip);

                }

                bytes[i] = (byte) (p < 128 ? p : p - 256);
            }
        } catch (NumberFormatException nfe) {
            throw new IPMatcherException("Malformed IP specification " + ip,
                                         nfe);
        }

        return parts.length;
    }

    /**
     * Determine whether the given full IP falls within the range this
     * {@code IPMatcher} was initialized with.
     *
     * @param ipIn IP address as dot-delimited String
     * @return {@code true} if the IP matches the range of this
     * {@code IPMatcher}; {@code false} otherwise
     * @throws IPMatcherException if the IP passed in cannot be parsed correctly (i.e. is
     *                            malformed)
     */
    public boolean match(String ipIn) throws IPMatcherException {
        log.debug("ipIn: " + ipIn);
        byte[] candidate;

        if (ipIn.indexOf(':') < 0) {
            candidate = new byte[4];
            ipToBytes(ipIn, candidate, true);
            candidate = ip4ToIp6(candidate);
        } else {
            try {
                candidate = Inet6Address.getByName(ipIn).getAddress();
            } catch (UnknownHostException e) {
                throw new IPMatcherException("Malformed IPv6 address ", e);
            }
        }

        for (int i = 0; i < netmask.length; i++) {
            if ((candidate[i] & netmask[i]) != (network[i] & netmask[i])) {
                if (log.isDebugEnabled()) {
                    log.debug("candidate[i]: " + candidate[i]);
                    log.debug("netmask[i]: " + netmask[i]);
                    log.debug("candidate[i] & netmask[i]: " + (candidate[i] & netmask[i]));
                    log.debug("network[i]: " + network[i]);
                    log.debug("network[i] & netmask[i]: " + (network[i] & netmask[i]));
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Convert an IPv4 address to an IPv6 IPv4-compatible address.
     *
     * @param ip4 an IPv4 address
     * @return the corresponding IPv6 address
     * @throws IllegalArgumentException if ip4 is not exactly four octets long.
     */
    private static byte[] ip4ToIp6(byte[] ip4) {
        if (ip4.length != 4) {
            throw new IllegalArgumentException("IPv4 address must be four octets");
        }

        byte[] ip6 = new byte[16];
        for (int i = 0; i < 16 - 4; i++) {
            ip6[i] = 0;
        }
        for (int i = 0; i < 4; i++) {
            ip6[12 + i] = ip4[i];
        }
        return ip6;
    }

    /**
     * Convert an IPv4 mask to the equivalent IPv6 mask.
     *
     * @param ip4 an IPv4 mask
     * @return the corresponding IPv6 mask
     * @throws IllegalArgumentException if ip4 is not exactly four octets long.
     */
    private static byte[] ip4MaskToIp6(byte[] ip4) {
        if (ip4.length != 4) {
            throw new IllegalArgumentException("IPv4 mask must be four octets");
        }

        byte[] ip6 = new byte[16];
        for (int i = 0; i < 16 - 4; i++) {
            ip6[i] = (byte) 0Xff;
        }
        for (int i = 0; i < 4; i++) {
            ip6[12 + i] = ip4[i];
        }
        return ip6;
    }
}
