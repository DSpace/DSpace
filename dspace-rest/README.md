#DSpace REST API (Jersey)

A RESTful web services API for DSpace, built using JAX-RS1 JERSEY.

##Getting Started
This REST API is integrated directly into the DSpace codebase.

 * Rebuild as usual: mvn + ant
 * Deploy the webapp (i.e to Tomcat)
  * ```<Context path="/rest" docBase="/dspace/webapps/rest" />```


REST API can do all CRUD (create, read, update, delete) operations over communities, collections, items, bitstream and bitstream policies. Without logging into the REST API, you have read access as an anonymous user (member of the Anonymous group). If you want to make changes in DSpace using the REST API, you must log into the API using the "login" endpoint and then use the returned token in request header of your subsequent API calls.

##Endpoints

| Resource      |CREATE|READ list|READ single|Edit|Delete|Search|
| ------------- |------|:-------:|-----------|----|------|------|
| /communities  |  Y   |   Y     |     Y     |  Y |  Y   |      |
| /collections  |  Y   |   Y     |     Y     |  Y |  Y   |  Y   |
| /items        |  Y   |   Y     |     Y     |  Y |  Y   |  Y   |
| /bitstreams   |  Y   |   Y     |     Y     |  Y |  Y   |      ||

Search in collections is possible only by name and search in items only by metadata field.

###Index
Get information on how to use the API
- GET http://localhost:8080

Test whether the REST API is running and available
- GET http://localhost:8080/rest/test

Log into REST API
- POST http://localhost:8080/rest/login

Logout from REST API
- POST http://localhost:8080/rest/logout

Get status of REST API and the logged-in user
- GET http://localhost:8080/rest/status


###Communities
View the list of top-level communities
- GET http://localhost:8080/rest/communities/top-communities

View the list of all communities
- GET http://localhost:8080/rest/communities[?expand={collections,parentCommunity,subCommunities,logo,all}]

View a specific community
- GET http://localhost:8080/rest/communities/:ID[?expand={collections,parentCommunity,subCommunities,logo,all}]

View the list of subcollections in community
- GET http://localhost:8080/rest/communities/:ID/collections[?expand={items,parentCommunityList,license,logo,all}]

View the list of subcommunities in community
- GET http://localhost:8080/rest/communities/:ID/communities[?expand={collections,parentCommunity,subCommunities,logo,all}]

Create new top-level community
- POST http://localhost:8080/rest/communities

Create new subcollection in community
- POST http://localhost:8080/rest/communities/:ID/collections

Create new subcommunity in community
- POST http://localhost:8080/rest/communities/:ID/communities

Update community
- PUT http://localhost:8080/rest/communities/:ID

Delete community
- DELETE http://localhost:8080/rest/communities/:ID

Delete subcollection in community
- DELETE http://localhost:8080/rest/communities/:ID/collections/:ID

Delete subcommunity in community
- DELETE http://localhost:8080/rest/communities/:ID/communities/:ID


###Collections
View the list of collections
- GET http://localhost:8080/rest/collections[?expand={items,parentCommunityList,license,logo,all}]

View a specific collection
- GET http://localhost:8080/rest/collections/:ID[?expand={items,parentCommunityList,license,logo,all}]

View items in collection
- GET http://localhost:8080/rest/collections/:ID/items[?expand={metadata,parentCollection,parentcollectionList,parentCommunityList,bitstreams,all}]

Create item in collection
- POST http://localhost:8080/rest/collections/:ID/items

Find collection by name
- POST http://localhost:8080/rest/collections/find-collection

Update collection
- PUT http://localhost:8080/rest/collections/:ID

Delete collection
- DELETE http://localhost:8080/rest/collections/:ID

Delete item in collection
- DELETE http://localhost:8080/rest/collections/:ID/items/:ID


###Items
View the list of items
- GET http://localhost:8080/rest/items[?expand={metadata,parentCollection,parentcollectionList,parentCommunityList,bitstreams,all}]

View speciific item
- GET http://localhost:8080/rest/items/:ID[?expand={metadata,parentCollection,parentcollectionList,parentCommunityList,bitstreams,all}]

View an Item and view its bitstreams
- GET http://localhost:8080/rest/items/:ID/bitstreams[?expand={parent,policies,all}]

View an Item, and view its metadata
- GET http://localhost:8080/rest/items/:ID/metadata

Find item by metadata
- POST http://localhost:8080/rest/items/find-by-metadata-field

Add metadata to item
- POST http://localhost:8080/rest/items/:ID/metadata

Create bitstream in item
- POST http://localhost:8080/rest/items/:ID/bitstreams

Update metadata in item
- PUT http://localhost:8080/rest/items/:ID/metadata

Delete item
- DELETE http://localhost:8080/rest/items/:ID

Delete all metadata in item
- DELETE http://localhost:8080/rest/items/:ID/metadata

Delete bitstream in item
- DELETE http://localhost:8080/rest/items/:ID/bitstreams/:ID


###Bitstreams
View the list of bitstreams
- GET http://localhost:8080/rest/bitstreams[?expand={parent,policies,all}]

View information about a bitstream
- GET http://localhost:8080/rest/bitstreams/:ID[?expand={parent,policies,all}]

View/Download a specific Bitstream
- GET http://localhost:8080/rest/bitstreams/:ID/retrieve

View the list of policies of bitstream
- GET http://localhost:8080/rest/bitstreams/:ID/policy

Add policy to bitstream
- POST http://localhost:8080/rest/bitstreams/:ID/policy

Update bitstream
- PUT http://localhost:8080/rest/bitstreams/:ID

Update data of bitstream
- PUT http://localhost:8080/rest/bitstreams/:ID/data

Delete bitstream
- DELETE http://localhost:8080/rest/bitstreams/:ID

Delete policy of bitstream
- DELETE http://localhost:8080/rest/bitstreams/:ID/policy/:ID


####Statistics
Recording view events of items and download events of bitstreams (set stats = true in rest.cfg to enable recording of events)
http://localhost:8080/rest/items/:ID?userIP=ip&userAgent=userAgent&xforwardedfor=xforwardedfor
If no parameters are given, the details of the HTTP request sender are used in statistics. 
This enables tools like proxies to supply the details of their user rather than themselves.


###Handles
Lookup a DSpaceObject by its Handle, this produces the name/ID that you look up in /bitstreams, /items, /collections, /communities
- http://localhost:8080/rest/handle/{prefix}/{suffix}

##Expand
There is an ?expand= query parameter for more expensive operations. You can add it at the end of the request URL.
It is optional, all, some or none. The response will usually indicate what the available "expand" options are.

##HTTP Responses
* 200 OK            - The requested object/objects exists
* 401 Unauthorized  - The anonymous user does not have READ access to that object
* 404 Not Found     - The specified object doesn't exist
* 405 Method Not Allowed - Wrong request method (GET,POST,PUT,DELETE) or wrong data format (JSON/XML). 
* 415 Unsupported Media Type - Missing "Content-Type: application/json" or "Content-Type: application/xml" request header
* 500 Server Error  - Likely a SQLException, IOException, more details in the logs.
