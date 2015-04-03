from __future__ import absolute_import

import datetime
import math
import operator
import uuid
import warnings

from lxml.builder import E
import lxml.etree

from .dates import datetime_from_w3_datestring
from .strings import RawString, SolrString, WildcardString

try:
    import pytz
except ImportError:
    warnings.warn(
        "pytz not found; cannot do timezone conversions for Solr DateFields",
        ImportWarning)
    pytz = None


class SolrError(Exception):
    pass


class solr_date(object):
    """This class can be initialized from either native python datetime
    objects and mx.DateTime objects, and will serialize to a format
    appropriate for Solr"""
    def __init__(self, v):
        if isinstance(v, solr_date):
            self._dt_obj = v._dt_obj
        elif isinstance(v, basestring):
            try:
                self._dt_obj = datetime_from_w3_datestring(v)
            except ValueError, e:
                raise SolrError(*e.args)
        elif hasattr(v, "strftime"):
            self._dt_obj = self.from_date(v)
        else:
            raise SolrError("Cannot initialize solr_date from %s object"
                            % type(v))

    @staticmethod
    def from_date(dt_obj):
        # Python datetime objects may include timezone information
        if hasattr(dt_obj, 'tzinfo') and dt_obj.tzinfo:
            # but Solr requires UTC times.
            if pytz:
                return dt_obj.astimezone(pytz.utc).replace(tzinfo=None)
            else:
                raise EnvironmentError("pytz not available, cannot do timezone conversions")
        else:
            return dt_obj

    @property
    def microsecond(self):
        if hasattr(self._dt_obj, "microsecond"):
            return self._dt_obj.microsecond
        else:
            return int(1000000*math.modf(self._dt_obj.second)[0])

    def __repr__(self):
        return repr(self._dt_obj)

    def __unicode__(self):
        """ Serialize a datetime object in the format required
        by Solr. See http://wiki.apache.org/solr/IndexingDates
        """
        if hasattr(self._dt_obj, 'isoformat'):
            return "%sZ" % (self._dt_obj.isoformat(), )
        strtime = self._dt_obj.strftime("%Y-%m-%dT%H:%M:%S")
        microsecond = self.microsecond
        if microsecond:
            return u"%s.%06dZ" % (strtime, microsecond)
        return u"%sZ" % (strtime,)

    def __cmp__(self, other):
        try:
            other = other._dt_obj
        except AttributeError:
            pass
        if self._dt_obj < other:
            return -1
        elif self._dt_obj > other:
            return 1
        else:
            return 0


def solr_point_factory(dimension):
    if dimension < 1:
        raise ValueError("dimension of PointType must be greater than one")
    class solr_point(object):
        dim = int(dimension)
        def __init__(self, *args):
            if dimension > 1 and len(args) == 1:
                v = args[0]
                if isinstance(v, basestring):
                    v_arr = v.split(',')
                else:
                    try:
                        v_arr = list(v)
                    except TypeError:
                        raise ValueError("bad value provided for point list")
            else:
                v_arr = args
            if len(v_arr) != self.dim:
                raise ValueError("point has wrong number of dimensions")
            self.point = tuple(float(v) for v in v_arr)

        def __repr__(self):
            return "solr_point(%s)" % unicode(self)

        def __unicode__(self):
            return ','.join(str(p) for p in self.point)

    return solr_point


class SolrField(object):
    def __init__(self, name, indexed=None, stored=None, required=False, multiValued=False, dynamic=False, **kwargs):
        self.name = name
        if indexed is not None:
            self.indexed = indexed
        if stored is not None:
            self.stored = stored
        # By default, indexed & stored are taken from the class attribute
        self.multi_valued = multiValued
        self.required = required
        self.dynamic = dynamic
        if dynamic:
            if self.name.startswith("*"):
                self.wildcard_at_start = True
            elif self.name.endswith("*"):
                self.wildcard_at_start = False
            else:
                raise SolrError("Dynamic fields must have * at start or end of name (field %s)" % 
                        self.name)

    def match(self, name):
        if self.dynamic:
            if self.wildcard_at_start:
                return name.endswith(self.name[1:])
            else:
                return name.startswith(self.name[:-1])

    def normalize(self, value):
        """ Normalize the given value according to the field type.
        
        This method does nothing by default, returning the given value
        as is. Child classes may override this method as required.
        """
        return value

    def instance_from_user_data(self, data):
        return SolrFieldInstance.from_user_data(self, data)

    def to_user_data(self, value):
        return value

    def from_user_data(self, value):
        return self.normalize(value)

    def to_solr(self, value):
        return unicode(value)

    def to_query(self, value):
        return RawString(self.to_solr(value)).escape_for_lqs_term()

    def from_solr(self, value):
        return self.normalize(value)


