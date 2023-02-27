import { Group } from '../../core/eperson/models/group.model';
import { EPersonMock } from './eperson.mock';
import { createSuccessfulRemoteDataObject$ } from '../remote-data.utils';

export const GroupMock2: Group = Object.assign(new Group(), {
    handle: null,
    subgroups: [],
    epersons: [],
    permanent: true,
    selfRegistered: false,
    _links: {
        self: {
            href: 'https://rest.api/server/api/eperson/groups/testgroupid2',
        },
        subgroups: { href: 'https://rest.api/server/api/eperson/groups/testgroupid2/subgroups' },
        object: { href: 'https://rest.api/server/api/eperson/groups/testgroupid2/object' },
        epersons: { href: 'https://rest.api/server/api/eperson/groups/testgroupid2/epersons' }
    },
    _name: 'testgroupname2',
    id: 'testgroupid2',
    uuid: 'testgroupid2',
    type: 'group',
    object: createSuccessfulRemoteDataObject$({ name: 'testgroupid2objectName'})
});

export const GroupMock: Group = Object.assign(new Group(), {
    handle: null,
    subgroups: [GroupMock2],
    epersons: [EPersonMock],
    selfRegistered: false,
    permanent: false,
    _links: {
        self: {
            href: 'https://rest.api/server/api/eperson/groups/testgroupid',
        },
        subgroups: { href: 'https://rest.api/server/api/eperson/groups/testgroupid/subgroups' },
        object: { href: 'https://rest.api/server/api/eperson/groups/testgroupid2/object' },
        epersons: { href: 'https://rest.api/server/api/eperson/groups/testgroupid/epersons' }
    },
    _name: 'testgroupname',
    id: 'testgroupid',
    uuid: 'testgroupid',
    type: 'group',
});
