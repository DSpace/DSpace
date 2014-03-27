# DRUM Features and Code

Summary of DRUM enhancements to base DSpace functionality as well as related changes in the code.

**[Administration](#administration)**

 * **[Add collection to community mapping](#collection-to-community-mapping)**
 * **[Select/Display the bundle of a bitstreamg](#display-bundle-by-bitstream)**
 * **[Add preservation bundle](#display-bundle-bitstream)**


##<a name="administration"></a>Administration

*	###<a name="collection-to-community-mapping"></a>Add collection to community mapping
### Customizations Summary
	*	Ability to select unmapped collections
	    
	    * Initial Commit: **0ae555614e462f0d45d726a09d132c7f6619d828**
		* **Java Source**
			*	New method _getCollectionsUnmapped()_  in the existing source [Community.java](../../dspace-api/src/main/java/org/dspace/content/Community.java)
			*	New class  _CollectionMappingServlet_ in jspui application [CollectionMappingServlet.java](../../dspace-jspui/dspace-jspui-api/src/main/java/edu/umd/lib/dspace/app/webui/servlet/admin/CollectionMappingServlet.java)
			
		* **JSP Pages**
			*	New jsp page [mapcollections.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/community-home.jsp)
			*	Updates to [community-home.jsp](../../dspace-jspui/dspace-jspui-webapp/src/main/webapp/tools/mapcollections.jsp)
		
