# DRUM DOI

## Introduction

This page describes the DRUM DOI functionality. See
<https://wiki.lyrasis.org/display/DSDOC7x/DOI+Digital+Object+Identifier>
for the DSpace documentation on DOI handling.

This document focuses on the customizations made for DRUM.

## DataCite API

DRUM uses the DataCite "MDS" API to mint DOIs. See
<https://support.datacite.org/docs/mds-api-guide>

## DataCite Schema

DRUM uses a custom file, "dspace/config/crosswalks/DIM2UmdDataCite.xsl", to
convert DSpace metadata into the DataCite schema. This file is based on the
stock "dspace/config/crosswalks/DIM2DataCite.xsl" file, and configured for use
in the `crosswalk.dissemination.DataCite.stylesheet` property of the "local.cfg"
file.

## Random DOIs

By default, DSpace creates ("mints") DOIs as integers that increment
sequentially. This requires that there be only one source doing the DOI minting,
as having two minting sources would potentially generate overlapping numbers.

In DRUM, DOIs are minted as a random 8 character code in the format "XXXX-XXXX"
(4 alphanumeric characters, a hyphen, then 4 more alphanumeric characters). This
eliminates the requirement to have a single minting source.

The generation of random DOIs is controlled by the "identifier.doi.mintRandom"
property. If "true", random DOIs are minted, if "false" the standard DSpace DOIs
are minted.

## DRUM DOI metadata field

In DRUM, the DOI is assigned to the unqualified "dc.identifier" field, unlike
in standard DSpace, where it is assigned to the "dc.identifier.doi" field.

## DOI Filter

We have disabled "doi-filter" filter config in the DOIIdentifierProvider in the
[identifier-service.xml](../config/spring/api/identifier-service.xml) as it is
not compatible with the our DOI generation workflow.

## Item Deletion and DOIs

DRUM has been customized to generate a DOI when an item is created, including
placing an entry in the Postgres "doi" table.

When an item is deleted, the DOI no longer has an associated item, but the entry
still exists in the Postgres "doi" table, with a null "dspace_object" field.
This is problematic if the DOI has not been registered, as the stock DSpace
registration process expects the associated item to be non-null.

This method for correcting the issue was chosen (instead of, for example,
modifying the DSpace code to delete the DOI when the item was deleted), as it
seemed the most straightforward, and only involved classes that UMD was already
modifying for DRUM.

To mitigate this issue, in the "DOIOrganiser" class, the "register-all" command
has been modified to purge any DOIs in the "doi" table with a status of
"TO_BE_REGISTERED", that do not a null associated item.

See [LIBDRUM-915](https://umd-dit.atlassian.net/browse/LIBDRUM-915) and
[LIBDRUM-914](https://umd-dit.atlassian.net/browse/LIBDRUM-914).

