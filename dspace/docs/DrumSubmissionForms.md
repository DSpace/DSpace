# DRUM Submission Forms

## Introduction

This document describes the DRUM customizations to the DSpace item submission
process.

## Jira Issues

* LIBDRUM-675
* LIBDRUM-682
* LIBDRUM-684
* LIBDRUM-711
* LIBDRUM-727
* LIBDRUM-728
* LIBDRUM-729
* LIBDRUM-747
* LIBDRUM-876

## Submission Forms

### Default and MHHEA submissions

The DRUM item submission process provides two separate submission forms, based
on the collection the item is being submitted to.

The "MHHEA" submission form is only used for item submissions to the
"Minority Health and Health Equity Archive" (MHHEA) collection.

The "default" submission form is used for all other collections.

### Default Submission Form

#### Required Fields

DRUM customized the submission form to require the following fields:

* Author
* Type

These fields are in addition to those required by stock DSpace:

* Date of Issue
* File Upload

#### Additions

The following fields were added to the submission form:

* Advisor
* External Link

The DSpace-provided "Creative Commons license" step was added to the submission
form.

#### Modifications

The following fields were modified from their DSpace defaults:

* Subject - modified to be a repeatable text box, instead of the stock "tag"
  field that uses a controlled vocabulary.
* Type - The entries in the drop-down use a UMD-customized list
* The embargo-related fields in the "Edit bitstream" modal dialog were removed
  (additional information about embargoes is in
  [dspace/docs/DrumEmbargoAndAccessRestrictions.md](DrumEmbargoAndAccessRestrictions.md)):

  * Access condition type
  * Grant access from
  * Grant access until

### MHHEA submission form

The MHHEA submission form differs from the default form in the following ways:

* the "Equitable Access" field is never displayed, and submissions are never
  added to the "Equitable Access Policy" collection
* File uploads are not required

## Data Community

Items with a "Type" field of "Dataset" or "Software" are automatically
added to the "UMD Data Collection" collection.

This behavior applies to both the default and MHHEA submission forms.

The "Data Community" functionality includes the following customizations to
DSpace:

### Data Community Submission Form Changes

Submission form fields removed when "Dataset" or "Software" is selected:

* dc.relation.ispartofseries - Series/Report No.
* dc.identifier - Identifiers

Submission form field label changes when "Dataset" or "Software" is selected:

* dc.identifier.citation - "Publication Citation" changed to
   "Related Publication Citation"
* dc.descripton.uri - "Publication or External Link" changed to
  "Related Publication Link"
* dc.description - "Description" changed to "Methodology Description"

### Data Community Mapping

The "dspace/modules/additions/src/main/java/edu/umd/lib/dspace/xmlworkflow/state/actions/DataCommunityCollectionMappingAction.java"
class supports mapping an item to the "UMD Data Collection" community. The
class will map the item into every collection held by the
"UMD Data Collection" community (this is in line with how mappings were done in
DSpace 6).

The "UMD Data Collection" community is specified by the
"data.community.handle" property in "dspace/config/local.cfg".

### Data Community Workflow

The "/dspace/config/spring/api/workflow-actions.xml" was modified to add
the "datacommunitycollectionmappingaction" WorkflowActionConfig bean, and
a "dataCommunityCollectionMappingAPI" bean configured to use the
"DataCommunityCollectionMappingAction" class.

A "umdcollectionmapping" step was added to the end of the "defaultWorkflow"
steps, and modified the "finaleditstep" to go to the "umdcollectionmapping" when
it completes. Modifying the "finaleditstep" appears to be necessary –
despite how it may seem from the documentation - as the workflow steps to not
naturally progress through all the steps in the list.

## Equitable Access

The "Equitable Access" functionality includes the following customizations to
DSpace:

### Equitable Access Submission Form Changes

Both the default and MHHEA submission forms have a "Submission Type" step that
contains the "Type" field.

In the default submission form, selecting "Article" in the "Type" field
dynamically displays the "Equitable Access" field.

In the MHHEA form, the "Equitable Access" field is never displayed, as MHHEA
submissions are never added to the "Equitable Access Policy" collection.

### "local.equitableAccessSubmission" metadata field

A "local.equitableAccessSubmission" metadata field was added to track the user's
response to the "Equitable Access" question on the submission form. This
metadata field (with a "Yes" or "No" response) shows up in the "Full item page"
view for the item.

### Equitable Access Community Mapping

The "dspace/modules/additions/src/main/java/edu/umd/lib/dspace/xmlworkflow/state/actions/EquitableAccessCollectionMappingAction.java"
class supports mapping an item to the "Equitable Access Policy" community. The
class will map the item into every collection held by the
"Equitable Access Policy" community (this is in line with how Data Community
mappings were done in DSpace 6).

The "Equitable Access Policy" community is specified by the
"equitable_access_policy.community.handle" property in
"dspace/config/local.cfg".

### Equitable Access Workflow

The "/dspace/config/spring/api/workflow-actions.xml" was modified to add
the "equitableaccesscollectionmappingaction" WorkflowActionConfig bean, and
a "equitableAccessCollectionMappingAPI" bean configured to use the
"EquitableAccessCollectionMappingAction" class.

A "umdcollectionmapping" step was added to the end of the "defaultWorkflow"
steps, and modified the "finaleditstep" to go to the "umdcollectionmapping" when
it completes. Modifying the "finaleditstep" appears to be necessary –
despite how it may seem from the documentation - as the workflow steps to not
naturally progress through all the steps in the list.
