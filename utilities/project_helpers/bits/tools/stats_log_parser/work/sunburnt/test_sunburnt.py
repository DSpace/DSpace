from __future__ import absolute_import

try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

import cgi, datetime, urlparse

from lxml.builder import E
from lxml.etree import tostring

from .sunburnt import SolrInterface

from nose.tools import assert_equal

debug = False

schema_string = \
"""<schema name="timetric" version="1.1">
  <types>
    <fieldType name="string" class="solr.StrField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="text" class="solr.TextField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="boolean" class="solr.BoolField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="int" class="solr.IntField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="sint" class="solr.SortableIntField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="long" class="solr.LongField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="slong" class="solr.SortableLongField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="float" class="solr.FloatField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="sfloat" class="solr.SortableFloatField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="double" class="solr.DoubleField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="sdouble" class="solr.SortableDoubleField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="date" class="solr.DateField" sortMissingLast="true" omitNorms="true"/>
  </types>
  <fields>
    <field name="string_field" required="true" type="string" multiValued="true"/>
    <field name="text_field" required="true" type="text"/>
    <field name="boolean_field" required="false" type="boolean"/>
    <field name="int_field" required="true" type="int"/>
    <field name="sint_field" type="sint"/>
    <field name="long_field" type="long"/>
    <field name="slong_field" type="slong"/>
    <field name="long_field" type="long"/>
    <field name="slong_field" type="slong"/>
    <field name="float_field" type="float"/>
    <field name="sfloat_field" type="sfloat"/>
    <field name="double_field" type="double"/>
    <field name="sdouble_field" type="sdouble"/>
    <field name="date_field" type="date"/>
  </fields>
  <defaultSearchField>text_field</defaultSearchField>
  <uniqueKey>int_field</uniqueKey>
</schema>"""


class MockResponse(object):
    mock_doc_seeds = [
        (0, 'zero'),
        (1, 'one'),
        (2, 'two'),
        (3, 'three'),
        (4, 'four'),
        (5, 'five'),
        (6, 'six'),
        (7, 'seven'),
        (8, 'eight'),
        (9, 'nine'),
    ]
    mock_docs = [
        dict(zip(("int_field", "string_field"), m)) for m in mock_doc_seeds
    ]

    def __init__(self, start, rows):
        self.start = start
        self.rows = rows

    @staticmethod
    def xmlify_doc(d):
        return E.doc(
            E.int({'name':'int_field'}, str(d['int_field'])),
            E.str({'name':'string_field'}, d['string_field'])
        )

    def extra_response_parts(self):
        return []

    def xml_response(self):
        response_portions = [
            E.lst({'name':'responseHeader'},
                E.int({'name':'status'}, '0'), E.int({'name':'QTime'}, '0')
            ),
            E.result({'name':'response', 'numFound':str(len(self.mock_docs)), 'start':str(self.start)},
                *[self.xmlify_doc(doc) for doc in self.mock_docs[self.start:self.start+self.rows]]
            )
            ] + self.extra_response_parts()
        return tostring(E.response(*response_portions))


class MockConnection(object):
    class MockStatus(object):
        def __init__(self, status):
            self.status = status

    def __init__(self, tracking_dict=None):
        if tracking_dict is None:
            tracking_dict = {}
        self.tracking_dict = tracking_dict

    def request(self, uri, method='GET', body=None, headers=None):

        u = urlparse.urlparse(uri)
        params = cgi.parse_qs(u.query)

        self.tracking_dict.update(url=uri,
                                  params=params,
                                  method=method,
                                  body=body or '',
                                  headers=headers or {})

        if method == 'GET' and u.path.endswith('/admin/file/') and params.get("file") == ["schema.xml"]:
            return self.MockStatus(200), schema_string

        rc = self._handle_request(u, params, method, body, headers)
        if rc is not None:
            return rc

        raise ValueError("Can't handle this URI")

class PaginationMockConnection(MockConnection):
    def _handle_request(self, uri_obj, params, method, body, headers):
        if method == 'GET' and uri_obj.path.endswith('/select/'):
            start = int(params.get("start", [0])[0])
            rows = int(params.get("rows", [10])[0])
            return self.MockStatus(200), MockResponse(start, rows).xml_response()


conn = SolrInterface("http://test.example.com/", http_connection=PaginationMockConnection())

