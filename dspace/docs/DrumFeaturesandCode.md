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

* [Community.java](../../dspace-api/src/main/java/org/dspace/content/Community.java) -  _getCollectionsUnmapped()_

* [Collection.java](../../dspace-api/src/main/java/org/dspace/content/Collection.java)

* [SelectCollectionTag.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/jsptag/SelectCollectionTag.java)

* [CollectionListServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionListServlet.java)

* [Community.java](../../dspace-api/src/main/java/org/dspace/content/Community.java) - updates in _findByCommunityGroupTop()_
	
* [CollectionMappingServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionMappingServlet.java) -  new class _CollectionMappingServlet_

* Workflow submission fixes:

    * [WorkflowItem.java](../../dspace-api/src/main/java/org/dspace/workflow/WorkflowItem.java
)
    * [WorkspaceItem](../../dspace-api/src/main/java/org/dspace/content/WorkspaceItem.java
)

* Updates to the selection of collection step:

    * [SelectCollectionStep.java](../../dspace-api/src/main/java/org/dspace/submit/step/SelectCollectionStep.java) 
    * [JSPSelectCollectionStep.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/submit/step/JSPSelectCollectionStep.java)
    * [InitialQuestionsStep.java](../../dspace-api/src/main/java/org/dspace/submit/step/InitialQuestionsStep.java)				   	   
* [DCInputSet.java](../../dspace-api/src/main/java/org/dspace/app/util/DCInputSet.java)			

				
*JSPUI webapp*					

*	[mapcollections.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/community-home.jsp)

*	[confirm-mapcollection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/confirm-mapcollection.jsp)

*	[confirm-unmapcollection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/confirm-unmapcollection.jsp)

*	[community-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/mapcollections.jsp)

*   [my dspace.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/mydspace/main.jsp)

*   [select-collection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/submit/select-collection.jsp)

*   [collection-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/collection-home.jsp)

*   [collection-select-list.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/collection-select-list.jsp)

*Configuration*
	
* [web.xml](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/web.xml)	
	
* [input-forms.xml](../../dspace/config/input-forms.xml)
		
* [Message Properties](../../dspace-api/src/main/resources/Messages.properties)

* [utils.js](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/utils.js)

* [dspace-tags.tld](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/dspace-tags.tld)


### Select/Display the bundle of a bitstream

Ability to view the bundle of a bitstream in the Administrator mode.

*JSPUI webapp*

* [upload-file-list.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/submit/upload-file-list.jsp) - allow bitstreams to be added and deleted during workflow processing


### Add preservation bundle

Add custom preservation bundle.

*Java Source*

 * Assign Letter of Support to a PRESERVATION bundle

   * [LibraryAwardUploadStep.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/submit/step/LibraryAwardUploadStep.java) 

   * [JSPLibraryAwardUploadStep.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/submit/step/JSPLibraryAwardUploadStep.java) 

* [Item.java](../../dspace-api/src/main/java/org/dspace/content/Item.java) - fix bug during file upload


*JSPUI webapp*

* [upload-bitstream.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/upload-bitstream.jsp) - add Preservation bundle as an option for uploading new bitstreams

<a name="authentication"></a>
## Authentication/Authorization

### Dual authentication: password/CAS

Add capability to login with the university directory id using CAS autentication or user id/passsword.

*Java Source*
  
 * [CASAuthentication.java](../../dspace-api/src/main/java/org/dspace/authenticate/CASAuthentication.java) - setup mechanics of dual authentication
 
*JSPUI webapp*

 * [chooser.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/login/chooser.jsp) - setup mechanics of dual authentication

*Configuration*

* [Messages.properties](../../dspace-api/src/main/resources/Messages.properties) - login messages for JSP

* [dspace.cfg](../../dspace/config/dspace.cfg) - CAS section for login, login preference section

### CAS Authentication

		login
    	gateway login
    	logout
    	automatic registration
    	admin override (force login as a specific user)
    	remove password updating on profile page

*Java Source*

* [PasswordServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/PasswordServlet.java) - change directory id login to use oit CAS
* [LogoutServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/LogoutServlet.java) - when logging out of DRUM, and user logged in using CAS, log out of CAS requires new cas.logoutUrl in the config file
* [CASAuthentication.java](../../dspace-api/src/main/java/org/dspace/authenticate/CASAuthentication.java) - add CAS authentication patch from Naveed Hashmi, University of Bristol
* [Ldap.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/authenticate/Ldap.java) - add self registration for CAS logins

