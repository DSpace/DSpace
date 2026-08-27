/*
 * Restrict.java
 *
 * Created on 03 September 2004, 09:47
 *
 * Version: $Revision: 1.1.1.1 $
 *
 * Date: $Date: 2014/08/12 19:47:49 $
 *
 * Copyright (c) 2004, The University of Edinburgh.  All rights reserved.
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
 * - Neither the name of the University of Edinburgh, or the names of the
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
//package ac.ed.dspace.submit;

//package org.dspace.app.webui.util;
package org.dspace.app.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


import org.apache.logging.log4j.Logger;
//import org.dspace.core.LogManager;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Umrestricted;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.sql.Timestamp;

//import org.dspace.storage.rdbms.DatabaseManager;
//import org.dspace.storage.rdbms.TableRow;
//import org.dspace.storage.rdbms.TableRowIterator;

//import ac.ed.dspace.submit.Licence;
import java.util.Iterator;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.UmrestrictedService;
import org.dspace.content.service.ItemService;
import java.util.UUID;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Apply the required restrictions to an item
 *
 * @author  Richard Jones
 */
public class Restrict {


    private static final UmrestrictedService umrestrictedService = ContentServiceFactory.getInstance().getUmrestrictedService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    //@Autowired(required = true)
    //protected UmrestrictedService umrestrictedService;    
    //protected final ItemService umrestrictedService = ContentServiceFactory.getInstance().getUmrestrictedService();

    //@Autowired(required = true)
    //protected ItemService itemService;
    //protected final ItemService itemService = ContentServiceFactory.getInstance().getItemService();


    /** Creates a new instance of RestrictRelease */
    //public  Restrict() 
    //{
    // }

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(Restrict.class);

    /**
     * apply one of the available restrictions to the item
     *
     * @param item          the item to have restrictions applied to
     * @param restriction   the integer identifier of the restriction type
     */
    public static void apply(Context context, Item item, int restriction)
        throws SQLException, AuthorizeException, IOException
    {
        switch (restriction)
        {
            case 0:
                generalRestrict(context, item, 3);
                break;
            case 1:
                generalRestrict(context, item, 6);
                break;
            case 2:
                generalRestrict(context, item, 12);
                break;
            case 3:
                domainRestrict(item, 1);
                break;
            case 4:
                domainRestrict(item, 2);
                break;
            case 5:
                totalRestrict(context, item);
                break;
            default:
                break;
        }
    }
    
     public static void applyByMonth(Context context, Item item, int restriction)
         throws SQLException, AuthorizeException, IOException
     {
       generalRestrict(context, item, restriction);
     }


     public static void applyByFullDate(Context context, Item item, String fulldate)
         throws SQLException, AuthorizeException, IOException
     {

        // we can't actually do policy restrictions, but we withdraw the item
        //item.withdraw();
        itemService.withdraw(context, item, "");

        umrestrictedService.createUmrestricted ( context, item.getID().toString(), fulldate );
     }


    /**
     * apply a general restriction to the item
     *
     * @param item          the item to have restrictions applied to
     * @param months         the number of months to restrict for
     */
    public static void generalRestrict(Context context, Item item, int months)
        throws SQLException, AuthorizeException, IOException
    {
        // we can't actually do policy restrictions, but we withdraw the item
        //item.withdraw();
        itemService.withdraw(context, item, "");

        // create a date object with the release date in it
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar release = Calendar.getInstance();
        release.setTime(new Date());
        release.add(release.MONTH, months);
        Date releaseDate = release.getTime();
        
	//log.warn(LogManager.getHeader(context, "in generalRestrict ",
		//		      "releaseDate" + releaseDate));


	umrestrictedService.createUmrestricted ( context, item.getID().toString(), df.format(releaseDate) );
    
	//umrestrictedService.update(context, item);
	}
    
     /**
     * apply a total restriction to the item
     *
     * @param item          the item to have restrictions applied to
     * @param yeras         the number of years to restrict for
     */
    public static void totalRestrict(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        // we can't actually do policy restrictions, but we withdraw the item
        //item.withdraw();
        itemService.withdraw(context, item, "");      


 
        // this item is not marked for release at any point, so we don't
        // enter it into the database
        
        // QUESTION: should items that are withdrawn in this manner be 
        // registered somewhere or are they OK just as withdrawn?
        
    }
    
