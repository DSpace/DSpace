import { createSidebarSearchListElementTests } from '../../sidebar-search-list-element.component.spec';
import { PublicationSidebarSearchListElementComponent } from './publication-sidebar-search-list-element.component';
import { ItemSearchResult } from '../../../../object-collection/shared/item-search-result.model';
import { Item } from '../../../../../core/shared/item.model';
import { Collection } from '../../../../../core/shared/collection.model';

const object = Object.assign(new ItemSearchResult(), {
  indexableObject: Object.assign(new Item(), {
    id: 'test-item',
    metadata: {
      'dc.title': [
        {
          value: 'title'
        }
      ],
      'dc.publisher': [
        {
          value: 'publisher'
        }
      ],
      'dc.date.issued': [
        {
          value: 'date'
        }
      ],
      'dc.contributor.author': [
        {
          value: 'author'
        }
      ]
    }
  })
});
const parent = Object.assign(new Collection(), {
  id: 'test-collection',
  metadata: {
    'dc.title': [
      {
        value: 'parent title'
      }
    ]
  }
});

describe('PublicationSidebarSearchListElementComponent',
  createSidebarSearchListElementTests(PublicationSidebarSearchListElementComponent, object, parent, 'parent title', 'title', '(publisher, date) author')
);
