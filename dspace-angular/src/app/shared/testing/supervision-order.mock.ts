import { Item } from '../../core/shared/item.model';
import { SupervisionOrder } from '../../core/supervision-order/models/supervision-order.model';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../remote-data.utils';
import { GroupMock, GroupMock2 } from './group-mock';
import { buildPaginatedList } from '../../core/data/paginated-list.model';
import { PageInfo } from '../../core/shared/page-info.model';

const itemMock = Object.assign(new Item(), {
  metadata: {
    'dc.title': [
      {
        value: 'Item one'
      }
    ],
    'dc.contributor.author': [
      {
        value: 'Smith, Donald'
      }
    ],
    'dc.publisher': [
      {
        value: 'a publisher'
      }
    ],
    'dc.date.issued': [
      {
        value: '2015-06-26'
      }
    ],
    'dc.description.abstract': [
      {
        value: 'This is the abstract'
      }
    ]
  }
});

const anotherItemMock = Object.assign(new Item(), {
  metadata: {
    'dc.title': [
      {
        value: 'Item two'
      }
    ],
    'dc.contributor.author': [
      {
        value: 'Smith, Donald'
      }
    ],
    'dc.publisher': [
      {
        value: 'a publisher'
      }
    ],
    'dc.date.issued': [
      {
        value: '2015-06-26'
      }
    ],
    'dc.description.abstract': [
      {
        value: 'This is the abstract'
      }
    ]
  }
});

export const supervisionOrderMock: any = Object.assign(new SupervisionOrder(),{
  id: '1',
  item: createSuccessfulRemoteDataObject$(itemMock),
  group: createSuccessfulRemoteDataObject$(GroupMock)
});

export const anotherSupervisionOrderMock: any = {
  id: '2',
  item: createSuccessfulRemoteDataObject$(anotherItemMock),
  group: createSuccessfulRemoteDataObject$(GroupMock2)
};

export const supervisionOrderListMock = [supervisionOrderMock, anotherSupervisionOrderMock];
export const supervisionOrderEntryMock = {
  supervisionOrder: supervisionOrderMock,
  group: GroupMock
};

const pageInfo = new PageInfo({
  elementsPerPage: 10,
  totalElements: 2,
  totalPages: 1,
  currentPage: 1
});
const array = [supervisionOrderMock, anotherSupervisionOrderMock];
const paginatedList = buildPaginatedList(pageInfo, array);
export const supervisionOrderPaginatedListRD = createSuccessfulRemoteDataObject(paginatedList);
export const supervisionOrderPaginatedListRD$ = createSuccessfulRemoteDataObject$(paginatedList);
