/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
