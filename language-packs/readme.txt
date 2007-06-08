DSPACE LANGUAGE PACKS
=====================


Layout of the source tree
=========================

The Messages.properties files for each language are stored in the
directories below this.  The top-level directory is the ISO 639-1 language
code, in lower case.  Sub-directories of that are the ISO 3166-1 codes for
country, if required.  For example:


el/
   language files for Greek

fr/
   FR/
      language files for French (France)
   CA/
      language files for Canadian French


For languages that can be fully represented using the Latin-1 character set,
the appropriate Messages.properties file is placed in the relevant
directory:

it/
   Messages_it.properties

For languages that require other encodings, they are stored in this tree in
their native encoding for ease of editing, patching and so forth.  The
filename extension representing the encoding, for example:

el/
   Messages_el.properties.UTF-8


SVN tags
========

Previous Release strategy:

Language packs are versioned according to the main DSpace version (1.x),
followed by a hyphen, followed by a sequence number.  Several language pack
versions might be produced for a single version of DSpace, as new
translations are updated and contributed.

So, for example, versions might be:

language-pack-1_3_2-1   (contains fr, zh)
language-pack-1_3_2-1   (contains fr, zh, pt)
language-pack-1_4-1     (contains zh)
language-pack-1_4-2     (contains zh, fr, pt)

Note that a language file should only be tagged when it is complete for a
particular *stable* version of DSpace.  This means tagging must be done
carefully on individual files, and not on the whole language-packs tree.

Maven Based release strategy, language packs need to be compiled into 
<dspace.home>/lib and any <war>/WEB-INF/lib. This is done when compiling
packaging DSpace with Maven via the following Mechanism.


Creating a language pack for download
=====================================

Language Packs are now a DSpace Addon. 
They can be compiled into your dspace distro via the addition of '-P lang' 
to the active profiles, they are inactive by default. 

The Maven build process supports native2ascii conversion on packaging of 
the Jar. When the profile is active, the jar will be a dependency compiled into all 
lib directories where dspace-api is present.


OTHER TOOLS
===========


checkkey.pl
===========

This is a handy Perl tool for determining which message keys are present in
one properties file versus another.  This is useful for determining whether
a translation is complete.  If you run:

checkkeys.pl dspace-1.4-source/config/language-packs/Messages.properties
  Messages_fr.properties

the tool will tell you which keys are missing from Messages_fr.properties,
and which (if any) are in Messages_fr.properties but not in the core
Messages.properties.  In a complete translation for a particular version,
there should be no missing or extra keys.
