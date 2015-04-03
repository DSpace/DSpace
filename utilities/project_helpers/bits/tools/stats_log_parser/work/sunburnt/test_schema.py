from __future__ import absolute_import

import cStringIO as StringIO
import datetime
import uuid

try:
    import mx.DateTime
    HAS_MX_DATETIME = True
except ImportError:
    HAS_MX_DATETIME = False
import pytz

from .schema import solr_date, SolrSchema, SolrError, SolrUpdate, SolrDelete
from .search import LuceneQuery

debug = False

not_utc = pytz.timezone('Etc/GMT-3')

samples_from_pydatetimes = {
    "2009-07-23T03:24:34.000376Z":
        [datetime.datetime(2009, 07, 23, 3, 24, 34, 376),
         datetime.datetime(2009, 07, 23, 3, 24, 34, 376, pytz.utc)],
    "2009-07-23T00:24:34.000376Z":
        [not_utc.localize(datetime.datetime(2009, 07, 23, 3, 24, 34, 376)),
         datetime.datetime(2009, 07, 23, 0, 24, 34, 376, pytz.utc)],
    "2009-07-23T03:24:34Z":
        [datetime.datetime(2009, 07, 23, 3, 24, 34),
         datetime.datetime(2009, 07, 23, 3, 24, 34, tzinfo=pytz.utc)],
    "2009-07-23T00:24:34Z":
        [not_utc.localize(datetime.datetime(2009, 07, 23, 3, 24, 34)),
         datetime.datetime(2009, 07, 23, 0, 24, 34, tzinfo=pytz.utc)]
    }

if HAS_MX_DATETIME:
    samples_from_mxdatetimes = {
        "2009-07-23T03:24:34.000376Z":
            [mx.DateTime.DateTime(2009, 07, 23, 3, 24, 34.000376),
             datetime.datetime(2009, 07, 23, 3, 24, 34, 376, pytz.utc)],
        "2009-07-23T03:24:34Z":
            [mx.DateTime.DateTime(2009, 07, 23, 3, 24, 34),
             datetime.datetime(2009, 07, 23, 3, 24, 34, tzinfo=pytz.utc)],
        }


samples_from_strings = {
    # These will not have been serialized by us, but we should deal with them
    "2009-07-23T03:24:34Z":
        datetime.datetime(2009, 07, 23, 3, 24, 34, tzinfo=pytz.utc),
    "2009-07-23T03:24:34.1Z":
        datetime.datetime(2009, 07, 23, 3, 24, 34, 100000, pytz.utc),
    "2009-07-23T03:24:34.123Z":
        datetime.datetime(2009, 07, 23, 3, 24, 34, 123000, pytz.utc)
    }

def check_solr_date_from_date(s, date, canonical_date):
    assert unicode(solr_date(date)) == s, "Unequal representations of %r: %r and %r" % (date, unicode(solr_date(date)), s)
    check_solr_date_from_string(s, canonical_date)

def check_solr_date_from_string(s, date):
    assert solr_date(s)._dt_obj == date

def test_solr_date_from_pydatetimes():
    for k, v in samples_from_pydatetimes.items():
        yield check_solr_date_from_date, k, v[0], v[1]

def test_solr_date_from_mxdatetimes():
    if HAS_MX_DATETIME:
        for k, v in samples_from_mxdatetimes.items():
            yield check_solr_date_from_date, k, v[0], v[1]

def test_solr_date_from_strings():
    for k, v in samples_from_strings.items():
        yield check_solr_date_from_string, k, v


good_schema = \
"""
<schema name="timetric" version="1.1">
  <types>
    <fieldType name="sint" class="solr.SortableIntField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="string" class="solr.StrField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="boolean" class="solr.BoolField" sortMissingLast="true" omitNorms="true"/>
    <fieldType name="location_rpt" class="solr.SpatialRecursivePrefixTreeFieldType" geo="true" distErrPct="0.025" maxDistErr="0.000009" units="degrees" />
  </types>
  <fields>
    <field name="int_field" required="true" type="sint"/>
    <field name="text_field" required="true" type="string" multiValued="true"/>
    <field name="boolean_field" required="false" type="boolean"/>
    <field name="location_field" required="false" type="location_rpt"/>
  </fields>
  <defaultSearchField>text_field</defaultSearchField>
  <uniqueKey>int_field</uniqueKey>
 </schema>
"""

