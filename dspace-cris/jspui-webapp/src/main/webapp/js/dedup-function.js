/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
function renderingDedupActions(data, type, full) {
	var obj = {};
	obj.actions=full.actions;
	obj.itemId=full.entityID;
	obj.label = full.actionsLabel;	
	return tmpl("tmpl-deduplication-actions", obj);
}

function renderingDedupSubmitter(data, type, full) {
	var firstname = data[0];
    var lastname = data[1];
    var display = firstname+' '+lastname;
	if (display  == "" || display  == null)
		return '<span class="label label-default">unknown</span>';
	return display;
}

function renderingDedupHandle(data, type, full) {
    var text = full.handle;
	if (text  == "" || text  == null)
		return '<span class="label label-default">N/A</span>';
	return '<a target="_blank" href="'+dspaceContextPath+'/handle/'+text+'">'+text+'</a>';
}

function renderingDedupYear(data, type, full) {
 var year = data;
	if (year != null && year == '9999')
		return tmpl("tmpl-deduplication-render-inpress",{});
	if (year == null)
		return '';
	return year;
}


function renderingDedupCollection(data, type, full) {
    var text = full.duplicateItem['collection'][1];
    var display = full.duplicateItem['collection'][0];
	if (text  == "" || text  == null)
		return 'N/A';
	return '<a href="'+dspaceContextPath+'/handle/'+text+'">'+display+'</a>';
}

function renderingDedupDetailHandle(text) {    
	if (text  == "" || text  == null)
		return 'N/A';
	return '<a target="_blank" href="'+dspaceContextPath+'/handle/'+text+'">'+text+'</a>';
}

function renderingDedupLastModified(data, type, full) {
	var currentTime = new Date(full.lastModified);
	var month = currentTime.getMonth() + 1;
	var day = currentTime.getDate();
	var year = currentTime.getFullYear();		
	var hr = currentTime.getHours();
	var min = currentTime.getMinutes(); 
	return (day + "/" + month + "/" + year + " "+(hr<10?"0"+hr:hr)+":"+(min<10?"0"+min:min));
}

