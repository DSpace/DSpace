import { EPerson } from '../../core/eperson/models/eperson.model';
import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { Item } from '../../core/shared/item.model';
import { ITEM_TYPE } from '../../core/shared/item-relationships/item-type.resource-type';

export const mockSubscriptionEperson = Object.assign(new EPerson(), {
  'id': 'fake-eperson-id',
  'uuid': 'fake-eperson-id',
  'handle': null,
  'metadata': {
    'eperson.firstname': [
      {
        'value': 'user',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }
    ],
    'eperson.lastname': [
      {
        'value': 'testr',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }
    ]
  },
  'netid': null,
  'lastActive': '2021-09-01T12:06:19.000+00:00',
  'canLogIn': true,
  'email': 'user@test.com',
  'requireCertificate': false,
  'selfRegistered': false,
  'type': 'eperson',
  '_links': {
    'groups': {
      'href': 'https://dspace.org/server/api/eperson/epersons/fake-eperson-id/groups'
    },
    'self': {
      'href': 'https://dspace.org/server/api/eperson/epersons/fake-eperson-id'
    }
  }
});

export const mockSubscriptionDSO = Object.assign(new Item(),
  {
    id: 'fake-item-id',
    uuid: 'fake-item-id',
    metadata: {
      'dc.title': [{ value: 'test item subscription' }]
    },
    type: ITEM_TYPE,
    _links: {
      self: {
        href: 'https://dspace.org/server/api/core/items/fake-item-id'
      }
    }
  }
);

export const mockSubscriptionDSO2 = Object.assign(new Item(),
  {
    id: 'fake-item-id2',
    uuid: 'fake-item-id2',
    metadata: {
      'dc.title': [{ value: 'test item subscription 2' }]
    },
    type: ITEM_TYPE,
    _links: {
      self: {
        href: 'https://dspace.org/server/api/core/items/fake-item-id2'
      }
    }
  }
);
export const findByEPersonAndDsoResEmpty = {
  'type': {
    'value': 'paginated-list'
  },
  'pageInfo': {
    'elementsPerPage': 0,
    'totalElements': 0,
    'totalPages': 1,
    'currentPage': 1
  },
  '_links': {
    'self': {
      'href': 'https://dspacecris7.4science.cloud/server/api/core/subscriptions/search/findByEPersonAndDso?resource=092b59e8-8159-4e70-98b5-93ec60bd3431&eperson_id=335647b6-8a52-4ecb-a8c1-7ebabb199bda'
    },
    'page': [
      {
        'href': 'https://dspacecris7.4science.cloud/server/api/core/subscriptions/22'
      },
      {
        'href': 'https://dspacecris7.4science.cloud/server/api/core/subscriptions/48'
      }
    ]
  },
  'page': []
};

export const subscriptionMock = {
  'id': 21,
  'type': 'subscription',
  'subscriptionParameterList': [
    {
      'id': 77,
      'name': 'frequency',
      'value': 'D'
    },
    {
      'id': 78,
      'name': 'frequency',
      'value': 'M'
    }
  ],
  'subscriptionType': 'test1',
  'ePerson': createSuccessfulRemoteDataObject$(mockSubscriptionEperson),
  'dSpaceObject': createSuccessfulRemoteDataObject$(mockSubscriptionDSO),
  '_links': {
    'dSpaceObject': {
      'href': 'https://dspace/server/api/core/subscriptions/21/dSpaceObject'
    },
    'ePerson': {
      'href': 'https://dspace/server/api/core/subscriptions/21/ePerson'
    },
    'self': {
      'href': 'https://dspace/server/api/core/subscriptions/21'
    }
  }
};

export const subscriptionMock2 = {
  'id': 21,
  'type': 'subscription',
  'subscriptionParameterList': [
    {
      'id': 77,
      'name': 'frequency',
      'value': 'D'
    },
  ],
  'subscriptionType': 'test2',
  'ePerson': createSuccessfulRemoteDataObject$(mockSubscriptionEperson),
  'dSpaceObject': createSuccessfulRemoteDataObject$(mockSubscriptionDSO2),
  '_links': {
    'dSpaceObject': {
      'href': 'https://dspacecris7.4science.cloud/server/api/core/subscriptions/21/dSpaceObject'
    },
    'ePerson': {
      'href': 'https://dspacecris7.4science.cloud/server/api/core/subscriptions/21/ePerson'
    },
    'self': {
      'href': 'https://dspacecris7.4science.cloud/server/api/core/subscriptions/21'
    }
  }
};

