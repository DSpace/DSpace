/**
 * Copyright (c) 2008, Aberystwyth University
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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

/**
 * Utility class. Creates a two column form. The left column is used to show 
 * the label for the row. The right column is used to show the control, e.g. 
 * text box, combo box or checkbox, for the row. 
 * 
 * @author Neil Taylor
 */
public class SWORDFormPanel extends JPanel
{
   /**
    * Constraints used to control the layout on the panel. 
    */
   private GridBagConstraints labelConstraints; 
   
   /**
    * Constraints used to control the layout of the input controls on the panel. 
    */
   private GridBagConstraints controlConstraints; 
   
   /** 
    * Index to the next row.
    */
   private int rowIndex = 0; 
   
   /**
    * Insets for the top row of the label column. 
    */
   private Insets labelTop = new Insets(10, 10, 0, 0);
   
   /**
    * Insets for the top row of the control column. 
    */
   private Insets controlTop = new Insets(10, 4, 0, 10);
   
   /**
    * Insets for a general row in the label column. 
    */
   private Insets labelGeneral = new Insets(3, 10, 0, 0);
   
   /**
    * Insets for a general row in the control column. 
    */
   private Insets controlGeneral = new Insets(3, 4, 0, 10); 
   
   /**
    * Create a new instance of the class. 
    */
   public SWORDFormPanel() 
   {
      super(); 
      setLayout(new GridBagLayout());
      
      labelConstraints = new GridBagConstraints();
      labelConstraints.fill = GridBagConstraints.NONE;
      labelConstraints.anchor = GridBagConstraints.LINE_END;
      labelConstraints.weightx = 0.1;
     
      controlConstraints = new GridBagConstraints();
      controlConstraints.fill = GridBagConstraints.HORIZONTAL;
      controlConstraints.weightx = 0.9;
   }
   
   /**
    * Add the specified component as the first row. It will occupy two 
    * columns. 
    * 
    * @param one The control to add.
    */
   public void addFirstRow(Component one)
   {
      addRow(one, null, labelTop, controlTop);
   }
   
   /**
    * Add the specified components as the first row in the form. 
    * @param one The label component. 
    * @param two The control component. 
    */
   public void addFirstRow(Component one, Component two)
   {
      addRow(one, two, labelTop, controlTop);
   }
   
   /**
    * Add a component to the general row. This will be added in the label column.  
    * @param one The component. 
    */
   public void addRow(Component one)
   {
      addRow(one, null);
   }
   
   /**
    * Add a component to the general row. 
    * @param one The component to add to the label column. 
    * @param two The component to add to the control column. 
    */
   public void addRow(Component one, Component two)
   {
      addRow(one, two, labelGeneral, controlGeneral);
   }
   
   /**
    * Add a row to the table. 
    * 
    * @param one The component to display in the label column. 
    * @param two The component to display in the control column. 
    * @param labels The insets for the label column. 
    * @param controls The insets for the controls column. 
    */
   protected void addRow(Component one, Component two, Insets labels, Insets controls )
   {
      labelConstraints.insets = labels; 
      labelConstraints.gridx = 0;
      labelConstraints.gridy = rowIndex;
      if( two == null )
      {
         labelConstraints.gridwidth = 2;
      }
      else
      {
         labelConstraints.gridwidth = 1;
      }
      
      add(one, labelConstraints);
      
      if( two != null )
      {
         controlConstraints.insets = controls; 
         controlConstraints.gridx = 1;
         controlConstraints.gridy = rowIndex;
         add(two, controlConstraints);
      }
      
      rowIndex++; 
   }
}
