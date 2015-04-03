# -*- coding: utf-8 -*-
#!/usr/bin/python

import sys
if len(sys.argv) < 2:
	print "Please, input oai request url!"
	sys.exit(1)
if not sys.argv[1].startswith("http"):
	print "Invalid url supplied (not starting with http)"
	sys.exit(1)

from oval import validator
validator.main({"base_url":sys.argv[1]})


