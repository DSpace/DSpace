export const relatedRelationships = {
    'type': {
        'value': 'paginated-list'
    },
    'pageInfo': {
        'elementsPerPage': 5,
        'totalElements': 2,
        'totalPages': 1,
        'currentPage': 1
    },
    '_links': {
        'self': {
            'href': 'http://localhost:8080/server/api/core/relationships/search/byItemsAndType?typeId=1&focusItem=b1b2c768-bda1-448a-a073-fc541e8b24d9&relationshipLabel=isPublicationOfAuthor&size=5&relatedItem=72635f7f-37b5-4875-b4f2-5ff45d97a09b&relatedItem=674f695e-8001-4150-8f9c-095c536a6bcb&relatedItem=a64719f8-ba7b-41d1-8eb6-f8feb0c000b7&relatedItem=75c0f7f5-5a69-40e8-aa1f-8f35b1ce5a63&relatedItem=10bc6f8b-0796-486f-94d8-4d2e1814586f'
        },
        'page': [
            {
                'href': 'http://localhost:8080/server/api/core/relationships/1408'
            },
            {
                'href': 'http://localhost:8080/server/api/core/relationships/1409'
            }
        ]
    },
    'page': [
        {
            'type': 'relationship',
            'uuid': 'relationship-1408',
            'id': 1408,
            'leftPlace': 0,
            'rightPlace': 0,
            'leftwardValue': null,
            'rightwardValue': null,
            '_links': {
                'relationshipType': {
                    'href': 'http://localhost:8080/server/api/core/relationshiptypes/1'
                },
                'self': {
                    'href': 'http://localhost:8080/server/api/core/relationships/1408'
                },
                'leftItem': {
                    'href': 'http://localhost:8080/server/api/core/items/75c0f7f5-5a69-40e8-aa1f-8f35b1ce5a63'
                },
                'rightItem': {
                    'href': 'http://localhost:8080/server/api/core/items/b1b2c768-bda1-448a-a073-fc541e8b24d9'
                }
            }
        },
        {
            'type': 'relationship',
            'uuid': 'relationship-1409',
            'id': 1409,
            'leftPlace': 0,
            'rightPlace': 1,
            'leftwardValue': null,
            'rightwardValue': null,
            '_links': {
                'relationshipType': {
                    'href': 'http://localhost:8080/server/api/core/relationshiptypes/1'
                },
                'self': {
                    'href': 'http://localhost:8080/server/api/core/relationships/1409'
                },
                'leftItem': {
                    'href': 'http://localhost:8080/server/api/core/items/10bc6f8b-0796-486f-94d8-4d2e1814586f'
                },
                'rightItem': {
                    'href': 'http://localhost:8080/server/api/core/items/b1b2c768-bda1-448a-a073-fc541e8b24d9'
                }
            }
        }
    ]
};
