export const relationshipTypes = [
    {
        'id': 1,
        'leftwardType': 'isAuthorOfPublication',
        'rightwardType': 'isPublicationOfAuthor',
        'copyToLeft': false,
        'copyToRight': false,
        'leftMinCardinality': 0,
        'leftMaxCardinality': null,
        'rightMinCardinality': 0,
        'rightMaxCardinality': null,
        'type': 'relationshiptype',
        '_links': {
            'leftType': {
                'href': 'http://localhost:8080/server/api/core/entitytypes/1'
            },
            'rightType': {
                'href': 'http://localhost:8080/server/api/core/entitytypes/2'
            },
            'self': {
                'href': 'http://localhost:8080/server/api/core/relationshiptypes/1'
            }
        }
    },
    {
        'id': 4,
        'leftwardType': 'isProjectOfPerson',
        'rightwardType': 'isPersonOfProject',
        'copyToLeft': false,
        'copyToRight': false,
        'leftMinCardinality': 0,
        'leftMaxCardinality': null,
        'rightMinCardinality': 0,
        'rightMaxCardinality': null,
        'type': 'relationshiptype',
        '_links': {
            'leftType': {
                'href': 'http://localhost:8080/server/api/core/entitytypes/2'
            },
            'rightType': {
                'href': 'http://localhost:8080/server/api/core/entitytypes/3'
            },
            'self': {
                'href': 'http://localhost:8080/server/api/core/relationshiptypes/4'
            }
        }
    },
    {
        'id': 5,
        'leftwardType': 'isOrgUnitOfPerson',
        'rightwardType': 'isPersonOfOrgUnit',
        'copyToLeft': false,
        'copyToRight': false,
        'leftMinCardinality': 0,
        'leftMaxCardinality': null,
        'rightMinCardinality': 0,
        'rightMaxCardinality': null,
        'type': 'relationshiptype',
        '_links': {
            'leftType': {
                'href': 'http://localhost:8080/server/api/core/entitytypes/2'
            },
            'rightType': {
                'href': 'http://localhost:8080/server/api/core/entitytypes/4'
            },
            'self': {
                'href': 'http://localhost:8080/server/api/core/relationshiptypes/5'
            }
        }
    }
];
