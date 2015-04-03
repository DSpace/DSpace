from __future__ import absolute_import

try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

import datetime

from lxml.builder import E
from lxml.etree import tostring
try:
    import mx.DateTime
    HAS_MX_DATETIME = True
except ImportError:
    HAS_MX_DATETIME = False

from .schema import SolrSchema, SolrError
from .search import SolrSearch, MltSolrSearch, PaginateOptions, SortOptions, FieldLimitOptions, FacetOptions, HighlightOptions, MoreLikeThisOptions, params_from_dict
from .strings import RawString
from .sunburnt import SolrInterface

from .test_sunburnt import MockConnection, MockResponse

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

schema = SolrSchema(StringIO(schema_string))

class MockInterface(object):
    schema = schema


interface = MockInterface()


good_query_data = {
    "query_by_term":(
        (["hello"], {},
         [("q", u"hello")]),
        (["hello"], {"int_field":3},
         [("q", u"hello AND int_field:3")]),
        (["hello", "world"], {},
         [("q", u"hello AND world")]),
        # NB this next is not really what we want,
        # probably this should warn
        (["hello world"], {},
         [("q", u"hello\\ world")]),
        ),

    "query_by_phrase":(
        (["hello"], {},
         [("q", u"hello")]),
        (["hello"], {"int_field":3},
         [("q", u"int_field:3 AND hello")]), # Non-text data is always taken to be a term, and terms come before phrases, so order is reversed
        (["hello", "world"], {},
         [("q", u"hello AND world")]),
        (["hello world"], {},
         [("q", u"hello\\ world")]),
        ([], {'string_field':['hello world', 'goodbye, cruel world']},
         [("q", u"string_field:goodbye,\\ cruel\\ world AND string_field:hello\\ world")]),
        ),

    "filter_by_term":(
        (["hello"], {},
         [("fq", u"hello"), ("q", "*:*")]),
        (["hello"], {"int_field":3},
         [("fq", u"hello AND int_field:3"), ("q", "*:*")]),
        (["hello", "world"], {},
         [("fq", u"hello AND world"), ("q", "*:*")]),
        # NB this next is not really what we want,
        # probably this should warn
        (["hello world"], {},
         [("fq", u"hello\\ world"), ("q", "*:*")]),
        ),

    "filter_by_phrase":(
        (["hello"], {},
         [("fq", u"hello"), ("q", "*:*")]),
        (["hello"], {"int_field":3},
         [("fq", u"int_field:3 AND hello"), ("q", "*:*")]),
        (["hello", "world"], {},
         [("fq", u"hello AND world"), ("q", "*:*")]),
        (["hello world"], {},
         [("fq", u"hello\\ world"), ("q", "*:*")]),
        ),

    "filter":(
        (["hello"], {},
         [("fq", u"hello"), ("q", "*:*")]),
        (["hello"], {"int_field":3},
         [("fq", u"hello AND int_field:3"), ("q", "*:*")]),
        (["hello", "world"], {},
         [("fq", u"hello AND world"), ("q", "*:*")]),
        (["hello world"], {},
         [("fq", u"hello\\ world"), ("q", "*:*")]),
        ),

    "query":(
        #Basic queries
        (["hello"], {},
         [("q", u"hello")]),
        (["hello"], {"int_field":3},
         [("q", u"hello AND int_field:3")]),
        (["hello", "world"], {},
         [("q", u"hello AND world")]),
        (["hello world"], {},
         [("q", u"hello\\ world")]),
        #Test fields
        # Boolean fields take any truth-y value
        ([], {"boolean_field":True},
         [("q", u"boolean_field:true")]),
        ([], {"boolean_field":'true'},
         [("q", u"boolean_field:true")]),
        ([], {"boolean_field":1},
         [("q", u"boolean_field:true")]),
        ([], {"boolean_field":"false"},
         [("q", u"boolean_field:false")]),
        ([], {"boolean_field":0},
         [("q", u"boolean_field:false")]),
        ([], {"boolean_field":False},
         [("q", u"boolean_field:false")]),
        ([], {"int_field":3},
         [("q", u"int_field:3")]),
        ([], {"int_field":3.1}, # casting from float should work
         [("q", u"int_field:3")]),
        ([], {"sint_field":3},
         [("q", u"sint_field:3")]),
        ([], {"sint_field":3.1}, # casting from float should work
         [("q", u"sint_field:3")]),
        ([], {"long_field":2**31},
         [("q", u"long_field:2147483648")]),
        ([], {"slong_field":2**31},
         [("q", u"slong_field:2147483648")]),
        ([], {"float_field":3.0},
         [("q", u"float_field:3.0")]),
        ([], {"float_field":3}, # casting from int should work
         [("q", u"float_field:3.0")]),
        ([], {"sfloat_field":3.0},
         [("q", u"sfloat_field:3.0")]),
        ([], {"sfloat_field":3}, # casting from int should work
         [("q", u"sfloat_field:3.0")]),
        ([], {"double_field":3.0},
         [("q", u"double_field:3.0")]),
        ([], {"double_field":3}, # casting from int should work
         [("q", u"double_field:3.0")]),
        ([], {"sdouble_field":3.0},
         [("q", u"sdouble_field:3.0")]),
        ([], {"sdouble_field":3}, # casting from int should work
         [("q", u"sdouble_field:3.0")]),
        ([], {"date_field":datetime.datetime(2009, 1, 1)},
         [("q", u"date_field:2009\\-01\\-01T00\\:00\\:00Z")]),
        #Test ranges
        ([], {"int_field__any":True},
         [("q", u"int_field:[* TO *]")]),
        ([], {"int_field__lt":3},
         [("q", u"int_field:{* TO 3}")]),
        ([], {"int_field__gt":3},
         [("q", u"int_field:{3 TO *}")]),
        ([], {"int_field__rangeexc":(-3, 3)},
         [("q", u"int_field:{\-3 TO 3}")]),
        ([], {"int_field__rangeexc":(3, -3)},
         [("q", u"int_field:{\-3 TO 3}")]),
        ([], {"int_field__lte":3},
         [("q", u"int_field:[* TO 3]")]),
        ([], {"int_field__gte":3},
         [("q", u"int_field:[3 TO *]")]),
        ([], {"int_field__range":(-3, 3)},
         [("q", u"int_field:[\-3 TO 3]")]),
        ([], {"int_field__range":(3, -3)},
         [("q", u"int_field:[\-3 TO 3]")]),
        ([], {"date_field__lt":datetime.datetime(2009, 1, 1)},
         [("q", u"date_field:{* TO 2009\\-01\\-01T00\\:00\\:00Z}")]),
        ([], {"date_field__gt":datetime.datetime(2009, 1, 1)},
         [("q", u"date_field:{2009\\-01\\-01T00\\:00\\:00Z TO *}")]),
        ([], {"date_field__rangeexc":(datetime.datetime(2009, 1, 1), datetime.datetime(2009, 1, 2))},
         [("q", "date_field:{2009\\-01\\-01T00\\:00\\:00Z TO 2009\\-01\\-02T00\\:00\\:00Z}")]),
        ([], {"date_field__lte":datetime.datetime(2009, 1, 1)},
         [("q", u"date_field:[* TO 2009\\-01\\-01T00\\:00\\:00Z]")]),
        ([], {"date_field__gte":datetime.datetime(2009, 1, 1)},
         [("q", u"date_field:[2009\\-01\\-01T00\\:00\\:00Z TO *]")]),
        ([], {"date_field__range":(datetime.datetime(2009, 1, 1), datetime.datetime(2009, 1, 2))},
         [("q", u"date_field:[2009\\-01\\-01T00\\:00\\:00Z TO 2009\\-01\\-02T00\\:00\\:00Z]")]),
        ([], {'string_field':['hello world', 'goodbye, cruel world']},
         [("q", u"string_field:goodbye,\\ cruel\\ world AND string_field:hello\\ world")]),
        # Raw strings
        ([], {'string_field':RawString("abc*???")},
         [("q", "string_field:abc\\*\\?\\?\\?")]),
        ),
    }
