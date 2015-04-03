var exportFormats = [ "bibtex", "cmdi" ];
var ufal = ufal || {};
ufal.citation = {

		citationBox: function(container) {
			container.html("");
			var div = jQuery("<div></div>").appendTo(container);
			var URI = container.attr("uri");
			var oaiURI = container.attr("oai");
			var handle = container.attr("handle");
			var dataTarget = container.attr("data-target");
			var title = container.attr("title");
			div.addClass("alert alert-warning bold");
			div.css("padding", "15px 10px 15px 10px");
			div.css("font-size", "13px");
			var exportSpan = jQuery("<span class='pull-right'></span>").appendTo(div);
			//exportSpan.append("<span class='bold'><i class='fa fa-magic'>&#160;</i>Export to</span>");
			for(var i=0;i<exportFormats.length;i++) {
				var format = exportFormats[i];
				var link = jQuery("<a>" + format + "</a>").appendTo(exportSpan);
				link.css("margin-left", "2px");
				link.css("text-transform", "uppercase");
				link.css("margin-right", "2px");
				link.css("text-shadow", "none");
				link.css("font-size", "11px");
				link.attr("data-toggle", "modal");
				link.attr("data-target", dataTarget);
				link.attr("title", title);
				link.addClass("label label-default exportto");
				link.attr("href", oaiURI + "/cite?metadataPrefix=" + format + "&handle=" + handle);
				link.click(ufal.citation.exporter_click);
			}

			jQuery("<div>" +
					"<i class='fa fa-quote-left fa-2x pull-left'>&#160;</i>" +
					"Please use the following text to cite this item or export to a predefined format:" +
					"</div>").appendTo(div);
			
			var textDiv = jQuery(
				"<div style='margin-top: 10px; padding: 10px; color: #999999;'>" +
				"</div>").appendTo(div);
			
			var copyLink = jQuery(
					"<a title='Copy Citation Text' data-div='.cite-text' href='#!' style='text-decoration: none;' data-toggle='modal' data-target='#exporter_copy_div'>" +
						"<span class='fa-stack fa-lg pull-right text-warning'>" +
							"<i class='fa fa-circle fa-stack-2x'></i>" +
							"<i class='fa fa-copy fa-inverse fa-stack-1x'></i>" +
						"</span>" +
					"</a>").appendTo(textDiv);					
			
			var citeText = jQuery("<div class='cite-text linkify'><i class='fa fa-spinner fa-spin fa-2x'> </i></div>").appendTo(textDiv);
			
			jQuery.ajax(
				{
					url : oaiURI + "/cite?metadataPrefix=html&handle=" + handle,
					context : document.body,
					dataType : 'text'
				}
			)
			.done(
					function(data) {
						var jdata_html = data;
						if(data.indexOf("<error code=")>0) {
							citeText.html("<a href='" + URI + "'>" + URI + "</a>")
						}else{
							jdata_html = ufal.citation.extract_metadata_html(jdata_html);
							citeText.html(ufal.citation.convert_metadata_to_html(jdata_html, "extract_metadata_html"));
						}
					}
			)
			.fail(
					function(data) {
						citeText.html("<a href='" + URI + "'>" + URI + "</a>")
					}
			);
						
			copyLink.click(ufal.citation.copy_click);
		},

		extract_metadata : function(xml_content) {
			return xml_content;
		},

		extract_metadata_bibtex : function(xml_content) {
			try {
				var xml = jQuery.parseXML(xml_content);
				var metadata = jQuery(xml.getElementsByTagNameNS(
						"http://lindat.mff.cuni.cz/ns/experimental/bibtex",
						"bibtex")[0]);
				if (!metadata) {
					throw "Not found."
				}
				return metadata.text();
			} catch (err) {
				return xml_content;
			}
		},

		extract_metadata_cmdi : function(xml_content) {
			return xml_content;
		},

		extract_metadata_html : function(xml_content) {
			try {
				var xml = jQuery.parseXML(xml_content);
				var metadata = jQuery(xml.getElementsByTagNameNS(
						"http://lindat.mff.cuni.cz/ns/experimental/html",
						"html")[0]).html();
				if (!metadata) {
					throw "Not found."
				}
				return metadata;
			} catch (err) {
				return xml_content;
			}
		},

		convert_metadata_to_html : function(metadata, name) {
			if(name == "extract_metadata_html") {1
				return metadata;
			}
			else {
				metadata = metadata.replace(/>/g, "&gt;").replace(/</g, "&lt;");
				return '<samp class="wordbreak linebreak">' + metadata + '</samp>';
			}
		},

		exporter_click : function(e) {
			e.preventDefault();
			var url = jQuery(this).attr("href");
			var name = "extract_metadata_" + jQuery(this).html().toLowerCase();
			var targ = jQuery(this).attr("data-target");
			var title = jQuery(this).attr("title");
			jQuery(targ + " .modal-body").html("<i class='fa fa-spinner fa-spin' style='margin: auto;'>&#160;</i>");
			jQuery(targ + " .modal-title").html(title + " (" + jQuery(this).html().toUpperCase() + ")");
			jQuery(targ).modal('show');
			jQuery
					.ajax({
						url : url,
						context : document.body,
						dataType : 'text'
					})
					.done(
							function(data) {
								var jdata_html = data;
								jdata_html = ufal.citation[name](jdata_html);
								jQuery(targ + " .modal-body").html(ufal.citation.convert_metadata_to_html(jdata_html, name));
							})
					.fail(
							function(data) {
								var jdata_html = data;
								jdata_html = ufal.citation.extract_metadata(jdata_html.responseText);
								if (jdata_html != null) {
									jQuery(targ + " .modal-body")
											.html('<samp class="wordbreak linebreak">'
													+ jdata_html
													+ '</samp>');
								} else {
									jQuery(
											targ + " .modal-body")
											.html('Failed to load requested data.');
								}
							});
		},
		
		copy_click : function(e) {
			e.preventDefault();
			var datadiv = jQuery(this).attr("data-div");			
			jQuery("#exporter_copy_div .modal-body textarea").text(jQuery(datadiv, jQuery(this).parent()).text());
			setTimeout(function(){
				jQuery("#exporter_copy_div .modal-body textarea").focus();
				jQuery("#exporter_copy_div .modal-body textarea").select();
			}, 300);
		}

};