class SolrUnicodeField(SolrField):
    def from_user_data(self, value):
        if isinstance(value, SolrString):
            return value
        else:
            return WildcardString(unicode(value))

    def to_query(self, value):
        return value.escape_for_lqs_term()

    def from_solr(self, value):
        try:
            return unicode(value)
        except UnicodeError:
            raise SolrError("%s could not be coerced to unicode (field %s)" % 
                    (value, self.name))


class SolrBooleanField(SolrField):
    def to_solr(self, value):
        return u"true" if value else u"false"

    def normalize(self, value):
        if isinstance(value, basestring):
            if value.lower() == "true":
                return True
            elif value.lower() == "false":
                return False
            else:
                raise ValueError("sorry, I only understand simple boolean strings (field %s)" % 
                        self.name)
        return bool(value)


class SolrBinaryField(SolrField):
    def from_user_data(self, value):
        try:
            return str(value)
        except (TypeError, ValueError):
            raise SolrError("Could not convert data to binary string (field %s)" % 
                    self.name)

    def to_solr(self, value):
        return unicode(value.encode('base64'))

    def from_solr(self, value):
        return value.decode('base64')


class SolrNumericalField(SolrField):
    def normalize(self, value):
        try:
            v = self.base_type(value)
        except (OverflowError, TypeError, ValueError):
            raise SolrError("%s is invalid value for %s (field %s)" % 
                    (value, self.__class__, self.name))
        if v < self.min or v > self.max:
            raise SolrError("%s out of range for a %s (field %s)" %
                    (value, self.__class__, self.name))
        return v


class SolrShortField(SolrNumericalField):
    base_type = int
    min = -(2**15)
    max = 2**15-1


class SolrIntField(SolrNumericalField):
    base_type = int
    min = -(2**31)
    max = 2**31-1


class SolrLongField(SolrNumericalField):
    base_type = long
    min = -(2**63)
    max = 2**63-1


class SolrFloatField(SolrNumericalField):
    base_type = float
    max = (2.0-2.0**(-23)) * 2.0**127
    min = -max


class SolrDoubleField(SolrNumericalField):
    base_type = float
    max = (2.0-2.0**(-52)) * 2.0**1023
    min = -max


class SolrDateField(SolrField):
    def normalize(self, v):
        return solr_date(v)

    def to_user_data(self, v):
        return v._dt_obj


class SolrRandomField(SolrField):
    def normalize(self, v):
        raise TypeError("Don't try and store or index values in a RandomSortField")


class SolrUUIDField(SolrUnicodeField):
    def from_solr(self, v):
        return uuid.UUID(v)

    def from_user_data(self, v):
        if v == 'NEW':
            return v
        elif isinstance(v, uuid.UUID):
            return v
        else:
            return uuid.UUID(v)

    def to_solr(self, v):
        if v == 'NEW':
            return v
        else:
            return v.urn[9:]


class SolrPointField(SolrField):
    def __init__(self, **kwargs):
        super(SolrPointField, self).__init__(**kwargs)
        # dimension will be set by the subclass
        self.value_class = solr_point_factory(self.dimension)

    def to_solr(self, v):
        return unicode(self.value_class(v))

    def normalize(self, v):
        return self.value_class(v).point


class SolrPoint2Field(SolrPointField):
    dimension = 2


