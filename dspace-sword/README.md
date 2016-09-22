# DSpace SWORD (v1) README

* Author: Richard Jones
* Last Updated: 18-02-2008
* SWORD Version: 1.3
* DSpace-SWORD Version: 1.3.1

This document describes the DSpace implementation of the SWORD (v1) deposit standard.  This is an extension to the ATOM
Publishing Protocol (APP), which provides a framework to discover deposit targets, and to deposit packaged content into
remote repositories.

For more information see:

http://www.swordapp.org/

## Changes

### Version 1.3.1

Second major version to be compliant with the SWORD 1.3 standard.

- Architectural changes to reflect need to support deposit targets other than collections
- Implementation of hierarchical service documents
- Add support for keeping and retrieving original deposit package
- Add support for depositing files into items
- Move previously hard-coded features out to configuration
- Re-write/re-architecture of authentication and authorisation system to encompass LDAP, and to support authorisations
  on items
- Meet request/response requirements of 1.3 specification

### Version 1.2.1

Initial version to be compliant with the SWORD 1.2 standard.

- Support for depositing METS DSpace SIP files into DSpace collections
- Expose all DSpace Collections as ATOM Collections in SWORD Service Documents

## Configuration

The SWORD (v1) interface is configured within the `[dspace]/config/modules/sword-server.cfg` file.

## Testing

Supplied along with the source code is a package which can be used for testing. This consists of a mets.xml file, which
is a METS document containing a Dublin Core XML section of descriptive metadata which conforms to the SWAP standard.
There are additionally 3 example PDF files.

These files are provided additionally inside a zip file which should form the  content of a deposit request
(example.zip).

These files are all available in the directory: `[dspace-sword]/example`

Testing can be performed using the separately available SWORD Client, or by invoking the sword deposit web service via a
command line tool such as `curl`:

### Service Documents

Authorised by dspace/dspace:

`curl -i http://dspace:dspace@localhost:8080/sword/servicedocument`

Authorised by dspace/dspace on behalf of admin:

`curl -i -H "X-On-Behalf-Of: admin" http://dspace:dspace@localhost:8080/sword/servicedocument`

### Deposits

Authorised by dspace/dspace on a Collection:

`curl -i --data-binary "@dspace-sword/example/example.zip" -H "Content-Disposition: filename=myDSpaceMETSItem.zip"
     -H "Content-Type: application/zip" -H "X-Packaging: http://purl.org/net/sword-types/METSDSpaceSIP"
     -H "X-No-Op: false" -H "X-Verbose: true" http://dspace:dspace@localhost:8080/sword/deposit/123456789/2`

Authorised by dspace/dspace on behalf of admin on a Collection:

`curl -i --data-binary "@dspace-sword/example/example.zip" -H "Content-Disposition: filename=myDSpaceMETSItem.zip"
     -H "X-On-Behalf-Of: admin" -H "Content-Type: application/zip"
     -H "X-Packaging: http://purl.org/net/sword-types/METSDSpaceSIP" -H "X-No-Op: false" -H "X-Verbose: true"
     http://dspace:dspace@localhost:8080/sword/deposit/123456789/2`

Authorised by dspace/dspace on an Item:

`curl -i --data-binary "@dspace-sword/example/pdf1.pdf" -H "Content-Disposition: filename=somepdf.pdf"
     -H "Content-Type: application/pdf" -H "X-No-Op: true" -H "X-Verbose: true"
     http://dspace:dspace@localhost:8080/sword/deposit/123456789/21`


## Implementation Notes

- The logic of onBehalfOf is as follows:  The list of collections which is supplied during a request which is done
  onBehalfOf another user is the intersection of the lists of collections that the authenticated user can submit to and
  the list that the onBehalfOf user can submit to.

- When items are deposited and pass into the DSpace workflow system, they cannot be assigned external identifiers
  immediately.  Therefore the returned id on "Accepted" items will be the front page of the repository on which the
  deposit happened.  Alternatives to this mechanism are being sought, but may require core DSpace modifications.

- If a request is made with an onBehalfOf user supplied, the authentication process requires that the username/password
  pair successfully  authenticate a user, and that the onBehalfOf user simply exists in the user database.  If any of
  these conditions fail, authentication fails.

- The ingest stylesheet used by default is available in the file:

  [dspace-source]/config/crosswalks/sword-swap-ingest.xsl

  but does not cover the complete SWAP profile yet.

- The scope of the dspace-sword module does not include the discoverability of deposit targets through the normal
  user interface.

- If Items are enabled as SWORD deposit targets, then all the bitstream formats in the bitstream format registry will
  be used to populate the atom:accepts field in the service document.

- If Communities are enabled for exposure in service documents, they will not specify an atom:accepts field, in
  contravention to the standard, as they do not take deposits.  Instead they are just gateways to the Collections
  and Items which they contain.
