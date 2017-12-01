#DSpace REST API (Jersey)

A RESTful web services API for DSpace, built using JAX-RS1 JERSEY.

##Getting Started
This REST API is integrated directly into the DSpace code-base.

 * Rebuild as normal: mvn + ant
 * Deploy the webapp (i.e to tomcat)
  * ```<Context path="/rest"  docBase="/dspace/webapps/rest"  allowLinking="true"/>```


At this point, this is a READ ONLY API for DSpace, for the anonymous user. Only Anonymous READ Communities, Collections, Items, and Bitstreams are available.

##Endpoints

| Resource      |CREATE|READ list|READ single|Edit|Delete|Search|
| ------------- |------|:-------:|-----------|----|------|------|
| /communities  |      |   Y     |     Y     |    |      |      |
| /collections  |      |   Y     |     Y     |    |      |      |
| /items        |      |         |     Y     |    |      |      |
| /bitstreams   |      |         |     Y     |    |      |      ||


###Communities
View the list of top-level communities
- http://localhost:8080/rest/communities

View a specific community
- http://localhost:8080/rest/communities/:ID

View a specific community, list its subcommunities, and subcollections
- http://localhost:8080/rest/communities/:ID?expand=all

###Collections
View the list of collections
- http://localhost:8080/rest/collections

View a specific collection
- http://localhost:8080/rest/collections/:ID

View a specific collection, and its items
- http://localhost:8080/rest/collections/:ID?expand=all

###Items
View an Item, and see its bitstreams
- http://localhost:8080/rest/items/:ID

###Bitstreams
View information about a bitstream
- http://localhost:8080/rest/bitstreams/:ID

View/Download a specific Bitstream
- http://localhost:8080/rest/bitstreams/:ID/retrieve

####Statistics
Recording of statistics for view of items or download of bitstreams (set stats = true in rest.cfg to enable stats recording)
http://localhost:8080/rest/items/:ID?userIP=ip&userAgent=userAgent&xforwarderfor=xforwarderfor
If no parameters are given the details of httprequest sender are used in statistics. 
This enables tools to record the details of their user rather then themselves.


###Handles
Lookup a DSpaceObject by its Handle, this produces the name/ID, that you lookup in /bitstreams, /items, /collections, /communities
- http://localhost:8080/rest/handle/{prefix}/{suffix}

##Expand
There is an ?expand= query parameter for more expensive operations. You can tack it on the end of endpoints.
It is optional, all, some or none. The response will usually indicate what the available "expand" options are.

##HTTP Responses
* 200 OK            - We have the requested object/objects
* 401 Unauthorized  - The anonymous user does not have READ access to that object
* 404 Not Found     - That object doesn't exist
* 500 Server Error  - Likely a SQLException, IOException, more details in the logs.