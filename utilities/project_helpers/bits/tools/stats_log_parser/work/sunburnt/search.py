from __future__ import absolute_import

import collections, copy, operator, re

from .schema import SolrError, SolrBooleanField, SolrUnicodeField, WildcardFieldInstance


class LuceneQuery(object):
    default_term_re = re.compile(r'^\w+$')
    def __init__(self, schema, option_flag=None, original=None):
        self.schema = schema
        self.normalized = False
        if original is None:
            self.option_flag = option_flag
            self.terms = collections.defaultdict(set)
            self.phrases = collections.defaultdict(set)
            self.ranges = set()
            self.subqueries = []
            self._and = True
            self._or = self._not = self._pow = False
            self.boosts = []
        else:
            self.option_flag = original.option_flag
            self.terms = copy.copy(original.terms)
            self.phrases = copy.copy(original.phrases)
            self.ranges = copy.copy(original.ranges)
            self.subqueries = copy.copy(original.subqueries)
            self._or = original._or
            self._and = original._and
            self._not = original._not
            self._pow = original._pow
            self.boosts = copy.copy(original.boosts)

    def clone(self):
        return LuceneQuery(self.schema, original=self)

    def options(self):
        opts = {}
        s = unicode(self)
        if s:
            opts[self.option_flag] = s
        return opts

    def serialize_debug(self, indent=0):
        indentspace = indent * ' '
        print '%s%s (%s)' % (indentspace, repr(self), "Normalized" if self.normalized else "Not normalized")
        print '%s%s' % (indentspace, '{')
        for term in self.terms.items():
            print '%s%s' % (indentspace, term)
        for phrase in self.phrases.items():
            print '%s%s' % (indentspace, phrase)
        for range in self.ranges:
            print '%s%s' % (indentspace, range)
        if self.subqueries:
            if self._and:
                print '%sAND:' % indentspace
            elif self._or:
                print '%sOR:' % indentspace
            elif self._not:
                print '%sNOT:' % indentspace
            elif self._pow is not False:
                print '%sPOW %s:' % (indentspace, self._pow)
            else:
                raise ValueError
            for subquery in self.subqueries:
                subquery.serialize_debug(indent+2)
        print '%s%s' % (indentspace, '}')

    # Below, we sort all our value_sets - this is for predictability when testing.
    def serialize_term_queries(self, terms):
        s = []
        for name, value_set in terms.items():
            if name:
                field = self.schema.match_field(name)
            else:
                field = self.schema.default_field
            if name:
                s += [u'%s:%s' % (name, value.to_query()) for value in value_set]
            else:
                s += [value.to_query() for value in value_set]
        return u' AND '.join(sorted(s))

    range_query_templates = {
        "any": u"[* TO *]",
        "lt": u"{* TO %s}",
        "lte": u"[* TO %s]",
        "gt": u"{%s TO *}",
        "gte": u"[%s TO *]",
        "rangeexc": u"{%s TO %s}",
        "range": u"[%s TO %s]",
    }
    def serialize_range_queries(self):
        s = []
        for name, rel, values in sorted(self.ranges):
            range_s = self.range_query_templates[rel] % \
                tuple(value.to_query() for value in sorted(values, key=lambda x: getattr(x, "value")))
            s.append(u"%s:%s" % (name, range_s))
        return u' AND '.join(s)

    def child_needs_parens(self, child):
        if len(child) == 1:
            return False
        elif self._or:
            return not (child._or or child._pow)
        elif (self._and or self._not):
            return not (child._and or child._not or child._pow)
        elif self._pow is not False:
            return True
        else:
            return True

    @staticmethod
    def merge_term_dicts(*args):
        d = collections.defaultdict(set)
        for arg in args:
            for k, v in arg.items():
                d[k].update(v)
        return dict((k, v) for k, v in d.items())

    def normalize(self):
        if self.normalized:
            return self, False
        mutated = False
        _subqueries = []
        _terms = self.terms
        _phrases = self.phrases
        _ranges = self.ranges
        for s in self.subqueries:
            _s, changed = s.normalize()
            if not _s or changed:
                mutated = True
            if _s:
                if (_s._and and self._and) or (_s._or and self._or):
                    mutated = True
                    _terms = self.merge_term_dicts(_terms, _s.terms)
                    _phrases = self.merge_term_dicts(_phrases, _s.phrases)
                    _ranges = _ranges.union(_s.ranges)
                    _subqueries.extend(_s.subqueries)
                else:
                    _subqueries.append(_s)
        if mutated:
            newself = self.clone()
            newself.terms = _terms
            newself.phrases = _phrases
            newself.ranges = _ranges
            newself.subqueries = _subqueries
            self = newself

        if self._not:
            if not len(self.subqueries):
                newself = self.clone()
                newself._not = False
                newself._and = True
                self = newself
                mutated = True
            elif len(self.subqueries) == 1:
                if self.subqueries[0]._not:
                    newself = self.clone()
                    newself.subqueries = self.subqueries[0].subqueries
                    newself._not = False
                    newself._and = True
                    self = newself
                    mutated = True
            else:
                raise ValueError
        elif self._pow:
            if not len(self.subqueries):
                newself = self.clone()
                newself._pow = False
                self = newself
                mutated = True
        elif self._and or self._or:
            if not self.terms and not self.phrases and not self.ranges \
               and not self.boosts:
                if len(self.subqueries) == 1:
                    self = self.subqueries[0]
                    mutated = True
        self.normalized = True
        return self, mutated

    def __unicode__(self, level=0, op=None):
        if not self.normalized:
            self, _ = self.normalize()
        if self.boosts:
            # Clone and rewrite to effect the boosts.
            newself = self.clone()
            newself.boosts = []
            boost_queries = [self.Q(**kwargs)**boost_score
                             for kwargs, boost_score in self.boosts]
            newself = newself | (newself & reduce(operator.or_, boost_queries))
            newself, _ = newself.normalize()
            return newself.__unicode__(level=level)
        else:
            u = [s for s in [self.serialize_term_queries(self.terms),
                             self.serialize_term_queries(self.phrases),
                             self.serialize_range_queries()]
                 if s]
            for q in self.subqueries:
                op_ = u'OR' if self._or else u'AND'
                if self.child_needs_parens(q):
                    u.append(u"(%s)"%q.__unicode__(level=level+1, op=op_))
                else:
                    u.append(u"%s"%q.__unicode__(level=level+1, op=op_))
            if self._and:
                return u' AND '.join(u)
            elif self._or:
                return u' OR '.join(u)
            elif self._not:
                assert len(u) == 1
                if level == 0 or (level == 1 and op == "AND"):
                    return u'NOT %s'%u[0]
                else:
                    return u'(*:* AND NOT %s)'%u[0]
            elif self._pow is not False:
                assert len(u) == 1
                return u"%s^%s"%(u[0], self._pow)
            else:
                raise ValueError

    def __len__(self):
        # How many terms in this (sub) query?
        if len(self.subqueries) == 1:
            subquery_length = len(self.subqueries[0])
        else:
            subquery_length = len(self.subqueries)
        return sum([sum(len(v) for v in self.terms.values()),
                    sum(len(v) for v in self.phrases.values()),
                    len(self.ranges),
                    subquery_length])

    def Q(self, *args, **kwargs):
        q = LuceneQuery(self.schema)
        q.add(args, kwargs)
        return q

    def __nonzero__(self):
        return bool(self.terms) or bool(self.phrases) or bool(self.ranges) or bool(self.subqueries)

    def __or__(self, other):
        q = LuceneQuery(self.schema)
        q._and = False
        q._or = True
        q.subqueries = [self, other]
        return q

    def __and__(self, other):
        q = LuceneQuery(self.schema)
        q.subqueries = [self, other]
        return q

    def __invert__(self):
        q = LuceneQuery(self.schema)
        q._and = False
        q._not = True
        q.subqueries = [self]
        return q

    def __pow__(self, value):
        try:
            float(value)
        except ValueError:
            raise ValueError("Non-numeric value supplied for boost")
        q = LuceneQuery(self.schema)
        q.subqueries = [self]
        q._and = False
        q._pow = value
        return q
        
    def add(self, args, kwargs):
        self.normalized = False
        _args = []
        for arg in args:
            if isinstance(arg, LuceneQuery):
                self.subqueries.append(arg)
            else:
                _args.append(arg)
        args = _args
        try:
            terms_or_phrases = kwargs.pop("__terms_or_phrases")
        except KeyError:
            terms_or_phrases = None
        for value in args:
            self.add_exact(None, value, terms_or_phrases)
        for k, v in kwargs.items():
            try:
                field_name, rel = k.split("__")
            except ValueError:
                field_name, rel = k, 'eq'
            field = self.schema.match_field(field_name)
            if not field:
                if (k, v) != ("*", "*"):
                    # the only case where wildcards in field names are allowed
                    raise ValueError("%s is not a valid field name" % k)
            elif not field.indexed:
                raise SolrError("Can't query on non-indexed field '%s'" % field_name)
            if rel == 'eq':
                self.add_exact(field_name, v, terms_or_phrases)
            else:
                self.add_range(field_name, rel, v)

    def add_exact(self, field_name, values, term_or_phrase):
        # We let people pass in a list of values to match.
        # This really only makes sense for text fields or
        # multivalued fields.
        if not hasattr(values, "__iter__"):
            values = [values]
        # We can only do a field_name == "*" if:
        if field_name and field_name != "*":
            field = self.schema.match_field(field_name)
        elif not field_name:
            field = self.schema.default_field
        else: # field_name must be "*"
            if len(values) == 1 and values[0] == "*":
                self.terms["*"].add(WildcardFieldInstance.from_user_data())
                return
            else:
                raise SolrError("If field_name is '*', then only '*' is permitted as the query")
        insts = [field.instance_from_user_data(value) for value in values]
        for inst in insts:
            if isinstance(field, SolrUnicodeField):
                this_term_or_phrase = term_or_phrase or self.term_or_phrase(inst.value)
            else:
                this_term_or_phrase = "terms"
            getattr(self, this_term_or_phrase)[field_name].add(inst)

    def add_range(self, field_name, rel, value):
        field = self.schema.match_field(field_name)
        if isinstance(field, SolrBooleanField):
            raise ValueError("Cannot do a '%s' query on a bool field" % rel)
        if rel not in self.range_query_templates:
            raise SolrError("No such relation '%s' defined" % rel)
        if rel in ('range', 'rangeexc'):
            try:
                assert len(value) == 2
            except (AssertionError, TypeError):
                raise SolrError("'%s__%s' argument must be a length-2 iterable"
                                 % (field_name, rel))
            insts = tuple(sorted(field.instance_from_user_data(v) for v in value))
        elif rel == 'any':
            if value is not True:
                raise SolrError("'%s__%s' argument must be True")
            insts = ()
        else:
            insts = (field.instance_from_user_data(value),)
        self.ranges.add((field_name, rel, insts))

    def term_or_phrase(self, arg, force=None):
        return 'terms' if self.default_term_re.match(arg) else 'phrases'

    def add_boost(self, kwargs, boost_score):
        for k, v in kwargs.items():
            field = self.schema.match_field(k)
            if not field:
                raise ValueError("%s is not a valid field name" % k)
            elif not field.indexed:
                raise SolrError("Can't query on non-indexed field '%s'" % field_name)
            value = field.instance_from_user_data(v)
        self.boosts.append((kwargs, boost_score))



