/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
/**
 * Stats Utils
 */
function getDateISO8601(str) {
	// we assume str is a UTC date ending in 'Z'

	var parts = str.split('T'), dateParts = parts[0].split('-'), timeParts = parts[1]
			.split('Z'), timeSubParts = timeParts[0].split(':'), timeSecParts = timeSubParts[2]
			.split('.'), timeHours = Number(timeSubParts[0]), _date = new Date;

	_date.setUTCFullYear(Number(dateParts[0]));
	_date.setUTCMonth(Number(dateParts[1]) - 1);
	_date.setUTCDate(Number(dateParts[2]));
	return Date.UTC(_date.setUTCFullYear(Number(dateParts[0])),
			(Number(dateParts[1]) - 1), Number(dateParts[2]));
}
function parseISO8601(str) {
	// we assume str is a UTC date ending in 'Z'

	var parts = str.split('T'), dateParts = parts[0].split('-'), timeParts = parts[1]
			.split('Z'), timeSubParts = timeParts[0].split(':'), timeSecParts = timeSubParts[2]
			.split('.'), timeHours = Number(timeSubParts[0]), _date = new Date;

	_date.setUTCFullYear(Number(dateParts[0]));
	_date.setUTCMonth(Number(dateParts[1]) - 1);
	_date.setUTCDate(Number(dateParts[2]));
	_date.setUTCHours(Number(timeHours));
	_date.setUTCMinutes(Number(timeSubParts[1]));
	_date.setUTCSeconds(Number(timeSecParts[0]));
	if (timeSecParts[1])
		_date.setUTCMilliseconds(Number(timeSecParts[1]));

	// by using setUTC methods the date has already been converted to local
	// time(?)
	// alert(_date);
	return _date;
}

function getImgData(chartContainer, indexMimeType) {
    var chartAreap = chartContainer.getElementsByTagName('svg');
    var chartAreapp = chartAreap[0];
    var chartArea = chartAreapp.parentNode;
    var svg = chartArea.innerHTML;
    var doc = chartContainer.ownerDocument;
    var canvas = doc.createElement('canvas');
    canvas.setAttribute('width', chartArea.offsetWidth);
    canvas.setAttribute('height', chartArea.offsetHeight);

    canvas.setAttribute(
        'style',
        'position: absolute; ' +
        'top: ' + (-chartArea.offsetHeight * 2) + 'px;' +
        'left: ' + (-chartArea.offsetWidth * 2) + 'px;');
    doc.body.appendChild(canvas);
    canvg(canvas, svg);
    var imgData = canvas.toDataURL(indexMimeType);
        
    canvas.parentNode.removeChild(canvas);
    return imgData;
  }

function saveAsImg(chartContainer, indexMimeType, ahrefcontainer) {	
	var mimetypesupported = ["image/png", "image/jpeg"];
	var mimetype = mimetypesupported[indexMimeType];
	var imgData = getImgData(chartContainer, mimetype);
	// Replacing the mime-type will force the browser to trigger a download
	// rather than displaying the image in the browser window.
	//window.location = imgData.replace(mimetype, "image/octet-stream");
	j('#'+ahrefcontainer).attr("href", imgData);	
}

function convertToImg(chartContainer, indexMimeType) {	
	var mimetypesupported = ["image/png", "image/jpeg"];
	var mimetype = mimetypesupported[indexMimeType];
	var imgData = getImgData(chartContainer, mimetype);
	var w=window.open('about:blank');
	w.document.write("<div style='background-color: yellow;'>Sorry, we detect that is using IE 9 or below. To download image, in this case, please with mouse point on image below and click right button to use browser download functionality. Thank you.</div><img src='"+imgData+"' alt='Stats from canvas'/>");	
}