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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Panel to display output messages. Text or characters can be sent to the 
 * panel for display. The panel also includes a button to clear any 
 * text that is currently displayed.  
 * 
 * @author Neil Taylor
 */
public class MessageOutputPanel extends JPanel
implements ActionListener 
{
   
    /**
     * The text area that displays the messages. 
     */
    private JTextArea messages = null; 
    
    /**
     * Create a new instance and initialise the panel.
     */
    public MessageOutputPanel()
    {
        super();
        
        setLayout(new GridBagLayout());
        
        messages = new JTextArea();
        
        JScrollPane detailsPane = new JScrollPane(messages, 
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(this);
        
        //add components and set constraints
        //dpc = details pane constraint
        GridBagConstraints dpc = new GridBagConstraints();
        dpc.gridx = 0;
        dpc.gridy = 0;
        dpc.fill = GridBagConstraints.BOTH;
        dpc.weightx = 0.75;
        dpc.weighty = 0.45;
        dpc.gridwidth = 2;
        dpc.insets = new Insets(5,5,5,5);
        add(detailsPane,dpc);
        
        //cbc = clear button constraint
        GridBagConstraints cbc = new GridBagConstraints();
        cbc.gridx = 1;
        cbc.gridy = 1;
        cbc.insets = new Insets(0,0,5,5);
        cbc.anchor = GridBagConstraints.LINE_END;
        add(clearButton,cbc);
       
    }
    
    /**
     * Add a message to the text area. The message will be added with a carriage return. 
     *  
     * @param message The message. 
     */
    public void addMessage(String message)
    {
        messages.insert(message + "\n", messages.getDocument().getLength());
    }
    
    /**
     * Add a single character to the text area. 
     * 
     * @param character The character. 
     */
    public void addCharacter(Character character)
    {
        messages.insert(character.toString(), messages.getDocument().getLength());
    }
    
    /**
     * Clear the text from the display. 
     * 
     * @param arg0 The action event. 
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent arg0)
    {
        messages.setText("");
    }
}