class BaseSearch(object):
    """Base class for common search options management"""
    option_modules = ('query_obj', 'filter_obj', 'paginator',
                      'more_like_this', 'highlighter', 'faceter',
                      'sorter', 'facet_querier', 'field_limiter',)

    result_constructor = dict

    def _init_common_modules(self):
        self.query_obj = LuceneQuery(self.schema, u'q')
        self.filter_obj = LuceneQuery(self.schema, u'fq')
        self.paginator = PaginateOptions(self.schema)
        self.highlighter = HighlightOptions(self.schema)
        self.faceter = FacetOptions(self.schema)
        self.sorter = SortOptions(self.schema)
        self.field_limiter = FieldLimitOptions(self.schema)
        self.facet_querier = FacetQueryOptions(self.schema)

    def clone(self):
        return self.__class__(interface=self.interface, original=self)

    def Q(self, *args, **kwargs):
        q = LuceneQuery(self.schema)
        q.add(args, kwargs)
        return q

    def query(self, *args, **kwargs):
        newself = self.clone()
        newself.query_obj.add(args, kwargs)
        return newself

    def query_by_term(self, *args, **kwargs):
        return self.query(__terms_or_phrases="terms", *args, **kwargs)

    def query_by_phrase(self, *args, **kwargs):
        return self.query(__terms_or_phrases="phrases", *args, **kwargs)

    def exclude(self, *args, **kwargs):
        # cloning will be done by query
        return self.query(~self.Q(*args, **kwargs))

    def boost_relevancy(self, boost_score, **kwargs):
        if not self.query_obj:
            raise TypeError("Can't boost the relevancy of an empty query")
        try:
            float(boost_score)
        except ValueError:
            raise ValueError("Non-numeric boost value supplied")

        newself = self.clone()
        newself.query_obj.add_boost(kwargs, boost_score)
        return newself

    def filter(self, *args, **kwargs):
        newself = self.clone()
        newself.filter_obj.add(args, kwargs)
        return newself

    def filter_by_term(self, *args, **kwargs):
        return self.filter(__terms_or_phrases="terms", *args, **kwargs)

    def filter_by_phrase(self, *args, **kwargs):
        return self.filter(__terms_or_phrases="phrases", *args, **kwargs)

    def filter_exclude(self, *args, **kwargs):
        # cloning will be done by filter
        return self.filter(~self.Q(*args, **kwargs))

    def facet_by(self, field, **kwargs):
        newself = self.clone()
        newself.faceter.update(field, **kwargs)
        return newself

    def facet_query(self, *args, **kwargs):
        newself = self.clone()
        newself.facet_querier.update(self.Q(*args, **kwargs))
        return newself

    def highlight(self, fields=None, **kwargs):
        newself = self.clone()
        newself.highlighter.update(fields, **kwargs)
        return newself

    def mlt(self, fields, query_fields=None, **kwargs):
        newself = self.clone()
        newself.more_like_this.update(fields, query_fields, **kwargs)
        return newself

    def paginate(self, start=None, rows=None):
        newself = self.clone()
        newself.paginator.update(start, rows)
        return newself

    def sort_by(self, field):
        newself = self.clone()
        newself.sorter.update(field)
        return newself

    def field_limit(self, fields=None, score=False, all_fields=False):
        newself = self.clone()
        newself.field_limiter.update(fields, score, all_fields)
        return newself

    def options(self):
        options = {}
        for option_module in self.option_modules:
            options.update(getattr(self, option_module).options())
        # Next line is for pre-2.6.5 python
        return dict((k.encode('utf8'), v) for k, v in options.items())

    def results_as(self, constructor):
        newself = self.clone()
        newself.result_constructor = constructor
        return newself

    def transform_result(self, result, constructor):
        if constructor is not dict:
            construct_docs = lambda docs: [constructor(**d) for d in docs]
            result.result.docs = construct_docs(result.result.docs)
            for key in result.more_like_these:
                result.more_like_these[key].docs = \
                        construct_docs(result.more_like_these[key].docs)
            # in future, highlighting chould be made available to
            # custom constructors; perhaps document additional
            # arguments result constructors are required to support, or check for
            # an optional set_highlighting method
        else:
            if result.highlighting:
                for d in result.result.docs:
                    # if the unique key for a result doc is present in highlighting,
                    # add the highlighting for that document into the result dict
                    # (but don't override any existing content)
                    # If unique key field is not a string field (eg int) then we need to
                    # convert it to its solr representation
                    unique_key = self.schema.fields[self.schema.unique_key].to_solr(d[self.schema.unique_key])
                    if 'solr_highlights' not in d and \
                           unique_key in result.highlighting:
                        d['solr_highlights'] = result.highlighting[unique_key]
        return result

    def params(self):
        return params_from_dict(**self.options())

    ## methods to allow SolrSearch to be used with Django paginator ##

    _count = None
    def count(self):
        # get the total count for the current query without retrieving any results 
        # cache it, since it may be needed multiple times when used with django paginator
        if self._count is None:
            # are we already paginated? then we'll behave as if that's
            # defined our result set already.
            if self.paginator.rows is not None:
                total_results = self.paginator.rows
            else:
                response = self.paginate(rows=0).execute()
                total_results = response.result.numFound
                if self.paginator.start is not None:
                    total_results -= self.paginator.start
            self._count = total_results
        return self._count

    __len__ = count

    def __getitem__(self, k):
        """Return a single result or slice of results from the query.
        """
        # are we already paginated? if so, we'll apply this getitem to the
        # paginated result - else we'll apply it to the whole.
        offset = 0 if self.paginator.start is None else self.paginator.start

        if isinstance(k, slice):
            # calculate solr pagination options for the requested slice
            step = operator.index(k.step) if k.step is not None else 1
            if step == 0:
                raise ValueError("slice step cannot be zero")
            if step > 0:
                s1 = k.start
                s2 = k.stop
                inc = 0
            else:
                s1 = k.stop
                s2 = k.start
                inc = 1

            if s1 is not None:
                start = operator.index(s1)
                if start < 0:
                    start += self.count()
                    start = max(0, start)
                start += inc
            else:
                start = 0
            if s2 is not None:
                stop = operator.index(s2)
                if stop < 0:
                    stop += self.count()
                    stop = max(0, stop)
                stop += inc
            else:
                stop = self.count()

            rows = stop - start
            if self.paginator.rows is not None:
                rows = min(rows, self.paginator.rows)
            rows = max(rows, 0)

            start += offset

            response = self.paginate(start=start, rows=rows).execute()
            if step != 1:
                response.result.docs = response.result.docs[::step]
            return response

        else:
            # if not a slice, a single result is being requested
            k = operator.index(k)
            if k < 0:
                k += self.count()
                if k < 0:
                    raise IndexError("list index out of range")

            # Otherwise do the query anyway, don't count() to avoid extra Solr call
            k += offset
            response = self.paginate(start=k, rows=1).execute()
            if response.result.numFound < k:
                raise IndexError("list index out of range")
            return response.result.docs[0]


