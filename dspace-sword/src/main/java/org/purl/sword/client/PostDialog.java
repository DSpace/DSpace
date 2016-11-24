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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog for users to enter details of post destinations. 
 * 
 * @author Neil Taylor
 */
public class PostDialog 
implements ActionListener, ChangeListener
{
    /**
     * label for the browse command. 
     */
    protected static final String BROWSE = "browse"; 
    
    /**
     * label for the add command. 
     */
    protected static final String ADD = "add";
    
    /**
     * label for the edit command. 
     */
    protected static final String EDIT = "edit";
    
    /**
     * label for the delete command. 
     */
    protected static final String DELETE = "delete";
    
    /**
     * label for the clear command. 
     */
    protected static final String CLEAR = "clear";
    
    /**
     * Username combo box. 
     */
    private SWORDComboBox username;
    
    /**
     * Post Location combo box. 
     */
    private SWORDComboBox postLocation;
    
    /**
     * Password field. 
     */
    private JPasswordField password;
    
    /**
     * The file combo box. 
     */
    private SWORDComboBox file;
    
    /**
     * The filetype combo box. 
     */
    private SWORDComboBox fileType;
    
    /**
     * The onBehalfOf combo box. 
     */
    private SWORDComboBox onBehalfOf;
    
    /**
     * The md5 checkbox. 
     */
    private JCheckBox useMD5;
    
    /**
     * The corruptMD5 checkbox.
     */
    private JCheckBox corruptMD5;
 
    /**
     * The corruptRequest checkbox.
     */
    private JCheckBox corruptRequest;
 
    /**
     * The useNoOp checkbox. 
     */
    private JCheckBox useNoOp;
    
    /**
     * The verbose checkbox. 
     */
    private JCheckBox useVerbose;
    
    /**
     * The format namespace combo box. 
     */
    private SWORDComboBox formatNamespace;
 
    /**
     * The list of post destinations. 
     */
    private JList list;
    
    /**
     * The parent frame for the dialog that is displayed.
     */
    private JFrame parentFrame = null; 
    
    /**
     * Array that lists the labels for the buttons on the panel.
     */
    private static Object[] options = {"Post File", "Cancel" };
    
    /**
     * The panel that holds the controls to show. 
     */
    private JPanel controls = null; 
 
    /**
     * 
     * @param parentFrame the parent of this dialog.
     */
    public PostDialog(JFrame parentFrame)
    {
       this.parentFrame = parentFrame; 
       controls = createControls();
    }
    
    /**
     * Show the dialog with ok and cancel options. 
     * @return The return value from displaying JOptionPane. Either 
     *         JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION.
     */
    public int show( )
    {
      int result =  JOptionPane.showOptionDialog(parentFrame,
         controls,
         "Post Document",
         JOptionPane.OK_CANCEL_OPTION,
         JOptionPane.PLAIN_MESSAGE,
         null,
         options,
         null);
      
      if ( result == JOptionPane.OK_OPTION )
      {
         // update the combo boxes with the values 
         username.updateList();
         file.updateList();
         fileType.updateList();
         onBehalfOf.updateList();
         formatNamespace.updateList();
      }
      
      return result; 
    }
    
    /**
     * Create the controls for the main panel. 
     * 
     * @return The panel. 
     */
    protected final JPanel createControls( )
    {
       file = new SWORDComboBox();
       JPanel filePanel = new JPanel(new BorderLayout());
       filePanel.add(file, BorderLayout.CENTER);
       JButton browse = new JButton("Browse...");
       browse.setActionCommand(BROWSE);
       browse.addActionListener(this);
       
       filePanel.add(browse, BorderLayout.SOUTH);
       
       fileType = new SWORDComboBox();
       String type = "application/zip";
       fileType.addItem(type);
       fileType.setSelectedItem(type);
       
       // controls that will be used in the second dialog
       postLocation = new SWORDComboBox();
       username = new SWORDComboBox();
       password = new JPasswordField();
       onBehalfOf = new SWORDComboBox();
       
       
       useMD5 = new JCheckBox();
       useMD5.addChangeListener(this);
       corruptMD5 = new JCheckBox();
       corruptRequest = new JCheckBox();
       useNoOp = new JCheckBox();
       useVerbose = new JCheckBox();
       formatNamespace = new SWORDComboBox();
       
       JLabel fileLabel = new JLabel("File:", JLabel.TRAILING);
       JLabel fileTypeLabel = new JLabel("File Type:", JLabel.TRAILING);
       JLabel useMD5Label = new JLabel("Use MD5:", JLabel.TRAILING);
       JLabel corruptMD5Label = new JLabel("Corrupt MD5:", JLabel.TRAILING);
       JLabel corruptRequestLabel = new JLabel("Corrupt Request:", JLabel.TRAILING);
       //JLabel corruptMD5Label = new JLabel("Corrupt MD5:", JLabel.TRAILING);
       JLabel useNoOpLabel = new JLabel("Use noOp:", JLabel.TRAILING);
       JLabel useVerboseLabel = new JLabel("Use verbose:", JLabel.TRAILING);
       JLabel formatNamespaceLabel = new JLabel("X-Packaging:", JLabel.TRAILING);
       JLabel userAgentLabel = new JLabel("User Agent:", JLabel.TRAILING);
       JLabel userAgentNameLabel = new JLabel(ClientConstants.SERVICE_NAME, JLabel.LEADING);
       
       SWORDFormPanel panel = new SWORDFormPanel(); 
       panel.addFirstRow(new JLabel("Please enter the details for the post operation"));
       
       JPanel destinations = createDestinationsPanel();
       
       panel.addRow(new JLabel("Destinations:"), destinations);
       panel.addRow(fileLabel, filePanel);
       panel.addRow(fileTypeLabel, fileType);
       panel.addRow(useMD5Label, useMD5);
       panel.addRow(corruptMD5Label, corruptMD5);
       panel.addRow(corruptRequestLabel, corruptRequest);
       panel.addRow(useNoOpLabel, useNoOp);
       panel.addRow(useVerboseLabel, useVerbose);
       panel.addRow(formatNamespaceLabel, formatNamespace);
       panel.addRow(userAgentLabel, userAgentNameLabel);
       
       return panel; 
    }
 
    /**
     * Create the destinations panel. This contains a list and four buttons
     * to operate on values in the list. 
     *  
     * @return The panel containing the controls. 
     */
    protected JPanel createDestinationsPanel()
    {
       DefaultListModel model = new DefaultListModel();
       list = new JList(model);
       JScrollPane jsp = new JScrollPane(list);
       
       JPanel destinations = new JPanel(new BorderLayout());
       destinations.add(jsp, BorderLayout.CENTER);
       JPanel destinationButtons = new JPanel();
       
       JButton addButton = new JButton("Add");
       addButton.setActionCommand(ADD);
       addButton.addActionListener(this);
       
       JButton editButton = new JButton("Edit");
       editButton.setActionCommand(EDIT);
       editButton.addActionListener(this);
       
       JButton deleteButton = new JButton("Delete");
       deleteButton.setActionCommand(DELETE);
       deleteButton.addActionListener(this);
       
       JButton clearButton = new JButton("Clear");
       clearButton.setActionCommand(CLEAR);
       clearButton.addActionListener(this);
       
       destinationButtons.add(addButton);
       destinationButtons.add(editButton);
       destinationButtons.add(deleteButton);
       destinationButtons.add(clearButton);
       
       destinations.add(destinationButtons, BorderLayout.SOUTH);
       
       return destinations; 
    }
    
    /**
     * Handle the button click to select a file to upload. 
     */
    public void actionPerformed(ActionEvent evt) 
    {
       String cmd = evt.getActionCommand();
       
       if ( BROWSE.equals(cmd) )
       {
          JFileChooser chooser = new JFileChooser();
          chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
          int returnVal = chooser.showOpenDialog(parentFrame);
          if (returnVal == JFileChooser.APPROVE_OPTION) {
              file.setSelectedItem(chooser.getSelectedFile().getAbsolutePath());
          }
       }
       else if ( ADD.equals(cmd))
       {
          PostDestination dest = showDestinationDialog(null);
          if ( dest != null ) 
          {
              ((DefaultListModel)list.getModel()).addElement(dest);
          }
       }
       else if ( EDIT.equals(cmd))
       {
          PostDestination dest = (PostDestination)list.getSelectedValue(); 
          if ( dest != null ) 
          {
             showDestinationDialog(dest);
             list.repaint();
          }
       }
       else if ( DELETE.equals(cmd))
       {
          if ( list.getSelectedIndex() != -1 )
          {
              ((DefaultListModel)list.getModel()).removeElementAt(list.getSelectedIndex());
          }
       }
       else if ( CLEAR.equals(cmd))
       {
           ((DefaultListModel)list.getModel()).clear();
       }
    }
    
    /**
     * Show the destination dialog. This is used to enter the URL, 
     * username, password and onBehalfOf name for a destination. 
     * 
     * @param destination The post destination. If this is not null, the values
     *                    in the object are used to set the current values 
     *                    in the dialog controls.  
     * @return The post destination value. 
     */
    public PostDestination showDestinationDialog( PostDestination destination )
    {
       SWORDFormPanel panel = new SWORDFormPanel(); 
       panel.addFirstRow(new JLabel("Please enter the details for the post operation"));
 
       JLabel postLabel = new JLabel("Post Location:", JLabel.TRAILING);
       JLabel userLabel = new JLabel("Username:", JLabel.TRAILING);
       JLabel passwordLabel = new JLabel("Password:", JLabel.TRAILING);
       JLabel onBehalfOfLabel = new JLabel("On Behalf Of:", JLabel.TRAILING);
 
       panel.addRow(postLabel, postLocation);
       panel.addRow(userLabel, username);
       panel.addRow(passwordLabel, password);
       panel.addRow(onBehalfOfLabel, onBehalfOf);
 
       if ( destination != null )
       {
          postLocation.insertItem(destination.getUrl());
          username.insertItem(destination.getUsername());
          password.setText(destination.getPassword());
          onBehalfOf.insertItem(destination.getOnBehalfOf());
       }
       else
       {
          String s = "";
          postLocation.insertItem(s);
          //postLocation.setSelectedItem(s);
          username.insertItem(s);
          username.setSelectedItem(s);
          password.setText(s);
          onBehalfOf.insertItem(s);
          onBehalfOf.setSelectedItem(s);
       }
 
       int result =  JOptionPane.showOptionDialog(null,
           panel,
           "Destination",
           JOptionPane.OK_CANCEL_OPTION,
           JOptionPane.PLAIN_MESSAGE,
           null,
           new String[] { "OK", "Cancel" },
           null);
 
       if ( result == JOptionPane.OK_OPTION )
       {
          postLocation.updateList();
          username.updateList();
          onBehalfOf.updateList();
          
          if ( destination == null ) 
          {
             destination = new PostDestination();   
          }
          
          destination.setUrl(postLocation.getText());
          destination.setUsername(username.getText());
          String pass = new String(password.getPassword());
          if ( pass.length() > 0 )
          {
             destination.setPassword(pass);
          }
          else
          {
             destination.setPassword(null);
          }
          
          String obo = onBehalfOf.getText();
          if ( obo.length() > 0 ) 
          {
              destination.setOnBehalfOf(onBehalfOf.getText());
          }
          else
          {
              destination.setOnBehalfOf(null);
          }
          
       }
 
       return destination; 
    }
    
    /**
     * Get the list of Post Destinations.  
     * @return The destinations. 
     */
    public PostDestination[] getDestinations()
    {
       DefaultListModel model = (DefaultListModel)list.getModel();
       PostDestination[] destinations = new PostDestination[model.size()];
       for ( int i = 0; i < model.size(); i++)
       {
          destinations[i] = (PostDestination)model.get(i);
       }
       return destinations;
    }
    
    /**
     * Get the file details.  
     * @return The value. 
     */
    public String getFile( ) 
    {
        return file.getText();
    }
    
    /**
     * Get the filetype value. 
     * @return The value. 
     */
    public String getFileType() 
    {
        return fileType.getText();
    }
    
    /**
     * Get the onBehalfOf value. 
     * @return The value. 
     */
    public String getOnBehalfOf()
    {
        return onBehalfOf.getText();
    }
    
    /**
     * Get the format namespace value. 
     * @return The value. 
     */
    public String getFormatNamespace()
    {
        return formatNamespace.getText();
    }
    
    /**
     * Determine if the MD5 checkbox is selected. 
     * 
     * @return True if the MD5 checkbox is selected. 
     */
    public boolean useMd5()
    {
        return useMD5.isSelected();
    }
    
    /**
     * Determine if the noOp checkbox is selected. 
     * 
     * @return True if the checkbox is selected. 
     */
    public boolean useNoOp()
    {
        return useNoOp.isSelected();
    }
    
    /**
     * Determine if the verbose checkbox is selected.
     *  
     * @return True if the checkbox is selected. 
     */
    public boolean useVerbose()
    {
        return useVerbose.isSelected();
    }
 
    /**
     * Get the post location. 
     * @return The post location. 
     */
    public String getPostLocation()
    {
        return postLocation.getText();
    }
    
    /**
     * Determine if the MD5 hash should be corrupted.
     * @return True if the corrupt MD5 checkbox is selected. The MD5 checkbox
     * must also be selected.
     */
    public boolean corruptMD5()
    {
        return (corruptMD5.isEnabled() && corruptMD5.isSelected());
    }
 
    /**
     * Determine if the POST request should be corrupted.
     * @return True if the corrupt request checkbox is selected.
     */
    public boolean corruptRequest()
    {
        return (corruptRequest.isSelected());
    }
 
    /**
     * Detect a state change event for the checkbox. 
     * 
     * @param evt The event. 
     */
    public void stateChanged(ChangeEvent evt) 
    {
       corruptMD5.setEnabled(useMD5.isSelected());  
    }
    
    /**
     * Add a list of user ids. 
     * 
     * @param users The user ids. 
     */
    public void addUserIds(String[] users)
    {
        username.insertItems(users);
    }
    
    /**
     * Add a list of deposit URLs.
     *  
     * @param deposits The URLs. 
     */
    public void addDepositUrls(String[] deposits)
    {
        postLocation.insertItems(deposits);
    }
    
    /**
     * Add a list of onBehalfOf names. 
     * 
     * @param users The names. 
     */
    public void addOnBehalfOf(String[] users)
    {
        onBehalfOf.insertItems(users);
    }
    
    /**
     * Add the list of formatNamespace strings. 
     * 
     * @param namespaces list of strings. 
     */
    public void addFormatNamespaces(String[] namespaces)
    {
        formatNamespace.insertItems(namespaces);
    }
    
    /**
     * Add a list of file types. 
     * 
     * @param types The file types. 
     */
    public void addFileTypes(String[] types)
    {
        fileType.insertItems(types);
    }
    
    /**
     * Add a list of file names. 
     * @param files The list of files. 
     */
    public void addFiles(String[] files)
    {
        file.insertItems(files);
    }
    
    /**
     * Set the deposit location. 
     * 
     * @param location The location. 
     */
    public void setDepositLocation(String location)
    {
        postLocation.insertItem(location);
        postLocation.setSelectedItem(location);
    }
}
