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


CVS tags
========

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


Creating a language pack for download
=====================================

The 'make-language-pack' script can be used to build these, for example to
make dspace-language-pack-1.3.2-2.tar.gz:

  create-language-pack -native2ascii language-pack-1_3_2-2 1.3.2-2

This will create dspace-language-pack-1.3.2-2.tar.gz

Up to version 1.3.2, any .properties files managed in UTF-8 encoding must be
converted by native2ascii before being put in the language pack.  In this
case use the '-native2ascii' parameter.

As of version 1.4, the DSpace build process itself converts UTF-8-encoded
.properties files, and so the '-native2ascii' parameter should be omitted.


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
