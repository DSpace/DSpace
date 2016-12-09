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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * Dialog that prompts the user to enter the details for a service 
 * document location. 
 * 
 * @author Neil Taylor
 */
public class ServiceDialog 
{
    /**
     * The username. 
     */
    private SWORDComboBox username;
    
    /**
     * The password.
     */
    private JPasswordField password;
    
    /**
     * Holds the URL for the collection. 
     */
    private SWORDComboBox location;
    
    /**
     * The combo box that shows the list of onBehalfOf items. 
     */
    private SWORDComboBox onBehalfOf;
     
    /**
     * Parent frame for the dialog. 
     */
    private JFrame parentFrame = null; 
 
    /**
     * The panel that holds the controls. 
     */
    private JPanel controls = null;
 
    /**
     * List of buttons. 
     */
    private static Object[] options = {"Get Service Document", "Cancel" };
    
    /**
     * Create a new instance. 
     * 
     * @param parentFrame The parent frame. The dialog will be shown over the 
     *                    centre of this frame. 
     */
    public ServiceDialog(JFrame parentFrame)
    {
       this.parentFrame = parentFrame; 
       controls = createControls(); 
    }
    
    /**
     * Show the dialog. 
     * 
     * @return The close option. This is one of the dialog options from 
     * JOptionPane. 
     */
    public int show( )
    {
        int result =  JOptionPane.showOptionDialog(parentFrame,
            controls,
            "Get Service Document",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[1]);
         
        if ( result == JOptionPane.OK_OPTION )
        {
           // update the combo boxes with the values 
           username.updateList();
           location.updateList();
           onBehalfOf.updateList();
        }
        
        return result; 
    }
    
    /**
     * Create the controls that are displayed in the dialog. 
     * 
     * @return The panel that contains the controls. 
     */
    protected final JPanel createControls( )
    {
       username = new SWORDComboBox();
       username.setEditable(true);
       password = new JPasswordField();
       location = new SWORDComboBox();
       location.setEditable(true);
       onBehalfOf = new SWORDComboBox();
       onBehalfOf.setEditable(true);
       
       JLabel userLabel = new JLabel("Username:", JLabel.TRAILING);
       JLabel passwordLabel = new JLabel("Password:", JLabel.TRAILING);
       JLabel locationLabel = new JLabel("Location:", JLabel.TRAILING);
       JLabel onBehalfOfLabel = new JLabel("On Behalf Of:", JLabel.TRAILING);
       
       SWORDFormPanel panel = new SWORDFormPanel(); 
       panel.addFirstRow(userLabel, username);
       panel.addRow(passwordLabel, password);
       panel.addRow(locationLabel, location);
       panel.addRow(onBehalfOfLabel, onBehalfOf);
       
       return panel; 
    }
    
    /**
     * Get the username from the controls on the dialog. 
     * 
     * @return The username. 
     */
    public String getUsername()
    {
       return username.getText();    
    }
    
    /**
     * Get the password from the dialog. 
     * 
     * @return The password. 
     */
    public String getPassword()
    {
       return new String(password.getPassword());
    }
    
    /**
     * The location from the dialog. 
     * 
     * @return The location. 
     */
    public String getLocation() 
    {
       return location.getText();     
    }
    
    /**
     * The onBehalfOf value from the dialog. 
     * 
     * @return The onBehalfOf value. 
     */
    public String getOnBehalfOf()
    {
       String text = onBehalfOf.getText().trim();
       if ( text.length() == 0 )
       {
          return null;
       }
       return text;
    }
    
    /**
     * Add the list of user ids to the dialog. 
     * 
     * @param users The list of user ids. 
     */
    public void addUserIds(String[] users)
    {
        username.insertItems(users);
    }
    
    /** 
     * Add the list of service URLs. 
     * 
     * @param services The service URLs. 
     */
    public void addServiceUrls(String[] services)
    {
        location.insertItems(services);
    }
    
    /**
     * Add a list of onBehalfOf names. 
     * 
     * @param users The list of onBehalfOf items. 
     */
    public void addOnBehalfOf(String[] users)
    {
        onBehalfOf.insertItems(users);
    }
   
}