class SolrSearch(BaseSearch):
    def __init__(self, interface, original=None):
        self.interface = interface
        self.schema = interface.schema
        if original is None:
            self.more_like_this = MoreLikeThisOptions(self.schema)
            self._init_common_modules()
        else:
            for opt in self.option_modules:
                setattr(self, opt, getattr(original, opt).clone())
            self.result_constructor = original.result_constructor

    def options(self):
        options = super(SolrSearch, self).options()
        if 'q' not in options:
            options['q'] = '*:*' # search everything
        return options

    def execute(self, constructor=None):
        if constructor is None:
            constructor = self.result_constructor
        result = self.interface.search(**self.options())
        return self.transform_result(result, constructor)


class MltSolrSearch(BaseSearch):
    """Manage parameters to build a MoreLikeThisHandler query"""
    trivial_encodings = ["utf_8", "u8", "utf", "utf8", "ascii", "646", "us_ascii"]
    def __init__(self, interface, content=None, content_charset=None, url=None,
                 original=None):
        self.interface = interface
        self.schema = interface.schema
        if original is None:
            if content is not None and url is not None:
                raise ValueError(
                    "Cannot specify both content and url")
            if content is not None:
                if content_charset is None:
                    content_charset = 'utf-8'
                if isinstance(content, unicode):
                    content = content.encode('utf-8')
                elif content_charset.lower().replace('-', '_') not in self.trivial_encodings:
                    content = content.decode(content_charset).encode('utf-8')
            self.content = content
            self.url = url
            self.more_like_this = MoreLikeThisHandlerOptions(self.schema)
            self._init_common_modules()
        else:
            self.content = original.content
            self.url = original.url
            for opt in self.option_modules:
                setattr(self, opt, getattr(original, opt).clone())

    def query(self, *args, **kwargs):
        if self.content is not None or self.url is not None:
            raise ValueError("Cannot specify query as well as content on an MltSolrSearch")
        return super(MltSolrSearch, self).query(*args, **kwargs)

    def query_by_term(self, *args, **kwargs):
        if self.content is not None or self.url is not None:
            raise ValueError("Cannot specify query as well as content on an MltSolrSearch")
        return super(MltSolrSearch, self).query_by_term(*args, **kwargs)

    def query_by_phrase(self, *args, **kwargs):
        if self.content is not None or self.url is not None:
            raise ValueError("Cannot specify query as well as content on an MltSolrSearch")
        return super(MltSolrSearch, self).query_by_phrase(*args, **kwargs)

    def exclude(self, *args, **kwargs):
        if self.content is not None or self.url is not None:
            raise ValueError("Cannot specify query as well as content on an MltSolrSearch")
        return super(MltSolrSearch, self).exclude(*args, **kwargs)

    def Q(self, *args, **kwargs):
        if self.content is not None or self.url is not None:
            raise ValueError("Cannot specify query as well as content on an MltSolrSearch")
        return super(MltSolrSearch, self).Q(*args, **kwargs)

    def boost_relevancy(self, *args, **kwargs):
        if self.content is not None or self.url is not None:
            raise ValueError("Cannot specify query as well as content on an MltSolrSearch")
        return super(MltSolrSearch, self).boost_relevancy(*args, **kwargs)

    def options(self):
        options = super(MltSolrSearch, self).options()
        if self.url is not None:
            options['stream.url'] = self.url
        return options

    def execute(self, constructor=dict):
        result = self.interface.mlt_search(content=self.content, **self.options())
        return self.transform_result(result, constructor)