function renderingDedupSummary(data, type, full) {
	var handle = ''+full.handle;
	var itemid = ''+full.entityID;
	var titolo = full.duplicateItem['dc.title'];
	var queryIdPart = (full.queryId) ? 'queryId='+full.queryId+'&' : '';
	var result = '';
	if (full.withdrawn) {
		result = '<strike>';
	}
	if(full.handle =='undefined' || full.handle =='' || full.handle == null){
		result += renderingDedupYear(full.duplicateItem['dc.date.issued'], type, full)+'. ';
	}
	else {
		result += '<a href="'+dspaceContextPath+'/handle/'+handle + '">' + 	renderingDedupYear(full.duplicateItem['dc.date.issued'], type, full)+'. ';
	}
	if(titolo!=null) {
		result = result + titolo;
	}
	else {
		result = result + tmpl('tmpl-deduplication-untitled',{});
	}
	
	if (full.duplicateItem['dc.identifier.doi']!=null && full.duplicateItem['dc.identifier.doi'].length > 0) {
		result = result + ". DOI:"+full.duplicateItem['dc.identifier.doi'];
	}
	if (full.duplicateItem['dc.description.lastpage']!=null && full.duplicateItem['dc.description.lastpage'].length > 0 || full.duplicateItem['dc.description.firstpage'] && full.duplicateItem['dc.description.firstpage'].length > 0) {
		if (full.duplicateItem['dc.description.firstpage'].length > 0) {
			result = result + ". pp."+full.duplicateItem['dc.description.firstpage'];
		}
		if (full.duplicateItem['dc.description.firstpage'].length > 0 && full.duplicateItem['dc.description.lastpage'].length > 0) {
			result = result + "-";
		}
		if (full.duplicateItem['dc.description.lastpage'].length > 0) {
			if (!(full.duplicateItem['dc.description.startpage'].length > 0)) {
				result = result + ". pp.";
			}	
			result = result + full.duplicateItem['dc.description.lastpage'];
		}
	}
	if (full.duplicateItem['dc.relation.ispartofbook']!=null && full.duplicateItem['dc.relation.ispartofbook'].length > 0) {
		result = result + ". In "+full.duplicateItem['dc.relation.ispartofbook'];
	}
	if (full.duplicateItem['dc.identifier.isbn']!=null && full.duplicateItem['dc.identifier.isbn'].length > 0) {
		result = result + " - ISBN:"+full.duplicateItem['dc.identifier.isbn'];
	}
	if (full.duplicateItem['dc.relation.ispartof']!=null && full.duplicateItem['dc.relation.ispartof'].length > 0) {
		result = result + ". In "+full.duplicateItem['dc.relation.ispartof'];
		if (full.duplicateItem['dc.identifier.issn'].length > 0) {
			result = result + " - ISSN:"+full.duplicateItem['dc.identifier.issn'];
		}
	}
	if (full.duplicateItem['dc.relation.ispartofseries']!=null && full.duplicateItem['dc.relation.ispartofseries'].length > 0) {
		result = result + ". In "+full.duplicateItem['dc.relation.ispartofseries'];
		if (full.duplicateItem['dc.relation.ispartofseries'].length > 0) {
			result = result + " - ISSN:"+full.duplicateItem['dc.identifier.issn'];
		}
	}
	if (full.duplicateItem['dc.relation.volume']!=null && full.duplicateItem['dc.relation.volume'].length > 0) {
		result = result + " vol. "+full.duplicateItem['dc.relation.volume'];
	}
	
	if(full.duplicateItem['dc.contributor.author']!=null && full.duplicateItem['dc.contributor.author'] != null)
	{
		var contrib = full.duplicateItem['dc.contributor.author'];
		if (contrib.length > 100) {
			contrib= contrib.substring(0, 100)+"<b>...</b>";
		}
		result = result + '<br/><em>'+contrib+'</em>';
	}
	
	if(full.handle =='undefined' || full.handle =='' || full.handle == null){
		result = result + '<br/>';
	}
	else {
		result = result + '</a><br/>';		
	}
	ref = '<dl class="dl-horizontal">';
	ref += '<dt>'+tmpl("tmpl-deduplication-label-owner",{})+'</dt><dd>' + renderingDedupSubmitter(full.duplicateItem['submitter'],type,full);
	ref += '<dt>'+tmpl("tmpl-deduplication-label-identifier",{})+'</dt><dd>';	
	
	if(full.handle =='undefined' || full.handle =='' || full.handle == null){
		ref = ref + 'ID:'+itemid;
	}else{
		ref = ref+ 'hdl:'+renderingDedupHandle(data, type, full);		
	}
	
	ref += '</dd>';
	var status = full.duplicateItem['status'];
	if (status == '0')
	{
		ref += '<dt>'+tmpl("tmpl-deduplication-statuslabel",{}) +'</dt><dd>' + tmpl("tmpl-deduplication-render-status-draft",{})+'</dd>';		
	}
	else if (status == '1')
	{
		ref += '<dt>'+tmpl("tmpl-deduplication-statuslabel",{}) +'</dt><dd>'+ tmpl("tmpl-deduplication-render-status-workflow",{})+'</dd>';
	}
	else if (status == '2')
	{
		ref += '<dt>'+tmpl("tmpl-deduplication-statuslabel",{}) +'</dt><dd>'+ tmpl("tmpl-deduplication-render-status-final",{})+'</dd>';
	}
	else if (status == '3')
	{
		ref += '<dt>'+tmpl("tmpl-deduplication-statuslabel",{}) +'</dt><dd>'+ tmpl("tmpl-deduplication-render-status-withdrawn",{})+'</dd>';
	}	
	
	result = result + ref + '</dl>';
	if (full.withdrawn) {
		result += '</strike>';
	}
	
	var note = duplicationData["dedupnotecontainer"]['' + full.entityID];
	if(note) {
		result += tmpl("tmpl-deduplication-submitternote", note);
	}

	return result;
}