    /**
     * apply a domain restriction to the item
     *
     * @param item          the item to have restrictions applied to
     * @param yeras         the number of years to restrict for
     */
    public static void domainRestrict(Item item, int years)
    {
        // there is no code here for the moment - we need to decide how best
        // to do domain restrictions
        return;
    }
    
    /**
     * release all restrictions on a given item
     *
     * @param item          the item to have restrictions applied to
     * @param context       the context of the request
     */
    public static void release(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
        // get the item ID as a string
        String itemID = item.getID().toString();
        
        // delete the database entry
        //DatabaseManager.deleteByValue(context, "umrestricted", "item_id", 
        //                              itemID);
        umrestrictedService.deleteUmrestricted ( context, item.getID().toString() );

    
        System.out.println("Item released: " + itemID.toString());
        
        // un-withdraw the item
        itemService.reinstate(context, item);
        itemService.update(context, item);
    }
    
    /**
     * release all restrictions on a given item
     *
     * @param item          the item to have restrictions applied to
     * @param context       the context of the request
     */
    public static void release(Context context, Item[] items)
        throws SQLException, AuthorizeException, IOException
    {
        for (int i = 0; i < items.length; i++)
        {
            release(context, items[i]);
        }
    }
    /**
     * find all of the items that are restricted
     *
     * @param context          the context of the request
     *
     * @return      an array of items.
     */
    public static Item[] getRestricted(Context context)
        throws SQLException, AuthorizeException, IOException
    {

        //String query="SELECT * FROM umrestricted";
        Iterator<Umrestricted> allUmrestricted = umrestrictedService.findAllUmrestricted ( context  );
            
        //List itemList = new ArrayList();
        List<Item> itemList = new ArrayList<Item>();
        Iterator<Item> allItems = itemService.findAll(context);
        while (allUmrestricted.hasNext()) {
            Umrestricted um = allUmrestricted.next();
            Item item = itemService.find(context, UUID.fromString(um.getItemId() ));
            itemList.add(item);
        }

        //Item[] itemArray = (Item[]) itemList.toArray(itemList);
        Item[] itemArray = itemList.toArray(new Item[itemList.size()]);

        return itemArray;
    }

    public static Item[] getRestrictedByItem(Context context, String item_id)
        throws SQLException, AuthorizeException, IOException
    {
        //String query="SELECT * FROM umrestricted where item_id = ? ";
        
        Iterator<Umrestricted> allUmrestricted = umrestrictedService.findAllByItemIdUmrestricted ( context, item_id  );

        //List itemList = new ArrayList();
        List<Item> itemList = new ArrayList<Item>();
        while (allUmrestricted.hasNext()) {
            Umrestricted um = allUmrestricted.next();
            Item item = itemService.find(context, UUID.fromString(um.getItemId() ));
            itemList.add(item);
        }

        //Item[] itemArray = (Item[]) itemList.toArray(itemList);
        Item[] itemArray = itemList.toArray(new Item[itemList.size()]);

        return itemArray;    

    }
    
    /**
     * find all of the items that are due to be derestricted
     *
     * @param context          the context of the request
     *
     * @return      an array of items.
     */
    public static Item[] getAvailable(Context context)
        throws SQLException, AuthorizeException, IOException
    {

        //String query="SELECT * FROM umrestricted " +
        //              "WHERE release_date < current_timestamp";
       

        Timestamp current_date = new Timestamp(System.currentTimeMillis());
        log.info ( "Release restricted current_date = " + current_date.toString() );


        Iterator<Umrestricted> allUmrestricted = umrestrictedService.findAllByDateUmrestricted ( context, current_date.toString()  );

        //List itemList = new ArrayList();
        List<Item> itemList = new ArrayList<Item>();
        while (allUmrestricted.hasNext()) {
            Umrestricted um = allUmrestricted.next();

	    // Output the item and the release date of the item
            log.info ( "Releasing item_id => " + um.getItemId() + " the release_date=> " + um.getReleaseDate());
	    System.out.println ( "Releasing item_id => " + um.getItemId() + " the release_date=> " + um.getReleaseDate());

            Item item = itemService.find(context, UUID.fromString(um.getItemId() ));
            itemList.add(item);
        }

        //Item[] itemArray = (Item[]) itemList.toArray(itemList);
        Item[] itemArray = itemList.toArray(new Item[itemList.size()]);

        return itemArray;
       
    }
    
}
