from .strings import RawString


def test_string_escape():
    """ Ensure that string characters are escaped correctly for Solr queries.
    """
    test_str = u'+-&|!(){}[]^"~*?: \t\v\\/'
    escaped = RawString(test_str).escape_for_lqs_term()
    assert escaped == u'\\+\\-\\&\\|\\!\\(\\)\\{\\}\\[\\]\\^\\"\\~\\*\\?\\:\\ \\\t\\\x0b\\\\\\/'


