# DRUM Embargo and Access Restrictions

## Introduction

This page describes the DRUM Embargo functionality and other access
restrictions, as of DSpace 7.

## Useful Resources

* <https://wiki.lyrasis.org/display/DSDOC7x/Embargo>
* <https://wiki.lyrasis.org/display/DSDOC7x/Authentication+Plugins#AuthenticationPlugins-IPAuthentication>

## Stock DSpace Embargo Functionality

**Note:** The DSpace documentation discusses embargoes for both items and
bitstreams. In the DRUM configuration, only bitstreams are embargoed, so this
document will use "bitstream" throughout.

DSpace embargoes a bitstream solely through the resource policies on that
bitstream.

As described in the [DSpace documentation](https://wiki.lyrasis.org/display/DSDOC7x/Embargo):

> An embargo is a temporary access restriction placed on metadata or bitstreams
> (i.e. files). Its scope or duration may vary, but the fact that it eventually
> expires is what distinguishes it from other content restrictions.

Therefore, there is no explicit representation of an embargo in the database,
it is simply an emergent result based on the resource policies of the bitstream,
the user, and the current date.

The resource policy for an embargo typically consists of setting an
"Anonymous READ" policy (a policg for the "Anonymous" group with "READ"
permission) that has a "Start Date" in the future. Once the date in the
"Start Date" is reached, anyone can download the bitstream. For bitstreams that
are permanently embargoed, an "Anonymous READ" policy is simply not created.

The DSpace Angular front-end shows a "lock" icon next to the download link. The
display of this icon is controlled by whether or not the user has "READ"
permission for the bitstream. If the user clicks the link, the stock DSpace
functionality is:

* If the user is not logged in, send them to a login page
* If the user is logged in, show a "Forbidden" page

## IP Address-based Download Restrictions

Another stock DSpace mechanism for restricting bitstream downloads is IP Address
authentication, see:

<https://wiki.lyrasis.org/display/DSDOC7x/Authentication+Plugins#AuthenticationPlugins-IPAuthentication>

This is currently used to enforce "on-campus" access to bitstreams, via the
"Campus" group.

This is not really an embargo, but intersects with the embargo functionality
when it comes to displaying the "Restricted Access" page (see below).

A bitstream that is that available to the user due to an IP address restriction
will show the "lock" icon next to the download link, as the user does not have
"READ" permission for the bitstream.

## Embargo Functionality in DRUM

UMD began embargoing items before the current DSpace embargo functionality
existed. The following functionality is currently supported:

* Automatically add embargo terms to "ETD" bitstreams uploaded from ProQuest.
* Display a list of embargoed items, with embargo lift dates
* Display a "Restricted Access" marker below the download link, if the bitstream
  is embargoed. This marker is displayed for all users (even those who can
  download the bitstream).
* A "Restricted Access" page is shown if a user attempts to download a bitstream
  for which they do not have a "READ" resource policy. Different messages
  are shown based on the user's login status, and (for anonymous users) whether
  or not the bitstream has an embargo lift date.

This UMD-specific functionality is supported by adding a resource policy a group
of "ETD Embargo" to the bitstreams to be embargoed. Permanently embargoed items
have an "ETD Embargo READ" policy without an "End Date" (and no
"Anonymous READ" policy). Embargoes that eventually end have an
"ETD Embargo READ" policy with an "End Date" corresponding to the "Start Date"
of the "Anonymous READ" policy.

There is nothing in the current system that enforces this pairing of
"ETD Embargo READ" and "Anonymous Embargo READ" resource policies.
Resource policies can be manually added/edited by administrators, and the
system simply relies on those administrators maintaining both policies.

### ETD Loader

When ingesting ETD items from ProQuest, the bitstreams will either have no
embargo, or a specific date for lifting the embargo. For embargoed items, the
ETD loaded  automatically adds both policies.

### Embargo List

The "Embargo List" functionality lists all items that have *any* bitstream
with an "ETD Embargo" resource policy whose "End Date" is not in the past.
This means an item may appear in the list even if its "primary bitstream"
(the one a user would typically download) is not embargoed.

Note that the "Embargo List" only uses "ETD Embargo" resource policies. It
does not use any "Anonymous READ" policies (or lack thereof) associated with
the item's bitstreams. The "End Date" column in the table uses the "End Date"
field in the "ETD Embargo" resource policy.

### Restricted Access Marker

A "(RESTRICTED ACCESS)" marker is shown below the download link for embargoed
bitstreams. This marker is displayed for all users (even those who can download
the bitstream).

This marker is separate from the stock DSpace "lock" icon that appears to the
left of the download link. The presence of the "lock" icon indicates that the
user cannot download the bitstream (for example, if there is a "Campus" IP
address restriction on the bitstream, and the user is not on campus).

The primary intention of the "(RESTRICTED ACCESS) marker is to enable
administrators to easily see that a bitstream is embargoed, and relies solely
on the "ETD Embargo" resource policy.

### Restricted Access Page

Attempting to download a bitstream to which the user does not have "READ"
permission (either due to an embargo, or some other factor) will result in
a "Restricted Access" page being shown. The page shows different messages
based on:

* Whether the user is logged in - this can happen, for example, if a bitstream
  has been restricted to on-campus IP addresses through a "Campus" group
  resource policy.
* For anoynmous users, whether the embargo has a lift date

This functionality relies on the stock DSpace resource policies to determine
if a user has "READ" permission to the bitstream. The "ETD Embargo" resource
policy is only used to determine the embargo lift date.

## Expected Functionality

The following table summarizes the expected behavior in the various scenarios
involving embargoes and "Campus" IP access restrictions:

| Situation                        | Anonymous                 | Logged-in User            | Administrator             |
| -------------------------------- | ------------------------- | ------------------------- | ------------------------- |
| No Embargo                       | No lock/No RAM/Can Access | No lock/No RAM/Can Access | No lock/No RAM/Can Access |
| Embargo - Past Lift Date         | No lock/No RAM/Can Access | No lock/No RAM/Can Access | No lock/No RAM/Can Access |
| Embargo - Future Lift Date       | Lock/RAM/No Access        | Lock/RAM/No Access        | No lock/RAM/Can Access    |
| Embargo - Forever                | Lock/RAM/No Access        | Lock/RAM/No Access        | No lock/RAM/Can Access    |
| "Campus" Restricted - On-campus  | No Lock/No RAM/No Access  | No Lock/No RAM/No Access  | No lock/No RAM/Can Access |
| "Campus" Restricted - Off-campus | Lock/No RAM/No Access     | Lock/No RAM/No Access     | No lock/No RAM/Can Access |

Where:

* "lock" - whether or not the "lock" icon is displayed
* "RAM" - whether or not the "Remote Access" text marker is shown beneath the
   download link
* "Access" - whether or not the user can actually download the bitstream.
