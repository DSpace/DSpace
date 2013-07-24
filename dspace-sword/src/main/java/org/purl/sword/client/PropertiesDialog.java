/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import java.awt.BorderLayout;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.DefaultCellEditor;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

/**
 * Dialog that is used to edit the collection of properties. 
 * 
 * @author Neil Taylor, Suzana Barreto
 */
public class PropertiesDialog
{
   /**
    * The parent frame for the dialog that is displayed.
    */
   private JFrame parentFrame = null; 
   
   /**
    * Array that lists the labels for the buttons on the panel.
    */
   private static Object[] options = {"OK", "Cancel" };
   
   /**
    * The panel that holds the controls to show. 
    */
   private JPanel controls = null; 
   
   /**
    * The configuration properties
    */
   private Properties properties = null;
   
   /**
    * Table that is used to display the list of properties. 
    */
   private JTable propertiesTable;
   
   /**
    * Create a new instance. 
    * 
    * @param parentFrame  The parent frame for the dialog. 
    * @param props        The properties lisst to display
    */
   public PropertiesDialog(JFrame parentFrame, Properties props)
   {
      this.parentFrame = parentFrame; 
      properties = props;
      controls = createControls();
   }
   
   /**
    * Create the controls that are to be displayed in the system. 
    * 
    * @return A panel that contains the controls. 
    */
   protected final JPanel createControls( )
   {
      JPanel panel = new JPanel(new BorderLayout());
      propertiesTable = new JTable(new PropertiesModel());
      ((DefaultCellEditor)propertiesTable.getDefaultEditor(String.class)).setClickCountToStart(1);
      JScrollPane scrollpane = new JScrollPane(propertiesTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
              JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      panel.add(scrollpane, BorderLayout.CENTER);
      return panel; 
   }


   /**
    * Show the dialog and return the status code. 
    * 
    * @return The status code returned from the dialog. 
    */
   public int show( )
   {
      int result = JOptionPane.showOptionDialog(parentFrame,
            controls,
            "Edit Properties",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            null);

      // cancel any edit in the table. If there is a cell editing, the getEditingColumn will 
      // return a non-negative column number. This can be used to retreive the cell editor. 
      // The code then gets the default editor and calls the stopCellEditing. If custom
      // editors are used, an additional check must be made to get the cell editor 
      // for a specific cell. 
      int column = propertiesTable.getEditingColumn();
      
      if( column > -1 )
      {
         TableCellEditor editor = propertiesTable.getDefaultEditor( propertiesTable.getColumnClass(column));
         if( editor != null ) 
         {
            editor.stopCellEditing();
         }
      }
      
      return result;       
   }

   
   /**
	 * A table model that is used to show the properties. The model links directly 
	 * to the underlying properties object. As changes are made in the table, the 
	 * corresponding changes are made in the properties object. The user can only 
	 * edit the value column in the table. 
	 */
	public class PropertiesModel extends AbstractTableModel
	{
	   /**
	    * Column names. 
	    */
		private String columns[] = {"Property Name","Value"};
		
		/**
		 * Create a new instance of the model. If no properties object exists, 
		 * a default model is created. Note, this will allow the table to
		 * continue editing, although this value will not be passed back to
		 * the calling window. 
		 */
		public PropertiesModel()
		{
			super();
			if(properties == null)
			{
				properties = new Properties();
			}
		}
		
		/**
		 * Get the number of columns. 
		 * 
		 * @return The number of columns. 
		 */
		public int getColumnCount() 
		{
			return columns.length;
		}
		
		/**
		 * Get the number of rows. 
		 * 
		 * @return The number of rows. 
		 */
		public int getRowCount() 
		{
			return properties.size();
		}
		
		/**
		 * Get the value that is at the specified cell. 
		 * 
		 * @param row The row for the cell. 
		 * @param col The column for the cell. 
		 * 
		 * @return The data value from the properties. 
		 */
		public Object getValueAt(int row, int col) 
		{
			if(col == 0)
			{
				return getKeyValue(row);
			}
			else
			{
				String key = getKeyValue(row);
				return properties.get(key);
			}
		}
		
		/**
		 * Retrieve the column name for the specified column. 
		 * 
		 * @param col The column number. 
		 * 
		 * @return The column name. 
		 */
		public String getColumnName(int col){
			return columns[col];
		}
		
		/**
		 * Retrieve the column class. 
		 * 
		 * @param col The column number. 
		 * 
		 * @return The class for the object found at the column position. 
		 */
		public Class getColumnClass(int col) 
		{
           return getValueAt(0, col).getClass();
      }

		/**
		 * Determine if the cell can be edited. This model will only 
		 * allow the second column to be edited. 
		 * 
		 * @param row The cell row. 
		 * @param col The cell column. 
		 * 
		 * @return True if the cell can be edited. Otherwise, false. 
		 */
		public boolean isCellEditable(int row, int col) 
		{
		   if(col == 1)
		   {
		      return true; 
		   }
		   return false; 
		}

		/**
		 * Set the value for the specified cell. 
		 * 
		 * @param value The value to set. 
		 * @param row   The row for the cell. 
		 * @param col   The column. 
		 */
		public void setValueAt(Object value, int row, int col)
		{
		   String key = getKeyValue(row);
		   properties.setProperty(key, ((String) value));
		   fireTableCellUpdated(row, col);
		}

		/** 
		 * Get the Key value for the specified row. 
		 * @param row The row. 
		 * @return A string that shows the key value. 
		 */
		public String getKeyValue(int row)
		{
		   int count = 0;
		   Enumeration<Object> k = properties.keys();
		   while (k.hasMoreElements()) {
		      String key = (String) k.nextElement();
		      if(count == row){
		         return key;
		      }
		      count++;
		   }
		   return null;
		}
	}  
}
