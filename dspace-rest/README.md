#DSpace REST API (Jersey)

A RESTful web services API for DSpace, built using JAX-RS1 JERSEY.

##Getting Started
This REST API is integrated directly into the DSpace code-base.

 * Rebuild as normal: mvn + ant
 * Deploy the webapp (i.e to tomcat)
  * ```<Context path="/rest"  docBase="/dspace/webapps/rest"  allowLinking="true"/>```


At this point, REST API can do all CRUD operations over communities, collections, items, bitstream and bitstream policies. Without login into REST api, you can read all as anynomous. If you want to make changes in DSpace by REST api, you must login into api and use generated token from api.

##Endpoints

| Resource      |CREATE|READ list|READ single|Edit|Delete|Search|
| ------------- |------|:-------:|-----------|----|------|------|
| /communities  |  Y   |   Y     |     Y     |  Y |  Y   |      |
| /collections  |  Y   |   Y     |     Y     |  Y |  Y   |  Y   |
| /items        |  Y   |   Y     |     Y     |  Y |  Y   |  Y   |
| /bitstreams   |  Y   |   Y     |     Y     |  Y |  Y   |      ||

Search in collections is only by name and search in items is only by metadata field.

###Index
Get some usefull information how to use API
- GET {rest-endpoint}

Test if REST api is up
- GET {rest-endpoint}/test

Login into REST api
- POST {rest-endpoint}/login

Logout from REST api
- POST {rest-endpoint}/logout

Get status of REST api and logged user
- GET {rest-endpoint}/status


###Communities
View the list of top-level communities
- GET {rest-endpoint}/communities/top-communities

View the list of all communities
- GET {rest-endpoint}/communities[?expand={collections,parentCommunity,subCommunities,logo,all}]

View a specific community
- GET {rest-endpoint}/communities/:ID[?expand={collections,parentCommunity,subCommunities,logo,all}]

View list of subcollections in community
- GET {rest-endpoint}/communities/:ID/collections[?expand={items,parentCommunityList,license,logo,all}]

View lsit of subcommunities in community
- GET {rest-endpoint}/communities/:ID/communities[?expand={collections,parentCommunity,subCommunities,logo,all}]

Create new top-level community
- POST {rest-endpoint}/communities

Create new subcollection in community
- POST {rest-endpoint}/communities/:ID/collections

Create new subcommunity in community
- POST {rest-endpoint}/communities/:ID/communities

Update community
- PUT {rest-endpoint}/communities/:ID

Delete community
- DELETE {rest-endpoint}/communities/:ID

Delete subcollection in community
- DELETE {rest-endpoint}/communities/:ID/collections/:ID

Delete subcommunity in community
- DELETE {rest-endpoint}/communities/:ID/communities/:ID


###Collections
View the list of collections
- GET {rest-endpoint}/collections[?expand={items,parentCommunityList,license,logo,all}]

View a specific collection
- GET {rest-endpoint}/collections/:ID[?expand={items,parentCommunityList,license,logo,all}]

View items in collection
- GET {rest-endpoint}/collections/:ID/items[?expand={metadata,parentCollection,parentcollectionList,parentCommunityList,bitsreams,all}]

Create item in collection
- POST {rest-endpoint}/collections/:ID/items

Find collection defined by name
- POST {rest-endpoint}/collections/find-collection

Update collection
- PUT {rest-endpoint}/collections/:ID

Delete collection
- DELETE {rest-endpoint}/collections/:ID

Delete item in collection
- DELETE {rest-endpoint}/collections/:ID/items/:ID


###Items
View the list of items
- GET {rest-endpoint}/items[?expand={metadata,parentCollection,parentcollectionList,parentCommunityList,bitsreams,all}]

View speciific item
- GET {rest-endpoint}/items/:ID[?expand={metadata,parentCollection,parentcollectionList,parentCommunityList,bitsreams,all}]

View an Item, and see its bitstreams
- GET {rest-endpoint}/items/:ID/bitstreams[?expand={parent,policies,all}]

View an Item, and see its metadata
- GET {rest-endpoint}/items/:ID/metadata

Find item by metadata
- POST {rest-endpoint}/items/find-by-metadata-field

Add metadata to item
- POST {rest-endpoint}/items/:ID/metadata

Create bitstream in item
- POST {rest-endpoint}/items/:ID/bitstreams

Update metadata in item
- PUT {rest-endpoint}/items/:ID/metadata

Delete item
- DELETE {rest-endpoint}/items/:ID

Delete all metadata in item
- DELETE {rest-endpoint}/items/:ID/metadata

Delete bitstream in item
- DELETE {rest-endpoint}/items/:ID/bitstreams/:ID


###Bitstreams
View the list of bitstreams
- GET {rest-endpoint}/bitstreams[?expand={parent,policies,all}]

View information about a bitstream
- GET {rest-endpoint}/bitstreams/:ID[?expand={parent,policies,all}]

View/Download a specific Bitstream
- GET {rest-endpoint}/bitstreams/:ID/retrieve

View the list of policies of bitstream
- GET {rest-endpoint}/bitstreams/:ID/policy

Add policy to bitstream
- POST {rest-endpoint}/bitstreams/:ID/policy

Update bitstream
- PUT {rest-endpoint}/bitstreams/:ID

Update data of bitstream
- PUT {rest-endpoint}/bitstreams/:ID/data

Delete bitstream
- DELETE {rest-endpoint}/bitstreams/:ID

Delte policy of bitstream
- DELETE {rest-endpoint}/bitstreams/:ID/policy/:ID


####Statistics
Recording of statistics for view of items or download of bitstreams (set stats = true in rest.cfg to enable stats recording)
{rest-endpoint}/items/:ID?userIP=ip&userAgent=userAgent&xforwardedfor=xforwardedfor
If no parameters are given the details of httprequest sender are used in statistics. 
This enables tools to record the details of their user rather then themselves.


###Handles
Lookup a DSpaceObject by its Handle, this produces the name/ID, that you lookup in /bitstreams, /items, /collections, /communities
- {rest-endpoint}/handle/{prefix}/{suffix}

##Expand
There is an ?expand= query parameter for more expensive operations. You can tack it on the end of endpoints.
It is optional, all, some or none. The response will usually indicate what the available "expand" options are.

##HTTP Responses
* 200 OK            - We have the requested object/objects
* 401 Unauthorized  - The anonymous user does not have READ access to that object
* 404 Not Found     - That object doesn't exist
* 405 Method Not Allowed - Bad method of request. (GET,POST,PUT,DELETE) Or data are in another format. (json or xml)
* 500 Server Error  - Likely a SQLException, IOException, more details in the logs.
