#! /bin/bash

# August 2008, Bradley McLean.  No rights reserved.  Do what you will.
#
# Developed On Ubuntu Hardy Heron, you'll need at least:
# apt-get install herold docbook-utils docbook2x xsltproc docbook-xsl dbdoclet

DBxml="docbook"
HTMLfinal="html"
PDFfinal="pdf"

# The first part of this script is hypothetically to be used only once, to make
# the initial transformation from html docs to docbook source.

haveDB=`ls $DBxml | wc -w`
if [ $haveDB -lt 1 ]; then
	if [ ! -d $DBxml ]; then
	  mkdir $DBxml
    fi
	cp *.html $DBxml
	cd $DBxml
	# A couple quick cleanups needed
	sed -ie "762s/it>/i>/g" configure.html
	sed -ie "2903,2905s/gt; . \&l/gt; ... \&l/" DRISchemaReference.html
	#  sed -ie "/<div class=\"element-type\">/,/<\/div>/d" DRISchemaReference.html
	sed -ie "s/href=\"#type/name=\"#type/g" DRISchemaReference.html
	sed -ie "s/href=\"#Meta/name=\"#Meta/g" DRISchemaReference.html
	sed -ie "s/href=\"#newfilter/name=\"#newfilter/g" configure.html
	sed -ie "s/href=\"#browse/name=\"#browse/g" configure.html
	sed -ie "s/href=\"#element/name=\"#element/g" DRISchemaReference.html
	sed -ie "s/href=\"#stepDefinitions/name=\"#stepDefinitions/g" submission.html
	sed -ie "s/href=\"#authenticate/name=\"#authenticate/g" configure.html
	sed -ie "s/300px/175px/g" configure.html
	sed -ie "/Back to contents/d" *html
	sed -ie "/\&copy/d" *html
	sed -ie "s/\&nbsp;/ /g" *html
	sed -ie "11,63d" configure.html
	sed -ie "44,56d" DRISchemaReference.html
	sed -ie "2,34d" DRISchemaReference.html
	sed -ie "11,28d" install.html
	sed -ie "20,28d" submission.html
    filelist=`ls *html`
	cd ..

	title="DSpace 1.5.1 Manual"

	# The initial raw conversion:
	for file in $filelist; do
	    herold --no-prolog --destination-encoding=UTF-8 \
    		-t "$title" -i $DBxml/$file -o $DBxml/${file/.html/.xml}
	done
	rm $DBxml/*html $DBxml/*htmle
	for file in `ls $DBxml`; do
		# Move the fixed section types into generic hierachies, built into chapters, and loose the article wrappers to form subset files.
		sed -i -e "s/sect1>/chapter>/" -e "s/<sect1 /<chapter /" $DBxml/$file
		sed -i -e "2,4d" -e "s,</article>,," $DBxml/$file
		sed -i -e "s/sect[2-9]>/section>/g" -e "s/<sect[2-9] /<section /"  $DBxml/$file
		# Tell the images to scale to fit
		sed -ie "s/\<imagedata.*format=...../& width=\"6.5in\" scalefit=\"1\"/g" $DBxml/$file
		sed -ie "s,docbook/image,image,g" $DBxml/$file
		# Forcibly clean up any literal blocks (screen elements) that don't line wrap narrowly enough for a print formatted manual.
		mv $DBxml/$file $DBxml/$file.prewrap
		./wrapscreen.py $DBxml/$file.prewrap $DBxml/$file
		rm $DBxml/$file.prewrap
	done
#    ln -s ../image $DBxml/image
	# Generate the Book Wrapper
	cat <<EOF >$DBxml/book.xml
<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE book PUBLIC '-//OASIS//DTD DocBook XML V4.5//EN' 'http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd' [

<!ENTITY art01 SYSTEM "index.xml">
<!ENTITY art02 SYSTEM "introduction.xml">
<!ENTITY art03 SYSTEM "install.xml">
<!ENTITY art04 SYSTEM "update.xml">
<!ENTITY art05 SYSTEM "configure.xml">
<!ENTITY art06 SYSTEM "storage.xml">
<!ENTITY art07 SYSTEM "directories.xml">
<!ENTITY art08 SYSTEM "architecture.xml">
<!ENTITY art09 SYSTEM "application.xml">
<!ENTITY art10 SYSTEM "business.xml">
<!ENTITY art11 SYSTEM "functional.xml">
<!ENTITY art12 SYSTEM "submission.xml">
<!ENTITY art13 SYSTEM "DRISchemaReference.xml">
<!ENTITY art14 SYSTEM "history.xml">
<!ENTITY art15 SYSTEM "appendix.xml">
]>

<book>
  <bookinfo>
    <title>DSpace 1.5.1 Manual</title>
    
    <author>
      <surname>The DSpace Foundation</surname>
      <affiliation>
        <address><email>webmaster@dspace.org</email></address>
      </affiliation>
    </author>

    <copyright>
      <year>2002-2008</year>
      <holder>
        <ulink url="http://www.dspace.org/">The DSpace Foundation</ulink>
      </holder>
    </copyright>
    <legalnotice>
      <para>
        <ulink url="http://creativecommons.org/licenses/by/3.0/us/">
        <inlinemediaobject>
          <imageobject>
            <imagedata fileref="http://i.creativecommons.org/l/by/3.0/us/88x31.png" format="PNG"/>
          </imageobject>
        </inlinemediaobject>
        Licensed under a Creative Commons Attribution 3.0 United States License
        </ulink>
      </para>
    </legalnotice>
    <abstract>
      <para></para>
    </abstract>
  </bookinfo>

  <preface>
    <title>Preface</title>

    <para></para>
  </preface>

<toc/>

<!-- &art01; -->
&art02;
&art03;
&art04;
&art05;
&art06;
&art07;
&art08;
&art09;
&art10;
&art11;
&art12;
&art13;
&art14;
&art15;
<index/>
</book>
EOF
	# Generate an XSL customization wrapper for print
	cat <<EOF >$DBxml/print.xsl
<?xml version='1.0'?> 
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    version="1.0"> 

<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/fo/docbook.xsl"/> 

<xsl:template match="lineannotation">
  <fo:inline font-style="italic">
    <xsl:call-template name="inline.charseq"/>
  </fo:inline>
</xsl:template>

<xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="hyphenation-character">\</xsl:attribute>
</xsl:attribute-set>

</xsl:stylesheet>
EOF
	# Generate an XSL customization wrapper for HTML
	cat <<EOF >$DBxml/html.xsl
<?xml version='1.0'?>
<xsl:stylesheet  
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:import href="http://docbook.sourceforge.net/release/xsl/current/html/chunk.xsl"/>

<!-- <xsl:param name="html.stylesheet" select="'corpstyle.css'"/>
<xsl:param name="admon.graphics" select="1"/> -->

<!--  footer xsl stuff?
(could use          user.footer.navigational too)
-->
<xsl:template name="user.footer.content">
  <HR/>
  <xsl:apply-templates select="//copyright[1]" mode="titlepage.mode"/>
  <xsl:apply-templates select="//legalnotice[1]" mode="titlepage.mode"/>
</xsl:template>


<xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="hyphenation-character">\</xsl:attribute>
</xsl:attribute-set>

</xsl:stylesheet>
EOF
fi

########################################################################
# The rest of this script contains processing to create two .pdfs using
# two different techniques, and regenerate HTML from the docbook.
# All of this should eventually get replaced with something that can be
# driven from our Maven builds, ideally in a pure java space to reduce
# the number of moving parts and dependencies.
#
# Part of this has already been done below;  the DSSL stuff is no longer
# used, and all of the processing is done with Fop (and Xalan under the
# hood).  The Maven linkage still needs to be done.
if [ ! -d $PDFfinal ]; then
  mkdir $PDFfinal
fi

# PDF it using DSSL - this can probably never be java-itized, and should
# be considered a legacy technique.  Hopefully nobody develops a preference
# for its appearance ;-)
# docbook2pdf $DBxml/book.xml
# mv book.pdf $PDFfinal/book-dssl.pdf

xslprint="$DBxml/print.xsl"
xslhtml="$DBxml/html.xsl"

# Some XSL appearance tweaks for our document.

XSLTP=" -param body.start.indent 0pt \
	-param section.autolabel 1 \
	-param section.autolabel.max.depth 2  \
	-param section.label.includes.component.label 1 \
	-param chunk.section.depth 0 \
	-param variablelist.as.blocks 1"

# PDF it using XSL, note that fop is actually java hiding under a script
if [ "a$FOP_HOME" == "a" ]; then
    FOP_HOME=/usr/local/share/fop-0.94
fi
java -Xmx128m -Dfop.home=$FOP_HOME -jar $FOP_HOME/build/fop.jar -xml $DBxml/book.xml -xsl $xslprint -pdf $PDFfinal/book-foppure.pdf $XSLTP

# HTML it using XSL
if [ ! -d $HTMLfinal ]; then
    mkdir $HTMLfinal
fi
rm $HTMLfinal/*html
# Lie to Fop, and tell it we're generating an fo (which will be empty with these stylesheets).
# This saves figuring out a separate set of dependencies, classpath and command line for the underlying Xalan.
java -Xmx64m -Dfop.home=$FOP_HOME -jar /usr/local/share/fop-0.94/build/fop.jar -xml $DBxml/book.xml  -xsl $xslhtml -foout fo.fo $XSLTP
rm fo.fo
mv $DBxml/*html $HTMLfinal