if HAS_MX_DATETIME:
    good_query_data['query'] += \
            ([], {"date_field":mx.DateTime.DateTime(2009, 1, 1)},
             [("q", u"date_field:2009-01-01T00\\:00\\:00Z")])

def check_query_data(method, args, kwargs, output):
    solr_search = SolrSearch(interface)
    p = getattr(solr_search, method)(*args, **kwargs).params()
    try:
        assert p == output, "Unequal: %r, %r" % (p, output)
    except AssertionError:
        if debug:
            print p
            print output
            import pdb;pdb.set_trace()
            raise
        else:
            raise

def test_query_data():
    for method, data in good_query_data.items():
        for args, kwargs, output in data:
            yield check_query_data, method, args, kwargs, output

bad_query_data = (
    {"int_field":"a"},
    {"int_field":2**31},
    {"int_field":-(2**31)-1},
    {"long_field":"a"},
    {"long_field":2**63},
    {"long_field":-(2**63)-1},
    {"float_field":"a"},
    {"float_field":2**1000},
    {"float_field":-(2**1000)},
    {"double_field":"a"},
    {"double_field":2**2000},
    {"double_field":-(2**2000)},
    {"date_field":"a"},
    {"int_field__gt":"a"},
    {"date_field__gt":"a"},
    {"int_field__range":1},
    {"date_field__range":1},
)

