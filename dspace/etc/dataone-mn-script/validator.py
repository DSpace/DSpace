#!/usr/bin/python

import sys
from xml.etree import ElementTree
import requests
import xml.etree.ElementTree
import csv
from subprocess import check_output, check_call, CalledProcessError, STDOUT

DRYAD_SERVER = 'http://dev.datadryad.org/'
DRYAD_MN_ROOT = DRYAD_SERVER + 'mn/'
DRYAD_SCHEMA_LOCATION = DRYAD_SERVER + 'themes/Dryad/meta/schema/v3.1/'
DRYAD_SCHEMA_FILE = 'dryad.xsd'
XMLLINT = '/usr/bin/xmllint'
DRYAD_METADATA_FORMAT = 'http://datadryad.org/profile/v3.1'
DRYAD_REM_FORMAT = 'http://www.openarchives.org/ore/terms'

def tidy(msg):
	msg = msg.replace(DRYAD_SCHEMA_LOCATION,'')
	return msg.replace("http://rs.tdwg.org/dwc/xsd/tdwg_dwcterms.xsd:28: element import: Schemas parser warning : Element '{http://www.w3.org/2001/XMLSchema}import': Skipping import of schema located at 'http://rs.tdwg.org/dwc/xsd/dublin_core.xsd' for the namespace 'http://purl.org/dc/terms/', since this namespace was already imported with the schema located at 'dcterms.xsd'.\nhttp://rs.tdwg.org/dwc/xsd/tdwg_dwcterms.xsd:29: element import: Schemas parser warning : Element '{http://www.w3.org/2001/XMLSchema}import': Skipping import of schema located at 'http://dublincore.org/schemas/xmls/qdc/dc.xsd' for the namespace 'http://purl.org/dc/elements/1.1/', since this namespace was already imported with the schema located at 'dc.xsd'.\n", "").rstrip()

def validate_metadata(url):
	try:
		result = check_output([XMLLINT, "--noout", "--schema", DRYAD_SCHEMA_LOCATION + '/' + DRYAD_SCHEMA_FILE, url], stderr=STDOUT)
	except CalledProcessError as e:
		try:
			content = requests.get(url).text
		except requests.RequestException:
			content = 'Unable to load content'
		return {'type': 'metadata', 'object': url, 'code': e.returncode, 'output': tidy(e.output), 'content': content.encode('utf8')}
	return {'type': 'metadata', 'object': url, 'code': 0, 'output': tidy(result), 'content': None}

def validate_rem(url):
	try:
		result = check_output([XMLLINT, "--noout", url], stderr=STDOUT)
	except CalledProcessError as e:
		try:
			content = requests.get(url).text
		except requests.RequestException:
			content = 'Unable to load content'
		return {'type': 'rem', 'object': url, 'code': e.returncode, 'output': e.output, 'content': content.encode('utf8')}
	if len(result) == 0:
		result = 'OK'
	return {'type': 'rem', 'object': url, 'code': 0, 'output': result, 'content': None}


def get_total(mn_object_url):
	r = requests.get(mn_object_url, params={'start': 0, 'count': 0})
	return int(ElementTree.fromstring(r.text).get('total'))


def get_objects(mn_object_url, start, count):
	r = requests.get(mn_object_url, params={'start': start, 'count': count})
	root = ElementTree.fromstring(r.text)
	infos = root.findall('.//objectInfo')
	objects = {DRYAD_REM_FORMAT: [], DRYAD_METADATA_FORMAT: []}
	for info in infos:
		formatId = info.find('formatId').text
		if formatId not in objects:
			objects[formatId] = []
		objects[formatId].append(mn_object_url + info.find('identifier').text)
	return objects

def validate_objects():
	object_url = DRYAD_MN_ROOT + 'object/'
	total_objects = get_total(object_url)
	step = 100
	with open('validation.csv', 'w') as f:
		writer = csv.DictWriter(f, ['type', 'object', 'code', 'output', 'content'])
		writer.writeheader()
		for offset in range(0, total_objects, step):
			urls = get_objects(object_url, offset, step)
			for rem_object in urls[DRYAD_REM_FORMAT]:
				result = validate_rem(rem_object)
				writer.writerow(result)
			for metadata_object in urls[DRYAD_METADATA_FORMAT]:
				result = validate_metadata(metadata_object)
				writer.writerow(result)
			for format_id in urls.keys():
				if format_id in [DRYAD_METADATA_FORMAT, DRYAD_REM_FORMAT]:
					continue
				for other_object in urls[format_id]:
					result = {'type': format_id, 'object': other_object, 'code': -1, 'output': 'ignored', 'content': None}
					writer.writerow(result)


if __name__ == '__main__':
	validate_objects()
