from __future__ import absolute_import

import cgi
import cStringIO as StringIO
from itertools import islice
import logging
import socket, time, urllib, urlparse
import warnings


from .schema import SolrSchema, SolrError
from .search import LuceneQuery, MltSolrSearch, SolrSearch, params_from_dict

MAX_LENGTH_GET_URL = 2048
# Jetty default is 4096; Tomcat default is 8192; picking 2048 to be conservative.

class SolrConnection(object):
    def __init__(self, url, http_connection, retry_timeout, max_length_get_url):
        if http_connection:
            self.http_connection = http_connection
        else:
            import httplib2
            self.http_connection = httplib2.Http()
        self.url = url.rstrip("/") + "/"
        self.update_url = self.url + "update/"
        self.select_url = self.url + "select/"
        self.mlt_url = self.url + "mlt/"
        self.retry_timeout = retry_timeout
        self.max_length_get_url = max_length_get_url

    def request(self, *args, **kwargs):
        try:
            return self.http_connection.request(*args, **kwargs)
        except socket.error:
            if self.retry_timeout < 0:
                raise
            time.sleep(self.retry_timeout)
            return self.http_connection.request(*args, **kwargs)

    def commit(self, waitSearcher=None, expungeDeletes=None, softCommit=None):
        response = self.update('<commit/>', commit=True,
                waitSearcher=waitSearcher, expungeDeletes=expungeDeletes, softCommit=softCommit)

    def optimize(self, waitSearcher=None, maxSegments=None):
        response = self.update('<optimize/>', optimize=True,
            waitSearcher=waitSearcher, maxSegments=maxSegments)

    # For both commit & optimize above, we use the XML body instead
    # of the URL parameter, because if we're using POST (which we
    # should) then only the former works.

    def rollback(self):
        response = self.update("<rollback/>")

    def update(self, update_doc, **kwargs):
        body = update_doc
        if body:
            headers = {"Content-Type":"text/xml; charset=utf-8"}
        else:
            headers = {}
        url = self.url_for_update()
        r, c = self.request(url, method="POST", body=body,
                            headers=headers)
        if r.status != 200:
            raise SolrError(r, c)

    def url_for_update(self, commit=None, commitWithin=None, softCommit=None, optimize=None, waitSearcher=None, expungeDeletes=None, maxSegments=None):
        extra_params = {}
        if commit is not None:
            extra_params['commit'] = "true" if commit else "false"
        if commitWithin is not None:
            try:
                extra_params['commitWithin'] = str(int(commitWithin))
            except (TypeError, ValueError):
                raise ValueError("commitWithin should be a number in milliseconds")
            if extra_params['commitWithin'] < 0:
                raise ValueError("commitWithin should be a number in milliseconds")
        if softCommit is not None:
            extra_params['softCommit'] = "true" if softCommit else "false"
        if optimize is not None:
            extra_params['optimize'] = "true" if optimize else "false"
        if waitSearcher is not None:
            extra_params['waitSearcher'] = "true" if waitSearcher else "false"
        if expungeDeletes is not None:
            extra_params['expungeDeletes'] = "true" if expungeDeletes else "false"
        if maxSegments is not None:
            try:
                extra_params['maxSegments'] = str(int(maxSegments))
            except (TypeError, ValueError):
                raise ValueError("maxSegments")
            if extra_params['maxSegments'] <= 0:
                raise ValueError("maxSegments should be a positive number")
        if 'expungeDeletes' in extra_params and 'commit' not in extra_params:
            raise ValueError("Can't do expungeDeletes without commit")
        if 'maxSegments' in extra_params and 'optimize' not in extra_params:
            raise ValueError("Can't do maxSegments without optimize")
        if extra_params:
            return "%s?%s" % (self.update_url, urllib.urlencode(sorted(extra_params.items())))
        else:
            return self.update_url

    def select(self, params):
        qs = urllib.urlencode(params)
        url = "%s?%s" % (self.select_url, qs)
        if len(url) > self.max_length_get_url:
            warnings.warn("Long query URL encountered - POSTing instead of "
                "GETting. This query will not be cached at the HTTP layer")
            url = self.select_url
            kwargs = dict(
                method="POST",
                body=qs,
                headers={"Content-Type": "application/x-www-form-urlencoded"},
            )
        else:
            kwargs = dict(method="GET")
        r, c = self.request(url, **kwargs)
        if r.status != 200:
            raise SolrError(r, c)
        return c

    def mlt(self, params, content=None):
        """Perform a MoreLikeThis query using the content specified
        There may be no content if stream.url is specified in the params.
        """
        qs = urllib.urlencode(params)
        base_url = "%s?%s" % (self.mlt_url, qs)
        if content is None:
            kwargs = {'uri': base_url, 'method': "GET"}
        else:
            get_url = "%s&stream.body=%s" % (base_url, urllib.quote_plus(content))
            if len(get_url) <= self.max_length_get_url:
                kwargs = {'uri': get_url, 'method': "GET"}
            else:
                kwargs = {'uri': base_url, 'method': "POST",
                    'body': content, 'headers': {"Content-Type": "text/plain; charset=utf-8"}}
        r, c = self.request(**kwargs)
        if r.status != 200:
            raise SolrError(r, c)
        return c


