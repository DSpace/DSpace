# DRUM Test Plan

## Introduction

This document provides a basic DRUM test plan that verifies as many UMD
customizations and other concerns (such as functionality when using read-only
containers) as feasible.

The intention is to provide basic assurance that the UMD customizations are in
place, and guard against regressions.

**The document is not intended to be an exhaustive test plan.**

As this test plan adds, modifies, and deletes data, it should ***not*** be used
to test the production system.

## Test Plan Assumptions

This test plan assumes that the user has "administrator" privileges, and can
log in via CAS.

The test plan steps are specified using URLs for the Kubernetes "test"
namespace, as that seems to be the most useful. Unless otherwise specified,
test steps should work in the local development environment as well.

## Test Plan

### 1) DRUM API Server

1.1) In a web browser, go to

<https://api.drum-test.lib.umd.edu/server>

The HAL Browser "Explorer" page will be displayed.

1.2) On the HAL Browser "Explorer" page, verify that:

* in the "Properties" pane the appropriate "dspaceVersion" is displayed.
* the following DRUM endpoints are available in the "Links" section:

  * embargo-list
  * etdunits
  * units
  * wufoo-feedback

### 2) DRUM Home Page

2.1) In a web browser, go to

<https://drum-test.lib.umd.edu/>

The DRUM home page will be displayed.

2.2) On the DRUM home page, verify that:

* The UMD favicon is displayed in the browser tab, and that the text in
  the browser tab is "DRUM :: Home"
* The appropriate SSDR environment banner is displayed.
* The lower half of the page contains two columns:
  * "List of Communities"
  * "Recent Submissions"
* In the "List of Communities" column there are two sections:
  * "Collections Organized by Department"
  * "UM Community-managed Collections"

2.3) In the "UM Community-managed Collections" section:

2.3.1) Left-click the "Show More" button. Additional entries for the list should
be displayed.

2.3.2) Find the "MARAC Mid-Atlantic Regional Archives Conference" entry in the
list, and left-click the arrow to the left of the text to expand the sublist.
Verify that the list contains a "Show More" button. Left-click the "Show More"
button and verify that additional entries are displayed for the sublist.

2.4) At the bottom of the page, verify that the footer has the following links:

* "Send Feedback"
* "Privacy policy"
* "Web Accessibility"
* "Give Now"
* "Cookie settings"

2.4.1) Left-click the "Send Feedback" link in the footer. Verify that the
browser is redirected to a Wufoo form.

2.4.2) Go back to the DRUM home page, and left-click the "Privacy policy" link.
Verify that a "Privacy Statement" page is displayed, with a link to privacy
policy on the UMD Libraries website.

2.4.3) Go back to the DRUM home page, and left-click the "Web Accessibility"
link. Verify that the web accessibility page on the university website is
displayed.

2.4.4) Go back to the DRUM home page, and left-click the "Give Now" link.
Verify that the "Giving to Maryland" page on the university website is
displayed.

2.4.5) Go back to the DRUM home page, and left-click the "Cookie settings"
link. Verify that an "Information that we collect" modal dialog is displayed.
Left-click the "privacy policy" link in the modal dialog, and verify that the
"Privacy Statement" page is displayed, with a link to privacy policy on the
UMD Libraries website.

### 3) CAS Login

3.1) On the DRUM home page, left-click the "Log In" dropdown in the navigation
bar, and then left-click the "UMD Login" button. Verify that the browser is
redirected to CAS.

3.2) Log in via CAS. Once returned to the DRUM home page, verify that an
administrative sidebar is displayed along the left edge of the page.

3.3) In the navigation bar, left-click the "User profile menu and logout"
dropdown in the navigation bar (the icon is a person in  silhouette) and verify
that the dropdown displays the name associated with the CAS account. Left-click
the "Profile" menu entry in the dropdown. The "Profile" page will be displayed.

3.4) On the "Profile" page, verify that a "Researcher Profile" panel is *not*
displayed. At the bottom of the page verify that there are two sections:

* "Authorization groups you belong to"
* "Authorization special groups you belong to"

Verify that the "Authorization groups you belong to" includes "Administrator"
and that "Authorization special groups you belong to" includes
"CAS Authenticated" (there may be other entries in these groups).

### 4) Community Creation

4.1) From the administrative sidebar, select "New | Community".
The "New community" modal dialog will be displayed.

4.2) In the "New community" modal dialog, left-click the
"Create a new top-level community" button. The "Create a Community" page will
be displayed.

4.3) On the "Create a Community" page, fill out the following fields:

| Field | Value |
| ----- | ----- |
| Name  | SSDR Test Community |
| Group | <Select "UM Community" from the dropdown> |

then left-click the "Save" button. A notification will display indicating that
the community was successfully added.  The "SSDR Test Community" page will be
displayed.

### 5) Collection Creation

5.1) From the administrative sidebar, select "New | Collection".
The "New collection" modal dialog will be displayed.

5.2) In the "New collection" modal dialog, left-click the
"SSDR Test Community" entry. The "Create a Collection" page will
be displayed.

5.3) On the "Create a Collection" page, fill out the following fields:

| Field | Value |
| ----- | ----- |
| Name  | SSDR Test Collection |

then left-click the "Save" button. A notification will display indicating that
the collection was successfully added. The "SSDR Test Collection" page will be
displayed.

### 6) Item Submission

6.1) From the administrative sidebar, select "New | Item".
The "New item" modal dialog will be displayed.

6.2) In the "New item" modal dialog, left-click the
"SSDR Test Collection" entry. The "Edit Submission" page will be displayed.

6.3) Upload a PDF file to the page by dragging and dropping it onto the page.

6.4) Change the "Type" dropdown in the "Submission Type" panel to "Article".
Verify that an additional "Equitable Access" dropdown with the label

> Is this article being submitted to comply with the "Equitable Access to
> Scholarly Articles Authored by University Faculty"
> (<https://equitableaccess.umd.edu/>) policy?"

is added to the "Submission Type" panel.

6.5) Change the "Type" dropdown to "Book". Verify that the "Equitable Access"
dropdown field is removed from the "Submission Type" panel.

6.6) Fill out the following fields:

| Field | Value |
| ----- | ----- |
| Type  | Article  |
| \<Equitable Access> | <Select "Yes" from the dropdown> |
| Author (Last Name, First Name) | Author, Test |
| Title | SSDR Test Item |
| Date of Issue | <Enter "2025" in the "Year" field> |

Verify that the "Creative commons license" section has a dropdown with two
entries (but do not select either of them):

* CC0
* Creative Commons

In the "Deposit license" section, left-click the "I confirm the license above"
checkbox, and then left-click the "Deposit" button. A notification will be
displayed indicating that the item was successfully deposited. The
"My submissions" page will be displayed.

6.7) On the "My submissions" page, verify that the item has been added as one
of the submissions. Left-click the "View" button for the submitted item. The
summary page for the item will be shown.

6.8) On the item summary page, verify that:

* A "URI (handle)" field is displayed with a URL. (Note: In the local
  development environment, this field will not be displayed unless a local
  handle server has been set up and configured).
* In the "Collections" field, two collections are listed:
  * SSDR Test Collection
  * Equitable Access Policy

Left-click the "Full item page" button. The full item page will be displayed.

6.9) On the full item page, verify that in the "Files" section at the at the
bottom of the page only the "Original bundle" is displayed. A "License bundle"
should *not* be displayed.

6.10) Check your email (or the email address associated with the CAS login) and
verify that an email with the subject line
"DRUM: Submission Approved and Archived" was received.

**Note**: An email will not be sent when using the local development
environment unless SMTP has been configured -- see the
"DSpace Scripts and Email Setup" section in
[dspace/docs/DockerDevelopmentEnvironment.md](DockerDevelopmentEnvironment.md).

