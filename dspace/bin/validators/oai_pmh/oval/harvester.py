# -*- coding: utf-8 -*-
"""
    harvester.py
    ~~~~~~~~~~~~

    Basic OAI-PMH harvesting utilities.


    :copyright: Copyright 2011 Mathias Loesch.
"""

OAI = '{http://www.openarchives.org/OAI/%s/}'

DC_NAMESPACE = "http://purl.org/dc/elements/1.1/"
DC = '{%s}' % DC_NAMESPACE


import time
from time import sleep
import hashlib
import pickle
import re

import urllib2
from urllib2 import URLError, Request
from urllib import urlencode

from functools import wraps

#from collections import OrderedDict
from oval import __version__ as ovalversion
from lxml import etree


CACHE = {}  # OrderedDict()

# Caching


def is_obsolete(entry, duration):
    return time.time() - entry['time'] > duration


def compute_key(function, args, kw):
    key = pickle.dumps((function.func_name, args, kw))
    return hashlib.sha1(key).hexdigest()


def memoize(duration=30, max_length=10):
    """Donald Michie's memo function for caching."""
    def _memoize(function):
        @wraps(function)
        def __memoize(*args, **kw):
            key = compute_key(function, args, kw)
            if len(CACHE) > max_length:
                # Pop the oldest item from the cache
                CACHE.popitem(last=False)
            # do we have a response for the request?
            if (key in CACHE and
                not is_obsolete(CACHE[key], duration)):
                return CACHE[key]['value']
            # new request
            result = function(*args, **kw)
            CACHE[key] = {
                            'value': result,
                            'time': time.time()
            }
            return result
        return __memoize
    return _memoize


def normalize_params(params):
    """Clean parameters in accordance with OAI-PMH.

    Explanation: OAI-PMH requires that the resumptionToken parameter be the
    exclusive argument.

    :param params: The HTTP parameters for a request.
    """
    if params.get('resumptionToken') is not None:
        #metadataPrefix/from/until not allowed if resumptionToken -> remove
        try:
            del params['metadataPrefix']
        except KeyError:
            pass
        try:
            del params['_from']
        except KeyError:
            pass
        try:
            del params['until']
        except KeyError:
            pass
    # from is a reserved word in Python; use _from instead
    if params.get("_from") is not None:
        params['from'] = params['_from']
        del params['_from']
    nparams = {}
    for param in params:
        if params[param] is not None:
            nparams[param] = params[param]
    return nparams


#@memoize()
def fetch_data(base_url, method, params, retries=5, timeout=None):
    """Perform actual request to the OAI interface and return the data
    as XML string.

       :param base_url: The endpoint of the OAI-PMH interface.
       :param method: The HTTP method to be used for the requests.
       :param params: The GET/POST variables.
       :param retries: How many retries should be performed on responses
                       with status code 503.
       :param timeout: The timeout in seconds for the requests.
    """
    data = urlencode(params)
    if method == 'POST':
        request = Request(base_url)
        request.add_data(data)
    elif method == 'GET':
        request = Request(base_url + data)
    request.add_header('User-Agent', 'oval/%s' % ovalversion)
    for _ in range(retries):
        try:
            response = urllib2.urlopen(request, None, timeout=timeout)
            return response.read()
        except URLError, e:
            if hasattr(e, 'reason'):
                raise
            elif hasattr(e, 'code'):
                if e.code == 503:
                    try:
                        wait_time = int(e.hdrs.get('Retry-After'))
                    except TypeError:
                        wait_time = None
                    if wait_time is None:
                        sleep(100)
                    else:
                        sleep(wait_time)
                else:
                    raise
        except Exception:
            raise


def configure_request(base_url, method='POST', timeout=None):
    """Closure to preconfigure the static request params. Return
    custom request_oai function.

    :param base_url: The endpoint of the OAI-PMH interface.
    :param method: The HTTP method to be used for the requests.
    :param timeout: The timeout in seconds for the requests.
    """
    def request_oai(**kw):
        """Perform OAI request to base_url. Return parsed response."""
        params = kw
        params = normalize_params(params)
        response = fetch_data(base_url, method, params, timeout=timeout)
        return etree.XML(response)
    return request_oai


def get_protocol_version(base_url, method):
    """Determine the version of the OAI-PMH spoken by the server.

    :param base_url: The URL of the OAI-PMH endpoint.
    :param method: The HTTP method for requests to the endpoint.
    """
    pversion_re = re.compile(r'<protocolVersion>(.*?)</protocolVersion>')
    try:
        response = fetch_data(base_url, method, {'verb': 'Identify'})
    except Exception:
        return None
    m = pversion_re.search(response)
    if m is not None:
        version = m.group(1)
        return version


