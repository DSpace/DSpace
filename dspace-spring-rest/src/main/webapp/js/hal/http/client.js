/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
HAL.Http.Client = function(opts) {
    this.vent = opts.vent;
    this.defaultHeaders = {'Accept': 'application/hal+json, application/json, */*; q=0.01'};
    var authorizationHeader = getAuthorizationHeader();
    authorizationHeader ? this.defaultHeaders.Authorization = authorizationHeader : '';
    console.log(this.defaultHeaders);
    this.headers = this.defaultHeaders;
};

function getAuthorizationHeader() {
    var cookie = document.cookie.match('(^|;)\\s*' + 'MyHalBrowserToken' + '\\s*=\\s*([^;]+)');
    if(cookie != undefined) {
        return 'Bearer ' + cookie.pop();
    } else {
        return undefined;
    }
}
function downloadFile(url) {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.responseType = 'blob';
    var authorizationHeader = getAuthorizationHeader();
    if (authorizationHeader != undefined) {
        request.setRequestHeader("Authorization", authorizationHeader);
    }
    request.onload = function () {
        // Only handle status code 200
        if (request.status === 200) {
            // Try to find out the filename from the content disposition `filename` value
            var disposition = request.getResponseHeader('content-disposition');
            var matches = /"([^"]*)"/.exec(disposition);
            var filename = (matches != null && matches[1] ? matches[1] : 'content');
            // The actual download
            var contentTypeHeader = request.getResponseHeader("content-type");
            if (contentTypeHeader === undefined || contentTypeHeader === "") {
                contentTypeHeader = "application/octet-stream";
            }
            var blob = new Blob([request.response], {type: contentTypeHeader});
            var link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
        // some error handling should be done here...
    };
    request.send();
}


HAL.Http.Client.prototype.get = function(url) {
    var self = this;
    this.vent.trigger('location-change', { url: url });
    var jqxhr = $.ajax({
        url: url,
        dataType: 'json',
        xhrFields: {
            withCredentials: true
        },
        headers: this.headers,
        success: function(resource, textStatus, jqXHR) {
            self.vent.trigger('response', {
                resource: resource,
                jqxhr: jqXHR,
                headers: jqXHR.getAllResponseHeaders()
            });
        }
    }).error(function (response) {
        self.vent.trigger('fail-response', {jqxhr: jqxhr});
        var contentTypeResponseHeader = jqxhr.getResponseHeader("content-type");
        if (contentTypeResponseHeader != undefined
                && !contentTypeResponseHeader.startsWith("application/hal")
                && !contentTypeResponseHeader.startsWith("application/json")) {
            downloadFile(url);
        }});
};

HAL.Http.Client.prototype.request = function(opts) {
    var self = this;
    opts.dataType = 'json';
    opts.xhrFields = opts.xhrFields || {};
    opts.xhrFields.withCredentials = opts.xhrFields.withCredentials || true;
    self.vent.trigger('location-change', { url: opts.url });
    return jqxhr = $.ajax(opts);
};

HAL.Http.Client.prototype.updateHeaders = function(headers) {
    this.headers = headers;
};

HAL.Http.Client.prototype.getHeaders = function() {
    return this.headers;
};