*JSPUI webapp*
 
* [chooser.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/login/chooser.jsp)

* [incorrect.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/login/incorrect.jsp)

* [password.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/login/password.jsp)

* [edit-profile.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/register/edit-profile.jsp) - don't allow password changes for CAS logins

* [utils.js](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/utils.js)


*Configuration*

* CAS dependencies
	* [pom.xml](../../dspace-api/pom.xml) - dspace-api pom
	* [pom.xml](../../pom.xml) - project parent pom
	
* [dspace.cfg](../../dspace/config/dspace.cfg) 


### LDAP Authorization

		determine faculty status
    	department affiliation for submission
    	admin: Unit -> Group mapping
    	admin: display Ldap info on Eperson edit page

*Java Source*

* [CASAuthentication.java](../../dspace-api/src/main/java/org/dspace/authenticate/CASAuthentication.java) - get directory information for all CAS logins
* [Ldap.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/authenticate/Ldap.java)
* [Unit.java](../../dspace-api/src/main/java/org/dspace/eperson/Unit.java) - add faculty_only option on Unit object
* [UnitEditServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/admin/UnitEditServlet.java) - add faculty_only editing in admin interface for Units

*JSPUI webapp*
 
* [unit-edit.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/unit-edit.jsp) - add Ldap.getGroups() to gather Group mappings for all units

* [eperson-edit.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/eperson-edit.jsp) - add faculty_only editing in admin interface for Units

*Scripts*

* [loadunits_faculty_only.groovy](../../dspace/bin/loadunits_faculty_only.groovy) - cant'find

* [showunits.groovy](../../dspace/bin/showunits.groovy) - can't find

* [showldap.groovy](../../dspace/bin/showldap.groovy) 

 
*Configuration*

* [Messages.properties](../../dspace-api/src/main/resources/Messages.properties) - login messages for JSP


*Database Schema*

* [database_schema_unit.sql](../../dspace/etc/database_schema_unit.sql) - add Unit to Group mapping

* [database_schema_etdunit.sql](../../dspace/etc/database_schema_unit_add_faculty_only.sql) - add faculty_only option on Unit table




<a name="etd"></a>
## Electronic Theses and Dissertations (ETD)
 
### Bitstream start/end authorization

Add display of start and end date for ResourcePolicy on "Policies for Item" edit page.

*JSPUI webapp*

* [authorize-item-edit.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/authorize-item-edit.jsp)

### Custom embargoed bitstream messaging

Custom messaging for unathorized access to Bitstream if the Bitstream is under embargo; relies on ResourcePolicy with group named 'ETD Embargo'

*Java Source*

* [Bitstream.java](../../dspace-api/src/main/java/org/dspace/content/Bitstream.java) - new methods getETDEmbargo(), isETDEmbargo(), getMetadata(String), getIntMetadata(String), setMetadata(String, String), setMetadata(String, int)
* [ItemTag.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/jsptag/ItemTag.java) - listBistreams() add 'RESTRICTED ACCESS' to the Bitstream description
* [BitstreamServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/BitstreamServlet.java) - doDSGet() if not authorized and is ETD embargo then redirect to /error/authorize-etd-embargo.jsp

*JSPUI webapp*

* [authorize-etd-embargo](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/error/authorize-etd-embargo.jsp) - customized message based on embargo dates

### Embargoed Item Statistics

*JSPUI webapp*

* [statistics.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics.jsp) - link from custom statistics page to embargo-list.jsp

* [embargo-list.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics/embargo-list.jsp) - page display of embargo list

* [embargo-list-csv.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics/embargo-list-csv.jsp) - CSV export of embargo list

* [embargo-list-sql.jspf](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics/embargo-list-sql.jspf) - SQL for embargo query
 
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


*JSPUI webapp*

* add EtdUnit table editing to dspace-admin

    * [web.xml](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/web.xml) - add /tools/etdunit-edit servlet handling
    * [etdunit-confirm-delete.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/etdunit-confirm-delete.jsp) - confirm deletion of an EtdUnit
    * [navbar-admin.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/layout/navbar-admin.jsp) - navbar link to /tools/etdunit-edit
    * [etdunit-edit](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/etdunit-edit.jsp) - edit single EtdUnit page
    * [etdunit-list](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/etdunit-list.jsp) - list of all EtdUnit


<a name="item-collection-community"></a>
## Item/Community/Collection Display


		live links for urls
		add handle display for Community/Collection
		change contributor.sponsor to relation.isAvailableAt
		bitstream views statistic (No. of Downloads)

