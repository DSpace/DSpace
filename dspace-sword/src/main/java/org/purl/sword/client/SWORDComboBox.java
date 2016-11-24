/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package org.purl.sword.client;

import javax.swing.JComboBox;

/**
 * An extension of the JComboBox class. This adds a method that 
 * can update the list of items with the item. The update will only
 * work on combo boxes that are set to editable. 
 * 
 * @author Neil Taylor
 */
public class SWORDComboBox extends JComboBox
{
    /**
     * Create an instance of the SWORD Combo box. 
     */
    public SWORDComboBox()
    {
        super();
        setEditable(true);
    }
 
    /**
     * Update the list for the Combo box with the currently selected
     * item. This will only add an item to the list if: i) the control 
     * is editable, ii) the selected item is not empty and iii) the
     * item is not already in the list. 
     */
    public void updateList()
    {
        Object s = getSelectedItem();
        
        if ( ! isEditable() || s == null || (s != null && ((String)s).trim().length() == 0 ) )
        {
            // don't update with an empty item or if the combo box is not editable.
            return;   
        }
        
        insertItem(s);
    }
    
    /**
     * Insert an item into the combo box. This will only be added
     * if the item is not already present in the combo box. 
     * 
     * @param newItem The item to insert. 
     */
    public void insertItem(Object newItem)
    {
        int count = getItemCount(); 
 
        boolean found = false; 
 
        for ( int i = 0; i < count && ! found; i++ )
        {
            Object item = getItemAt(i);
            if ( item != null && item.equals(newItem) )
            {
                found = true; 
            }
        }
 
        if ( ! found )
        {
            addItem(newItem);
        } 
    }
    
    /**
     * Insert multiple items into the combo box. 
     * 
     * @param items The array of items. 
     */
    public void insertItems(String[] items)
    {
        for ( String item : items )
        {
             insertItem(item);
        }
    }
    
    /**
     * Get the text of the currently selected item in the combo box.
     * @return The text. <code>null</code> is returned if no item 
     * is selected. 
     */
    public String getText()
    {
        Object o = getSelectedItem(); 
        if ( o != null )
        {
            return o.toString().trim();
        }
 
        return null;
    }
}
