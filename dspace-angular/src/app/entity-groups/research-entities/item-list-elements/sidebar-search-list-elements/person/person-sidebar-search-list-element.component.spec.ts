import { Item } from '../../../../../core/shared/item.model';
import { Collection } from '../../../../../core/shared/collection.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { createSidebarSearchListElementTests } from '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component.spec';
import { PersonSidebarSearchListElementComponent } from './person-sidebar-search-list-element.component';

const object = Object.assign(new ItemSearchResult(), {
  indexableObject: Object.assign(new Item(), {
    id: 'test-item',
    metadata: {
      'dspace.entity.type': [
        {
          value: 'Person',
        }
      ],
      'person.familyName': [
        {
          value: 'family name'
        }
      ],
      'person.givenName': [
        {
          value: 'given name'
        }
      ],
      'person.jobTitle': [
        {
          value: 'job title'
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

describe('PersonSidebarSearchListElementComponent',
  createSidebarSearchListElementTests(PersonSidebarSearchListElementComponent, object, parent, 'parent title', 'family name, given name', 'job title', [
  ])
);