class Options(object):
    def clone(self):
        return self.__class__(self.schema, self)

    def invalid_value(self, msg=""):
        assert False, msg

    def update(self, fields=None, **kwargs):
        if fields:
            self.schema.check_fields(fields)
            if isinstance(fields, basestring):
                fields = [fields]
            for field in set(fields) - set(self.fields):
                self.fields[field] = {}
        elif kwargs:
            fields = [None]
        checked_kwargs = self.check_opts(kwargs)
        for k, v in checked_kwargs.items():
            for field in fields:
                self.fields[field][k] = v

    def check_opts(self, kwargs):
        checked_kwargs = {}
        for k, v in kwargs.items():
            if k not in self.opts:
                raise SolrError("No such option for %s: %s" % (self.option_name, k))
            opt_type = self.opts[k]
            try:
                if isinstance(opt_type, (list, tuple)):
                    assert v in opt_type
                elif isinstance(opt_type, type):
                    v = opt_type(v)
                else:
                    v = opt_type(self, v)
            except:
                raise SolrError("Invalid value for %s option %s: %s" % (self.option_name, k, v))
            checked_kwargs[k] = v
        return checked_kwargs

    def options(self):
        opts = {}
        if self.fields:
            opts[self.option_name] = True
            fields = [field for field in self.fields if field]
            self.field_names_in_opts(opts, fields)
        for field_name, field_opts in self.fields.items():
            if not field_name:
                for field_opt, v in field_opts.items():
                    opts['%s.%s'%(self.option_name, field_opt)] = v
            else:
                for field_opt, v in field_opts.items():
                    opts['f.%s.%s.%s'%(field_name, self.option_name, field_opt)] = v
        return opts


