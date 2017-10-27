
Dryad Storage
==============

This directory combines the old (DSpace 1.7) bitstream storage manager with the current version (DSpace 7.0). 

Design of the assetstores:
- assetstores defined as numbers will be the local filesystem, as always
- there will be a single amazon S3 assetstore 
- all reads will check both assetstores
- writes will go only to amazon, so we can transition
  - but can't use a bitstream ID that is already used in #0
- SRB assetstores are no longer supported

BitstreamStorageManager.java 
- the original version, which is being converted to work with both the
  local filesystem and DSpace 7 S3BitStoreService. 
- stores assestores in an array



