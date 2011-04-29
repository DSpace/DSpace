/*
 * OrderFormatDate.java
 *
 * Version: $Revision: 4250 $
 *
 * Date: $Date: 2009-09-09 17:23:36 -0400 (Wed, 09 Sep 2009) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.sort;

/**
 * Standard date ordering delegate implementation. The only "special" need is
 * for treat with date with only "small" year < 4 digit
 * 
 * @author Andrea Bollini
 */
public class OrderFormatDate implements OrderFormatDelegate
{
    public String makeSortString(String value, String language)
    {
        int padding = 0;
        int endYearIdx = value.indexOf("-");

        if (endYearIdx >= 0 && endYearIdx < 4)
        {
            padding = 4 - endYearIdx;
        }
        else if (value.length() < 4)
        {
            padding = 4 - value.length();
        }

        if (padding > 0)
        {
            // padding the value from left with 0 so that 87 -> 0087, 687-11-24
            // -> 0687-11-24
            return String.format("%1$0" + padding + "d", 0)
                    + value;
        }
        else
        {
            return value;
        }
    }
}
