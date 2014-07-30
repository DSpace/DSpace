<%--
  - formats.jsp
  -
  - Version: $Revision: 1.7 $
  -
  - Date: $Date: 2003/02/21 19:51:50 $
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>


<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout title="DRUM Format Guide">

<h1>DRUM Supported Formats</h1>

<p><a href="#formats">Currently supported formats</a></p>

<a name="policy"><h2>DRUM Format Support Policy</h2></a>

<p>We want to provide support for as many file formats as possible. Over time, items stored in DRUM will be preserved as 
is, using a combination of time-honored techniques for data management and best practices for digital preservation. As for 
specific formats, however, the proprietary nature of many file types makes it impossible to guarantee much more than this. 
Put simply, our policy for file formats is:</p>

<ul>
  <li>everything put in will be retrievable</li>
  <li>we will recognize as many files' formats as possible</li>
  <li>we will support as many known file formats as possible</li>
</ul>

<p>By "support", we mean "make useable in the future, using whatever combination of techniques (such as migration, 
emulation, etc.) is appropriate given the context of need". For supported formats, we might choose to bulk-transform 
files from a current format version to a future one, for instance. But we can't predict which services will be necessary 
down the road, so we'll continually monitor formats and techniques to ensure we can accomodate needs as they arise.</p>

<p>In the meantime, we can choose to "support" a format if we can gather enough documentation to capture how the format 
works. In particular, we collect file specifications, descriptions, and code samples. Unfortunately, this means that 
proprietary formats for which these materials are not publicly available cannot be supported in DRUM. We will still 
preserve these files, and in cases where those formats are native to tools supported by the Office of Information 
Technology, we will provide you with guidance on converting your files into formats we do support. It is also likely 
that for extremely popular but proprietary formats (such as Microsoft .doc, .xls, and .ppt), we will be able to help 
make files in those formats more useful in the future simply because their prevalence makes it likely tools will be 
available. Even so, we cannot guarantee this level of service without also having more information about the formats, 
so we will still list these formats as "known", not "supported".</p>

<a name="formats"><h2>Currently supported formats</h2></a>

<p> In the table below, <em>MIME type</em> is the Multipurpose Internet Mail Extensions (MIME) type identifier; for more
information on MIME, see the <a href="http://www.nacs.uci.edu/indiv/ehood/MIME/MIME.html">MIME RFCs</a> or the 
<a href="http://www.faqs.org/faqs/mail/mime-faq/">MIME FAQ</a>. <em>Description</em> is what most people use as the name 
for the format. <em>Extensions</em> are typical file name extensions (the part after the dot, e.g. the extension for 
"index.html" is "html"). These are not case-sensitive in DRUM, so either "sample.XML" or "sample.xml" will be recognized 
as XML. <em>Level</em> is DRUM's support level for each format: </p>

<ul>
 	<li><a name="supported"></a><strong>supported</strong>: we fully support the format</li>
  	<li><a name="known"></a><strong>known</strong>: we can recognize the format, but cannot guarantee full support</li>
  	<li><a name="unsupported"></a><strong>unsupported</strong>: we cannot recognize a format; these will be listed as "application/octet-stream", aka Unknown</li>
</ul>

<p>Please see the full <a href="#policy">format policy</a> below for a complete explanation of these terms.If your format
is not listed below, see <a href="#notlisted">What to do if your format isn't listed</a> at the bottom of this page.</p>

