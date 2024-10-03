# DRUM Features

This summary of DRUM enhancements to the base DSpace functionality is primarily
derived from the Jira issues that were part of the "Upgrade DRUM to DSpace 7
Jira epic ([LIBDRUM-645](https://umd-dit.atlassian.net/browse/LIBDRUM-645)).

Only features that required customization are indicated below. Stock DSpace
behavior used by features is not recorded.

Customizations for both the DRUM back-end and DRUM Angular front-end are
recorded in this list.

## Authentication/Authorization

See [dspace/docs/CASAuthentication.md](CASAuthentication.md) for additional
information.

* LIBDRUM-657 - "UMD Login" via CAS provided as a log-in option, in addition to
  the standard DSPace username/password login
  * First-time users are automatically registered using user's information
      from LDAP.
  * "CAS Authenticated" special group is added to session of CAS users
  * Faculty status of user is determined using LDAP
  * Determines the department affiliation for submission from LDAP using the Unit
    to Group mapping

* LDAP
  * LIBDRUM-660 - User profile page modified to display to administrators
    UM Directory information from LDAP (Name, Email, Phone, Faculty, UM Appt,
    Groups)
  * LIBDRUM-669 - Group membership based on LDAP attributes is displayed in the
    user profile page (see also LIBDRUM-703)

## Electronic Theses and Dissertations (ETD)

* LIBDRUM-671 - "ETD Department" CRUD functionality
* LIBDRUM-680 - Loader for loading ProQuest ETDs into DRUM
  * transform ProQuest metadata to dublin core
  * transform ProQuest metadata to marc and transfer to TSD
  * map into department etd collection
  * email notification

## Embargo

See [dspace/docs/DrumEmbargoAndAccessRestrictions.md](DrumEmbargoAndAccessRestrictions.md)
for additional information

* LIBDRUM-678 - ETD Embargo Policies
  * ETD embargo policies for date-based and "forever" embargoes
  * Embargoed files are marked as "Restricted Access" in the GUI
  * Restricted access page some different messages based on date-based
    and "forever" embargoes.

* LIBDRUM-679 - Added "Embargo List" page for administrators reporting all the
  embargoed bitstreams, and enabling CSV download for administrators.

See also the changes to the submission form in detailed in
[dspace/docs/DrumSubmissionForms.md](DrumSubmissionForms.md).

## Email Templates

The email templates in "dspace/config/emails" have been modified to use the
"dspace.shortname" property, in place of the "dspace.name" property, as the
DRUM "dspace.name" value is too long to comfortably fit in email "Subject"
lines and signatures.

## Submissions

See [dspace/docs/DrumSubmissionForms.md](DrumSubmissionForms.md).

## Data Community

See [dspace/docs/DrumSubmissionForms.md](DrumSubmissionForms.md).

## Minority Health and Health Equity Archive (MHHEA)

See [dspace/docs/DrumSubmissionForms.md](DrumSubmissionForms.md).

## Community Groups

* LIBDRUM-664 - Enable communities to organized to groups (i.e.,
  "UM Community", "Faculty (UM Department)", "UM Libraries").
* See also LIBDRUM-701

## JSON-LD

* LIBDRUM-715 - JSON-LD "Website" descriptor added
* LIBDRUM-683 - JSON-LD "Dataset" descriptor added for "Dataset" items (used by
  Google Dataset Search)

## WuFoo Feedback Form

See [dspace/docs/DrumWufooFeedback.md](DrumWufooFeedback.md) for additional
information.

* LIBDRUM-748 - Wufoo feedback form was added.

## User Interface Changes

* General UI Changes
  * LIBDRUM-654 - Standard UMD header banner added
  * LIBDRUM-655 - SSDR-standard Environment Banners added for non-production servers
  * LIBDRUM-659 - Custom "DRUM" theme
  * LIBDRUM-745 - Page Footer modifications
  * LIBDRUM-737 - Replaced "DSpace" text with "DRUM"
* Home page modifications:
  * LIBDRUM-702 - Display community lists grouped by community groups
  * LIBDRUM-704 - Display "Recent Submissions" list
* LIBDRUM-656 - Bitstream download count added on item pages, viewable by any user (including
  users that are not logged in).
* LIBDRUM-658 - Following fields added to the "Simple" item view:
  * Citation: dc.identifier.citation
  * Advisor: dc.contributor.advisor
  * DRUM DOI: dc.identifier
  * LIBDRUM-740 - Fields/Labels displayed based on item type
* LIBDRUM-685 - "Author" browse index only displays values from
  "contributor.author" index
* LIBDRUM-698 - Standard DSpace "End User Agreement" is suppressed
* LIBDRUM-735 - Default License Agreement for submissions was updated for DRUM
* LIBDRUM-738 - "Statistics" navigation bar entry removed for non-administrators
  * "No. of Downloads" for bitstreams displays for all (even anonymous) users
* LIBDRUM-739 - "Has Files" removed from search options
* LIBDRUM-746 - "Abstract" field modified to not preserve line breaks when
  displayed
* LIBDRUM-844 - Disable "By Subject Category" Browse option

## DRUM DOI

See [dspace/docs/DrumDOI.md](DrumDOI.md) for additional information.

* LIBDRUM-753 - "Random" DOIs are minted for items

## Cron Jobs

See [dspace/docs/DrumCronTasks.md](DrumCronTasks.md) for additional information.

* LIBDRUM-720 - DSpace Cron jobs

## dspace/bin/script-mail-wrapper

The "script-mail-wrapper" runs a specified script, and then emails the output
of that script to a specified email address. It is mainly intended for use
with the "load-etd-nightly" script, but is usable by any script.

## Miscellaneous

* LIBDRUM-662 - Custom "Unit" to manage the UMD Campus Units, and manage access
  to Faculty collections. (see also LIBDRUM-676)
* LIBDRUM-666 - "Preservation" bundle type option provided for bitstreams
  bundles
* LIBDRUM-811 - Maximum file upload size is set to 2GB for a single file
  and 15GB for a record with multiple files.