def check_bad_query_data(kwargs):
    solr_search = SolrSearch(interface)
    try:
        solr_search.query(**kwargs).params()
    except SolrError:
        pass
    else:
        assert False

def test_bad_query_data():
    for kwargs in bad_query_data:
        yield check_bad_query_data, kwargs


good_option_data = {
    PaginateOptions:(
        ({"start":5, "rows":10},
         {"start":5, "rows":10}),
        ({"start":5, "rows":None},
         {"start":5}),
        ({"start":None, "rows":10},
         {"rows":10}),
        ),
    FacetOptions:(
        ({"fields":"int_field"},
         {"facet":True, "facet.field":["int_field"]}),
        ({"fields":["int_field", "text_field"]},
         {"facet":True, "facet.field":["int_field","text_field"]}),
        ({"prefix":"abc"},
         {"facet":True, "facet.prefix":"abc"}),
        ({"prefix":"abc", "sort":True, "limit":3, "offset":25, "mincount":1, "missing":False, "method":"enum"},
         {"facet":True, "facet.prefix":"abc", "facet.sort":True, "facet.limit":3, "facet.offset":25, "facet.mincount":1, "facet.missing":False, "facet.method":"enum"}),
        ({"fields":"int_field", "prefix":"abc"},
         {"facet":True, "facet.field":["int_field"], "f.int_field.facet.prefix":"abc"}),
        ({"fields":"int_field", "prefix":"abc", "limit":3},
         {"facet":True, "facet.field":["int_field"], "f.int_field.facet.prefix":"abc", "f.int_field.facet.limit":3}),
        ({"fields":["int_field", "text_field"], "prefix":"abc", "limit":3},
         {"facet":True, "facet.field":["int_field", "text_field"], "f.int_field.facet.prefix":"abc", "f.int_field.facet.limit":3, "f.text_field.facet.prefix":"abc", "f.text_field.facet.limit":3, }),
        ),
    SortOptions:(
        ({"field":"int_field"},
         {"sort":"int_field asc"}),
        ({"field":"-int_field"},
         {"sort":"int_field desc"}),
    ),
    HighlightOptions:(
        ({"fields":"int_field"},
         {"hl":True, "hl.fl":"int_field"}),
        ({"fields":["int_field", "text_field"]},
         {"hl":True, "hl.fl":"int_field,text_field"}),
        ({"snippets":3},
         {"hl":True, "hl.snippets":3}),
        ({"snippets":3, "fragsize":5, "mergeContinuous":True, "requireFieldMatch":True, "maxAnalyzedChars":500, "alternateField":"text_field", "maxAlternateFieldLength":50, "formatter":"simple", "simple.pre":"<b>", "simple.post":"</b>", "fragmenter":"regex", "usePhraseHighlighter":True, "highlightMultiTerm":True, "regex.slop":0.2, "regex.pattern":"\w", "regex.maxAnalyzedChars":100},
        {"hl":True, "hl.snippets":3, "hl.fragsize":5, "hl.mergeContinuous":True, "hl.requireFieldMatch":True, "hl.maxAnalyzedChars":500, "hl.alternateField":"text_field", "hl.maxAlternateFieldLength":50, "hl.formatter":"simple", "hl.simple.pre":"<b>", "hl.simple.post":"</b>", "hl.fragmenter":"regex", "hl.usePhraseHighlighter":True, "hl.highlightMultiTerm":True, "hl.regex.slop":0.2, "hl.regex.pattern":"\w", "hl.regex.maxAnalyzedChars":100}),
        ({"fields":"int_field", "snippets":"3"},
         {"hl":True, "hl.fl":"int_field", "f.int_field.hl.snippets":3}),
        ({"fields":"int_field", "snippets":3, "fragsize":5},
         {"hl":True, "hl.fl":"int_field", "f.int_field.hl.snippets":3, "f.int_field.hl.fragsize":5}),
        ({"fields":["int_field", "text_field"], "snippets":3, "fragsize":5},
         {"hl":True, "hl.fl":"int_field,text_field", "f.int_field.hl.snippets":3, "f.int_field.hl.fragsize":5, "f.text_field.hl.snippets":3, "f.text_field.hl.fragsize":5}),
        ),
    MoreLikeThisOptions:(
        ({"fields":"int_field"},
         {"mlt":True, "mlt.fl":"int_field"}),
        ({"fields":["int_field", "text_field"]},
         {"mlt":True, "mlt.fl":"int_field,text_field"}),
        ({"fields":["text_field", "string_field"], "query_fields":{"text_field":0.25, "string_field":0.75}},
         {"mlt":True, "mlt.fl":"string_field,text_field", "mlt.qf":"text_field^0.25 string_field^0.75"}),
        ({"fields":"text_field", "count":1},
         {"mlt":True, "mlt.fl":"text_field", "mlt.count":1}),
        ),
    FieldLimitOptions:(
        ({},
         {}),
        ({"fields":"int_field"},
         {"fl":"int_field"}),
        ({"fields":["int_field", "text_field"]},
         {"fl":"int_field,text_field"}),
        ({"score": True},
         {"fl":"score"}),
        ({"all_fields": True, "score": True},
         {"fl":"*,score"}),
        ({"fields":"int_field", "score": True},
         {"fl":"int_field,score"}),
        ),
    }

