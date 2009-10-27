/*
 * IPMatcher.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.authenticate;

/**
 * Quickly tests whether a given IPv4 4-byte address matches an IP range. An
 * {@code IPMatcher} is initialized with a particular IP range specification.
 * Calls to {@link IPMatcher#match(String) match} method will then quickly
 * determine whether a given IP falls within that range.
 * <p>
 * Supported range specifications areL
 * <p>
 * <ul>
 * <li>Full IP address, e.g. {@code 12.34.56.78}</li>
 * <li>Partial IP address, e.g. {@code 12.34} (which matches any IP starting
 * {@code 12.34})</li>
 * <li>Network/netmask, e.g. {@code 18.25.0.0/255.255.0.0}</li>
 * <li>CIDR slash notation, e.g. {@code 18.25.0.0/16}</li>
 * </ul>
 * 
 * @version $Revision$
 * @author Robert Tansley
 */
public class IPMatcher
{
    /** Network to match */
    private byte[] network;

    /** Network mask */
    private byte[] netmask;

    /**
     * Construct an IPMatcher that will test for the given IP specification
     * 
     * @param ipSpec
     *            IP specification (full or partial URL, network/netmask,
     *            network/cidr)
     * @throws IPMatcherException
     *             if there is an error parsing the specification (i.e. it is
     *             somehow malformed)
     */
    public IPMatcher(String ipSpec) throws IPMatcherException
    {
        // Boil all specs down to network + mask
        network = new byte[4];
        netmask = new byte[] { -1, -1, -1, -1 };

        // Allow partial IP
        boolean mustHave4 = false;

        String ipPart = ipSpec;
        String[] parts = ipSpec.split("/");

        switch (parts.length)
        {
        case 2:
            // Some kind of slash notation -- we'll need a full network IP
            ipPart = parts[0];
            mustHave4 = true;

            String[] maskParts = parts[1].split("\\.");
            if (maskParts.length == 1)
            {
                // CIDR slash notation
                int x;

                try
                {
                    x = Integer.parseInt(maskParts[0]);
                }
                catch (NumberFormatException nfe)
                {
                    throw new IPMatcherException(
                            "Malformed IP range specification " + ipSpec, nfe);
                }

                if (x < 0 || x > 32)
                {
                    throw new IPMatcherException();
                }

                int fullMask = -1 << (32 - x);
                netmask[0] = (byte) ((fullMask & 0xFF000000) >>> 24);
                netmask[1] = (byte) ((fullMask & 0x00FF0000) >>> 16);
                netmask[2] = (byte) ((fullMask & 0x0000FF00) >>> 8);
                netmask[3] = (byte) (fullMask & 0x000000FF);
            }
            else
            {
                // full subnet specified
                ipToBytes(parts[1], netmask, true);
            }

        case 1:
            // Get IP
            int partCount = ipToBytes(ipPart, network, mustHave4);

            // If partial IP, set mask for remaining bytes
            for (int i = 3; i >= partCount; i--)
            {
                netmask[i] = 0;
            }

            break;

        default:
            throw new IPMatcherException("Malformed IP range specification "
                    + ipSpec);
        }
    }

    /**
     * Fill out a given four-byte array with the IP address specified in the
     * given String
     * 
     * @param ip
     *            IP address as a dot-delimited String
     * @param bytes
     *            4-byte array to fill out
     * @param mustHave4
     *            if true, will require that the given IP string specify all
     *            four bytes
     * @return the number of actual IP bytes found in the given IP address
     *         String
     * @throws IPMatcherException
     *             if there is a problem parsing the IP string -- e.g. number
     *             outside of range 0-255, too many numbers, less than 4 numbers
     *             if {@code mustHave4} is true
     */
    private int ipToBytes(String ip, byte[] bytes, boolean mustHave4)
            throws IPMatcherException
    {
        String[] parts = ip.split("\\.");

        if (parts.length > 4 || mustHave4 && parts.length != 4)
        {
            throw new IPMatcherException("Malformed IP specification " + ip);
        }

        try
        {

            for (int i = 0; i < parts.length; i++)
            {
                int p = Integer.parseInt(parts[i]);
                if (p < 0 || p > 255)
                {
                    throw new IPMatcherException("Malformed IP specification "
                            + ip);

                }

                bytes[i] = (byte) (p < 128 ? p : p - 256);
            }
        }
        catch (NumberFormatException nfe)
        {
            throw new IPMatcherException("Malformed IP specification " + ip,
                    nfe);
        }

        return parts.length;
    }

    /**
     * Determine whether the given full IP falls within the range this
     * {@code IPMatcher} was initialized with.
     * 
     * @param ipIn
     *            IP address as dot-delimited String
     * @return {@code true} if the IP matches the range of this
     *         {@code IPMatcher}; {@code false} otherwise
     * @throws IPMatcherException
     *             if the IP passed in cannot be parsed correctly (i.e. is
     *             malformed)
     */
    public boolean match(String ipIn) throws IPMatcherException
    {
        byte[] bytes = new byte[4];

        ipToBytes(ipIn, bytes, true);

        for (int i = 0; i < 4; i++)
        {
            if ((bytes[i] & netmask[i]) != (network[i] & netmask[i]))
            {
                return false;
            }
        }

        return true;
    }
}
