<%--
  - about_submitting.jsp
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

<dspace:layout title="About Submitting">

<h1>How to submit</h1>

<p>DRUM accepts a wide variety of research materials from all disciplines. There are a small number of conditions:</p> 

	<ul>
  		<li>You must be a University of Maryland faculty member to submit material. If you're a graduate student, your thesis or dissertation will be automatically submitted to DRUM by the Graduate School.</li>
  		<li>Submissions should be substantial works of research or scholarship.</li>
  		<li>You should be the author or coauthor of the materials. You may submit only materials for which you hold the copyright. <a href="mailto:drum-help@umd.edu">Email us</a> for assistance with copyright matters and see our guide to <a href="http://lib.guides.umd.edu/authorrights">author rights</a> for more information.</li>
  		<ul><li>If you transferred rights to a publisher or another entity, you may need their permission to submit material. <a href="mailto:drum-help@umd.edu">Email us</a> for assistance.</li></ul>
  		<li>Depending on the nature or volume of your materials, the University Libraries may decline your submission. In this situation, we will make every effort to help you find a more appropriate repository.</li>
	</ul>

<h2>File size</h2>

<p>You can upload multiple files with each submission, but for technical reasons the submission form will not accept individual files over 2GB. You can submit zipped, tarred, or other archive files.</p>

<p>If you have individual files larger than 2GB, we will load them into DRUM for you. <a href="mailto:drum-help@umd.edu">Email us</a> to get started.</p>

<h2>Preparing data for submission</h2>

<p>The submission process has two steps:</p>
<ol>
	<li>Create a basic description of your work (author, title, date, abstract, etc. - <a href="http://drum.lib.umd.edu/handle/1903/13387">example</a>)</li>
	<li>Upload files</li>
<ol>
<p>We can provide advice and assistance at any stage. If you would like to submit a complex research study with multiple data files and supporting documents, we can help you prepare your materials and organize your submission. <a href="mailto:drum-help@umd.edu">Email us</a> in advance.</p>

<h2>Get your permanent DOI</h2>

<p>After we review your submission, you will receive an email that contains the permanent DOI to your materials. Use the link in publications and wherever you refer to the materials.</p>

<h2>Recommendations for specific materials</h2>

<p><strong>1. Papers, articles, pre-prints, reports, posters, slides</strong></p>

<p>Submit PDF rather than Word or PowerPoint documents. Other suitable formats include LaTex, OpenDocument formats (odt, odp), plain text (txt), rich text (rtf), and HTML.</p>

<p>If you plan to submit a paper that has been published or presented elsewhere, please have the citation information ready.</p>

<p><strong>2. Data</strong></p>

<p>Submit comma-separated, tab-separated, or other delimited or fixed-width files for tabular or 'ascii' data (e.g txt, csv, tab, tsv). Excel and Access files are acceptable, but avoid submitting data in specialized instrument formats. See our <a href="http://www.lib.umd.edu/data/formats">file format recommendations</a> for additional suggestions.</p>

<p>Data should be accompanied by a 'readme' file, data dictionary, codebook, or similar document that contains, where applicable:</p>

<ul>
	<li>a file manifest describing file names and contents</li>
	<li>state of the data (raw, cleaned, processed, subset, summary)</li>
	<li>instruments and software used to create the data</li>
	<li>processing steps</li>
	<li>explanation of variables, column headers, value codes, flags, etc.</li>
	<li>software required to view or use the data</li>
	<li>licensing and any terms or conditions of use</li>
	<li>funding source and grant number</li>
	<li>contact information</li>
</ul>

<p><strong>2. Code</strong></p>

<p>Submit code files with the appropriate language extension (e.g. mat, py, r, pl, f). Software should be accompanied by a 'readme' file that contains, where applicable:</p>

<ul>
	<li>required libraries or packages</li>
	<li>version information and changelog</li>
	<li>installation and configuration instructions</li>
	<li>known bugs and troubleshooting steps</li>
	<li>credits and acknowledgements</li>
	<li>licensing and any terms or conditions of use</li>
	<li>funding source and grant number</li>
	<li>contact information</li>
</ul>

<p><strong>4. Images, audio, and video</strong></p>

<p>For image files, submit JPEG, TIFF, PNG, PDF, or SVG. For audio or video file, submit MPEG (mp3, mp4), WAV, AVI, or Quicktime (mov). Other formats are acceptable, but they may be less useful to other researchers.</p>

<p>Images, audio, and video should be accompanied by a 'readme' file or similar document that contains any of the items listed for data (above) that apply to your material.</p>

<p><strong>5. Everything else</strong></p>

<p>DRUM can accept 3D models, biological and chemical sequences, geospatial data, text corpora, and many other research outputs. We can help you select file formats, prepare documentation, and organize your submission.</p>

<p>Consider including a 'readme' file or similar document that contains any of the items listed for data (above) that apply to your material.</p>

<p><strong>Contact us with questions or comments</strong><br />
General inquiries: <a href="mailto:drum-help@umd.edu">drum-help@umd.edu</a>, (301) 314-1328<br />
DRUM Coordinator: Terry Owen, <a href="mailto:towen@umd.edu">towen@umd.edu</a></p>


</dspace:layout>