def check_good_option_data(OptionClass, kwargs, output):
    optioner = OptionClass(schema)
    optioner.update(**kwargs)
    assert optioner.options() == output

def test_good_option_data():
    for OptionClass, option_data in good_option_data.items():
        for kwargs, output in option_data:
            yield check_good_option_data, OptionClass, kwargs, output


# All these tests should really nominate which exception they're going to throw.
bad_option_data = {
    PaginateOptions:(
        {"start":-1, "rows":None}, # negative start
        {"start":None, "rows":-1}, # negative rows
        ),
    FacetOptions:(
        {"fields":"myarse"}, # Undefined field
        {"oops":True}, # undefined option
        {"limit":"a"}, # invalid type
        {"sort":"yes"}, # invalid choice
        {"offset":-1}, # invalid value
        ),
    SortOptions:(
        {"field":"myarse"}, # Undefined field
        {"field":"string_field"}, # Multivalued field
        ),
    HighlightOptions:(
        {"fields":"myarse"}, # Undefined field
        {"oops":True}, # undefined option
        {"snippets":"a"}, # invalid type
        {"alternateField":"yourarse"}, # another invalid option
        ),
    MoreLikeThisOptions:(
        {"fields":"myarse"}, # Undefined field
        {"fields":"text_field", "query_fields":{"text_field":0.25, "string_field":0.75}}, # string_field in query_fields, not fields
        {"fields":"text_field", "query_fields":{"text_field":"a"}}, # Non-float value for boost
        {"fields":"text_field", "oops":True}, # undefined option
        {"fields":"text_field", "count":"a"} # Invalid value for option
        ),
    }

