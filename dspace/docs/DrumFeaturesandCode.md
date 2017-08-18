# DRUM Features and Code

Summary of DRUM enhancements to base DSpace functionality as well as related changes in the code.

[Administration](#administration)

[Authentication/Authorization](#authentication)

[Electronic Theses and Dissertations (ETD)](#etd)

[Item/Community/Collection Display](#item-collection-community)

[Loaders (other than ETD)](#loaders-others)

[Navigation](#navigation)

[Miscellaneous](#miscellaneous)

[Search / Browse](#search-browse)

[Statistics](#statistics)

[Submission](#submission)

<a name="administration"></a>
## Administration

### <a name="collection-to-community-mapping"></a>Add collection to community mapping

Ability to select unmapped collections
	    
*Java Source*

* [Community.java](../modules/additions/src/main/java/org/dspace/content/Community.java) -  _getCollectionsUnmapped()_

* [Collection.java](../modules/additions/src/main/java/org/dspace/content/Collection.java)

* [SelectCollectionTag.java](../modules/jspui/src/main/java/edu/umd/lib/dspace/app/webui/jsptag/SelectCollectionTag.java)

* [CollectionListServlet.java](../modules/jspui/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionListServlet.java)

* [Community.java](../modules/additions/src/main/java/org/dspace/content/Community.java) - updates in _findByCommunityGroupTop()_
	
* [CollectionMappingServlet.java](../modules/jspui/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionMappingServlet.java) -  new class _CollectionMappingServlet_

* Workflow submission fixes:

    * [WorkflowItem.java](../modules/additions/src/main/java/org/dspace/workflow/WorkflowItem.java
)
    * [WorkspaceItem](../modules/additions/src/main/java/org/dspace/content/WorkspaceItem.java
)

* Updates to the selection of collection step:

    * [SelectCollectionStep.java](../modules/additions/src/main/java/org/dspace/submit/step/SelectCollectionStep.java) 
    * [JSPSelectCollectionStep.java](../modules/jspui/src/main/java/org/dspace/app/webui/submit/step/JSPSelectCollectionStep.java)
    * [InitialQuestionsStep.java](../modules/additions/src/main/java/org/dspace/submit/step/InitialQuestionsStep.java)				   	   
* [DCInputSet.java](../modules/additions/src/main/java/org/dspace/app/util/DCInputSet.java)			

				
*JSPUI webapp*					

*	[mapcollections.jsp](../modules/jspui/src/main/webapp/community-home.jsp)

*	[confirm-mapcollection.jsp](../modules/jspui/src/main/webapp/tools/confirm-mapcollection.jsp)

*	[confirm-unmapcollection.jsp](../modules/jspui/src/main/webapp/tools/confirm-unmapcollection.jsp)

*	[community-home.jsp](../modules/jspui/src/main/webapp/tools/mapcollections.jsp)

*   [my dspace.jsp](../modules/jspui/src/main/webapp/mydspace/main.jsp)

*   [select-collection.jsp](../modules/jspui/src/main/webapp/submit/select-collection.jsp)

*   [collection-home.jsp](../modules/jspui/src/main/webapp/collection-home.jsp)

*   [collection-select-list.jsp](../modules/jspui/src/main/webapp/tools/collection-select-list.jsp)

*Configuration*
	
* [web.xml](../modules/jspui/src/main/webapp/WEB-INF/web.xml)	
	
* [input-forms.xml](../config/input-forms.xml)
		
* [Message Properties](../modules/additions/src/main/resources/Messages.properties)

* [utils.js](../modules/jspui/src/main/webapp/utils.js)

* [dspace-tags.tld](../modules/jspui/src/main/webapp/WEB-INF/dspace-tags.tld)


### Select/Display the bundle of a bitstream

Ability to view the bundle of a bitstream in the Administrator mode.

*JSPUI webapp*

* [upload-file-list.jsp](../modules/jspui/src/main/webapp/submit/upload-file-list.jsp) - allow bitstreams to be added and deleted during workflow processing


### Add preservation bundle

Add custom preservation bundle.

*Java Source*

 * Assign Letter of Support to a PRESERVATION bundle

   * [LibraryAwardUploadStep.java](../modules/additions/src/main/java/edu/umd/lib/dspace/submit/step/LibraryAwardUploadStep.java) 

   * [JSPLibraryAwardUploadStep.java](../modules/jspui/src/main/java/edu/umd/lib/dspace/app/webui/submit/step/JSPLibraryAwardUploadStep.java) 

* [Item.java](../modules/additions/src/main/java/org/dspace/content/Item.java) - fix bug during file upload


*JSPUI webapp*

* [upload-bitstream.jsp](../modules/jspui/src/main/webapp/tools/upload-bitstream.jsp) - add Preservation bundle as an option for uploading new bitstreams

<a name="authentication"></a>
## Authentication/Authorization

### Dual authentication: password/CAS

XMLUI comes with dual authentication capability. No customization needed.


### CAS Authentication

		login
    	gateway login
    	logout
    	automatic registration
    	admin override (force login as a specific user)
    	remove password updating on profile page

*Java Source*

* [PasswordServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/PasswordServlet.java) - change directory id login to use oit CAS
* [LogoutServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/LogoutServlet.java) - when logging out of DRUM, and user logged in using CAS, log out of CAS requires new cas.logoutUrl in the config file
* [CASAuthentication.java](../modules/additions/src/main/java/org/dspace/authenticate/CASAuthentication.java) - add CAS authentication patch from Naveed Hashmi, University of Bristol
* [Ldap.java](../modules/additions/src/main/java/edu/umd/lib/dspace/authenticate/Ldap.java) - add self registration for CAS logins

*JSPUI webapp*
 
* [chooser.jsp](../modules/jspui/src/main/webapp/login/chooser.jsp)

* [incorrect.jsp](../modules/jspui/src/main/webapp/login/incorrect.jsp)

* [password.jsp](../modules/jspui/src/main/webapp/login/password.jsp)

* [edit-profile.jsp](../modules/jspui/src/main/webapp/register/edit-profile.jsp) - don't allow password changes for CAS logins

* [utils.js](../modules/jspui/src/main/webapp/utils.js)


*Configuration*

* CAS dependencies
	* [pom.xml](../modules/additions/pom.xml) - dspace-api pom
	* [pom.xml](../../pom.xml) - project parent pom
	
* [dspace.cfg](../config/dspace.cfg) 

*XMLUI Webapp*

* [CASAuthentication.java](../modules/additions/src/main/java/org/dspace/authenticate/CASAuthentication.java) - made minor edit to CAS URL for redirect request.
* [EditProfile.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/eperson/EditProfile.java) - don't allow password changes for CAS logins
* [EPerson/sitemap.xmap](../modules/xmlui/src/main/resources/aspects/EPerson/sitemap.xmap) - Added location for CAS login.
* [CASAuthenticateAction.java](..//modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/eperson/CASAuthenticateAction.java) - Attempts to log the user in using CAS.


### LDAP Authorization

		determine faculty status
    	department affiliation for submission
    	admin: Unit -> Group mapping
    	admin: display Ldap info on Eperson edit page

*Java Source*

* [CASAuthentication.java](../modules/additions/src/main/java/org/dspace/authenticate/CASAuthentication.java) - get directory information for all CAS logins
* [Ldap.java](../modules/additions/src/main/java/edu/umd/lib/dspace/authenticate/Ldap.java)
* [Unit.java](../modules/additions/src/main/java/org/dspace/eperson/Unit.java) - add faculty_only option on Unit object
* [UnitEditServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/admin/UnitEditServlet.java) - add faculty_only editing in admin interface for Units
* [EPersonAdminServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/admin/EPersonAdminServlet.java
) - add Ldap information to EPerson edit page

*JSPUI webapp*
 
* [unit-edit.jsp](../modules/jspui/src/main/webapp/tools/unit-edit.jsp) - add Ldap.getGroups() to gather Group mappings for all units

* [eperson-edit.jsp](../modules/jspui/src/main/webapp/dspace-admin/eperson-edit.jsp) - add faculty_only editing in admin interface for Units

*XMLUI webapp*

* [EditEPersonForm.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/eperson/EditEPersonForm.java) - Added UM Directory Information
* [EditUnitsForm.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/units/EditUnitsForm.java) - Added "Faculty Only" checkbox
* [FlowUnitsUtils.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/FlowUnitsUtils.java) - Code to update "faculty_only" field based on EditUnitsForm checkbox.
* [administrative.js](../modules/xmlui/src/main/resources/aspects/Administrative/administrative.js) - Updated one method of FlowUnitsUtils
* [messages.xml](../modules/xmlui/src/main/webapp/i18n/messages.xml)

*Scripts*

* [loadunits_faculty_only.groovy](../bin/loadunits_faculty_only.groovy) - cant'find

* [showunits.groovy](../bin/showunits.groovy) - can't find

* [showldap.groovy](../bin/showldap.groovy) 

 
*Configuration*

* [Messages.properties](../modules/additions/src/main/resources/Messages.properties) - login messages for JSP


*Database Schema*

* [database_schema_unit.sql](../etc/database_schema_unit.sql) - add Unit to Group mapping

* [database_schema_etdunit.sql](../etc/database_schema_unit_add_faculty_only.sql) - add faculty_only option on Unit table




<a name="etd"></a>
## Electronic Theses and Dissertations (ETD)
 
### Bitstream start/end authorization

Add display of start and end date for ResourcePolicy on "Policies for Item" edit page.

*JSPUI webapp*

* [authorize-item-edit.jsp](../modules/jspui/src/main/webapp/dspace-admin/authorize-item-edit.jsp)

### Custom embargoed bitstream messaging

Custom messaging for unathorized access to Bitstream if the Bitstream is under embargo; relies on ResourcePolicy with group named 'ETD Embargo'

*Java Source*

* [Bitstream.java](../modules/additions/src/main/java/org/dspace/content/Bitstream.java) - new methods getETDEmbargo(), isETDEmbargo(), getMetadata(String), getIntMetadata(String), setMetadata(String, String), setMetadata(String, int)
* [ItemTag.java](../modules/jspui/src/main/java/org/dspace/app/webui/jsptag/ItemTag.java) - listBistreams() add 'RESTRICTED ACCESS' to the Bitstream description
* [BitstreamServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/BitstreamServlet.java) - doDSGet() if not authorized and is ETD embargo then redirect to /error/authorize-etd-embargo.jsp

*JSPUI webapp*

* [authorize-etd-embargo](../modules/jspui/src/main/webapp/error/authorize-etd-embargo.jsp) - customized message based on embargo dates

### Embargoed Item Statistics

*JSPUI webapp*

* [statistics.jsp](../modules/jspui/src/main/webapp/dspace-admin/statistics.jsp) - link from custom statistics page to embargo-list.jsp

* [embargo-list.jsp](../modules/jspui/src/main/webapp/dspace-admin/statistics/embargo-list.jsp) - page display of embargo list

* [embargo-list-csv.jsp](../modules/jspui/src/main/webapp/dspace-admin/statistics/embargo-list-csv.jsp) - CSV export of embargo list

* [embargo-list-sql.jspf](../modules/jspui/src/main/webapp/dspace-admin/statistics/embargo-list-sql.jspf) - SQL for embargo query

*XMLUI webapp*

* [BitstreamReader.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/cocoon/BitstreamReader.java) - query DSpace for a particular bitstream and transmit it to the user

* [ItemAdapter.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/objectmanager/ItemAdapter.java) - check if bitstream has an embargo and display message accordingly.  

* [item-view.xsl](../modules/xmlui-mirage2/src/main/webapp/themes/Mirage2/xsl/aspect/artifactbrowser/item-view.xsl) - attribute EMBARGO is added.


 
### Loader
        transform Proquest metadata to dublin core
        transform Proquest metadata to marc and transfer to TSD
        map into department etd collections
        transfer of files to bindery
        email notification: duplicate titles, .csv with embargoes, load report, marc transfer, bindery transfer
 
*Scripts*

* [load-etd-nightly](../bin/load-etd-nightly) - nightly check for new etd zip files

* [load-etd](../bin/load-etd) - load one etd zip file

* [etd2marc-mail](../bin/etd2marc-mail) - email notice of Marc file to Technical Services

* [etd2marc-transfer](../bin/etd2marc-transfer) - transfer Marc file to Technical Services (LAN)

*Configuration*

* [config/load/etd2dc.xsl](../config/load/etd2dc.xsl) - crosswalk Proquest metadata to dublin core

* [config/load/etd2marc.xsl](../config/load/etd2marc.xsl) - crosswalk Proquest metadata to Marc

*Java Source*

* [EtdLoader.java](../modules/additions/src/main/java/edu/umd/lib/dspace/app/EtdLoader.java) - main loader control class

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

* [Messages.properties](../modules/additions/src/main/resources/Messages.properties) - etdunit messages for JSP

*Database Schema*

* [database_schema_etdunit.sql](../etc/database_schema_etdunit.sql) - add EtdUnit and Collection2EtdUnit tables

*Java Source*

* [EtdUnit.java](../modules/additions/src/main/java/org/dspace/content/EtdUnit.java) - EtdUnit table Controller

* [EtdUnitEditServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/admin/EtdUnitEditServlet.java) - servlet controller for EtdUnit administrat


*JSPUI webapp*

* add EtdUnit table editing to dspace-admin

    * [web.xml](../modules/jspui/src/main/webapp/WEB-INF/web.xml) - add /tools/etdunit-edit servlet handling
    * [etdunit-confirm-delete.jsp](../modules/jspui/src/main/webapp/dspace-admin/etdunit-confirm-delete.jsp) - confirm deletion of an EtdUnit
    * [navbar-admin.jsp](../modules/jspui/src/main/webapp/layout/navbar-admin.jsp) - navbar link to /tools/etdunit-edit
    * [etdunit-edit](../modules/jspui/src/main/webapp/tools/etdunit-edit.jsp) - edit single EtdUnit page
    * [etdunit-list](../modules/jspui/src/main/webapp/tools/etdunit-list.jsp) - list of all EtdUnit

*XMLUI Webapp*

* JAVA files
    * [DeleteDepartmentsConfirm.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/etd_departments/DeleteDepartmentsConfirm.java) - loads the page for deleting a ETD Department.
    * [EditDepartmentsForm.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/etd_departments/EditDepartmentsForm.java) - loads the page for editing a ETD Department.
    * [ManageDepartmentsMain.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/etd_departments/ManageDepartmentsMain.java)  - loads the page for managing a ETD Department and its mapping to collections.
    * [FlowDepartmentUtils.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/FlowDepartmentUtils.java) - contains utility functions for managing the ETD Department workflows.

* Administrative Aspects
    * [administrative.js](../modules/xmlui/src/main/resources/aspects/Administrative/administrative.js) - added new functions for creating new departments and deleting them, managing them, editing and managing them.
    * [sitemap.xmap](../modules/xmlui/src/main/resources/aspects/Administrative/sitemap.xmap) - added administrative configurations for managing the ETD Department workflows.

* Supporting files
    * [Navigation.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/Navigation.java) - added code for linking ETD Department functionality to the Navigation bar menu.
    * [Collection.java](../modules/additions/src/main/java/org/dspace/content/Collection.java) - added new "Search" and "Search Result Count" functionality in Collection.java
    * [EtdUnit.java](../modules/additions/src/main/java/org/dspace/content/EtdUnit.java) - added new functionality to check whether given collection is mapped to a department. 
    * [Constants.java](../modules/additions/src/main/java/org/dspace/core/Constants.java) - added support for ETDUNITs.

* Labels
    * [messages.xml](../modules/xmlui/src/main/webapp/i18n/messages.xml) - updated messages to include new labels for ETD Units.
    
<a name="item-collection-community"></a>
## Item/Community/Collection Display


		live links for urls
		add handle display for Community/Collection
		change contributor.sponsor to relation.isAvailableAt
		bitstream views statistic (No. of Downloads)

*Java Source*

* [UpdateStats.java](../modules/additions/src/main/java/edu/umd/lib/dspace/app/UpdateStats.java) - add item and bitstream statistics
* [ItemTag.java](../modules/jspui/src/main/java/org/dspace/app/webui/jsptag/ItemTag.java) - add item and bitstream statistics
* [Bitstream.java](../modules/additions/src/main/java/org/dspace/content/Bitstream.java) - add item and bitstream statistics
* [Item.java](../modules/additions/src/main/java/org/dspace/content/Item.java) - add item and bitstream statistics

*JSPUI webapp*

* [about_submitting.html](../modules/jspui/src/main/webapp/help/about_submitting.html) - eliminate hardcoded references to /dspace in links

* [how_to.html](../modules/jspui/src/main/webapp/help/how_to.html) - eliminate hardcoded references to /dspace in links

* [index.html](../modules/jspui/src/main/webapp/help/index.html) - eliminate hardcoded references to /dspace in links

* [navbar-default.jsp](../modules/jspui/src/main/webapp/layout/navbar-default.jsp) - eliminate hardcoded references to /dspace in links

* [main.jsp](../modules/jspui/src/main/webapp/mydspace/main.jsp) - eliminate hardcoded references to /dspace in links

*XMLUI webapp*

* [messages.xml](../modules/xmlui/src/main/webapp/i18n/messages.xml) - Actual text for content that is linked to from the code.

* [ItemAdapter.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/objectmanager/ItemAdapter.java) - Added the code to populate the mets file with the download count.

* [item-view.xsl](../modules/xmlui-mirage2/src/main/webapp/themes/Mirage2/xsl/aspect/artifactbrowser/item-view.xsl) - Added the text and formatting to display the "No. of  Downloads" for each item.


*Scripts*

* [update-stats](../bin/update-stats) - add item and bitstream statistics

*Configuration*

* [Messages.properties](../modules/additions/src/main/resources/Messages.properties) - add view stats to bitstream display


<a name="loaders-others"></a>
## Loaders (other than ETD)

legacy, single-use loaders; do not need testing

		CS Tech Reports
		ISR
		CISSM
		
* ISR Tech Reports

*Java Source*

* [Extract.java](../modules/additions/src/main/java/edu/umd/lib/dspace/app/isr/Extract.java)
* [Loader.java](../modules/additions/src/main/java/edu/umd/lib/dspace/app/isr/Loader.java)

* CISSM Tech Reports

*Java Source*

* [CissmLoader.java](../modules/additions/src/main/java/edu/umd/lib/dspace/app/CissmLoader.java)


<a name="navigation"></a>
## Navigation


		community groups (display, admin)
		remove /index.jsp from url
		navbar: mydspace links
		navbar: login status placement
		add context link to search help

*Java Source*

* [Community.java](../modules/additions/src/main/java/org/dspace/content/Community.java) - fix Community.findByCommunityGroupTop()
* [FeedServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/FeedServlet.java) - for feeds, use 'name' field instead of 'short_description' for Community/Collection feed title
* [EditCommunitiesServlet.java](../modules/jspui/src/main/java/org/dspace/app/webui/servlet/admin/EditCommunitiesServlet.java) - set the CommunityGroups 'groups' attribute before calling processUploadLogo()

*JSPUI webapp*

* [collection-home.jsp](../modules/jspui/src/main/webapp/collection-home.jsp) - eliminate hardcoded references to /dspace in links

* [community-home.jsp](../modules/jspui/src/main/webapp/community-home.jsp) - eliminate hardcoded references to /dspace in links

* [display-item.jsp](../modules/jspui/src/main/webapp/display-item.jsp) - remove link to statistics on Community, Collection, Record pages

* [index.jsp](../modules/jspui/src/main/webapp/index.jsp) - remove /index.jsp from url

* [main.jsp](../modules/jspui/src/main/webapp/mydspace/main.jsp) - fix task list on mydspace page

* [navbar-default.jsp](../modules/jspui/src/main/webapp/layout/navbar-default.jsp) - navbar fixups

* [navbar-admin.jsp](../modules/jspui/src/main/webapp/layout/navbar-admin.jsp) - admin navbar fixups

*XMLUI webapp*

* [Navigation.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/administrative/Navigation.java) - Added links in the sidebar for the DRUM Customizations.

* [messages.xml](../modules/xmlui/src/main/webapp/i18n/messages.xml) - Actual text for content that is linked to from the code in Navigation.java.


<a name="miscellaneous"></a>
## Miscellaneous


		duplicate title detection
		static browse page for crawlers
		fix bitstream servlet when user is not authorized
		upgrade to Handle server 6.2
		fix audio upload problem?
		fix bitstream format registry problem?
		fix oai-pmh to xml escape set names
		add ability for attachments on email notices
		
*Java Source*

* [EtdLoader.java](../modules/additions/src/main/java/edu/umd/lib/dspace/app/EtdLoader.java) - fix duplicate title detection in EtdLoader

* [SimpleDataSource.java](../modules/additions/src/main/java/edu/umd/lib/activation/SimpleDataSource.java) - add ability to add attachments to email

* [Email.java](../modules/additions/src/main/java/org/dspace/core/Email.java) - add ability to add attachments to email

*Configuration*

* [oaicat.properties](../config/oaicat.properties) - update oai-pmh properties

* [web.xml](../modules/jspui/src/main/webapp/WEB-INF/web.xml) - change oai-pmh url (remove /request)

handle-6.2.jar - add handle 6.2 to the lib folder


<a name="search-browse"></a>
## Search / Browse


		add Advisor to advanced search
		fix diacritic problem: add synonyms with a non-diacritic version
		fix browse of items in multiple collections
		change author browse to contributor.author instead of contributor.*
		
*Java Source*

* [StripDiacriticSynonymFilter.java](../modules/additions/src/main/java/edu/umd/lib/dspace/search/StripDiacriticSynonymFilter.java) - fix location of edu.umd.lims.dspace.search.StripDiacriticSynonymFilter source file
		
*Configuration*

* [dspace.cfg](../config/dspace.cfg) - Addition to Advanced Search Dropdown [LIBDRUM-98](https://issues.umd.edu/browse/LIBDRUM-98); display only contributor.author in browse results

<a name="statistics"></a>
## Statistics

		show bitstream download count on item page
		
			
*Java Source*

* [ItemAdapter.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/objectmanager/ItemAdapter.java) - get the view count for bitstream

* [Item.java](../modules/additions/src/main/java/org/dspace/content/Item.java) - add item and bitstream statistics

* [Bitstream.java](../modules/additions/src/main/java/org/dspace/content/Bitstream.java) - add item and bitstream statistics

* [ItemTag.java](../modules/jspui/src/main/java/org/dspace/app/webui/jsptag/ItemTag.java) - add item and bitstream statistics

* [UpdateStats.java](../modules/additions/src/main/java/edu/umd/lib/dspace/app/UpdateStats.java) - add item and bitstream statistics

*JSPUI webapp*

* [monthly.jsp](../modules/jspui/src/main/webapp/dspace-admin/statistics/monthly.jsp) - move monthly stats dir outside of the webapp; ant update wipes out the webapp

* [statistics.jsp](../modules/jspui/src/main/webapp/dspace-admin/statistics.jsp) - add new admin statistics page

* [embargo-list-csv.jsp](../modules/jspui/src/main/webapp/dspace-admin/statistics/embargo-list-csv.jsp) - add CSV version of the embargo list

* [embargo-list-sql.jspf](../modules/jspui/src/main/webapp/dspace-admin/statistics/embargo-list-sql.jspf) - add CSV version of the embargo list

* [embargo-list.jsp](../modules/jspui/src/main/webapp/dspace-admin/statistics/embargo-list.jsp) - add a list of all current embargoes to the statistics page

* [navbar-admin.jsp](../modules/jspui/src/main/webapp/layout/navbar-admin.jsp)

*XMLUI webapp*

* [messages.xml](../modules/xmlui/src/main/webapp/i18n/messages.xml) - Actual text for content that is linked to from the code.

* [EmbargoListHelper.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/artifactbrowser/EmbargoListHelper.java) - Helper class that gets the list of embargo files.

* [EmbargoListDisplay.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/artifactbrowser/EmbargoListDisplay.java) - Class used to display the table with the list of embargos.

* [EmbargoListDownloader.java](../modules/xmlui/src/main/java/org/dspace/app/xmlui/aspect/artifactbrowser/EmbargoListDownloader.java) - Class responsible for populating the CSV of the embargo data.


*Database Schema*

* [statistics.sql](../etc/statistics.sql) - add sql to setup the statistics in the database

*Scripts*

* [update-stats](../bin/update-stats) - add item and bitstream statistics

* [stats_report_generator](../bin/stats_report_generator) - update stats_report_generator with new stats dir (cf r320)

*Configuration*

* [Messages.properties](../modules/additions/src/main/resources/Messages.properties) - add view stats to bitstream display


<a name="submission"></a>
## Submission

		select multiple collections
		require contributor.author
		add contributor.advisor
		require date.issued for all submission types
		show required submission metadata fields (submit/edit-metadata.jsp)
		allow bitstream editing in workflow mode in addition to workspace 		(submission) mode (submit/upload-file-list.jsp)
		make citation required if previously published
		add "Submit to This Collection"
		
		
*Java Source*

* [SelectCollectionStep.java](../modules/additions/src/main/java/org/dspace/submit/step/SelectCollectionStep.java) - submission fixes for selecting multiple collections

* [SelectCollectionStep.java](../modules/jspui/src/main/java/org/dspace/app/webui/submit/step/JSPSelectCollectionStep.java) - submission fixes for selecting multiple collections; fix "Submit to This Collection"

* [SelectCollectionTag.java](../modules/jspui/src/main/java/edu/umd/lib/dspace/app/webui/jsptag/SelectCollectionTag.java) - add selectcollection jsp tag to selecting multiple collections from a popup window

* [CollectionListServlet.java](../modules/jspui/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionListServlet.java) - submission fixes for selecting multiple collections

* [LibraryAwardCompleteStep.java](../modules/additions/src/main/java/edu/umd/lib/dspace/submit/step/LibraryAwardCompleteStep.java) - LibraryAward: set dc.date.issued to the submission date

* [WorkflowItem.java](../modules/additions/src/main/java/org/dspace/workflow/WorkflowItem.java
) - fix queries for mapped collections in submission process

* [WorkspaceItem.java](../modules/additions/src/main/java/org/dspace/content/WorkspaceItem.java
) - fix queries for mapped collections in submission process

* [WorkflowManager.java](../modules/additions/src/main/java/org/dspace/workflow/WorkflowManager.java
) - [Library Award: new email notices for submission approved/rejected](https://issues.umd.edu/browse/LIBDRUM-60); fix submit_task email in workflow

	
*JSPUI webapp*

* [monthly.jsp](../modules/jspui/src/main/webapp/mydspace/main.jsp) - submission fixes for selecting multiple collections

* [select-collection.jsp](../modules/jspui/src/main/webapp/submit/select-collection.jsp) - submission fixes for selecting multiple collections	

* [collection-home.jsp](../modules/jspui/src/main/webapp/collection-home.jsp) - fix "Submit to This Collection"; broken by submit to multiple collections functionality	

* [styles.css.3col.jsp](../modules/jspui/src/main/webapp/styles.css.3col.jsp) - mark required elements during submission

* [edit-metadata.jsp](../modules/jspui/src/main/webapp/submit/edit-metadata.jsp) - mark required elements during submission

* [review-upload.jsp](../modules/jspui/src/main/webapp/submit/review-upload.jsp) - show bitstream description when reviewing submission

* [complete.jsp](../modules/jspui/src/main/webapp/submit/complete.jsp) - remove Submit to Collection link when submission is complete

*Configuration*

* [Messages.properties](../modules/additions/src/main/resources/Messages.properties) - submission fixes for selecting multiple collections

* [web.xml](../modules/jspui/src/main/webapp/WEB-INF/web.xml) - fix "Submit to This Collection"; broken by submit to multiple collections functionality

* [utils.js](../modules/jspui/src/main/webapp/utils.js) - fix "Submit to This Collection"; broken by submit to multiple collections functionality

* [dspace-tags.tld](../modules/jspui/src/main/webapp/WEB-INF/dspace-tags.tld) - fix "Submit to This Collection"; broken by submit to multiple collections functionality

* [dspace.cfg](../config/dspace.cfg) - display only contributor.author in browse results; add contributor.advisor

* [item-submission.xml](../config/item-submission.xml) - LibraryAward: set dc.date.issued to the submission date