def SolrFieldTypeFactory(cls, name, **kwargs):
    atts = {'stored':True, 'indexed':True}
    atts.update(kwargs)
    # This next because otherwise the class names aren't globally
    # visible or useful, which is confusing for debugging.
    # We give the new class a name which uniquely identifies it
    # (but we don't need Solr class, because we've got the same
    # information in cls anyway.
    name = 'SolrFieldType_%s_%s' % (cls.__name__, '_'.join('%s_%s' % kv for kv in sorted(atts.items()) if kv[0] != 'class'))
    # and its safe to put in globals(), because the class is
    # defined by the constituents of its name.
    if name not in globals():
        globals()[name] = type(name, (cls,), atts)
    return globals()[name]


class SolrFieldInstance(object):
    @classmethod
    def from_solr(cls, field, data):
        self = cls()
        self.field = field
        self.value = self.field.from_solr(data)
        return self

    @classmethod
    def from_user_data(cls, field, data):
        self = cls()
        self.field = field
        self.value = self.field.from_user_data(data)
        return self

    def to_solr(self):
        return self.field.to_solr(self.value)

    def to_query(self):
        return self.field.to_query(self.value)

    def to_user_data(self):
        return self.field.to_user_data(self.value)


# These are artificial field classes/instances:
class SolrWildcardField(SolrUnicodeField):
    def __init__(self):
        pass


class SolrScoreField(SolrDoubleField):
    def __init__(self):
       pass


class WildcardFieldInstance(SolrFieldInstance):
    @classmethod
    def from_user_data(cls):
        return super(WildcardFieldInstance, cls).from_user_data(SolrWildcardField(), "*")