class FacetOptions(Options):
    option_name = "facet"
    opts = {"prefix":unicode,
            "sort":[True, False, "count", "index"],
            "limit":int,
            "offset":lambda self, x: int(x) >= 0 and int(x) or self.invalid_value(),
            "mincount":lambda self, x: int(x) >= 0 and int(x) or self.invalid_value(),
            "missing":bool,
            "method":["enum", "fc"],
            "enum.cache.minDf":int,
            }

    def __init__(self, schema, original=None):
        self.schema = schema
        if original is None:
            self.fields = collections.defaultdict(dict)
        else:
            self.fields = copy.copy(original.fields)

    def field_names_in_opts(self, opts, fields):
        if fields:
            opts["facet.field"] = sorted(fields)


class HighlightOptions(Options):
    option_name = "hl"
    opts = {"snippets":int,
            "fragsize":int,
            "mergeContinuous":bool,
            "requireFieldMatch":bool,
            "maxAnalyzedChars":int,
            "alternateField":lambda self, x: x if x in self.schema.fields else self.invalid_value(),
            "maxAlternateFieldLength":int,
            "formatter":["simple"],
            "simple.pre":unicode,
            "simple.post":unicode,
            "fragmenter":unicode,
            "useFastVectorHighlighter":bool,	# available as of Solr 3.1
            "usePhraseHighlighter":bool,
            "highlightMultiTerm":bool,
            "regex.slop":float,
            "regex.pattern":unicode,
            "regex.maxAnalyzedChars":int
            }
    def __init__(self, schema, original=None):
        self.schema = schema
        if original is None:
            self.fields = collections.defaultdict(dict)
        else:
            self.fields = copy.copy(original.fields)

    def field_names_in_opts(self, opts, fields):
        if fields:
            opts["hl.fl"] = ",".join(sorted(fields))