*Java Source*

* [UpdateStats.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/app/UpdateStats.java) - add item and bitstream statistics
* [ItemTag.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/jsptag/ItemTag.java) - add item and bitstream statistics
* [Bitstream.java](../../dspace-api/src/main/java/org/dspace/content/Bitstream.java) - add item and bitstream statistics
* [Item.java](../../dspace-api/src/main/java/org/dspace/content/Item.java) - add item and bitstream statistics

*JSPUI webapp*

* [about_submitting.html](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/help/about_submitting.html) - eliminate hardcoded references to /dspace in links

* [how_to.html](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/help/how_to.html) - eliminate hardcoded references to /dspace in links

* [index.html](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/help/index.html) - eliminate hardcoded references to /dspace in links

* [navbar-default.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/layout/navbar-default.jsp) - eliminate hardcoded references to /dspace in links

* [main.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/mydspace/main.jsp) - eliminate hardcoded references to /dspace in links


*Scripts*

* [update-stats](../../dspace/bin/update-stats) - add item and bitstream statistics

*Configuration*

* [Messages.properties](../../dspace-api/src/main/resources/Messages.properties) - add view stats to bitstream display


<a name="loaders-others"></a>
## Loaders (other than ETD)

legacy, single-use loaders; do not need testing

		CS Tech Reports
		ISR
		CISSM
		
* ISR Tech Reports

*Java Source*

* [Extract.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/app/isr/Extract.java)
* [Loader.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/app/isr/Loader.java)

* CISSM Tech Reports

*Java Source*

* [CissmLoader.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/app/CissmLoader.java)


<a name="navigation"></a>
## Navigation


		community groups (display, admin)
		remove /index.jsp from url
		navbar: mydspace links
		navbar: login status placement
		add context link to search help

*Java Source*

* [Community.java](../../dspace-api/src/main/java/org/dspace/content/Community.java) - fix Community.findByCommunityGroupTop()
* [FeedServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/FeedServlet.java) - for feeds, use 'name' field instead of 'short_description' for Community/Collection feed title
* [EditCommunitiesServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/servlet/admin/EditCommunitiesServlet.java) - set the CommunityGroups 'groups' attribute before calling processUploadLogo()

*JSPUI webapp*

* [collection-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/collection-home.jsp) - eliminate hardcoded references to /dspace in links

* [community-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/community-home.jsp) - eliminate hardcoded references to /dspace in links

* [display-item.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/display-item.jsp) - remove link to statistics on Community, Collection, Record pages

* [index.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/index.jsp) - remove /index.jsp from url

* [main.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/mydspace/main.jsp) - fix task list on mydspace page

* [navbar-default.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/layout/navbar-default.jsp) - navbar fixups

* [navbar-admin.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/layout/navbar-admin.jsp) - admin navbar fixups

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

* [EtdLoader.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/app/EtdLoader.java) - fix duplicate title detection in EtdLoader

* [SimpleDataSource.java](../../dspace-api/src/main/java/edu/umd/lib/activation/SimpleDataSource.java) - add ability to add attachments to email

* [Email.java](../../dspace-api/src/main/java/org/dspace/core/Email.java) - add ability to add attachments to email

*Configuration*

* [oaicat.properties](../../dspace/config/oaicat.properties) - update oai-pmh properties

* [web.xml](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/web.xml) - change oai-pmh url (remove /request)

handle-6.2.jar - add handle 6.2 to the lib folder


<a name="search-browse"></a>
## Search / Browse


		add Advisor to advanced search
		fix diacritic problem: add synonyms with a non-diacritic version
		fix browse of items in multiple collections
		change author browse to contributor.author instead of contributor.*
		
*Java Source*

* [StripDiacriticSynonymFilter.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/search/StripDiacriticSynonymFilter.java) - fix location of edu.umd.lims.dspace.search.StripDiacriticSynonymFilter source file
		
*Configuration*