class SolrSchema(object):
    solr_data_types = {
        'solr.StrField':SolrUnicodeField,
        'solr.TextField':SolrUnicodeField,
        'solr.BoolField':SolrBooleanField,
        'solr.ShortField':SolrShortField,
        'solr.IntField':SolrIntField,
        'solr.SortableIntField':SolrIntField,
        'solr.TrieIntField':SolrIntField,
        'solr.LongField':SolrLongField,
        'solr.SortableLongField':SolrLongField,
        'solr.TrieLongField':SolrLongField,
        'solr.FloatField':SolrFloatField,
        'solr.SortableFloatField':SolrFloatField,
        'solr.TrieFloatField':SolrFloatField,
        'solr.DoubleField':SolrDoubleField,
        'solr.SortableDoubleField':SolrDoubleField,
        'solr.TrieDoubleField':SolrDoubleField,
        'solr.DateField':SolrDateField,
        'solr.TrieDateField':SolrDateField,
        'solr.RandomSortField':SolrRandomField,
        'solr.UUIDField':SolrUUIDField,
        'solr.BinaryField':SolrBinaryField,
        'solr.PointType':SolrPointField,
        'solr.LatLonType':SolrPoint2Field,
        'solr.GeoHashField':SolrPoint2Field,
    }

    def __init__(self, f):
        """initialize a schema object from a
        filename or file-like object."""
        self.fields, self.dynamic_fields, self.default_field_name, self.unique_key \
            = self.schema_parse(f)
        self.default_field = self.fields[self.default_field_name] \
            if self.default_field_name else None
        self.unique_field = self.fields[self.unique_key] \
            if self.unique_key else None

    def Q(self, *args, **kwargs):
        from .search import LuceneQuery
        q = LuceneQuery(self)
        q.add(args, kwargs)
        return q

    def schema_parse(self, f):
        try:
            schemadoc = lxml.etree.parse(f)
        except lxml.etree.XMLSyntaxError, e:
            raise SolrError("Invalid XML in schema:\n%s" % e.args[0])

        field_type_classes = {}
        for field_type_node in schemadoc.xpath("/schema/types/fieldType|/schema/types/fieldtype"):
            name, field_type_class = self.field_type_factory(field_type_node)
            field_type_classes[name] = field_type_class

        field_classes = {}
        for field_node in schemadoc.xpath("/schema/fields/field"):
            name, field_class = self.field_factory(field_node, field_type_classes, dynamic=False)
            field_classes[name] = field_class

        dynamic_field_classes = []
        for field_node in schemadoc.xpath("/schema/fields/dynamicField"):
            _, field_class = self.field_factory(field_node, field_type_classes, dynamic=True)
            dynamic_field_classes.append(field_class)

        default_field_name = schemadoc.xpath("/schema/defaultSearchField")
        default_field_name = default_field_name[0].text \
            if default_field_name else None
        unique_key = schemadoc.xpath("/schema/uniqueKey")
        unique_key = unique_key[0].text if unique_key else None
        return field_classes, dynamic_field_classes, default_field_name, unique_key

    def field_type_factory(self, field_type_node):
        try:
            name, class_name = field_type_node.attrib['name'], field_type_node.attrib['class']
        except KeyError, e:
            raise SolrError("Invalid schema.xml: missing %s attribute on fieldType" % e.args[0])
        #Obtain field type for given class. Defaults to generic SolrField.
        field_class = self.solr_data_types.get(class_name, SolrField)
        return name, SolrFieldTypeFactory(field_class,
            **self.translate_attributes(field_type_node.attrib))

    def field_factory(self, field_node, field_type_classes, dynamic):
        try:
            name, field_type = field_node.attrib['name'], field_node.attrib['type']
        except KeyError, e:
            raise SolrError("Invalid schema.xml: missing %s attribute on field" % e.args[0])
        try:
            field_type_class = field_type_classes[field_type]
        except KeyError, e:
            raise SolrError("Invalid schema.xml: %s field_type undefined" % field_type)
        return name, field_type_class(dynamic=dynamic,
            **self.translate_attributes(field_node.attrib))

    # From XML Datatypes
    attrib_translator = {"true": True, "1": True, "false": False, "0": False}
    def translate_attributes(self, attribs):
        return dict((k, self.attrib_translator.get(v, v))
            for k, v in attribs.items())

    def missing_fields(self, field_names):
        return [name for name in set(self.fields.keys()) - set(field_names)
                if self.fields[name].required]

    def check_fields(self, field_names, required_atts=None):
        if isinstance(field_names, basestring):
            field_names = [field_names]
        if required_atts is None:
            required_atts = {}
        undefined_field_names = []
        for field_name in field_names:
            field = self.match_field(field_name)
            if not field:
                undefined_field_names.append(field_name)
            else:
                for k, v in required_atts.items():
                    if getattr(field, k) != v:
                        raise SolrError("Field '%s' does not have %s=%s" % (field_name, k, v))
        if undefined_field_names:
            raise SolrError("Fields not defined in schema: %s" % list(undefined_field_names))

    def match_dynamic_field(self, name):
        for field in self.dynamic_fields:
            if field.match(name):
                return field

    def match_field(self, name):
        try:
            return self.fields[name]
        except KeyError:
            field = self.match_dynamic_field(name)
        return field

    def field_from_user_data(self, k, v):
        field = self.match_field(k)
        if not field:
            raise SolrError("No such field '%s' in current schema" % k)
        return field.instance_from_user_data(v)

    def make_update(self, docs, **kwargs):
        return SolrUpdate(self, docs, **kwargs)

    def make_delete(self, docs, query):
        return SolrDelete(self, docs, query)

    def parse_response(self, msg):
        return SolrResponse.from_xml(self, msg)

    def parse_result_doc(self, doc, name=None):
        if name is None:
            name = doc.attrib.get('name')
        if doc.tag in ('lst', 'arr'):
            values = [self.parse_result_doc(n, name) for n in doc.getchildren()]
            return name, tuple(v[1] for v in values)
        if doc.tag in 'doc':
            return dict([self.parse_result_doc(n) for n in doc.getchildren()])
        field_class = self.match_field(name)
        if field_class is None and name == "score":
            field_class = SolrScoreField()
        elif field_class is None:
            raise SolrError("unexpected field found in result (field name: %s)" % name)
        return name, SolrFieldInstance.from_solr(field_class, doc.text or '').to_user_data()


