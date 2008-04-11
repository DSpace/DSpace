DSpace SWORD README
===================

Author: Richard Jones
Last Updated: 07-02-2007

This document describes the DSpace implementation of the SWORD deposit
standard.  This is an extension to the ATOM Publishing Protocol (APP),
which provides a framework to discover deposit targets, and to deposit packaged
content into remote repositories.

For more information see:

http://www.ukoln.ac.uk/repositories/digirep/index/SWORD

Testing
-------

Supplied along with the source code is a package which can be used for testing.
This consists of a mets.xml file, which is a METS document containing a Dublin 
Core XML section of descriptive metadata which conforms to the SWAP standard.
There are additionally 3 example PDF files.

These files are provided additionally inside a zip file which should form the 
content of a deposit request (example.zip).

These files are all available in the directory: [dspace-sword]/example

Testing can be performed using the separately available SWORD Client

Implementation Notes
--------------------

- DSpace has no equivalent concept for the SWORD term "treatment".  This is 
  therefore always null in the Service Document
  
- The logic of onBehalfOf is as follows:  The list of collections which is
  supplied during a request which is done onBehalfOf another user is the 
  intersection of the lists of collections that the authenticated user can 
  submit to and the list that the onBehalfOf user can submit to.

- This implementation only supports the default PasswordAuthentication
  mechanism for DSpace.  Modifications are required to tie it in to alternative
  authentication mechanisms.
  
- When items are deposited and pass into the DSpace workflow system, they 
  cannot be assigned external identifiers immediately.  Therefore the returned
  id on "Accepted" items will be the front page of the repository on which the 
  deposit happened.  Alternatives to this mechanism are being sought, but may 
  require core DSpace modifications.
  
- If a request is made with an onBehalfOf user supplied, the authentication
  process requires that the username/password pair successfully  authenticate a
  user, and that the onBehalfOf user simply exists in the user database.  If 
  any of these conditions fail, authentication fails.
  
- The DSpace package ingester does not permit for a copy of the orignal package
  to be retained.  To modify this requires changes to the core DSpace which 
  could be considered.  In the mean time, it is not possible to return a link 
  to the original package in the <atom:link> element.

- The ingest stylesheet used by default is available in the file:
  [dspace-source]/config/crosswalks/sword-swap-ingest.xsl but does not cover
  the complete SWAP profile yet.