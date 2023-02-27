import { EPerson } from '../../core/eperson/models/eperson.model';

export const EPersonMock: EPerson = Object.assign(new EPerson(), {
  handle: null,
  groups: [],
  netid: 'test@test.com',
  lastActive: '2018-05-14T12:25:42.411+0000',
  canLogIn: true,
  email: 'test@test.com',
  requireCertificate: false,
  selfRegistered: false,
  _links: {
    self: {
      href: 'https://rest.api/dspace-spring-rest/api/eperson/epersons/testid',
    },
    groups: { href: 'https://rest.api/dspace-spring-rest/api/eperson/epersons/testid/groups' }
  },
  id: 'testid',
  uuid: 'testid',
  type: 'eperson',
  metadata: {
    'dc.title': [
      {
        language: null,
        value: 'User Test'
      }
    ],
    'eperson.firstname': [
      {
        language: null,
        value: 'User'
      }
    ],
    'eperson.lastname': [
      {
        language: null,
        value: 'Test'
      },
    ],
    'eperson.language': [
      {
        language: null,
        value: 'en'
      },
    ]
  }
});

export const EPersonMock2: EPerson = Object.assign(new EPerson(), {
  handle: null,
  groups: [],
  netid: 'test2@test.com',
  lastActive: '2019-05-14T12:25:42.411+0000',
  canLogIn: false,
  email: 'test2@test.com',
  requireCertificate: false,
  selfRegistered: true,
  _links: {
    self: {
      href: 'https://rest.api/dspace-spring-rest/api/eperson/epersons/testid2',
    },
    groups: { href: 'https://rest.api/dspace-spring-rest/api/eperson/epersons/testid2/groups' }
  },
  id: 'testid2',
  uuid: 'testid2',
  type: 'eperson',
  metadata: {
    'dc.title': [
      {
        language: null,
        value: 'User Test 2'
      }
    ],
    'eperson.firstname': [
      {
        language: null,
        value: 'User2'
      }
    ],
    'eperson.lastname': [
      {
        language: null,
        value: 'MeepMeep'
      },
    ],
    'eperson.language': [
      {
        language: null,
        value: 'fr'
      },
    ]
  }
});