* [dspace.cfg](../../dspace/config/dspace.cfg) - Addition to Advanced Search Dropdown [LIBDRUM-98](https://issues.umd.edu/browse/LIBDRUM-98); display only contributor.author in browse results

<a name="statistics"></a>
## Statistics

		add item,bitstream views count (with monthly update)
		add Item option to not update last modified date
		view standard stats from admin interface
		embargo statistics	
		
			
*Java Source*

* [Item.java](../../dspace-api/src/main/java/org/dspace/content/Item.java) - add item and bitstream statistics

* [Bitstream.java](../../dspace-api/src/main/java/org/dspace/content/Bitstream.java) - add item and bitstream statistics

* [ItemTag.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/jsptag/ItemTag.java) - add item and bitstream statistics

* [UpdateStats.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/app/UpdateStats.java) - add item and bitstream statistics

*JSPUI webapp*

* [monthly.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics/monthly.jsp) - move monthly stats dir outside of the webapp; ant update wipes out the webapp

* [statistics.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics.jsp) - add new admin statistics page

* [embargo-list-csv.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics/embargo-list-csv.jsp) - add CSV version of the embargo list

* [embargo-list-sql.jspf](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics/embargo-list-sql.jspf) - add CSV version of the embargo list

* [embargo-list.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/dspace-admin/statistics/embargo-list.jsp) - add a list of all current embargoes to the statistics page

* [navbar-admin.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/layout/navbar-admin.jsp)


*Database Schema*

* [statistics.sql](../../dspace/etc/statistics.sql) - add sql to setup the statistics in the database

*Scripts*

* [update-stats](../../dspace/bin/update-stats) - add item and bitstream statistics

* [stats_report_generator](../../dspace/bin/stats_report_generator) - update stats_report_generator with new stats dir (cf r320)

*Configuration*

* [Messages.properties](../../dspace-api/src/main/resources/Messages.properties) - add view stats to bitstream display


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

* [SelectCollectionStep.java](../../dspace-api/src/main/java/org/dspace/submit/step/SelectCollectionStep.java) - submission fixes for selecting multiple collections

* [SelectCollectionStep.java](../../dspace-jspui/dspace-jspui-api/src/main/java/org/dspace/app/webui/submit/step/JSPSelectCollectionStep.java) - submission fixes for selecting multiple collections; fix "Submit to This Collection"

* [SelectCollectionTag.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/jsptag/SelectCollectionTag.java) - add selectcollection jsp tag to selecting multiple collections from a popup window

* [CollectionListServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionListServlet.java) - submission fixes for selecting multiple collections

* [LibraryAwardCompleteStep.java](../../dspace-api/src/main/java/edu/umd/lib/dspace/submit/step/LibraryAwardCompleteStep.java) - LibraryAward: set dc.date.issued to the submission date

* [WorkflowItem.java](../../dspace-api/src/main/java/org/dspace/workflow/WorkflowItem.java
) - fix queries for mapped collections in submission process

* [WorkspaceItem.java](../../dspace-api/src/main/java/org/dspace/content/WorkspaceItem.java
) - fix queries for mapped collections in submission process

* [WorkflowManager.java](../../dspace-api/src/main/java/org/dspace/workflow/WorkflowManager.java
) - [Library Award: new email notices for submission approved/rejected](https://issues.umd.edu/browse/LIBDRUM-60); fix submit_task email in workflow

	
*JSPUI webapp*

* [monthly.jsp](../../dspace-jspui-webapp/src/main/webapp/mydspace/main.jsp) - submission fixes for selecting multiple collections

* [select-collection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/submit/select-collection.jsp) - submission fixes for selecting multiple collections	

* [collection-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/collection-home.jsp) - fix "Submit to This Collection"; broken by submit to multiple collections functionality	

* [styles.css.3col.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/styles.css.3col.jsp) - mark required elements during submission

* [edit-metadata.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/submit/edit-metadata.jsp) - mark required elements during submission

* [review-upload.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/submit/review-upload.jsp) - show bitstream description when reviewing submission

* [complete.jsp](../../space-jspui/dspace-jspui-webapp/src/main/webapp/submit/complete.jsp) - remove Submit to Collection link when submission is complete

*Configuration*

* [Messages.properties](../../dspace-api/src/main/resources/Messages.properties) - submission fixes for selecting multiple collections

* [web.xml](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/web.xml) - fix "Submit to This Collection"; broken by submit to multiple collections functionality

* [utils.js](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/utils.js) - fix "Submit to This Collection"; broken by submit to multiple collections functionality

* [dspace-tags.tld](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/dspace-tags.tld) - fix "Submit to This Collection"; broken by submit to multiple collections functionality

* [dspace.cfg](../../dspace/config/dspace.cfg) - display only contributor.author in browse results; add contributor.advisor

* [item-submission.xml](../../dspace/config/item-submission.xml) - LibraryAward: set dc.date.issued to the submission date