def check_bad_option_data(OptionClass, kwargs):
    option = OptionClass(schema)
    try:
        option.update(**kwargs)
    except SolrError:
        pass
    else:
        assert False

def test_bad_option_data():
    for OptionClass, option_data in bad_option_data.items():
        for kwargs in option_data:
            yield check_bad_option_data, OptionClass, kwargs


complex_boolean_queries = (
    (lambda q: q.query("hello world").filter(q.Q(text_field="tow") | q.Q(boolean_field=False, int_field__gt=3)),
     [('fq', u'text_field:tow OR (boolean_field:false AND int_field:{3 TO *})'), ('q', u'hello\\ world')]),
    (lambda q: q.query("hello world").filter(q.Q(text_field="tow") & q.Q(boolean_field=False, int_field__gt=3)),
     [('fq', u'boolean_field:false AND text_field:tow AND int_field:{3 TO *}'), ('q',  u'hello\\ world')]),
# Test various combinations of NOTs at the top level.
# Sometimes we need to do the *:* trick, sometimes not.
    (lambda q: q.query(~q.Q("hello world")),
     [('q',  u'NOT hello\\ world')]),
    (lambda q: q.query(~q.Q("hello world") & ~q.Q(int_field=3)),
     [('q',  u'NOT hello\\ world AND NOT int_field:3')]),
    (lambda q: q.query("hello world", ~q.Q(int_field=3)),
     [('q', u'hello\\ world AND NOT int_field:3')]),
    (lambda q: q.query("abc", q.Q("def"), ~q.Q(int_field=3)),
     [('q', u'abc AND def AND NOT int_field:3')]),
    (lambda q: q.query("abc", q.Q("def") & ~q.Q(int_field=3)),
     [('q', u'abc AND def AND NOT int_field:3')]),
    (lambda q: q.query("abc", q.Q("def") | ~q.Q(int_field=3)),
     [('q', u'abc AND (def OR (*:* AND NOT int_field:3))')]),
    (lambda q: q.query(q.Q("abc") | ~q.Q("def")),
     [('q', u'abc OR (*:* AND NOT def)')]),
    (lambda q: q.query(q.Q("abc") | q.Q(~q.Q("def"))),
     [('q', u'abc OR (*:* AND NOT def)')]),
# Make sure that ANDs are flattened
    (lambda q: q.query("def", q.Q("abc"), q.Q(q.Q("xyz"))),
     [('q', u'abc AND def AND xyz')]),
# Make sure that ORs are flattened
    (lambda q: q.query(q.Q("def") | q.Q(q.Q("xyz"))),
     [('q', u'def OR xyz')]),
# Make sure that empty queries are discarded in ANDs
    (lambda q: q.query("def", q.Q("abc"), q.Q(), q.Q(q.Q() & q.Q("xyz"))),
     [('q', u'abc AND def AND xyz')]),
# Make sure that empty queries are discarded in ORs
    (lambda q: q.query(q.Q() | q.Q("def") | q.Q(q.Q() | q.Q("xyz"))),
     [('q', u'def OR xyz')]),
# Test cancellation of NOTs.
    (lambda q: q.query(~q.Q(~q.Q("def"))),
     [('q', u'def')]),
    (lambda q: q.query(~q.Q(~q.Q(~q.Q("def")))),
     [('q', u'NOT def')]),
# Test it works through sub-sub-queries
    (lambda q: q.query(~q.Q(q.Q(q.Q(~q.Q(~q.Q("def")))))),
     [('q', u'NOT def')]),
# Even with empty queries in there
    (lambda q: q.query(~q.Q(q.Q(q.Q() & q.Q(q.Q() | ~q.Q(~q.Q("def")))))),
     [('q', u'NOT def')]),
# Test escaping of AND, OR, NOT
    (lambda q: q.query("AND", "OR", "NOT"),
     [('q', u'"AND" AND "NOT" AND "OR"')]),
# Test exclude (rather than explicit NOT
    (lambda q: q.query("blah").exclude(q.Q("abc") | q.Q("def") | q.Q("ghi")),
     [('q', u'blah AND NOT (abc OR def OR ghi)')]),
# Try boosts
    (lambda q: q.query("blah").query(q.Q("def")**1.5),
     [('q', u'blah AND def^1.5')]),
    (lambda q: q.query("blah").query((q.Q("def") | q.Q("ghi"))**1.5),
     [('q', u'blah AND (def OR ghi)^1.5')]),
    (lambda q: q.query("blah").query(q.Q("def", ~q.Q("pqr") | q.Q("mno"))**1.5),
     [('q', u'blah AND (def AND ((*:* AND NOT pqr) OR mno))^1.5')]),
# And boost_relevancy
    (lambda q: q.query("blah").boost_relevancy(1.5, int_field=3),
     [('q', u'blah OR (blah AND int_field:3^1.5)')]),
    (lambda q: q.query("blah").boost_relevancy(1.5, int_field=3).boost_relevancy(2, string_field='def'),
     [('q', u'blah OR (blah AND (int_field:3^1.5 OR string_field:def^2))')]),
    (lambda q: q.query("blah").query("blah2").boost_relevancy(1.5, int_field=3),
     [('q', u'(blah AND blah2) OR (blah AND blah2 AND int_field:3^1.5)')]),
    (lambda q: q.query(q.Q("blah") | q.Q("blah2")).boost_relevancy(1.5, int_field=3),
     [('q', u'blah OR blah2 OR ((blah OR blah2) AND int_field:3^1.5)')]),
# And ranges
    (lambda q: q.query(int_field__any=True),
     [('q', u'int_field:[* TO *]')]),
    (lambda q: q.query("blah", ~q.Q(int_field__any=True)),
     [('q', u'blah AND NOT int_field:[* TO *]')]),
)

