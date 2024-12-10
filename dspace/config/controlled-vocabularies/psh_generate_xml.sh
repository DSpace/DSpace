#!/bin/sh
#
# download latest PSH (Polythematic Structured Subject Headings) and save them as DSpace controlled vocabulary XML files psh_en.xml, psh_cs.xml
#
# requires: curl, jq, xmllint
#   sudo apt install curl jq libxml2-utils
# input: PSH in JSON-LD fetched via API: https://psh.techlib.cz/api/doc/
# output: DSpace controlled vocabulary XML; schema and examples: https://github.com/DSpace/DSpace/tree/dspace-6_x/dspace/config/controlled-vocabularies
# input/output sample: https://jqplay.org/s/iom2q4fKqw

printf '<?xml version="1.0" encoding="UTF-8"?>
<!--
retrieved on '$(date +%Y-%m-%d)' from https://psh.techlib.cz/api/doc/
Polythematic Structured Subject Heading System (PSH) created by the Czech National Library of Technology is subject to the Attribution-ShareAlike 3.0 Czech Republic (CC BY-SA 3.0 CZ) license
http://creativecommons.org/licenses/by-sa/3.0/cz/
-->
<node id="PSH" label="Polythematic Structured Subject Heading System">
  <isComposedBy>
' | tee psh_cs.xml > psh_en.xml


max_results_page=$(curl -s --header 'Accept: application/ld+json' 'https://psh.techlib.cz/api/concepts?format=label&page=1' | jq '.info."Max results page"')

for page in $(seq 1 $max_results_page); do
  echo "requesting page $page / $max_results_page";
  curl -s --header 'Accept: application/ld+json' 'https://psh.techlib.cz/api/concepts?format=label&page='$page > $page.json
  jq --raw-output '."@graph" | map("    <node id=\"\(.pshid)\" label=\"\(.prefLabel.cs | gsub("\""; "&quot;"))\"></node>") | .[]' $page.json >> psh_cs.xml
  jq --raw-output '."@graph" | map("    <node id=\"\(.pshid)\" label=\"\(.prefLabel.en)\"></node>") | .[]' $page.json >> psh_en.xml
  rm $page.json
done;

printf '  </isComposedBy>
</node>' | tee -a psh_cs.xml >> psh_en.xml

xmllint --noout --schema controlledvocabulary.xsd psh_cs.xml
xmllint --noout --schema controlledvocabulary.xsd psh_en.xml
