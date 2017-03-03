# MediaFilter Task Suite #

Project intended to serve as a general replacement for the DSpace MediaFilter
command-line tool and its filters.

The motivation is discussed at greater length here:
<https://wiki.duraspace.org/display/DSPACE/MediaFilter+Task+Suite>,
but briefly these tasks, unlike MedaiFilter operations, may be run from the
Admin UI, embedded in workflow, etc and are highly configurable. In addition,
use of the Apache Tika library greatly expands the functional scope of text
extraction to many more file types.

## How to use ##

Each task needs a specific configuration of both properties _common_ to all
media filters, and any properties _specific_ to the task. First, let's review
the common properties. 

Media filters all operate in the same basic way: they read selected bitstreams
of Items and produce some sort of derivative or transformation from the
bitstream, and store this derivative in a new bitstream. Thus the base
configuration essentially consists of a _mapping_ from a *source* to a *target*
bitstream, with instructions on how the transform (or *filter* as it is called)
works.  The configuration property names reflect this:

* source.selector (required) a bundle name, and optionally a file pattern to match: 'ORIGINAL/*.jpg'
* source.formats (optional) a comma separated list of DSpace bitstream formats to filter with
* source.minsize (optional - defaults to 0) minimum file size to process

* filter.force (optional - defaults to false) whether to produce a derivative even if one exists

* target.spec (required) a bundle name, an optional specification of target name (if absent: sourcename.ext)
* target.format (required) bitstream format to assign to derivative file
* target.description (required) bitstream description
* target.policy (required) rule to assign resource policy to derivative file

The values of the the policy property are:

* bitstream = same policy as source bitstream
* bundle = inherit from target bundle
* item = inherit from item
* collection = inherit from item's owning collection
* open = anonymous read permission
* closed = all resource policies removed

As an example of specific properties, suppose our mediafilter task generates
thumbnail images. In this case, our configuration would *also* include:

* image.maxwidth (required) maximum width (pixels) of thumbnail
* image.maxheight (required) maximum heght (pixels) of thumbnail

As with all 'profiled' curation tasks, these configuration files must live in
confg/modules, and have the same name as the logical task. For example, we would
have the above set of properties in `dspace/config/modules/thumbnail.cfg` and
the task called 'thumbnail'.
