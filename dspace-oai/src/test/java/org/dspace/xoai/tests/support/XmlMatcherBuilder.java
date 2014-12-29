/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.support;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.xmlmatchers.XmlMatchers;
import org.xmlmatchers.transform.StringSource;

import javax.xml.namespace.NamespaceContext;
import java.util.*;


public class XmlMatcherBuilder extends MatcherBuilder<XmlMatcherBuilder, String> {
    public static XmlMatcherBuilder xml () {
        return new XmlMatcherBuilder();
    }

    private Namespaces namespaces = new Namespaces();

    public XmlMatcherBuilder() {
        with(new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                try {
                    StringSource.toSource(item);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is valid XML");
            }
        });
    }

    public XmlMatcherBuilder withXPath(final String xPath) {
        return with(new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return XmlMatchers.hasXPath(xPath, namespaces).matches(StringSource.toSource(item));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has XPath ").appendValue(xPath);
            }
        });
    }

    public XmlMatcherBuilder withXPath(final String xPath, final Matcher<? super String> subMatcher) {
        return with(new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return XmlMatchers
                        .hasXPath(xPath, namespaces, subMatcher)
                        .matches(StringSource.toSource(item));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has XPath ").appendValue(xPath);
            }
        });
    }

    public XmlMatcherBuilder withNamespace (String prefix, String uri) {
        namespaces.with(prefix, uri);
        return this;
    }

    private static class Namespaces implements NamespaceContext {
        private Map<String, String> namespace = new HashMap<>();

        @Override
        public String getNamespaceURI(String prefix) {
            return namespace.get(prefix);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            Iterator<String> prefixes = getPrefixes(namespaceURI);

            if (!prefixes.hasNext()) return null;
            else return prefixes.next();
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, String> stringStringEntry : namespace.entrySet()) {
                if (stringStringEntry.getValue().equals(namespaceURI))
                    list.add(stringStringEntry.getKey());
            }

            return list.iterator();
        }

        public void with(String prefix, String uri) {
            namespace.put(prefix, uri);
        }
    }
}