class SolrUpdate(object):
    ADD = E.add
    DOC = E.doc
    FIELD = E.field

    def __init__(self, schema, docs, **kwargs):
        self.schema = schema
        self.boosts = kwargs.get( "boosts", {} )
        self.xml = self.add(docs)

    def fields(self, name, values):
        # values may be multivalued - so we treat that as the default case
        if not hasattr(values, "__iter__"):
            values = [values]
        field_values = [self.schema.field_from_user_data(name, value) for value in values]
        ret = []
        for field_value in field_values:
            name_dict = {'name': name}
            if name in self.boosts:
                name_dict["boost"] = self.boosts[name]
            ret += [self.FIELD(name_dict, field_value.to_solr()) ]
        return ret

    def doc(self, doc):
        missing_fields = self.schema.missing_fields(doc.keys())
        if missing_fields:
            raise SolrError("These required fields are unspecified:\n %s" %
                            missing_fields)
        if not doc:
            return self.DOC()
        else:
            return self.DOC(*reduce(operator.add,
                                    [self.fields(name, values)
                                     for name, values in doc.items()]))

    def add(self, docs):
        if hasattr(docs, "items") or not hasattr(docs, "__iter__"):
            # is a dictionary, or anything else except a list
            docs = [docs]
        docs = [(doc if hasattr(doc, "items")
                 else object_to_dict(doc, self.schema))
                for doc in docs]
        return self.ADD(*[self.doc(doc) for doc in docs])

    def __str__(self):
        return lxml.etree.tostring(self.xml, encoding='utf-8')


class SolrDelete(object):
    DELETE = E.delete
    ID = E.id
    QUERY = E.query
    def __init__(self, schema, docs=None, queries=None):
        self.schema = schema
        deletions = []
        if docs is not None:
            deletions += self.delete_docs(docs)
        if queries is not None:
            deletions += self.delete_queries(queries)
        self.xml = self.DELETE(*deletions)

    def delete_docs(self, docs):
        if not self.schema.unique_key:
            raise SolrError("This schema has no unique key - you can only delete by query")
        if hasattr(docs, "items") or not hasattr(docs, "__iter__"):
            # docs is a dictionary, or an object which is not a list
            docs = [docs]
        doc_id_insts = [self.doc_id_from_doc(doc) for doc in docs]
        return [self.ID(doc_id_inst.to_solr()) for doc_id_inst in doc_id_insts]

    def doc_id_from_doc(self, doc):
        # Is this a dictionary, or an document object, or a thing
        # that can be cast to a uniqueKey? (which could also be an
        # arbitrary object.
        if isinstance(doc, (basestring, int, long, float)):
            # It's obviously not a document object, just coerce to appropriate type
            doc_id = doc
        elif hasattr(doc, "items"):
            # It's obviously a dictionary
            try:
                doc_id = doc[self.schema.unique_key]
            except KeyError:
                raise SolrError("No unique key on this document")
        else:
            doc_id = get_attribute_or_callable(doc, self.schema.unique_key)
            if doc_id is None:
                # Well, we couldn't get an ID from it; let's try
                # coercing the doc to the type of an ID field.
                doc_id = doc
        try:
            doc_id_inst = self.schema.unique_field.instance_from_user_data(doc_id)
        except SolrError:
            raise SolrError("Could not parse argument as object or document id")
        return doc_id_inst

    def delete_queries(self, queries):
        if not hasattr(queries, "__iter__"):
            queries = [queries]
        return [self.QUERY(unicode(query)) for query in queries]

    def __str__(self):
        return lxml.etree.tostring(self.xml, encoding='utf-8')


class SolrFacetCounts(object):
    members= ["facet_dates", "facet_fields", "facet_queries"]
    def __init__(self, **kwargs):
        for member in self.members:
            setattr(self, member, kwargs.get(member, ()))
        self.facet_fields = dict(self.facet_fields)

    @classmethod
    def from_response(cls, response):
        facet_counts_dict = dict(response.get("facet_counts", {}))
        return SolrFacetCounts(**facet_counts_dict)