class MoreLikeThisOptions(Options):
    option_name = "mlt"
    opts = {"count":int,
            "mintf":int,
            "mindf":int,
            "minwl":int,
            "maxwl":int,
            "maxqt":int,
            "maxntp":int,
            "boost":bool,
            }
    def __init__(self, schema, original=None):
        self.schema = schema
        if original is None:
            self.fields = set()
            self.query_fields = {}
            self.kwargs = {}
        else:
            self.fields = copy.copy(original.fields)
            self.query_fields = copy.copy(original.query_fields)
            self.kwargs = copy.copy(original.kwargs)

    def update(self, fields, query_fields=None, **kwargs):
        if fields is None:
            fields = [self.schema.default_field_name]
        self.schema.check_fields(fields)
        if isinstance(fields, basestring):
            fields = [fields]
        self.fields.update(fields)

        if query_fields is not None:
            for k, v in query_fields.items():
                if k not in self.fields:
                    raise SolrError("'%s' specified in query_fields but not fields"% k)
                if v is not None:
                    try:
                        v = float(v)
                    except ValueError:
                        raise SolrError("'%s' has non-numerical boost value"% k)
            self.query_fields.update(query_fields)

        checked_kwargs = self.check_opts(kwargs)
        self.kwargs.update(checked_kwargs)

    def options(self):
        opts = {}
        if self.fields:
            opts['mlt'] = True
            opts['mlt.fl'] = ','.join(sorted(self.fields))

        if self.query_fields:
            qf_arg = []
            for k, v in self.query_fields.items():
                if v is None:
                    qf_arg.append(k)
                else:
                    qf_arg.append("%s^%s" % (k, float(v)))
            opts["mlt.qf"] = " ".join(qf_arg)

        for opt_name, opt_value in self.kwargs.items():
            opt_type = self.opts[opt_name]
            opts["mlt.%s" % opt_name] = opt_type(opt_value)

        return opts


