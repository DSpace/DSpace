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
import java.awt.Component;
import java.util.*;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.purl.sword.atom.Author;
import org.purl.sword.atom.Content;
import org.purl.sword.atom.Contributor;
import org.purl.sword.atom.Generator;
import org.purl.sword.atom.Link;
import org.purl.sword.atom.TextConstruct;
import org.purl.sword.base.Collection;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.Service;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.Workspace;
import org.purl.sword.base.SwordAcceptPackaging;

/**
 * The main panel for the GUI client. This contains the top-two sub-panels: the 
 * tree and the text area to show the details of the selected node. 
 * 
 * @author Neil Taylor
 */
public class ServicePanel extends JPanel
implements TreeSelectionListener 
{
   /**
    * The top level item in the tree that lists services. 
    */
   DefaultMutableTreeNode top;

   /**
    * The tree model used to display the items. 
    */
   DefaultTreeModel treeModel = null; 

   /**
    * Tree that holds the list of services. 
    */
   private JTree services; 

   /**
    * The panel that shows an HTML table with any details for the selected 
    * node in the services tree. 
    */
   private JEditorPane details;

   /**
    * A registered listener. This listener will be notified when there is a 
    * different node selected in the service tree.  
    */
   private ServiceSelectedListener listener; 
   
   /**
    * Create a new instance of the panel.  
    */
   public ServicePanel()
   {
      super(); 
      setLayout(new BorderLayout());

      top = new DefaultMutableTreeNode("Services & Posted Files");
      treeModel = new DefaultTreeModel(top);

      services = new JTree(treeModel); 
      services.setCellRenderer(new ServicePostTreeRenderer());
      
      JScrollPane servicesPane = new JScrollPane(services, 
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

      details = new JEditorPane("text/html", "<html><body><h1>Details</h1><p>This panel will show the details for the currently selected item in the tree.</p></body></html>");

      JScrollPane detailsPane = new JScrollPane(details, 
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            servicesPane,
            detailsPane);
      splitPane.setOneTouchExpandable(true);
      splitPane.setResizeWeight(0.5);
      splitPane.setDividerLocation(200);

      services.addTreeSelectionListener(this);
      ToolTipManager.sharedInstance().registerComponent(services);

      add(splitPane, BorderLayout.CENTER);
   }
   
   /**
    * Renderer that displays the icons for the tree nodes. 
    * 
    * @author Neil Taylor
    */
   static class ServicePostTreeRenderer extends DefaultTreeCellRenderer 
   {
      Icon workspaceIcon; 
      Icon serviceIcon; 
      Icon collectionIcon; 
      Icon fileIcon; 

      /**
       * Initialise the renderer. Load the icons. 
       */
      public ServicePostTreeRenderer() 
      {
          ClassLoader loader = this.getClass().getClassLoader();
          workspaceIcon = new ImageIcon(loader.getResource("images/WorkspaceNodeImage.gif"));
          serviceIcon = new ImageIcon(loader.getResource("images/ServiceNodeImage.gif"));
          collectionIcon = new ImageIcon(loader.getResource("images/CollectionNodeImage.gif"));
          fileIcon = new ImageIcon(loader.getResource("images/ServiceNodeImage.gif"));
      }

      /** 
       * Return the cell renderer. This will be the default tree cell renderer 
       * with a different icon depending upon the type of data in the node. 
       * 
       * @param tree The JTree control. 
       * @param value The value to display. 
       * @param sel True if the node is selected. 
       * @param expanded True if the node is expanded. 
       * @param leaf True if the node is a leaf. 
       * @param row The row. 
       * @param hasFocus True if the node has focus. 
       */
      public Component getTreeCellRendererComponent(
                          JTree tree,
                          Object value,
                          boolean sel,
                          boolean expanded,
                          boolean leaf,
                          int row,
                          boolean hasFocus) {

          JComponent comp = (JComponent)super.getTreeCellRendererComponent(
                          tree, value, sel,
                          expanded, leaf, row,
                          hasFocus);
      
          DefaultMutableTreeNode node =
             (DefaultMutableTreeNode)value;

          Object o = node.getUserObject();
          if( o instanceof TreeNodeWrapper )
          {
             TreeNodeWrapper wrapper = (TreeNodeWrapper)o;
             comp.setToolTipText(wrapper.toString());
             Object data = wrapper.getData();
             if( data instanceof Service ) 
             {
                setIcon(serviceIcon);
             }
             else if( data instanceof Workspace )
             {
                setIcon(workspaceIcon);
             }
             else if( data instanceof Collection )
             {
                setIcon(collectionIcon); 
             }
             else if( data instanceof SWORDEntry )
             {
                setIcon(fileIcon);
             }
          }
          else
          {
             comp.setToolTipText(null);
          }
          return comp;
       }

      
  }

   /**
    * Set the service selected listener. This listener will be notified when 
    * there is a selection change in the tree. 
    * 
    * @param listener The listener. 
    */
   public void setServiceSelectedListener(ServiceSelectedListener listener)
   {
      this.listener = listener; 
   }

   /**
    * Process the specified service document. Add the details as a new child of the
    * root of the tree. 
    * 
    * @param url The url used to access the service document. 
    * @param doc The service document. 
    */
   public void processServiceDocument(String url, 
                                      ServiceDocument doc)
   {
      TreeNodeWrapper wrapper = null; 

      Service service = doc.getService();
      wrapper = new TreeNodeWrapper(url, service);
      DefaultMutableTreeNode serviceNode = new DefaultMutableTreeNode(wrapper);
      treeModel.insertNodeInto(serviceNode, top, top.getChildCount());
      services.scrollPathToVisible(new TreePath(serviceNode.getPath()));

      // process the workspaces
      DefaultMutableTreeNode workspaceNode = null; 

      Iterator<Workspace> workspaces = service.getWorkspaces();
      for (; workspaces.hasNext();)
      {
         Workspace workspace = workspaces.next();
         wrapper = new TreeNodeWrapper(workspace.getTitle(), workspace);
         workspaceNode = new DefaultMutableTreeNode(wrapper);
         treeModel.insertNodeInto(workspaceNode, serviceNode, serviceNode.getChildCount());
         services.scrollPathToVisible(new TreePath(workspaceNode.getPath()));

         DefaultMutableTreeNode collectionNode = null; 
         Iterator<Collection> collections = workspace.collectionIterator();
         for (; collections.hasNext();)
         {
            Collection collection = collections.next();
            wrapper = new TreeNodeWrapper(collection.getTitle(), collection);
            collectionNode = new DefaultMutableTreeNode(wrapper);
            treeModel.insertNodeInto(collectionNode, workspaceNode, workspaceNode.getChildCount());
            services.scrollPathToVisible(new TreePath(collectionNode.getPath()));
         }
      } // for 
   } 

   /**
    * Holds the data for a tree node. It specifies the name that will be displayed
    * in the node, and stores associated data. 
    *  
    * @author Neil Taylor
    */
   static class TreeNodeWrapper
   {
      /**
       * The node name. 
       */
      private String name;
      
      /**
       * The user data. 
       */
      private Object userObject; 

      /** 
       * Create a new instance. 
       * 
       * @param name The name of the node. 
       * @param data The data in the node. 
       */
      public TreeNodeWrapper(String name, Object data)
      {
         this.name = name; 
         this.userObject = data; 
      }

      /**
       * Retrieve the data that is stored in this node. 
       * 
       * @return The data. 
       */
      public Object getData()
      {
         return userObject; 
      }

      /**
       * Get a string description for this node. 
       */
      public String toString()
      {
         if( name == null || name.trim().equals("") )
         {
            return "Unspecified";
         }

         return name;
      }
   }

   /**
    * Respond to a changed tree selection event. Update the details panel to 
    * show an appropriate message for the newly selected node. Also, 
    * alert the selection listener for this panel. The listener will receive 
    * a path, if a collection has been selected. Otherwise, the listener 
    * will receive <code>null</code>. 
    */
   public void valueChanged(TreeSelectionEvent evt) 
   {
      // Get all nodes whose selection status has changed
      TreePath[] paths = evt.getPaths();
     
      for (int i=0; i<paths.length; i++) 
      {
         if (evt.isAddedPath(i)) 
         {
            // process new selections
            DefaultMutableTreeNode node;
            node = (DefaultMutableTreeNode)(paths[i].getLastPathComponent());

            Object o = node.getUserObject();
            if( o instanceof TreeNodeWrapper )
            {
               try
               {
                  TreeNodeWrapper wrapper = (TreeNodeWrapper)o;
                  Object data = wrapper.getData();
                  if( data instanceof Service ) 
                  {
                     showService((Service)data);
                     alertListener(null);
                  }
                  else if( data instanceof Workspace )
                  {
                     showWorkspace((Workspace)data);
                     if( listener != null )
                     {
                         alertListener(null);
                     }
                  }
                  else if( data instanceof Collection )
                  {
                     Collection c = (Collection)data;
                     showCollection(c);
                     alertListener(c.getLocation()); 
                  }
                  else if( data instanceof SWORDEntry )
                  {
                     showEntry((SWORDEntry)data);
                     alertListener(null);
                  }
                  else
                  {
                     details.setText("<html><body>unknown</body></html>");
                     alertListener(null);
                  }
               }
               catch( Exception e )
               {
                  details.setText("<html><body>An error occurred. The message was: " + e.getMessage() + "</body></html>");
                  alertListener(null);
                  e.printStackTrace();
               }
            }
            else
            {
               details.setText("<html><body>please select one of the other nodes</body></html>");
               alertListener(null);
            }
         } 

      }
   }
   
   /**
    * Notify the listener that there has been a change to the currently selected 
    * item in the tree. 
    * 
    * @param value The value to send to the listener. 
    */
   private void alertListener(String value)
   {
      if( listener != null )
      {
         listener.selected(value);
      }
   }

   /**
    * Add a new HTML table row to the specified StringBuffer. The label is displayed in
    * the left column and the value is displayed in the right column.  
    * 
    * @param buffer The destination string buffer. 
    * @param label The label to add. 
    * @param value The corresponding value to add. 
    */
   private void addTableRow(StringBuffer buffer, String label, Object value)
   {
      buffer.append("<tr bgcolor=\"#ffffff;\"><td>");
      buffer.append(label);
      buffer.append("</td><td>");
      buffer.append(displayableValue(value));
      buffer.append("</td></tr>");
   }
   
   /**
    * Show the specified service data in the details panel. 
    * 
    * @param service The service node to display. 
    */
   private void showService(Service service)
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<html>");
      buffer.append("<body>");

      buffer.append("<table border=\"1\" width=\"100%\">");
      buffer.append("<tr bgcolor=\"#69a5c8;\"><td colspan=\"2\"><font size=\"+2\">Service Summary</font></td></tr>");
      addTableRow(buffer, "SWORD Version", service.getVersion());
      addTableRow(buffer, "NoOp Support ", service.isNoOp());
      addTableRow(buffer, "Verbose Support ", service.isVerbose());

      String maxSize = "";

       // Commented out the following code as the client code is out of step with the
       // Sword 'base' library and wont compile. - Robin Taylor.
       //if( service.maxUploadIsDefined() )
      //{
      //    maxSize = "" + service.getMaxUploadSize() + "kB";
      //}
      //else
      //{
          maxSize = "undefined";
      //}

      addTableRow(buffer, "Max File Upload Size ", maxSize);
      
      buffer.append("</table>");

      buffer.append("</body>");
      buffer.append("</html>");
      details.setText(buffer.toString());
   }

   /**
    * Display the workspace data in the details panel. 
    * 
    * @param workspace The workspace. 
    */
   private void showWorkspace(Workspace workspace)
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<html>");
      buffer.append("<body>");

      buffer.append("<table border=\"1\" width=\"100%\">");
      buffer.append("<tr bgcolor=\"#69a5c8;\"><td colspan=\"2\"><font size=\"+2\">Workspace Summary</font></td></tr>");
      addTableRow(buffer, "Workspace Title", workspace.getTitle());
      buffer.append("</table>");

      buffer.append("</body>");
      buffer.append("</html>");
      details.setText(buffer.toString());
   }

   /**
    * Return the parameter unmodified if set, or the not defined text if null
    * @param s
    * @return s or ClientConstants.NOT_DEFINED_TEXT
    */
   private Object displayableValue(Object s)
   {
       if (null == s)
       {
           return ClientConstants.NOT_DEFINED_TEXT;
       }else{
           return s;
       }
   }

   /** 
    * Add a string within paragraph tags. 
    * 
    * @param buffer The buffer to add the message to. 
    * @param message The message to add. 
    */
   private void addPara(StringBuffer buffer, String message)
   {
      buffer.append("<p>" + message + "</p>");	
   }

   /**
    * Show the specified collection data in the details panel. 
    * 
    * @param collection The collection data. 
    */
   private void showCollection(Collection collection)
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<html>");
      buffer.append("<body>");

      if( collection == null ) 
      {
         addPara(buffer, "Invalid Collection object. Unable to display details.");
      }
      else
      {
         buffer.append("<table border=\"1\" width=\"100%\">");
         buffer.append("<tr bgcolor=\"#69a5c8;\"><td colspan=\"2\"><font size=\"+2\">Collection Summary</font></td></tr>");
         addTableRow(buffer, "Collection location", collection.getLocation());
         addTableRow(buffer, "Collection title", collection.getTitle());
         addTableRow(buffer, "Abstract", collection.getAbstract());
         addTableRow(buffer, "Collection Policy", collection.getCollectionPolicy());
         addTableRow(buffer, "Treatment", collection.getTreatment());
         addTableRow(buffer, "Mediation", collection.getMediation());
         addTableRow(buffer, "Nested Service Document", collection.getService());

         String[] accepts = collection.getAccepts();
         StringBuilder acceptList = new StringBuilder();
         if( accepts != null && accepts.length == 0 )
         {
            acceptList.append("None specified");
         }
         else
         {
            for (String s : accepts)
            {
               acceptList.append(s).append("<br>");
            }
         }
         addTableRow(buffer, "Accepts", acceptList.toString());

         List<SwordAcceptPackaging> acceptsPackaging = collection.getAcceptPackaging();

         StringBuilder acceptPackagingList = new StringBuilder();
         for (Iterator i = acceptsPackaging.iterator(); i.hasNext();) {
             SwordAcceptPackaging accept = (SwordAcceptPackaging) i.next();
             acceptPackagingList.append(accept.getContent()).append(" (").append(accept.getQualityValue()).append(")");
             
             // add a , separator if there are any more items in the list 
             if( i.hasNext() ) {
                acceptPackagingList.append(", ");
             }
         }

         addTableRow(buffer, "Accepts Packaging", acceptPackagingList.toString());

         buffer.append("</table>");
      }

      buffer.append("</body>");
      buffer.append("</html>");
      details.setText(buffer.toString());
   }

   /**
    * Display the contents of a Post entry in the display panel. 
    * 
    * @param entry The entry to display. 
    */
   private void showEntry(SWORDEntry entry)
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("<html>");
      buffer.append("<body>");

      if( entry == null ) 
      {
         addPara(buffer, "Invalid Entry object. Unable to display details.");
      }
      else
      {
         buffer.append("<table border=\"1\" width=\"100%\">");
         buffer.append("<tr bgcolor=\"#69a5c8;\"><td colspan=\"2\"><font size=\"+2\">Entry Summary</font></td></tr>");

         // process atom:title
         String titleString = getTextConstructDetails(entry.getSummary());
         addTableRow(buffer, "Title", titleString);

         // process id
         addTableRow(buffer, "ID", entry.getId());

         // process updated
         addTableRow(buffer, "Date Updated", entry.getUpdated());

         String authorString = getAuthorDetails(entry.getAuthors());
         addTableRow(buffer, "Authors", authorString);

         // process summary 
         String summaryString = getTextConstructDetails(entry.getSummary());
         addTableRow(buffer, "Summary", summaryString);

         // process content 
         Content content = entry.getContent();
         String contentString = ""; 
         if( content == null ) 
         {
            contentString = "Not defined.";
         }
         else
         {
            contentString += "Source: '" + content.getSource() + "', Type: '" + 
            content.getType() + "'";
         }
         addTableRow(buffer, "Content", contentString);

         // process links 
         Iterator<Link> links = entry.getLinks();
         StringBuffer linkBuffer = new StringBuffer(); 
         for( ; links.hasNext(); )
         {
            Link link = links.next();
            linkBuffer.append("href: '");
            linkBuffer.append(link.getHref());
            linkBuffer.append("', href lang: '");
            linkBuffer.append(link.getHreflang());
            linkBuffer.append("', rel: '");
            linkBuffer.append(link.getRel());
            linkBuffer.append("')<br>");
         }
         if( linkBuffer.length() == 0 )
         {
            linkBuffer.append("Not defined");
         }
         addTableRow(buffer, "Links", linkBuffer.toString());

         // process contributors
         String contributorString = getContributorDetails(entry.getContributors());
         addTableRow(buffer, "Contributors", contributorString);

         // process source
         String sourceString="";
         Generator generator = entry.getGenerator();
         if( generator != null ) 
         {
            sourceString += "Content: '" + generator.getContent() + "' <br>'";
            sourceString += "Version: '" + generator.getVersion() + "' <br>'";
            sourceString += "Uri: '" + generator.getUri() + "'";
         }
         else
         {
            sourceString += "No generator defined.";
         }

         addTableRow(buffer, "Generator", sourceString);

         // process treatment 
         addTableRow(buffer, "Treatment", entry.getTreatment());

         // process verboseDescription 
         addTableRow(buffer, "Verbose Description", entry.getVerboseDescription());

         // process noOp
         addTableRow(buffer, "NoOp", entry.isNoOp());

         // process formatNamespace
         addTableRow(buffer, "Packaging", entry.getPackaging());

         // process userAgent
         addTableRow(buffer, "User Agent", entry.getUserAgent());


         buffer.append("</table>");
      }

      buffer.append("</body>");
      buffer.append("</html>");
      details.setText(buffer.toString());
   }

   /**
    * Retrieve the details for a TextConstruct object.
    *  
    * @param data The text construct object to display. 
    * 
    * @return Either 'Not defined' if the data is <code>null</code>, or 
    *         details of the text content element. 
    */
   private String getTextConstructDetails(TextConstruct data)
   {
      String summaryStr = "";
      if( data == null ) 
      {
         summaryStr = "Not defined";
      }
      else
      {
         summaryStr = "Content: '" + data.getContent() + "', Type: "; 
         if( data.getType() != null )
         {
            summaryStr += "'" + data.getType().toString() + "'";
         }
         else
         {
            summaryStr += "undefined.";
         }
      }

      return summaryStr;
   }

   /**
    * Get the author details and insert them into a string. 
    * 
    * @param authors the list of authors to process. 
    * 
    * @return A string containing the list of authors. 
    */
   private String getAuthorDetails(Iterator<Author> authors)
   {
      // process author
      StringBuffer authorBuffer = new StringBuffer(); 
      for( ; authors.hasNext(); )
      {
         Author a = authors.next(); 
         authorBuffer.append(getAuthorDetails(a));
      }

      if( authorBuffer.length() == 0 )
      {
         authorBuffer.append("Not defined");
      }

      return authorBuffer.toString();
   }

   /**
    * Get the contributor details and insert them into a string.
    * 
    * @param contributors The contributors. 
    * 
    * @return The string that lists the details of the contributors. 
    */
   private String getContributorDetails(Iterator<Contributor> contributors)
   {
      // process author
      StringBuffer authorBuffer = new StringBuffer(); 
      for( ; contributors.hasNext(); )
      {
         Contributor c = contributors.next(); 
         authorBuffer.append(getAuthorDetails(c));
      }

      if( authorBuffer.length() == 0 )
      {
         authorBuffer.append("Not defined");
      }

      return authorBuffer.toString();
   }

   /**
    * Build a string that describes the specified author. 
    * 
    * @param author The author. 
    * 
    * @return The string description. 
    */
   private String getAuthorDetails(Author author)
   {
      // process author
      StringBuffer authorBuffer = new StringBuffer();
      authorBuffer.append(author.getName());
      authorBuffer.append(" (email: '");
      authorBuffer.append(author.getEmail());
      authorBuffer.append("', uri: '");
      authorBuffer.append(author.getUri());
      authorBuffer.append("')<br>");

      return authorBuffer.toString();
   }

   /**
    * Process the deposit response and insert the details into the tree. If the url 
    * matches one of the collections in the tree, the deposit is added as a child
    * node. Otherwise, the node is added as a child of the root. 
    * 
    * @param url The url of the collection that the file was posted to.
    *  
    * @param response The details of the deposit. 
    */
   public void processDepositResponse(String url, 
                                      DepositResponse response)
   {
      SWORDEntry entry = response.getEntry();
      Object title = entry.getTitle(); 
      if( title == null )
      {
         title = "Undefined";
      }
      else
      {
         title = entry.getTitle().getContent();
      }

      TreeNodeWrapper wrapper = new TreeNodeWrapper(title.toString(), entry);
      DefaultMutableTreeNode entryNode = new DefaultMutableTreeNode(wrapper);

      DefaultMutableTreeNode newParentNode = top; 
      List<DefaultMutableTreeNode> nodes = getCollectionNodes();
      for( DefaultMutableTreeNode node : nodes )
      {
         Object o = node.getUserObject();
         if( o instanceof TreeNodeWrapper )
         {
            TreeNodeWrapper collectionWrapper = (TreeNodeWrapper)o;
            Object data = collectionWrapper.getData();
            if( data instanceof Collection )
            {
               Collection col = (Collection)data;
               String location = col.getLocation();
               if( location != null && location.equals(url))
               {
                  newParentNode = node; 
                  break;
               }
            }
         }     
      }
      
      treeModel.insertNodeInto(entryNode, newParentNode, newParentNode.getChildCount());
      services.scrollPathToVisible(new TreePath(entryNode.getPath()));
   }
   
   /**
    * Get a list of all current collections displayed in the tree.
    *  
    * @return An array of the URLs for the collections. 
    */
   public String[] getCollectionLocations() 
   {
      List<DefaultMutableTreeNode> nodes = getCollectionNodes();
      String[] locations = new String[nodes.size()];
      
      DefaultMutableTreeNode node;
      for( int i = 0; i < nodes.size(); i++ )
      {
         node = nodes.get(i);
         Object o = node.getUserObject();
         if( o instanceof TreeNodeWrapper )
         {
            TreeNodeWrapper collectionWrapper = (TreeNodeWrapper)o;
            Object data = collectionWrapper.getData();
            if( data instanceof Collection )
            {
               Collection col = (Collection)data;
               String location = col.getLocation();
               if( location != null )
               {
                  locations[i] = location;                    
               }
            }
         }     
      }
      return locations;
   }
   
   /**
    * Get a list of nodes that contain collections. 
    * 
    * @return A vector of the collection nodes. 
    */
   private List<DefaultMutableTreeNode> getCollectionNodes()
   {
      List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
      
      DefaultMutableTreeNode node;
      Enumeration treeNodes = top.depthFirstEnumeration(); 
      
      while( treeNodes.hasMoreElements() ) 
      {
         node = (DefaultMutableTreeNode)treeNodes.nextElement();
         Object o = node.getUserObject();
         if( o instanceof TreeNodeWrapper )
         {
            TreeNodeWrapper wrapper = (TreeNodeWrapper)o;
            Object data = wrapper.getData();
            if( data instanceof Collection )
            {
               nodes.add(node);
            }
         }
      }
      
      return nodes;
   }
}