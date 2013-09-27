/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
submissionLookupIdentifiers = function(identInputs){
	var mydata = new Object();
	mydata['s_uuid'] = j('#suuid').val();
	mydata['type'] = 'identifiers';
	for (var i=0;i<identInputs.length;i++)
	{
		mydata[j(identInputs[i]).attr('id')] = j(identInputs[i]).val();
	}
	var ajaxCall = j.ajax({url: dspaceContextPath+"/json/submissionLookup", 
		type: "POST",
		dataType: "json", 
		async: true,
		contentType: "application/x-www-form-urlencoded;charset=UTF-8", 
		data: mydata, 
		error: function(info){
			j('#loading-search-result').dialog("close");
			var message = j('#jserrormessage').text();
			alert(message);
		},
		success: function(info) {
			if (info == null || info.result == null || info.result.length == 0)
			{
				j('#result-list').hide();
				j('#empty-result').show();
			}
			else
			{
				submissionLookupShowResult(info);
			}
			j('#loading-search-result').dialog("close");
			j('#tabs').find('a[href="#tabs-result"]').click();
		}
	});
	j('#loading-search-result').data('ajaxCall', ajaxCall);
	j('#loading-search-result').dialog("open");
}

submissionLookupSearch = function(){
	var mydata = new Object();
	mydata['s_uuid'] = j('#suuid').val();
	mydata['type'] = 'search';
	mydata['title'] = j('#search_title').val();
	mydata['authors'] = j('#search_authors').val();
	mydata['year'] = j('#search_year').val();
	var ajaxCall = j.ajax({url: dspaceContextPath+"/json/submissionLookup", 
		type: "POST",
		dataType: "json", 
		async: true,
		contentType: "application/x-www-form-urlencoded;charset=UTF-8", 
		data: mydata, 
		error: function(info){
			j('#loading-search-result').dialog("close");
			var message = j('#jserrormessage').text();
			alert(message);
		},
		success: function(info) {
			if (info == null || info.result == null || info.result.length == 0)
			{
				j('#result-list').hide();
				j('#empty-result').show();
			}
			else
			{
				submissionLookupShowResult(info);
			}
			j('#loading-search-result').dialog("close");
			j('#tabs').find('a[href="#tabs-result"]').click();
		}
	});
	j('#loading-search-result').data('ajaxCall', ajaxCall);
	j('#loading-search-result').dialog("open");
}

submissionLookupDetails = function(button){
	var uuid = j(button).data('uuid');
	var mydata = new Object();
	mydata['s_uuid'] = j('#suuid').val();
	mydata['type'] = 'details';
	mydata['i_uuid'] = uuid;
	j.ajax({url: dspaceContextPath+"/json/submissionLookup", 
		type: "POST",
		dataType: "json", 
		async: false,
		contentType: "application/x-www-form-urlencoded;charset=UTF-8", 
		data: mydata, 
		error: function(info){var message = j('#jserrormessage').text();alert(message);},
		success: function(info) {
			if (info == null || info.result == null || info.result.uuid != uuid)
			{
				var message = j('#jserrormessage').text();
				alert(message);
			}
			else
			{
				submissionLookupShowDetails(info.result);
			}
			j('#tabs').find('a[href="#tabs-result"]').click();
		}
	});
}


submissionLookupShowResult = function(info){
	j('#result-list').show();
	j('#empty-result').hide();
	j('#result-list').html(" ");
	for (var i=0;i<info.result.length;i++)
	{
		var bt = j('<button type="button">').append(j('#jsseedetailsbuttonmessage').text());
		var par = j('<p class="sl-result">');
		var divImg = j('<div class="submission-lookup-providers">');
		par.append(divImg);
		for (var k=0;k<info.result[i].providers.length;k++)
		{
			var prov = info.result[i].providers[k];
			divImg.append(j('<img src="'+dspaceContextPath+'/image/submission-lookup-small-'+prov+'.jpg">'));
		}	
		par
				.append(j('<span class="sl-result-title">').text(info.result[i].title))
				.append(j('<span class="sl-result-authors">').text(info.result[i].authors))
				.append(j('<span class="sl-result-date">').text(info.result[i].issued))
				.append(bt);
		j('#result-list').append(par);
		bt.button();
		bt.data({uuid: info.result[i].uuid});
		bt.click(function(){
			submissionLookupDetails(this);
		});
	}
}

submissionLookupShowDetails = function(info){
	var popup = j('<div title="'+j('#jstitlepopupmessage').text()+'">');
	var divImg = j('<div class="submission-lookup-providers">');
	popup.append(divImg);
	for (var k=0;k<info.providers.length;k++)
	{
		var prov = info.providers[k];
		divImg.append(j('<img src="'+dspaceContextPath+'/image/submission-lookup-small-'+prov+'.jpg">'));
	}
	var detailsDiv = j('<div class="submission-lookup-details">');
	var details = j('<table>');
	detailsDiv.append(details);
	popup.append(detailsDiv);
	for (var i=0;i<info.fieldsLabels.length;i++)
	{
		var fieldName = info.fieldsLabels[i][0];
		var fieldLabel = info.fieldsLabels[i][1]; 
		var values = info.publication.storage[fieldName];
		var tr = j('<tr>');
		tr.append(j('<td class="submission-lookup-label">').append(fieldLabel));
		var td = j('<td>');
		tr.append(td);
		for (var k=0;k<values.length;k++)
		{
			td.append(j('<span>').text(values[k]));
			if (k != values.length-1)
			{
				td.append('<br>');
			}
		}
		details.append(tr);
	}
	popup.append(j('#select-collection-div'));
	j('#select-collection').val(info.collection);	
	var start = j('<button type="button">');
	start.append(j('#jsfilldatabuttonmessage').text());
	start.button();
	start.click(function(){
		j('#collectionid').val(j('#select-collection').val());
		j('#iuuid').val(info.uuid);
		j('#form-submission').submit();
	});
	popup.append(start);
	
	j('body').append(popup);
	popup.dialog({modal: true,width:800,height:600,
		 close: function( event, ui ) {
			 j('#hidden-area').append(j('#select-collection-div'));
			 j(this).dialog('destroy').remove();
		 }
	});
}