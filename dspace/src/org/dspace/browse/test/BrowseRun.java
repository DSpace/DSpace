/*
 * BrowseRun.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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


package org.dspace.browse.test;

import java.util.Iterator;
import java.util.List;

import org.dspace.browse.*;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Run the Browse API
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class BrowseRun
{
    /**
     * Embedded test harness
     *
     * @param argv - Command-line arguments
     */
    public static void main(String[] argv)
        throws Exception
    {
        Context context = new Context();
        BrowseScope scope = new BrowseScope(context);
        scope.setTotal(5);

        List last = Browse.getLastSubmitted(scope);
        if (last.isEmpty())
        {
            System.out.println("No items submitted");
        }
        for (Iterator iterator = last.iterator(); iterator.hasNext(); )
        {
            Item item = (Item) iterator.next();
            printItem(item, Item.ANY, Item.ANY, Item.ANY);
        }

        scope.setNumberBefore(4);
        scope.setTotal(10);
        BrowseInfo info = Browse.getAuthors(scope);
        printResults(info);

        scope.setTotal(40);
        info = Browse.getItemsByTitle(scope);
        printTitleResults(info);

        // Should hit browse cache
        info = Browse.getItemsByTitle(scope);
        printTitleResults(info);

        scope.setTotal(6);
        info = Browse.getItemsByAuthor(scope, false);
        printAuthorResults(info);
        // Should hit browse cache
        info = Browse.getItemsByAuthor(scope, false);
        printAuthorResults(info);

        info = Browse.getItemsByAuthor(scope, true);
        printAuthorResults(info);

        scope.setTotal(40);
        info = Browse.getItemsByDate(scope, false);
        printDateResults(info);
        info = Browse.getItemsByDate(scope, true);
        printDateResults(info);
        // Should hit browse cache
        info = Browse.getItemsByDate(scope, true);
        printDateResults(info);
        context.complete();
    }

    ////////////////////////////////////////
    // printResults methods
    ////////////////////////////////////////

    private static void printTitleResults(BrowseInfo info)
        throws Exception
    {
        printResults(info, "title", null);
    }

    private static void printDateResults(BrowseInfo info)
        throws Exception
    {
        printResults(info, "date", "issued");
    }

    private static void printAuthorResults(BrowseInfo info)
        throws Exception
    {
        printResults(info, "contributor", "author");
    }

    private static void printResults(BrowseInfo info)
        throws Exception
    {
        printResults(info, null, null);
    }

    private static void printResults(BrowseInfo info,
        String element, String qualifier)
        throws Exception
    {
        final String lang = "en";
        final String banner = "==============================";
        System.out.println(banner);
        for (Iterator iterator = info.getResults().iterator(); iterator.hasNext();)
        {
            Object obj = iterator.next();

            if (obj instanceof Item)
            {
                printItem((Item) obj, element, qualifier, Item.ANY);
            }
            else
            {
                System.out.println("  " + obj);
            }
        }
        System.out.println("Finished");
        System.out.println();
    }

    private static void printItem (Item item,
                                   String element,
                                   String qualifier,
                                   String lang)
    {
        DCValue[] values = item.getDC(element, qualifier, lang);

        for (int i = 0; i < values.length; i++)
        {
            DCValue value = values[i];

            System.out.println("  " +
                               value.value +
                               "  (item " +
                               item.getID() + ")");
        }
    }
}