jQuery(document).ready(function (){
	
	jQuery("<div id='exporter_model_div' class='modal fade'>" +
						"<div class='modal-dialog'>" +
							"<div class='modal-content'>" +
								"<div class='modal-header'>" +
									"<button type='button' class='close' data-dismiss='modal'><span aria-hidden='true'>&#215;</span>" +
									"<span class='sr-only'>Close</span></button>" +
									"<h4 class='modal-title'>&#160;</h4>" +
								"</div>" +
								"<div class='modal-body'><i class='fa fa-spinner fa-spin' style='margin: auto;'>&#160;</i></div>" +
							"</div>" +
						"</div>" +
					"</div>").appendTo("body");
	
	jQuery("<div class='modal fade' id='exporter_copy_div'>" +
			 "<div class='modal-dialog modal-sm'>" +
			 	"<div class='modal-content'>" +
		        "<div class='modal-header'>" +
		          "<button type='button' class='close' data-dismiss='modal'><span aria-hidden='true'>×</span><span class='sr-only'>Close</span></button>" +
		          "<h4 class='modal-title'>Press <kbd class='label label-default'><kbd>ctrl</kbd> + <kbd>c</kbd></kbd> to copy</h4>" +
		        "</div>" + 
		        "<div class='modal-body'>" +
		          "<textarea class='form-control' readonly style='height: 200px;'></textarea>" +
		        "</div>" +
		      "</div>" +			 			 
			 "</div>" +
			"</div>").appendTo("body");
		
	jQuery(".citationbox").each(function(){
		ufal.citation.citationBox(jQuery(this));
	});
});