pagination_slice_tests = (
((None, None), range(0, 10),
    (slice(None, None, None),
     slice(0, 10, None),
     slice(0, 10, 1),
     slice(0, 5, None),
     slice(5, 10, None),
     slice(0, 5, 2),
     slice(5, 10, 2),
     slice(9, None, -1),
     slice(None, 0, -1),
     slice(7, 3, -2),
    # out of range but ok
     slice(0, 12, None),
     slice(-100, 12, None),
    # out of range but empty
     slice(12, 20, None),
     slice(-100, -90),
    # negative offsets
     slice(0, -1, None),
     slice(-5, -1, None),
     slice(-1, -5, -1),
    # zero-range produced
     slice(10, 0, None),
     slice(0, 10, -1),
     slice(0, -3, -1),
     slice(-5, -9, None),
     slice(-9, -5, -1))),

### and now with pre-paginated queries:
((2, 6), range(2, 8),
    (slice(None, None, None),
     slice(0, 6, None),
     slice(0, 6, 1),
     slice(0, 5, None),
     slice(5, 6, None),
     slice(0, 5, 2),
     slice(3, 6, 2),
     slice(5, None, -1), 
     slice(None, 0, -1),
    # out of range but ok
     slice(0, 12, None),
     slice(-100, 12, None),
    # negative offsets
     slice(0, -1, None),
     slice(-3, -1, None),
     slice(-1, -3, -1),
    # zero-range produced
     slice(6, 0, None),
     slice(0, 6, -1),
     slice(0, -3, -1),
     slice(-2, -5, None),
     slice(-5, -2, -1))),
)

def check_slice_pagination(p_args, a, s):
    assert [d['int_field'] for d in conn.query("*").paginate(*p_args)[s]] == a[s]

def test_slice_pagination():
    for p_args, a, slices in pagination_slice_tests:
        for s in slices:
            yield check_slice_pagination, p_args, a, s

# indexing to cells

# IndexErrors as appropriate

pagination_index_tests = (
((None, None), range(0, 10),
   ((0, None),
    (5, None),
    (9, None),
    (-1, None),
    (-5, None),
    (-9, None),
    (10, IndexError),
    (20, IndexError),
    (-10, IndexError),
    (-20, IndexError))),
((2, 6), range(2, 8),
   ((0, None),
    (3, None),
    (5, None),
    (-1, None),
    (-3, None),
    (-6, None),
    (6, IndexError),
    (20, IndexError),
    (-7, IndexError),
    (-20, IndexError))),
)

def check_index_pagination(p_args, a, s, e):
    if e is None:
        assert conn.query("*").paginate(*p_args)[s]['int_field'] == a[s]
    else:
        q = conn.query("*").paginate(*p_args)
        try:
            q[s]
        except IndexError:
            pass

def test_index_pagination():
    for p_args, a, slices in pagination_index_tests:
        for s, e in slices:
            yield check_index_pagination, p_args, a, s, e


class MLTMockConnection(MockConnection):
    def _handle_request(self, u, params, method, body, headers):
        return self.MockStatus(200), MockResponse(1, 2).xml_response()


mlt_query_tests = (
        # basic query
        (("Content", None, None), ({'stream.body': ['Content'], 'mlt.fl': ['text_field']}, 'GET', ''), None),
        (("Content with space", None, None), ({'stream.body': ['Content with space'], 'mlt.fl': ['text_field']}, 'GET', ''), None),
        ((None, None, "http://source.example.com"), ({'stream.url': ['http://source.example.com'], 'mlt.fl': ['text_field']}, 'GET', ''), None),
        (("long "*1024+"content", None, None), ({'mlt.fl': ['text_field']}, 'POST', 'long '*1024+"content"), None),
        (("Content", None, "http://source.example.com"), (), ValueError),
        ((None, None, None), ({'mlt.fl': ['text_field']}, 'GET', ''), None),
        (('Content', 'not-an-encoding', None), (), LookupError),
        ((u'Content', None, None), ({'stream.body': ['Content'], 'mlt.fl': ['text_field']}, 'GET', ''), None),
        (('Cont\xe9nt', 'iso-8859-1', None), ({'stream.body': ['Cont\xc3\xa9nt'], 'mlt.fl': ['text_field']}, 'GET', ''), None),
        )

def check_mlt_query(i, o, E):
    if E is None:
        query_params, method, body = o
    content, content_charset, url = i
    d = {}
    conn = SolrInterface("http://test.example.com/", http_connection=MLTMockConnection(d))
    if E is None:
        conn.mlt_query(content=content, content_charset=content_charset, url=url).execute()
        assert_equal(d['params'], query_params)
        assert_equal(d['method'], method)
        assert_equal(d['body'], body)
    else:
        try:
            conn.mlt_query(content=content, content_charset=content_charset, url=url).execute()
        except E:
            pass
        else:
            assert False

def test_mlt_queries():
    for i, o, E in mlt_query_tests:
        yield check_mlt_query, i, o, E