class MoreLikeThisHandlerOptions(MoreLikeThisOptions):
    opts = {'match.include': bool,
            'match.offset': int,
            'interestingTerms': ["list", "details", "none"],
           }
    opts.update(MoreLikeThisOptions.opts)
    del opts['count']

    def options(self):
        opts = {}
        if self.fields:
            opts['mlt.fl'] = ','.join(sorted(self.fields))

        if self.query_fields:
            qf_arg = []
            for k, v in self.query_fields.items():
                if v is None:
                    qf_arg.append(k)
                else:
                    qf_arg.append("%s^%s" % (k, float(v)))
            opts["mlt.qf"] = " ".join(qf_arg)

        for opt_name, opt_value in self.kwargs.items():
            opts["mlt.%s" % opt_name] = opt_value

        return opts


class PaginateOptions(Options):
    def __init__(self, schema, original=None):
        self.schema = schema
        if original is None:
            self.start = None
            self.rows = None
        else:
            self.start = original.start
            self.rows = original.rows

    def update(self, start, rows):
        if start is not None:
            if start < 0:
                raise SolrError("paginator start index must be 0 or greater")
            self.start = start
        if rows is not None:
            if rows < 0:
                raise SolrError("paginator rows must be 0 or greater")
            self.rows = rows

    def options(self):
        opts = {}
        if self.start is not None:
            opts['start'] = self.start
        if self.rows is not None:
            opts['rows'] = self.rows
        return opts