### 7) Unit CRUD Operations

7.1) From the administrative sidebar, select "Access Control | Units". The
"Units" page will be displayed.

7.2) On the "Units" page, left-click the "Add Unit" button. The "Create unit"
page will be displayed.

7.3) On the "Create unit" page, fill out the following fields:

| Field | Value |
| ----- | ----- |
| Unit name | SSDR Test Unit |
| Faculty Only | \<Checked> |

then left-click the "Save" button. Verify that a notification is displayed
indicating that the unit was successfully created, and the "Edit unit"
page is displayed.

7.4) On the "Edit unit" page, add a group to the unit be left-clicking the
"Search" button in the "Add Groups" section, and left-clicking the "+" button
next to the group to add. Verify that a notification is shown indicating that
the group was successfully added.

7.5) Add another group to the unit, then delete the first group by left-clicking
the "trashcan" button. Verify that a notification is shown indicating that
the group was successfully deleted.

7.6) Delete the unit by left-clicking the "Delete Unit" button. A modal
confirmation dialog will be displayed. After selecting "Delete" in the
confirmation dialog, verify that a notification is shown indicating that the
unit was successfully deleted. The "Units" page will be displayed.

### 8) ETD Departments CRUD Operations

8.1) From the administrative sidebar, select
"DRUM Customizations | ETD Departments". The "ETD Departments" page will be
displayed.

8.2) On the "ETD Departments" page, left-click the "Add ETD department" button.
The "Create ETD department" page will be displayed.

8.3) On the "Create ETD department" page, fill out the following fields:

| Field | Value |
| ----- | ----- |
| ETD Department name | SSDR Test ETD Department |

then left-click the "Save" button. Verify that a notification is displayed
indicating that the ETD department was successfully created, and the
"Edit ETD department" page is displayed.

8.4) On the "Edit ETD department" page, add a collection to the department by
left-clicking the "Search" button in the "Add Collections" section, and
left-clicking the "+" button next to the collection to add. Verify that a
notification is shown indicating that the collection was successfully added.

8.5) Add another collection to the unit, then delete the first collection by
left-clicking the "trashcan" button. Verify that a notification is shown
indicating that the collection was successfully deleted.

8.6) Delete the group by left-clicking the "Delete ETD Department" button. A
modal confirmation dialog will be displayed. After selecting "Delete" in the
confirmation dialog, verify that a notification is shown indicating that the
ETD department was successfully deleted. The "ETD Departments" page will be
displayed.

### 9) Embargo List

9.1) From the administrative sidebar, select
"DRUM Customizations | Embargo List". The "Embargo List" page will be displayed.

**Note:** The table is likely wider than the browser page allows, so some
horizontal scrolling may be necessary to view all the columns.

9.2) On the "Embargo List" page, locate a permanently embargoed item, i.e., an
item on the list without an "End Date", such as
<https://drum-test.lib.umd.edu/handle/1903/9747>

Open the link in a private browser window (so that it displays as if you are
not logged in).

Verify that on the item page, the "Files" section in the left sidebar shows
"(RESTRICTED ACCESS)" under one or more of the files. Left-click the link for
the restricted item and verify that a "Restricted Access" page is shown
indicating that the document is not available.

**Note:** The "Restricted Access" functionality has multiple edge cases. See
[dspace/docs/DrumEmbargoAndAccessRestrictions.md](DrumEmbargoAndAccessRestrictions.md)
for more information.

### 10) Impersonate Functionality

10.1) From the administrative sidebar, select "Access Control | People". The
"EPeople" page will be displayed.

10.2) On the "EPeople" page, enter "4a301439-f448-44a7-aa99-1778f0c3f433"
into the search box. Verify that one person is found. If an entry is not found
look for a faculty member who is *not* an administrator.

10.3) Left-click the "Edit" button on the search result entry. The "Edit EPerson"
page will be displayed. Verify that the page contains a
"UM Directory Information" section that includes:

* Name
* Email
* Phone Number
* Faculty ("true" or "false")
* UM Appt
* Groups
* Units

For the example person, all the fields should be populated, and the "Faculty"
field should be "true". Both the "Groups" and "Units" field should have one or
more entries.

10.4) Left-click the "Impersonate EPerson" button on the page. The DRUM home
page will be displayed. Left-click the "User profile menu and logout"
dropdown in the navigation bar (the icon is a person in  silhouette) and verify
that the dropdown displays the name and email of the person being impersonated.

10.5) Left-click the "Stop impersonating EPerson" button in the navigation bar
to stop the impersonation. The DRUM home page will be displayed.

### 11) Batch Export (Zip)

11.1) From the administrative sidebar, select "Export | Batch Export (Zip)". A
"Export Batch (ZIP) from" modal dialog will be displayed.

11.2) In the "Export Batch (ZIP) from" modal dialog, select the
"SSDR Test Collection" and in the resulting dialog, left-click the "Export"
button. A "Process" page will be shown.

11.3) After the process completes, the "Process" page will refresh. Download the
"saf-export.zip" file to the local workstation and extract it. Verify that the
extracted contents include the PDF that was originally loaded in the steps
above, as well as additional files containing the metadata.

### 12) Batch Import (Zip)

**Note:** This step cannot be tested in the local development environment,
unless a local handle server is available.

12.1) Download the "drum_batch_import.zip" file attached to
<https://umd-dit.atlassian.net/browse/LIBDRUM-897> to the local workstation.

12.2) From the administrative sidebar, select “Import | Batch Import (ZIP)”.
The “Import Batch” page will be displayed.

12.3) Drag-and-drop the “drum-batch-import.zip” file onto the “Import Batch” page.
Then do the following:

* Left-click the “Select collection” button, and left-click the
  “SSDR Test Collection”
* Uncheck the “Validate Only” checkbox
* Left-click the “Proceed” button. A notification will be displayed indicating
  that a process was successfully created and a “Process” page will be
  displayed.

12.4) Once the “Process” page has a “Status” field of “COMPLETED”, left-click
the “University of Maryland | DRUM” logo on the left-side of the navigation bar
to return to the home page.

12.5) On the home page, go to the “SSDR Test Community” in the
“UM Community-managed Collections” section, and left-click the
“SSDR Test Collection” link. The “SSDR Test Collection” page will be shown.
Verify that there is a “SSDR Test Item - Meno” item from the import (you may
need to refresh the page).

### 13) OAI PMH

13.1) In a web browser, go to

<https://drum-test.lib.umd.edu/oai/request?verb=Identify>

and verify that a “DSpace OAI-PMH Data Provider” page with repository
information is displayed.

**Note:** In the local development environment, go to
<https://api.drum-local.lib.umd.edu/server/oai/request?verb=Identify>
instead.

### 14) JSON-LD

14.1) In a web browser, go to

<https://validator.schema.org/>

The Schema.org validator page will be displayed. In the
"Test your structured data" modal dialog, left-click the "Code snippet" link.

14.2) In a terminal, run the following command to retrieve the DRUM home page
and send it to the clipboard:

```zsh
$ curl -L https://drum-test.lib.umd.edu/ | pbcopy
```

Then paste the contents of the clipboard into the "Code snippet" field on the
Schema.org validator page, then left-click the "Run test" button.

Verify that the validator reports no errors or warnings, and that a single
"Website" element is displayed with information about the DRUM website.

14.3) In a terminal, run the following command to retrieve an item containing a
dataset and send it to the clipboard:

```zsh
$ curl -L https://drum-test.lib.umd.edu/handle/1903/29279 | pbcopy
```

14.4) On the Schema.org validator website, left-click the "Run new test" button
and paste in the contents of clipboard and then left-click the "Run test"
button.

Verify that the validator reports no errors or warnings, and that two elements:

* Dataset
* Website