<table align="center" border="0" cellspacing="5" cellpadding="5"
 width="95%" class="standard">
  <tbody>
    <tr>
      <th align="left">MIME type</th>
      <th align="left">Description</th>
      <th align="left">Extensions</th>
      <th align="left">Level</th>
    </tr>
    <tr>
      <td>application/octet-stream</td>
      <td>Unknown</td>
      <td><br>
      </td>
      <td>unsupported</td>
    </tr>
    <tr>
      <td>application/pdf</td>
      <td>Adobe PDF</td>
      <td>pdf</td>
      <td>known</td>
    </tr>
    <tr>
      <td>text/xml</td>
      <td>XML</td>
      <td>xml</td>
      <td>known</td>
    </tr>
    <tr>
      <td>text/plain</td>
      <td>Text</td>
      <td>txt, asc</td>
      <td>known</td>
    </tr>
    <tr>
      <td>text/html</td>
      <td>HTML</td>
      <td>htm, html</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/msword</td>
      <td>Microsoft Word</td>
      <td>doc</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/vnd.ms-powerpoint</td>
      <td>Microsoft Powerpoint</td>
      <td>ppt</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/vnd.ms-excel</td>
      <td>Microsoft Excel</td>
      <td>xls</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/marc</td>
      <td>MARC</td>
      <td><br>
      </td>
      <td>known</td>
    </tr>
    <tr>
      <td>image/jpeg</td>
      <td>JPEG</td>
      <td>jpeg, jpg</td>
      <td>known</td>
    </tr>
    <tr>
      <td>image/gif</td>
      <td>GIF</td>
      <td>gif</td>
      <td>known</td>
    </tr>
    <tr>
      <td>image/png</td>
      <td>image/png</td>
      <td>png</td>
      <td>known</td>
    </tr>
    <tr>
      <td>image/tiff</td>
      <td>TIFF</td>
      <td>tiff, tif</td>
      <td>known</td>
    </tr>
    <tr>
      <td>audio/x-aiff</td>
      <td>AIFF</td>
      <td>aiff, aif, aifc</td>
      <td>known</td>
    </tr>
    <tr>
      <td>audio/basic</td>
      <td>audio/basic</td>
      <td>au, snd</td>
      <td>known</td>
    </tr>
    <tr>
      <td>audio/x-wav</td>
      <td>WAV</td>
      <td>wav</td>
      <td>known</td>
    </tr>
    <tr>
      <td>video/mpeg</td>
      <td>MPEG</td>
      <td>mpeg, mpg, mpe</td>
      <td>known</td>
    </tr>
    <tr>
      <td>text/richtext</td>
      <td>RTF</td>
      <td>rtf</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/vnd.visio</td>
      <td>Microsoft Visio</td>
      <td>vsd</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/x-filemaker</td>
      <td>FMP3</td>
      <td>fm</td>
      <td>known</td>
    </tr>
    <tr>
      <td>image/x-ms-bmp</td>
      <td>BMP</td>
      <td>bmp</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/x-photoshop</td>
      <td>Photoshop</td>
      <td>psd, pdd</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/postscript</td>
      <td>Postscript</td>
      <td>ps, eps, ai</td>
      <td>known</td>
    </tr>
    <tr>
      <td>video/quicktime</td>
      <td>Video Quicktime</td>
      <td>mov, qt</td>
      <td>known</td>
    </tr>
    <tr>
      <td>audio/x-mpeg</td>
      <td>MPEG Audio</td>
      <td>mpa, abs, mpega</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/vnd.ms-project</td>
      <td>Microsoft Project</td>
      <td>mpp, mpx, mpd</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/mathematica</td>
      <td>Mathematica</td>
      <td>ma</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/x-latex</td>
      <td>LateX</td>
      <td>latex</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/x-tex</td>
      <td>TeX</td>
      <td>tex</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/x-dvi</td>
      <td>TeX dvi</td>
      <td>dvi</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/sgml</td>
      <td>SGML</td>
      <td>sgm, sgml</td>
      <td>known</td>
    </tr>
    <tr>
      <td>application/wordperfect5.1</td>
      <td>WordPerfect</td>
      <td>wpd</td>
      <td>known</td>
    </tr>
    <tr>
      <td>audio/x-pn-realaudio</td>
      <td>RealAudio</td>
      <td>ra, ram</td>
      <td>known</td>
    </tr>
    <tr>
      <td>image/x-photo-cd</td>
      <td>Photo CD</td>
      <td>pcd</td>
      <td>known</td>
    </tr>
  </tbody>
</table>

<a name="notlisted"><h2>What to do if your format isn't listed</h2></a>

<p>We understand that there are always more formats to consider, and we would appreciate your help in identifying and 
studying the suitability of support for formats you care about. If we can't identify a format, DRUM will record it as 
"unknown", aka "application/octet-stream", but we would like to keep the percentage of supported format materials in 
DRUM as high as possible. Don't hesitate to contact us at <a href="mailto:drum-help@umd.edu">drum-help@umd.edu</a> if 
you have any questions or concerns.</p>


</dspace:layout>
