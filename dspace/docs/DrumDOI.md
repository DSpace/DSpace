# DRUM DOI

## Introduction

This page describes the DRUM DOI functionality, as of DSpace 7. See
<https://wiki.lyrasis.org/display/DSDOC7x/DOI+Digital+Object+Identifier>
for the DSpace documentation on DOI handling.

This document focuses on the customizations made for DRUM.

## DataCite API

DRUM uses the DataCite "MDS" API to mint DOIs. See
<https://support.datacite.org/docs/mds-api-guide>

## Random DOIs

By default, DSpace creates ("mints") DOIs as integers that increment
sequentially. This requires that there be only one source doing the DOI minting,
as having two minting sources would potentially generate overlapping numbers.

In DRUM, DOIs are minted as a random 8 character code in the format "XXXX-XXXX"
(4 alphanumeric characters, a hypen, then 4 more alphanumeric characters). This
eliminates the requirement to have a single minting source.

The generation of random DOIs is controlled by the "identifier.doi.mintRandom"
property. If "true", random DOIs are minted, if "false" the standard DSpace DOIs
are minted.

## DRUM DOI metadata field

In DRUM, the DOI is assigned to the unqualified "dc.identifier" field, unlike
in standard DSpace, where it is assigned to the "dc.identifier.doi" field.