class SolrResponse(object):
    @classmethod
    def from_xml(cls, schema, xmlmsg):
        self = cls()
        self.schema = schema
        self.original_xml = xmlmsg
        doc = lxml.etree.fromstring(xmlmsg)
        details = dict(value_from_node(n) for n in
                       doc.xpath("/response/lst[@name!='moreLikeThis']"))
        details['responseHeader'] = dict(details['responseHeader'])
        for attr in ["QTime", "params", "status"]:
            setattr(self, attr, details['responseHeader'].get(attr))
        if self.status != 0:
            raise ValueError("Response indicates an error")
        result_node = doc.xpath("/response/result")[0]
        self.result = SolrResult.from_xml(schema, result_node)
        self.facet_counts = SolrFacetCounts.from_response(details)
        self.highlighting = dict((k, dict(v))
                                 for k, v in details.get("highlighting", ()))
        more_like_these_nodes = \
            doc.xpath("/response/lst[@name='moreLikeThis']/result")
        more_like_these_results = [SolrResult.from_xml(schema, node)
                                  for node in more_like_these_nodes]
        self.more_like_these = dict((n.name, n)
                                         for n in more_like_these_results)
        if len(self.more_like_these) == 1:
            self.more_like_this = self.more_like_these.values()[0]
        else:
            self.more_like_this = None

        # can be computed by MoreLikeThisHandler
        termsNodes = doc.xpath("/response/*[@name='interestingTerms']")
        if len(termsNodes) == 1:
            _, value = value_from_node(termsNodes[0])
        else:
            value = None
        self.interesting_terms = value
        return self

    def __str__(self):
        return str(self.result)

    def __len__(self):
        return len(self.result.docs)

    def __getitem__(self, key):
        return self.result.docs[key]


class SolrResult(object):
    @classmethod
    def from_xml(cls, schema, node):
        self = cls()
        self.schema = schema
        self.name = node.attrib['name']
        self.numFound = int(node.attrib['numFound'])
        self.start = int(node.attrib['start'])
        self.docs = [schema.parse_result_doc(n) for n in node.xpath("doc")]
        return self

    def __str__(self):
        return "%(numFound)s results found, starting at #%(start)s\n\n" % self.__dict__ + str(self.docs)


def object_to_dict(o, names):
    return dict((name, getattr(o, name)) for name in names
                 if (hasattr(o, name) and getattr(o, name) is not None))

# This is over twice the speed of the shorter one immediately above.
# apparently hasattr is really slow; try/except is faster.
# Also, the one above doesn't and can't do callables with exception handling
def object_to_dict(o, schema):
    d = {}
    for name in schema.fields.keys():
        a = get_attribute_or_callable(o, name)
        if a is not None:
            d[name] = a
    # and now try for dynamicFields:
    try:
        names = o.__dict__.keys()
    except AttributeError:
        names = []
    for name in names:
        field = schema.match_dynamic_field(name)
        if field:
            a = get_attribute_or_callable(o, name)
            if a is not None:
                d[name] = a
    try:
        names = o.__class__.__dict__.keys()
    except AttributeError:
        names = []
    for name in names:
        field = schema.match_dynamic_field(name)
        if field:
            a = get_attribute_or_callable(o, name)
            if a is not None:
                d[name] = a
    return d

def get_attribute_or_callable(o, name):
    try:
        a = getattr(o, name)
        # Might be attribute or callable
        if callable(a):
            try:
                a = a()
            except TypeError:
                a = None
    except AttributeError:
        a = None
    return a

def value_from_node(node):
    name = node.attrib.get('name')
    if node.tag in ('lst', 'arr'):
        value = [value_from_node(n) for n in node.getchildren()]
    if node.tag in 'doc':
        value = dict(value_from_node(n) for n in node.getchildren())
    elif node.tag == 'null':
        value = None
    elif node.tag in ('str', 'byte'):
        value = node.text or ""
    elif node.tag in ('short', 'int'):
        value = int(node.text)
    elif node.tag == 'long':
        value = long(node.text)
    elif node.tag == 'bool':
        value = True if node.text == "true" else False
    elif node.tag in ('float', 'double'):
        value = float(node.text)
    elif node.tag == 'date':
        value = solr_date(node.text)
    if name is not None:
        return name, value
    else:
        return value