class SortOptions(Options):
    option_name = "sort"
    def __init__(self, schema, original=None):
        self.schema = schema
        if original is None:
            self.fields = []
        else:
            self.fields = copy.copy(original.fields)

    def update(self, field):
        # We're not allowing function queries a la Solr1.5
        if field.startswith('-'):
            order = "desc"
            field = field[1:]
        elif field.startswith('+'):
            order = "asc"
            field = field[1:]
        else:
            order = "asc"
        if field != 'score':
            f = self.schema.match_field(field)
            if not f:
                raise SolrError("No such field %s" % field)
            elif f.multi_valued:
                raise SolrError("Cannot sort on a multivalued field")
            elif not f.indexed:
                raise SolrError("Cannot sort on an un-indexed field")
        self.fields.append([order, field])

    def options(self):
        if self.fields:
            return {"sort":", ".join("%s %s" % (field, order) for order, field in self.fields)}
        else:
            return {}


class FieldLimitOptions(Options):
    option_name = "fl"

    def __init__(self, schema, original=None):
        self.schema = schema
        if original is None:
            self.fields = set()
            self.score = False
            self.all_fields = False
        else:
            self.fields = copy.copy(original.fields)
            self.score = original.score
            self.all_fields = original.all_fields

    def update(self, fields=None, score=False, all_fields=False):
        if fields is None:
            fields = []
        if isinstance(fields, basestring):
            fields = [fields]
        self.schema.check_fields(fields, {"stored": True})
        self.fields.update(fields)
        self.score = score
        self.all_fields = all_fields

    def options(self):
        opts = {}
        if self.all_fields:
            fields = set("*")
        else:
            fields = self.fields
        if self.score:
            fields.add("score")
        if fields:
            opts['fl'] = ','.join(sorted(fields))
        return opts


class FacetQueryOptions(Options):
    def __init__(self, schema, original=None):
        self.schema = schema
        if original is None:
            self.queries = []
        else:
            self.queries = [q.clone() for q in original.queries]

    def update(self, query):
        self.queries.append(query)

    def options(self):
        if self.queries:
            return {'facet.query':[unicode(q) for q in self.queries],
                    'facet':True}
        else:
            return {}

def params_from_dict(**kwargs):
    utf8_params = []
    for k, vs in kwargs.items():
        if isinstance(k, unicode):
            k = k.encode('utf-8')
        # We allow for multivalued options with lists.
        if not hasattr(vs, "__iter__"):
            vs = [vs]
        for v in vs:
            if isinstance(v, bool):
                v = u"true" if v else u"false"
            else:
                v = unicode(v)
            v = v.encode('utf-8')
            utf8_params.append((k, v))
    return sorted(utf8_params)