def check_complex_boolean_query(solr_search, query, output):
    p = query(solr_search).params()
    try:
        assert p == output
    except AssertionError:
        if debug:
            print p
            print output
            import pdb;pdb.set_trace()
            raise
        else:
            raise
    # And check no mutation of the base object
    q = query(solr_search).params()
    try:
        assert p == q
    except AssertionError:
        if debug:
            print p
            print q
            import pdb;pdb.set_trace()
            raise

def test_complex_boolean_queries():
    solr_search = SolrSearch(interface)
    for query, output in complex_boolean_queries:
        yield check_complex_boolean_query, solr_search, query, output


param_encode_data = (
    ({"int":3, "string":"string", "unicode":u"unicode"},
     [("int", "3"), ("string", "string"), ("unicode", "unicode")]),
    ({"int":3, "string":"string", "unicode":u"\N{UMBRELLA}nicode"},
     [("int", "3"), ("string", "string"), ("unicode", "\xe2\x98\x82nicode")]),
    ({"int":3, "string":"string", u"\N{UMBRELLA}nicode":u"\N{UMBRELLA}nicode"},
     [("int", "3"), ("string", "string"), ("\xe2\x98\x82nicode", "\xe2\x98\x82nicode")]),
    ({"true":True, "false":False},
     [("false", "false"), ("true", "true")]),
    ({"list":["first", "second", "third"]},
     [("list", "first"), ("list", "second"), ("list", "third")]),
)

def check_url_encode_data(kwargs, output):
    # Convert for pre-2.6.5 python
    s_kwargs = dict((k.encode('utf8'), v) for k, v in kwargs.items())
    assert params_from_dict(**s_kwargs) == output

def test_url_encode_data():
    for kwargs, output in param_encode_data:
        yield check_url_encode_data, kwargs, output

mlt_query_options_data = (
    ('text_field', {}, {},
     [('mlt.fl', 'text_field')]),
    (['string_field', 'text_field'], {'string_field': 3.0}, {},
     [('mlt.fl', 'string_field,text_field'), ('mlt.qf', 'string_field^3.0')]),
    ('text_field', {}, {'mindf': 3, 'interestingTerms': 'details'},
     [('mlt.fl', 'text_field'), ('mlt.interestingTerms', 'details'),
      ('mlt.mindf', '3')]),
)