are displayed with information about the dataset and the DRUM website.

### 15) Open Search

15.1) In a web browser, go to

<https://drum-test.lib.umd.edu/open-search/discover?query=author:smith>

and verify that an XML file can be downloaded, and contains item information.

**Note:** In the local development environment, go to
<https://api.drum-local.lib.umd.edu/server/opensearch/search?query=author:smith>
instead.

### 16) robots.txt

**Note:** This step cannot be tested in the local development environment.

16.1) In a web browser go to

<https://drum-test.lib.umd.edu/robots.txt>

Verify the contents of a "robots.txt" file is displayed, and contains the
following uncommented line (among many others):

```text
Disallow: /browse/*
```

16.2) In a web browser go to

<https://api.drum-test.lib.umd.edu/robots.txt>

and verify that the "robots.txt" file disallows all crawling.

### 17) sitemap.xml

**Note:** This step cannot be tested in the local development environment.

17.1) In a web browser go to

<https://drum-test.lib.umd.edu/sitemap_index.xml>

Verify that a "sitemap" file is returned (with a pointer to "sitemap0.xml").

17.2) In a web browser go to

<https://api.drum-test.lib.umd.edu/sitemap.xml>

and verify that either an empty page is returned (Chrome) or a page indicating
and XML Parsing error (Firefox) is returned.

### 18) SFTP Connectivity

**Note:** This step cannot be tested in the local development environment.

18.1) Create a “drum-ssdr-pdf-sftp-private-key” file:

```zsh
$ vi drum-ssdr-pdf-sftp-private-key
```

Copy the contents of the “drum-ssdr-pdf-sftp-private-key” entry in LastPass into
the file, and save the file.

18.2) Set the permissions on that file so that it is acceptable to SSH:

```zsh
$ chmod 600 drum-ssdr-pdf-sftp-private-key
```

18.3) In a terminal, run the following commands to port-forward the “2200” port on
the local machine to the “22” port of the “drum-0” pod in the Kubernetes
"test" namespace:

```zsh
$ kubectl config use-context test
$ kubectl port-forward drum-0 2200:22
```

18.4) In a second terminal, create a simple “test-upload.txt” file:

```zsh
$ echo "Test" > test-upload.txt
```

18.5) SFTP into the “drum-0” container, using the “drum-ssdr-pdf-sftp-private-key”
key, and the “pdf” username:

```zsh
$ sftp -P 2200 -i drum-ssdr-pdf-sftp-private-key pdf@localhost
```

18.6) Once connected to the SFTP server, run the following commands to switch to
the “incoming” directory and upload the “test-upload.txt” file:

```zsh
sftp> cd incoming
sftp> put test-upload.txt
```

Verify that the upload is successful.

18.7) Delete the “test-upload.txt” file, and exit the SFTP session:

```zsh
sftp> rm test-upload.txt
sftp> exit
```

18.8) In the first terminal, end the port forwarding:

```zsh
$ <Ctrl-C>
```

### 19) Collection and Community Deletion

19.1) From the administrative sidebar, select "Edit | Collection".
The "Edit collection" modal dialog will be displayed.

19.2) In the "Edit collection" modal dialog, select the
"SSDR Test Collection" entry. The "Edit Collection" page will
be displayed.

19.3) On the "Edit Collection" page, left-click the "Delete the collection"
button. A "Delete Collection" confirmation page will be displayed. Left-click
the "Confirm" button on the page, and verify that a notification is displayed
indicating that the collection was deleted.

19.4) From the administrative sidebar, select "Edit | Community".
The "Edit community" modal dialog will be displayed.

19.5) In the "Edit community" modal dialog, select the
"SSDR Test Community" entry. The "Edit Community" page will
be displayed.

19.6) On the "Edit Community" page, left-click the "Delete the community"
button. A "Delete Community" confirmation page will be displayed. Left-click
the "Confirm" button on the page, and verify that a notification is displayed
indicating that the community was deleted.