class TestReadingSchema(object):
    def setUp(self):
        self.schema = StringIO.StringIO(good_schema)
        self.s = SolrSchema(self.schema)

    def test_read_schema(self):
        """ Test that we can read in a schema correctly,
        that we get the right set of fields, the right
        default field, and the right unique key"""
        assert set(self.s.fields.keys()) \
            == set(['boolean_field',
                    'int_field',
                    'text_field',
                    'location_field'])
        assert self.s.default_field_name == 'text_field'
        assert self.s.unique_key == 'int_field'

    def test_serialize_dict(self):
        """ Test that each of the fields will serialize the relevant
        datatype appropriately."""
        for k, v, v2 in (('int_field', 1, u'1'),
                         ('text_field', 'text', u'text'),
                         ('text_field', u'text', u'text'),
                         ('boolean_field', True, u'true'),
                         ('location_field', 'POINT (30 10)', 'POINT (30 10)')):
            assert self.s.field_from_user_data(k, v).to_solr() == v2

    def test_missing_fields(self):
        assert set(self.s.missing_fields([])) \
            == set(['int_field', 'text_field'])
        assert set(self.s.missing_fields(['boolean_field'])) \
            == set(['int_field', 'text_field'])
        assert set(self.s.missing_fields(['int_field'])) == set(['text_field'])

    def test_serialize_value_list_fails_with_bad_field_name(self):
        try:
            self.s.field_from_user_data('text_field2', "a")
        except SolrError:
            pass
        else:
            assert False

    def test_serialize_value_list_fails_when_wrong_datatype(self):
        try:
            self.s.field_from_user_data('int_field', "a")
        except SolrError:
            pass
        else:
            assert False

    def test_unknown_field_type(self):
        """ Check operation of a field type that is unknown to Sunburnt.
        """
        assert 'solr.SpatialRecursivePrefixTreeFieldType' \
                not in SolrSchema.solr_data_types
        field = self.s.fields['location_field']
        assert field

        #Boolean attributes are converted accordingly
        assert field.geo == True
        #All other attributes are strings
        assert field.units == 'degrees'
        assert field.distErrPct == '0.025'
        assert field.maxDistErr == '0.000009'

        #Test that the value is always consistent - both to and from Solr
        value = 'POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))'
        assert field.to_user_data(value) \
                == field.from_user_data(value) \
                == field.to_solr(value) \
                == field.from_solr(value)

        #Queried values will be escaped accordingly
        assert field.to_query(value) == u'POLYGON\\ \\(\\(30\\ 10,\\ 10\\ 20,\\ 20\\ 40,\\ 40\\ 40,\\ 30\\ 10\\)\\)'


broken_schemata = {
"missing_name":
"""
<schema name="timetric" version="1.1">
  <types>
    <fieldType name="sint" class="solr.SortableIntField" sortMissingLast="true" omitNorms="true"/>
  </types>
  <fields>
    <field required="true" type="sint"/>
  </fields>
 </schema>
""",
"missing_type":
"""
<schema name="timetric" version="1.1">
  <types>
    <fieldType name="sint" class="solr.SortableIntField" sortMissingLast="true" omitNorms="true"/>
  </types>
  <fields>
    <field name="int_field" required="true"/>
  </fields>
 </schema>
""",
"misnamed_type":
"""
<schema name="timetric" version="1.1">
  <types>
    <fieldType name="sint" class="solr.SortableIntField" sortMissingLast="true" omitNorms="true"/>
  </types>
  <fields>
    <field name="int_field" required="true" type="sint2"/>
  </fields>
 </schema>
""",
"invalid XML":
"kjhgjhg"
}

def check_broken_schemata(n, s):
    try:
        SolrSchema(StringIO.StringIO(s))
    except SolrError:
        pass
    else:
        assert False

def test_broken_schemata():
    for k, v in broken_schemata.items():
        yield check_broken_schemata, k, v


class D(object):
    def __init__(self, int_field, text_field=None, my_arse=None):
        self.int_field = int_field
        if text_field:
            self.text_field = text_field
        if my_arse:
            self.my_arse = my_arse


class StringWrapper(object):
    def __init__(self, s):
        self.s = s

    def __unicode__(self):
        return self.s


class D_with_callables(object):
    def __init__(self, int_field, text_field=None, my_arse=None):
        self._int_field = int_field
        if text_field:
            self._text_field = text_field
        if my_arse:
            self._my_arse = my_arse

    def int_field(self):
        return self._int_field

    def text_field(self):
        return self._text_field

    def my_arse(self):
        return self._my_arse