def check_mlt_query_options(fields, query_fields, kwargs, output):
    q = MltSolrSearch(interface, content="This is the posted content.")
    q = q.mlt(fields, query_fields=query_fields, **kwargs)
    assert_equal(q.params(), output)

def test_mlt_query_options():
    for (fields, query_fields, kwargs, output) in mlt_query_options_data:
        yield check_mlt_query_options, fields, query_fields, kwargs, output


class HighlightingMockResponse(MockResponse):
    def __init__(self, highlighting, *args, **kwargs):
        self.highlighting = highlighting
        super(HighlightingMockResponse, self).__init__(*args, **kwargs)

    def extra_response_parts(self):
        contents = []
        if self.highlighting:
            contents.append(
                    E.lst({'name':'highlighting'}, E.lst({'name':'0'}, E.arr({'name':'string_field'}, E.str('zero'))))
                    )
        return contents

class HighlightingMockConnection(MockConnection):
    def _handle_request(self, uri_obj, params, method, body, headers):
        highlighting = params.get('hl') == ['true']
        if method == 'GET' and uri_obj.path.endswith('/select/'):
            return self.MockStatus(200), HighlightingMockResponse(highlighting, 0, 1).xml_response()

highlighting_interface = SolrInterface("http://test.example.com/", http_connection=HighlightingMockConnection())

solr_highlights_data = (
    (None, dict, None),
    (['string_field'], dict, {'string_field': ['zero']}),
    )

def check_transform_results(highlighting, constructor, solr_highlights):
    q = highlighting_interface.query('zero')
    if highlighting:
        q = q.highlight(highlighting)
    docs = q.execute(constructor=constructor).result.docs
    assert_equal(docs[0].get('solr_highlights'), solr_highlights)
    assert isinstance(docs[0], constructor)

def test_transform_result():
    for highlighting, constructor, solr_highlights in solr_highlights_data:
        yield check_transform_results, highlighting, constructor, solr_highlights

#Test More Like This results
class MltMockResponse(MockResponse):

    def extra_response_parts(self):
        contents = []
        create_doc = lambda value: E.doc(E.str({'name':'string_field'}, value))
        #Main response result
        contents.append(
            E.result({'name': 'response'},
                     create_doc('zero')
                    )
        )
        #More like this results
        contents.append(
            E.lst({'name':'moreLikeThis'},
                  E.result({'name': 'zero', 'numFound': '3', 'start': '0'},
                           create_doc('one'),
                           create_doc('two'),
                           create_doc('three')
                          )
                 )
        )
        return contents

class MltMockConnection(MockConnection):
    def _handle_request(self, uri_obj, params, method, body, headers):
        if method == 'GET' and uri_obj.path.endswith('/select/'):
            return self.MockStatus(200), MltMockResponse(0, 1).xml_response()

mlt_interface = SolrInterface("http://test.example.com/",
                              http_connection=MltMockConnection())

class DummyDocument(object):

    def __init__(self, **kw):
        self.kw = kw

    def __repr__(self):
        return "DummyDocument<%r>" % self.kw

    def get(self, key):
        return self.kw.get(key)

def make_dummydoc(**kwargs):
    return DummyDocument(**kwargs)

solr_mlt_transform_data = (
    (dict, dict),
    (DummyDocument, DummyDocument),
    (make_dummydoc, DummyDocument),
    )

def check_mlt_transform_results(constructor, _type):
    q = mlt_interface.query('zero')
    query = q.mlt(fields='string_field')
    response = q.execute(constructor=constructor)

    for doc in response.result.docs:
        assert isinstance(doc, _type)

    for key in response.more_like_these:
        for doc in response.more_like_these[key].docs:
            assert isinstance(doc, _type)

def test_mlt_transform_result():
    for constructor, _type in solr_mlt_transform_data:
        yield check_mlt_transform_results, constructor, _type
