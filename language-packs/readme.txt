DSPACE LANGUAGE PACKS
=====================


Layout of the source tree
=========================

The Messages.properties files for each language are stored in the following
directory below this. 

src/main/resources

The file naming conventions are as follows:

Messages_<ISO 639-1>_<ISO 3166-1>.properties

ISO 639-1: language code, in lower case.  
ISO 3166-1: codes for country, if required.  For example:

Language files for Greek

src/main/resources/
           Messages.properties - file for default (English)
           Messages_el.properties - file for Greek
           Messages_fr_FR.properties - file for French (France)
           Messages_fr_CA.properties - file for French (Canada)
           

For languages that can be fully represented using the Latin-1 character set,
the appropriate Messages.properties file is placed in the relevant
directory:

src/main/resources/Messages_it.properties

For languages that require other encodings, they are stored in this tree in
their native encoding for ease of editing, patching and so forth.  The
filename extension representing the encoding, for example:

src/main/resources/Messages_el.properties.UTF-8

The Maven build process supports native2ascii conversion on packaging of 
the UTF-8 files prior to packaging the jar. 

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

1.5 or Greater release Strategy:

The 1.5 Build process is Maven based and language packs is currently included
in the distributions by default.  The "dspace-1.5-release" distribution will
download them from the Maven Central repository while the the 
"dspace-1.5-src-release" includes them as a addon project that is compiled into 
<dspace.home>/lib and any <war>/WEB-INF/lib. This is done when compiling
packaging DSpace with Maven via the mechanism described in the documentation.

Creating a language pack for download
=====================================

Language Packs are currently added into the distributions by default.  If
wish to do development on them it is recommended that you either check them 
out from svn in the appropriate branch for your distribution; 

http://dspace.svn.sourceforge.net/svnroot/dspace/branches/dspace-1_5_x/language-packs

or by editing the existing project in dspace-1.5-src-release.zip.


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
