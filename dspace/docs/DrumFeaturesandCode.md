# DRUM Features and Code

Summary of DRUM enhancements to base DSpace functionality as well as related changes in the code.

[Administration](#administration)

 * [Add collection to community mapping](#collection-to-community-mapping)
 * [Select/Display the bundle of a bitstreamg](#display-bundle-by-bitstream)
 * [Add preservation bundle](#display-bundle-bitstream)

[Electronic Theses and Dissertations (ETD)](#etd)

##<a name="administration"></a>Administration

* ### <a name="collection-to-community-mapping"></a>Add collection to community mapping
### Customizations Summary
	*	Ability to select unmapped collections
	    
	   	* **Java Source**
			*	New method _getCollectionsUnmapped()_  in the existing source [Community.java](../../dspace-api/src/main/java/org/dspace/content/Community.java)
			*	Updates to in the existing source [Collection.java](../../dspace-api/src/main/java/org/dspace/content/Collection.java)
			*	Updates to in the existing source [SelectCollectionTag.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/jsptag/SelectCollectionTag.java)
			*	Updates to in the existing source [CollectionListServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionListServlet.java)
			*	Updates to  method _findByCommunityGroupTop()_  in the existing source [Community.java](../../dspace-api/src/main/java/org/dspace/content/Community.java)
			*	New class  _CollectionMappingServlet_ in jspui application [CollectionMappingServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionMappingServlet.java)
			*	Updates to  the workflow in the existing source:
				* [WorkflowItem.java](../../dspace-api/src/main/java/org/dspace/workflow/WorkflowItem.java
)
				* [WorkflowItem.java](../../dspace-api/src/main/java/org/dspace/content/WorkspaceItem.java
)
		   * Updates to the selection of collection step:
			   * [SelectCollectionStep.java](../../dspace-api/src/main/java/org/dspace/submit/step/SelectCollectionStep.java) 
			   * [JSPSelectCollectionStep.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/submit/step/JSPSelectCollectionStep.java)	
			   * [InitialQuestionsStep.java](../../dspace-api/src/main/java/org/dspace/submit/step/InitialQuestionsStep.java)				   	   

			* Updates to the DC input set in the existing source [DCInputSet.java](../../dspace-api/src/main/java/org/dspace/app/util/DCInputSet.java)
								
 	* **JSP Pages**					

		*	New jsp page [mapcollections.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/community-home.jsp)
		*	New jsp page [confirm-mapcollection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/confirm-mapcollection.jsp)
		*	New jsp page [confirm-unmapcollection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/confirm-unmapcollection.jsp)
		*	Updates to [community-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/mapcollections.jsp)
		*   Updates to [my dspace.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/mydspace/main.jsp)
		*   Updates to [select-collection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/submit/select-collection.jsp)
		*   Updates to [collection-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/collection-home.jsp)
		*   Updates to [collection-select-list.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/collection-select-list.jsp)
	
   * **Collection Servlet Mappings**
	  
	 * Updates in [web.xml](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/web.xml)		
	
	  * **Input forms**	
		* [input-forms.xml](../../dspace/config/input-forms.xml)
		
	  * **Properties**
			* [Message Properties](../../dspace-api/src/main/resources/Messages.properties)
	 * **JavaScript**
	 		* [utils.js](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/utils.js)
	 * **Tag Libraries**
	 		* [dspace-tags.tld](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/dspace-tags.tld)


<a name="etd"></a>
##Electronic Theses and Dissertations (ETD)
 
### Bitstream start/end authorization

Add display of start and end date for ResourcePolicy on "Policies for Item" edit page.

*JSP Pages*

* [authorize-item-edit.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/authorize-item-edit.jsp)

### Custom embargoed bitstream messaging

Custom messaging for unathorized access to Bitstream if the Bitstream is under embargo; relies on ResourcePolicy with group named 'ETD Embargo'

*Java Source*

* [Bitstream.java](../../dspace-api/src/main/java/org/dspace/content/Bitstream.java) - new methods getETDEmbargo(), isETDEmbargo(), getMetadata(String), getIntMetadata(String), setMetadata(String, String), setMetadata(String, int)
* [ItemTag.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/jsptag/ItemTag.java) - listBistreams() add 'RESTRICTED ACCESS' to the Bitstream description
* [BitstreamServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/BitstreamServlet.java) - doDSGet() if not authorized and is ETD embargo then redirect to /error/authorize-etd-embargo.jsp

*JSP Pages*

* [error/authorize-etd-embargo](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/error/authorize-etd-embargo.jsp) - customized message based on embargo dates

### Embargoed Item Statistics

### Loader
        transform Proquest metadata to dublin core
        transform Proquest metadata to marc and transfer to TSD
        map into department etd collections
        transfer of files to bindery
        email notification: duplicate titles, .csv with embargoes, load report, marc transfer, bindery transfer
 
*Scripts*

* [load-etd-nightly](../../dspace/bin/load-etd-nightly) - nightly check for new etd zip files

* [load-etd](../../dspace/bin/load-etd) - load one etd zip file

* [etd2marc-mail](../../dspace/bin/etd2marc-mail) - email notice of Marc file to Technical Services

* [etd2marc-transfer](../../dspace/bin/etd2marc-transfer) - transfer Marc file to Technical Services (LAN)

*Configuration*

* configuration is currently housed in drum-env source repository

* config/load/etd2dc.xsl - crosswalk Proquest metadata to dublin core

* config/load/etd2marc.xsl - crosswalk Proquest metadata to Marc

*Java Source*

* [EtdLoader.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/app/EtdLoader.java) - main loader control class

        Get command line parameters
        Get properties
        Open zipfile for reading
        Open marc file for writing (append)
        Read all items from zipfile
        Foreach item
           XSL transform: Proquest metadata to dublin core
           Begin new Item
           Add dublin core
           Add additional mapped collections
           Foreach file
              Add Bitstream
           Commit Item
           Add embargo to Bitstreams
           XSL transform: Proquest metadata to marc
           Save to Marc output file
           Save to CSV output file
           If duplicate title
              send email notice
           If no mapped collections
              send email notice

### ETD Departments

Maintain mapping from campus departments (from Proquest metadata) to DSpace collections.

*Configuration*

* [Messages.properties](../../dspace-api/src/main/resources/Messages.properties) - etdunit messages for JSP

*Database Schema*

* [database_schema_etdunit.sql](../../dspace/etc/database_schema_etdunit.sql) - add EtdUnit and Collection2EtdUnit tables

*Java Source*

* [EtdUnit.java](../../dspace-api/src/main/java/org/dspace/content/EtdUnit.java) - EtdUnit table Controller

* [EtdUnitEditServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/admin/EtdUnitEditServlet.java) - servlet controller for EtdUnit administrat


*JSP Pages*

add EtdUnit table editing to dspace-admin

* [web.xml](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/web.xml) - add /tools/etdunit-edit servlet handling

* [etdunit-confirm-delete.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/etdunit-confirm-delete.jsp) - confirm deletion of an EtdUnit

* [navbar-admin.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/layout/navbar-admin.jsp) - navbar link to /tools/etdunit-edit

* [etdunit-edit](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/etdunit-edit.jsp) - edit single EtdUnit page

* [etdunit-list](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/etdunit-list.jsp) - list of all EtdUnit