class SolrInterface(object):
    readable = True
    writeable = True
    remote_schema_file = "admin/file/?file=schema.xml"
    def __init__(self, url, schemadoc=None, http_connection=None, mode='', retry_timeout=-1, max_length_get_url=MAX_LENGTH_GET_URL):
        self.conn = SolrConnection(url, http_connection, retry_timeout, max_length_get_url)
        self.schemadoc = schemadoc
        if mode == 'r':
            self.writeable = False
        elif mode == 'w':
            self.readable = False
        self.init_schema()

    def init_schema(self):
        if self.schemadoc:
            schemadoc = self.schemadoc
        else:
            r, c = self.conn.request(
                urlparse.urljoin(self.conn.url, self.remote_schema_file))
            if r.status != 200:
                raise EnvironmentError("Couldn't retrieve schema document from server - received status code %s\n%s" % (r.status, c))
            schemadoc = StringIO.StringIO(c)
        self.schema = SolrSchema(schemadoc)

    def add(self, docs, chunk=100, **kwargs):
        if not self.writeable:
            raise TypeError("This Solr instance is only for reading")
        if hasattr(docs, "items") or not hasattr(docs, "__iter__"):
            docs = [docs]
        # to avoid making messages too large, we break the message every
        # chunk docs.
        for doc_chunk in grouper(docs, chunk):
            update_message = self.schema.make_update(doc_chunk, **kwargs)
            self.conn.update(str(update_message), **kwargs)

    def delete(self, docs=None, queries=None, **kwargs):
        if not self.writeable:
            raise TypeError("This Solr instance is only for reading")
        if not docs and not queries:
            raise SolrError("No docs or query specified for deletion")
        elif docs is not None and (hasattr(docs, "items") or not hasattr(docs, "__iter__")):
            docs = [docs]
        delete_message = self.schema.make_delete(docs, queries)
        self.conn.update(str(delete_message), **kwargs)

    def commit(self, *args, **kwargs):
        if not self.writeable:
            raise TypeError("This Solr instance is only for reading")
        self.conn.commit(*args, **kwargs)

    def optimize(self, *args, **kwargs):
        if not self.writeable:
            raise TypeError("This Solr instance is only for reading")
        self.conn.optimize(*args, **kwargs)

    def rollback(self):
        if not self.writeable:
            raise TypeError("This Solr instance is only for reading")
        self.conn.rollback()

    def delete_all(self):
        if not self.writeable:
            raise TypeError("This Solr instance is only for reading")
        # When deletion is fixed to escape query strings, this will need fixed.
        self.delete(queries=self.Q(**{"*":"*"}))

    def search(self, **kwargs):
        if not self.readable:
            raise TypeError("This Solr instance is only for writing")
        params = params_from_dict(**kwargs)
        return self.schema.parse_response(self.conn.select(params))

    def query(self, *args, **kwargs):
        if not self.readable:
            raise TypeError("This Solr instance is only for writing")
        q = SolrSearch(self)
        if len(args) + len(kwargs) > 0:
            return q.query(*args, **kwargs)
        else:
            return q

    def mlt_search(self, content=None, **kwargs):
        if not self.readable:
            raise TypeError("This Solr instance is only for writing")
        params = params_from_dict(**kwargs)
        return self.schema.parse_response(self.conn.mlt(params, content=content))

    def mlt_query(self, fields=None, content=None, content_charset=None, url=None, query_fields=None,
                  **kwargs):
        """Perform a similarity query on MoreLikeThisHandler

        The MoreLikeThisHandler is expected to be registered at the '/mlt'
        endpoint in the solrconfig.xml file of the server.

        fields is the list of field names to compute similarity upon. If not
        provided, we just use the default search field.
        query_fields can be used to adjust boosting values on a subset of those
        fields.

        Other MoreLikeThis specific parameters can be passed as kwargs without
        the 'mlt.' prefix.
        """
        if not self.readable:
            raise TypeError("This Solr instance is only for writing")
        q = MltSolrSearch(self, content=content, content_charset=content_charset, url=url)
        return q.mlt(fields=fields, query_fields=query_fields, **kwargs)

    def Q(self, *args, **kwargs):
        q = LuceneQuery(self.schema)
        q.add(args, kwargs)
        return q


def grouper(iterable, n):
    "grouper('ABCDEFG', 3) --> [['ABC'], ['DEF'], ['G']]"
    i = iter(iterable)
    g = list(islice(i, 0, n))
    while g:
        yield g
        g = list(islice(i, 0, n))