update_docs = [
    # One single dictionary, not making use of multivalued field
    ({"int_field":1, "text_field":"a"},
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc></add>"""),
    # One single dictionary, with multivalued field
    ({"int_field":1, "text_field":["a", "b"]},
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field><field name="text_field">b</field></doc></add>"""),
    # List of dictionaries
    ([{"int_field":1, "text_field":"a"}, {"int_field":2, "text_field":"b"}],
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc><doc><field name="int_field">2</field><field name="text_field">b</field></doc></add>"""),
    # One single object, not making use of multivalued fields
    (D(1, "a"),
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc></add>"""),
    # One single object, with multivalued field
    (D(1, ["a", "b"]),
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field><field name="text_field">b</field></doc></add>"""),
    # List of objects
    ([D(1, "a"), D(2, "b")],
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc><doc><field name="int_field">2</field><field name="text_field">b</field></doc></add>"""),
    # Mixed list of objects & dictionaries
    ([D(1, "a"), {"int_field":2, "text_field":"b"}],
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc><doc><field name="int_field">2</field><field name="text_field">b</field></doc></add>"""),

    # object containing key to be ignored
    (D(1, "a", True),
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc></add>"""),

    # Make sure we distinguish strings and lists
    ({"int_field":1, "text_field":"abcde"},
      """<add><doc><field name="int_field">1</field><field name="text_field">abcde</field></doc></add>"""),

    # Check attributes which are objects to be converted.
    (D(1, StringWrapper("a"), True),
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc></add>"""),

    # Check attributes which are callable methods.
    (D_with_callables(1, "a", True),
     """<add><doc><field name="int_field">1</field><field name="text_field">a</field></doc></add>"""),

    # Check that strings aren't query-escaped
    (D(1, "a b", True),
     """<add><doc><field name="int_field">1</field><field name="text_field">a b</field></doc></add>"""),
    ]

def check_update_serialization(s, obj, xml_string):
    p = str(SolrUpdate(s, obj))
    if debug:
        try:
            assert p == xml_string
        except AssertionError:
            print p
            print xml_string
            import pdb;pdb.set_trace()
    else:
        assert p == xml_string

def test_update_serialization():
    s = SolrSchema(StringIO.StringIO(good_schema))
    for obj, xml_string in update_docs:
        yield check_update_serialization, s, obj, xml_string

bad_updates = [
    # Dictionary containing bad field name
    {"int_field":1, "text_field":"a", "my_arse":True},
    # Dictionary missing required field name
    {"int_field":1},
    # Object missing required field_name
    D(1),
    ]

def check_broken_updates(s, obj):
    try:
        SolrUpdate(s, obj)
    except SolrError:
        pass
    else:
        assert False

def test_bad_updates():
    s = SolrSchema(StringIO.StringIO(good_schema))
    for obj in bad_updates:
        yield check_broken_updates, s, obj


delete_docs = [
    # One single string for id
    ("1",
     """<delete><id>1</id></delete>"""),
    # One single int as id
    (1,
     """<delete><id>1</id></delete>"""),
    # List of string ids
    (["1", "2", "3"],
     """<delete><id>1</id><id>2</id><id>3</id></delete>"""),
    # Mixed list of string and int ids
    (["1", 2, "3"],
     """<delete><id>1</id><id>2</id><id>3</id></delete>"""),
    # Dictionary
    ({"int_field":1, "text_field":"a"},
     """<delete><id>1</id></delete>"""),
    # List of dictionaries
    ([{"int_field":1, "text_field":"a"}, {"int_field":2, "text_field":"b"}],
     """<delete><id>1</id><id>2</id></delete>"""),
    # Object
    (D(1, "a"),
     """<delete><id>1</id></delete>"""),
    # List of objects
    ([D(1, "a"), D(2, "b")],
     """<delete><id>1</id><id>2</id></delete>"""),
    # Mixed string & int ids, dicts, and objects
    (["0", {"int_field":1, "text_field":"a"}, D(2, "b"), 3],
     """<delete><id>0</id><id>1</id><id>2</id><id>3</id></delete>"""),
    ]

def check_delete_docs(s, doc, xml_string):
    assert str(SolrDelete(s, docs=doc)) == xml_string

def test_delete_docs():
    s = SolrSchema(StringIO.StringIO(good_schema))
    for doc, xml_string in delete_docs:
        yield check_delete_docs, s, doc, xml_string


delete_queries = [
    ([(["search"], {})],
     """<delete><query>search</query></delete>"""),
    ([(["search1"], {}), (["search2"], {})],
     """<delete><query>search1</query><query>search2</query></delete>"""),
    ([([], {"*":"*"})],
     """<delete><query>*:*</query></delete>"""),
    ]

def check_delete_queries(s, queries, xml_string):
    p = str(SolrDelete(s, queries=[s.Q(*args, **kwargs) for args, kwargs in queries]))
    if debug:
        try:
            assert p == xml_string
        except AssertionError:
            print p
            print xml_string
            import pdb;pdb.set_trace()
            raise
    else:
        assert p == xml_string

def test_delete_queries():
    s = SolrSchema(StringIO.StringIO(good_schema))
    for queries, xml_string in delete_queries:
        yield check_delete_queries, s, queries, xml_string


new_field_types_schema = \
"""
<schema name="timetric" version="1.1">
  <types>
    <fieldType name="binary" class="solr.BinaryField"/>
    <fieldType name="point" class="solr.PointType" dimension="2" subFieldSuffix="_d"/>
    <fieldType name="location" class="solr.LatLonType" subFieldSuffix="_coordinate"/>
    <fieldtype name="geohash" class="solr.GeoHashField"/>
    <!-- And just to check it works: -->
    <fieldType name="point3" class="solr.PointType" dimension="3" subFieldSuffix="_d"/>
    <fieldType name="uuid" class="solr.UUIDField" indexed="true" />
  </types>
  <fields>
    <field name="binary_field" required="false" type="binary"/>
    <field name="point_field" required="false" type="point"/>
    <field name="location_field" required="false" type="location"/>
    <field name="geohash_field" required="false" type="geohash"/>
    <field name="point3_field" required="false" type="point3"/>
    <field name="id" type="uuid" indexed="true" stored="true" default="NEW"/>
  </fields>
 </schema>
"""

def test_binary_data_understood_ok():
    s = SolrSchema(StringIO.StringIO(new_field_types_schema))
    blob = "jkgh"
    coded_blob = blob.encode('base64')
    field_inst = s.field_from_user_data("binary_field", blob)
    assert field_inst.value == blob
    assert field_inst.to_solr() == coded_blob
    binary_field = s.match_field("binary_field")
    assert binary_field.from_solr(coded_blob) == blob


def test_2point_data_understood_ok():
    s = SolrSchema(StringIO.StringIO(new_field_types_schema))
    user_data = (3.5, -2.5)
    solr_data = "3.5,-2.5"
    field_inst = s.field_from_user_data("geohash_field", user_data)
    assert field_inst.value == user_data
    assert field_inst.to_solr() == solr_data
    point_field = s.match_field("geohash_field")
    assert point_field.from_solr(solr_data) == user_data


def test_3point_data_understood_ok():
    s = SolrSchema(StringIO.StringIO(new_field_types_schema))
    user_data = (3.5, -2.5, 1.0)
    solr_data = "3.5,-2.5,1.0"
    field_inst = s.field_from_user_data("point3_field", user_data)
    assert field_inst.value == user_data
    assert field_inst.to_solr() == solr_data
    point_field = s.match_field("point3_field")
    assert point_field.from_solr(solr_data) == user_data


def test_uuid_data_understood_ok():
    s = SolrSchema(StringIO.StringIO(new_field_types_schema))

    user_data = "12980286-591b-40c6-aa08-b4393a6d13b3"
    field_inst = s.field_from_user_data('id', user_data)
    assert field_inst.value == uuid.UUID("12980286-591b-40c6-aa08-b4393a6d13b3")

    user_data = uuid.UUID("12980286-591b-40c6-aa08-b4393a6d13b3")
    field_inst = s.field_from_user_data('id', user_data)
    assert field_inst.value == uuid.UUID("12980286-591b-40c6-aa08-b4393a6d13b3")

    user_data = "NEW"
    field_inst = s.field_from_user_data('id', user_data)

    solr_data = "12980286-591b-40c6-aa08-b4393a6d13b3"
    uuid_field = s.match_field("id")
    assert uuid_field.from_solr(solr_data) == uuid.UUID("12980286-591b-40c6-aa08-b4393a6d13b3")