def get_granularity(base_url, method):
    granularity_re = re.compile(r'<granularity>(.*?)</granularity>')
    try:
        response = fetch_data(base_url, method, {'verb': 'Identify'})
    except Exception:
        return None
    m = granularity_re.search(response)
    if m is not None:
        granularity = m.group(1)
        if granularity == 'YYYY-MM-DDThh:mm:ssZ':
            return 'full'
        elif granularity == 'YYYY-MM-DD':
            return 'day'


def check_HTTP_methods(base_url):
    """Determine the HTTP methods supported by the server. Return supported
    methods in list or [].

    :param base_url: The endpoint of the OAI-PMH interface.
    """
    methods = []
    for method in ['GET', 'POST']:
        response = None
        try:
            response = fetch_data(base_url, method, {'verb': 'Identify'})
        except Exception:
            pass
        if response and not "badVerb" in response:
            methods.append(method)
    return methods


def get_repository_information(base_url, method):
    name_re = re.compile(r'<repositoryName>(.*?)</repositoryName>')
    email_re = re.compile(r'<adminEmail>(.*?)</adminEmail>')
    try:
        response = fetch_data(base_url, method, {'verb': 'Identify'})
    except Exception:
        return ('[ERROR: Could not fetch Identify response]',
                '[ERROR: Could not fetch Identify response]')
    name_match = name_re.search(response)
    if name_match is None:
        name = '[Could not find name in Identify.]'
    else:
        name = name_match.group(1).decode('utf8')
    email_match = email_re.search(response)
    if email_match is None:
        email = '[Could not find email in Identify.]'
    else:
        email = email_match.group(1).decode('utf8')
    return name, email


def configure_record_iterator(base_url, protocol_version, HTTPmethod, timeout=None):
    """Class factory for record iterators.

       :param base_url: The endpoint of the OAI-PMH interface.
       :param protocol_version: The version of the OAI-PMH interface.
       :param HTTPmethod: The HTTP method supported by the server.
       :param timeout: Optional timeout for the HTTP requests sent to the server.
    """
    class RecordIterator(object):
        """Iterator over OAI records/identifiers transparently aggregated via
        OAI-PMH.

           :param verb: The OAI-PMH verb for the items to iterate over.
           :param metadataPrefix: The OAI-PMH metadataPrefix attribute.
           :param _from: Optional date offset.
           :param until: Optional date limit.
           :param deleted: Flag specifiying whether deleted records should be
                           included
        """
        def __init__(self, verb, metadataPrefix, _from=None, until=None,
                    deleted=False):
            self.base_url = base_url
            self.verb = verb
            self.metadataPrefix = metadataPrefix
            self._from = _from
            self.until = until
            self.deleted = deleted  # include deleted records?
            self.protocol_version = protocol_version
            self.HTTPmethod = HTTPmethod
            self.timeout = timeout

            #OAI namespace
            self.oai_namespace = OAI % self.protocol_version
            # record list
            self.record_list = []
            # resumptionToken
            self.token = None
            if self.verb == 'ListRecords':
                self.element = 'record'
            elif self.verb == 'ListIdentifiers':
                self.element = 'header'
            #Configure request method
            self.request_oai = configure_request(
                self.base_url, self.HTTPmethod, self.timeout)
            #Fetch the initial portion
            response = self.request_oai(verb=self.verb,
                        metadataPrefix=self.metadataPrefix,
                        _from=self._from, until=self.until,
                        resumptionToken=self.token)
            self.record_list = self._get_records(response)
            self.token = self._get_resumption_token(response)

        def __iter__(self):
            return self

        def _is_not_deleted(self, record):
            if self.element == 'record':
                header = record.find('.//' + self.oai_namespace + 'header')
            elif self.element == 'header':
                header = record  # work on header element directly in case of ListId
            if header.attrib.get('status') == 'deleted':
                return False
            else:
                return True

        def _get_resumption_token(self, xml_tree):
            token = xml_tree.find(
                './/' + self.oai_namespace + 'resumptionToken')
            if token is None:
                return None
            else:
                return token.text

        def _get_records(self, xml_tree):
            records = xml_tree.findall(
                './/' + self.oai_namespace + self.element)
            if self.deleted == False:
                records = filter(self._is_not_deleted, records)
            return records

        def _next_batch(self):
            while self.record_list == []:
                response = self.request_oai(verb=self.verb,
                            metadataPrefix=self.metadataPrefix,
                            _from=self._from, until=self.until,
                            resumptionToken=self.token)
                self.record_list = self._get_records(response)
                self.token = self._get_resumption_token(response)
                if self.record_list == [] and self.token is None:
                    raise StopIteration

        def next(self):
            if (len(self.record_list) == 0 and self.token is None):
                raise StopIteration
            elif len(self.record_list) == 0:
                self._next_batch()
            current_record = self.record_list.pop()
            return current_record
    return RecordIterator
