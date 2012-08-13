----- Export items in various bibliographic formats (as RIS, BibTeX, EndNote, IEEE,...) -----

-- Description --

The new feature that has been developed for DSpace 1.8.2 provides a mechanism for exporting the DSpace items metadata in many 
different formats of representing bibliographic references, such as RIS, BibTeX, EndNote, IEEE, APA, Harvard, etc. This functionality
can be added in the single page of an item, in the search results page, in the collection page and in community page, so a user will have 
the capability of exporting the metadata at these different levels.

This feature was developed and supported by the National Document Centre (http://www.ekt.gr).  

-- Third party software installation --

At the core of the mechanism lies a third party software, citeproc-node, which has been developed by Zotero 
(http://www.zotero.org/support/dev/citation_styles/citeproc-node) and is distributed under the 'GNU AFFERO GENERAL PUBLIC 
LICENSE Version 3' license. Citeproc-node is a web-service that takes as input the bibliographic data structured as a javascript array 
and with names the ones that are defined by the CSL (Citation Style Language) prototype and also a bibliographic format (ris, ieee, apa, etc) 
and the response format (text or html) and exports the data in the desired bibliographic format.

Thus, apart from the patch that should be applied in DSpace source code, it is necessary to install and host the citeproc-node
that DSpace will communicate with it.
Following there are listed the steps that one needs to accomplish in order to install citeproc-node (see also
http://www.zotero.org/support/dev/citation_styles/citeproc-node for full installation details):

1. Citeproc-node runs server side in nodeJS Server. So you first need to install NodeJS version v.0.2.4 (be carefull to install 
this version and not the last one since there problems with newer versions)

2. Get citeproc-node, CSL citation styles and CSL locale files from the following git repos accordingly:
git clone https://github.com/fcheslack/citeproc-node.git
git clone https://github.com/citation-style-language/styles.git csl
git clone https://github.com/citation-style-language/locales.git csl-locales
!!Important note: sometimes you have to delete the comments that are included in the CSL citation styles files otherwise you 
will face undefined problems in the transformation phase.

3. Copy of citeproc-node to the /home directory of the server

4. Copy of CSL citation styles, CSL locale files and node-o3-xml to the root directory of citeproc-node.

5. Modifications in citeServerConf.json file in order to define the appropriate variables, such as the path to CSL citation styles dir,
the path to CSL locale files dir, the path to node-o3-xml lib. 

6. Start citeproc-node application with the command 'node citeServer.js' in its root directory.

!!Important note:Together with the patch that contains the modifications that should be made in DSpace for this new feauture, we have also included the 
source code of citeproc-node, which contains some changes that needed to be done in order to work correctly and also the CSL citation styles 
for RIS and EndNote that haven't been implemented.

-- DSpace Configuration --

After the insertion of the patch into the DSpace source code, the first important step is to install citeproc-node the way that is described above.
Next, one should make the following configuration to the relative files:

1. dspace.cfg: 
	(a) Complete the parameter citeproc.url with the base url of the citeproc-node webservice in order to include this funcionality in DSpace.
	(b) For each of the parameters export.references.item, export.references.rearchresult, export.references.collection, export.references.community
	it should be entered the value true or false in order to define the pages the functionality of exporting to bibliographic references would be included
	(c) An entry in the format of export.references.format.<n> = <CSL citation style format>:<display value> should be entered, where <CSL citation style format> 
	should take value from the set of the available CSL citation styles and <display value> should be the value that will appear in the drod down list
	
2. repo2csl-metadata.cfg: in this file one should define the mapping between the fields of the metadata schema registry of the dspace and the ones
that the CLS schema recognizes. See here https://bitbucket.org/bdarcus/csl-schema/src/855dcc00cba7/csl-variables.rnc for the CSL variables.

3. repo2csl-types.cfg: in this file one should define the mapping between the types that appear in the DSpace installation and the ones
that the CLS schema recognizes. See here https://bitbucket.org/bdarcus/csl-schema/src/855dcc00cba7/csl-types.rnc for the CSL types.

-- DSpace Version --
The proposed new feature applies to DSpace version 1.8.2

-- Command Line execution --
The capability of exporting the DSpace items metadata in many different formats of bibliographic references is also provided via command line. 
More specifically, the 'csljsonexport' action has been added in the launcher.xml file, thus the user can execute the command 
./dspace csljsonexport 
and define the desired paremeters in order to produce the appropriate bibliographic reference.

The possible paremeters that can be defined by the user are:
"c": the handle of the collection to export
"i": the handle of item to export
"a": export all items in the archive
"d": the destination directory
"h": help
"f": the format (ieee, apa, science,...)
"o": the citation output format (text or html)

After executing the above command, a folder containing 2 files is created:
-- a json file containing the metadata formatted with the appropriate cls specifications
-- a file containing the produced bibliographic references.
