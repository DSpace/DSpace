# DRUM Features and Code

Summary of DRUM enhancements to base DSpace functionality as well as related changes in the code.

**[Administration](#administration)**

 * **[Add collection to community mapping](#collection-to-community-mapping)**
 * **[Select/Display the bundle of a bitstreamg](#display-bundle-by-bitstream)**
 * **[Add preservation bundle](#display-bundle-bitstream)**


##<a name="administration"></a>Administration

* ### <a name="collection-to-community-mapping"></a>Add collection to community mapping
### Customizations Summary
	*	Ability to select unmapped collections
	    
	    * Initial Commit: _0ae555614e462f0d45d726a09d132c7f6619d828_
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
								
 	* JSP Pages					
			*	New jsp page [mapcollections.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/community-home.jsp)
			*	New jsp page [confirm-mapcollection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/confirm-mapcollection.jsp)			*	New jsp page [confirm-unmapcollection.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/confirm-unmapcollection.jsp)
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
	 * JavaScript
	 		* [utils.js](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/utils.js)
	 * Tag Libraries
	 		* [dspace-tags.tld](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/WEB-INF/dspace-tags.tld)
	 * **All Commits**
			* _0ae555614e462f0d45d726a09d132c7f6619d828_ - (first step in adding collections mapping)
			* _99998ce5f7075ae01c63027444b968f9efa4a35e_ - (finish first cut at collection mapping functionality)
			* _e6c3ce5f5462e4c167bbd70ff58ce3ae6012da82_ - (fix Collection unmapping confirmation)
			* _650296ea9baef456e5949f8ad0dcfbffbbed1b8f_ - (fix Community.findByCommunityGroupTop())
			* _519690cfd40a1b3eaf0d17079f93bfb356a380a6_ - (fix queries for mapped collections in submission process)
			* _766c43c384fdfb73859f17ff2fb26bd9ad26c4ac_ - (submission fixes for selecting multiple collections)
			* _9b62bc5cefc9f8f532e9128da7f5dab86c047aca_ - (more fixes for submission process)
			* _1e8ada8d016b04eee944fd73af946517c7e365a9_ - (fix collection to community mapping)
			* _4018f23ba379244cc34c3d9c4c005056bd8ec7cd_ - (increase size of collection list in submission process)
			* _1a63a7ebe5f7714066b89c7b8671db2345f9a82f_ - (fix "Submit to This Collection"; broken by submit to multiple collections functionality
)
			* fc6804209708cb7c9e128420bd5a2f30dfe1e224 - (add selectcollection jsp tag to selecting multiple collections from a popup window)