import {
  WorkspaceitemSectionSherpaPoliciesObject
} from '../../core/submission/models/workspaceitem-section-sherpa-policies.model';

export const SherpaDataResponse = {
  'id': 'sherpaPolicies',
  'retrievalTime': '2022-04-20T09:44:39.870+00:00',
  'sherpaResponse':
  {
    'error': false,
    'message': null,
    'metadata': {
      'id': 23803,
      'uri': 'http://v2.sherpa.ac.uk/id/publication/23803',
      'dateCreated': '2012-11-20 14:51:52',
      'dateModified': '2020-03-06 11:25:54',
      'inDOAJ': false,
      'publiclyVisible': true
    },
    'journals': [{
      'titles': ['The Lancet', 'Lancet'],
      'url': 'http://www.thelancet.com/journals/lancet/issue/current',
      'issns': ['0140-6736', '1474-547X'],
      'romeoPub': 'Elsevier: The Lancet',
      'zetoPub': 'Elsevier: The Lancet',
      'publisher': {
        'name': 'Elsevier',
        'relationshipType': null,
        'country': null,
        'uri': 'http://www.elsevier.com/',
        'identifier': null,
        'publicationCount': 0,
        'paidAccessDescription': 'Open access',
        'paidAccessUrl': 'https://www.elsevier.com/about/open-science/open-access'
      },
      'publishers': [{
        'name': 'Elsevier',
        'relationshipType': null,
        'country': null,
        'uri': 'http://www.elsevier.com/',
        'identifier': null,
        'publicationCount': 0,
        'paidAccessDescription': 'Open access',
        'paidAccessUrl': 'https://www.elsevier.com/about/open-science/open-access'
      }],
      'policies': [{
        'id': 0,
        'openAccessPermitted': false,
        'uri': null,
        'internalMoniker': 'Lancet',
        'permittedVersions': [{
          'articleVersion': 'submitted',
          'option': 1,
          'conditions': ['Upon publication publisher copyright and source must be acknowledged', 'Upon publication must link to publisher version'],
          'prerequisites': [],
          'locations': ['Author\'s Homepage', 'Preprint Repository'],
          'licenses': [],
          'embargo': null
        }, {
          'articleVersion': 'accepted',
          'option': 1,
          'conditions': ['Publisher copyright and source must be acknowledged', 'Must link to publisher version'],
          'prerequisites': [],
          'locations': ['Author\'s Homepage', 'Institutional Website'],
          'licenses': ['CC BY-NC-ND'],
          'embargo': null
        }, {
          'articleVersion': 'accepted',
          'option': 2,
          'conditions': ['Publisher copyright and source must be acknowledged', 'Must link to publisher version'],
          'prerequisites': ['If Required by Funder'],
          'locations': ['Non-Commercial Repository'],
          'licenses': ['CC BY-NC-ND'],
          'embargo': { amount: 6, units: 'Months' }
        }, {
          'articleVersion': 'accepted',
          'option': 3,
          'conditions': ['Publisher copyright and source must be acknowledged', 'Must link to publisher version'],
          'prerequisites': [],
          'locations': ['Non-Commercial Repository'],
          'licenses': [],
          'embargo': null
        }],
        'urls': {
          'http://download.thelancet.com/flatcontentassets/authors/lancet-information-for-authors.pdf': 'Guidelines for Authors',
          'http://www.thelancet.com/journals/lancet/article/PIIS0140-6736%2813%2960720-5/fulltext': 'The Lancet journals welcome a new open access policy',
          'http://www.thelancet.com/lancet-information-for-authors/after-publication': 'What happens after publication?',
          'http://www.thelancet.com/lancet/information-for-authors/disclosure-of-results': 'Disclosure of results before publication',
          'https://www.elsevier.com/__data/assets/pdf_file/0005/78476/external-embargo-list.pdf': 'Journal Embargo Period List',
          'https://www.elsevier.com/__data/assets/pdf_file/0011/78473/UK-Embargo-Periods.pdf': 'Journal Embargo List for UK Authors'
        },
        'openAccessProhibited': false,
        'publicationCount': 0,
        'preArchiving': 'can',
        'postArchiving': 'can',
        'pubArchiving': 'cannot'
      }],
      'inDOAJ': false
    }]
  }
} as WorkspaceitemSectionSherpaPoliciesObject;
