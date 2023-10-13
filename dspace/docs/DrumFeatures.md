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
* LIBDRUM-680 - Loader for loading Proquest ETDs into DRUM
  * transform Proquest metadata to dublin core
  * transform Proquest metadata to marc and transfer to TSD
  * map into department etd collection
  * email notification

## Embargo

See [dspace/docs/DrumEmbargoAndAccessRestrictions.md](DrumEmbargoAndAccessRestrictions.md)
for additional information

* LIBDRUM-678 - ETD Embargo Policies
  * ETD embargo policies for date-based and "forever" embargoes
  * Embargoed files are marked as "Restriced Access" in the GUI
  * Restricted access page some different messages based on date-based
    and "forever" embargoes.

* LIBDRUM-679 - Added "Embargo List" page for administrators reporting all the
  embargoed bitstreams, and enabling CSV download for administrators.

## Submissions

* LIBDRUM-675 - For default submission workflow:
  * "Author" field is required
  * "Advisor" field added
  * See also LIBDRUM-728

* LIBDRUM-711 - "Equitable Access" field added to default submission form
  * Automatically maps Equitable Access-submitted materials to the
    "Equitable Access" group.

* LIBDRUM-727 - "Creative Commons" license field added to submission form

* LIBDRUM-729 - List of available types for "Type"  was simplified from DSpace
  defaults

* LIBDRUM-747 - "Modifying access conditions" section allowing users to create
  item embargoes was removed.

## Data Community

* LIBDRUM-682:
  * Modified submission form fields based on item type of "Dataset" or
    "Software"
  * "Dataset" or "Sofware" items automatically mapped to "UMD Data Community"

## Minority Health and Health Equity Archive (MHHEA)

* LIBDRUM-684 - Created custom workdfow and submission form for submissions to
  the MHHEA group
* See also LIBDRUM-728

## Community Groups

* LIBDRUM-664 - Enable communities to orgranized to groups (i.e.,
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
  * LIBDRUM-655 - SSDR-standard Envirnoment Banners added for non-production servers
  * LIBDRUM-659 - Custom "DRUM" theme
  * LIBDRUM-745 - Page Footer modifications
  * LIBDRUM-737 - Replaced "DSpace" text with "DRUM"
* Home page modifications:
  * LIBDRUM-702 - Display community lists grouped by community groups
  * LIBDRUM-704 - Display "Recent Submissions" list
* LIBDRUM-656 - Bitstream download count added on item pages, vieweable by any user (including
  users that are not logged in).
* LIBDRUM-658 - Following fields added to the "Simple" item view:
  * Citation: dc.identifier.citation
  * Advisor: dc.contributor.advisor
  * DRUM DOI: dc.identifier
  * LIBDRUM-740 - Fields/Labels displayed based on item type
* LIBDRUM-685 - "Author" browse index only displays values from
  "contributor.author" index
* LIBDRUM-698 - Standard DSpace "End User Agreement" is suppresed
* LIBDRUM-735 - Default License Agreement for submissions was updated for DRUM
* LIBDRUM-738 - "Statistics" navigation bar entry removed for non-administrators
  * "No. of Downloads" for bitstreams displays for all (even anoynomous) users
* LIBDRUM-739 - "Has Files" removed from search options
* LIBDRUM-746 - "Abstract" field modified to not preserve line breaks when
  displayed

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
