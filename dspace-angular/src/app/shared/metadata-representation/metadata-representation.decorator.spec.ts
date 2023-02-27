  /* eslint-disable max-classes-per-file */
import {
  DEFAULT_ENTITY_TYPE,
  DEFAULT_REPRESENTATION_TYPE,
  getMetadataRepresentationComponent,
  metadataRepresentationComponent
} from './metadata-representation.decorator';
import { MetadataRepresentationType } from '../../core/shared/metadata-representation/metadata-representation.model';
import { Context } from '../../core/shared/context.model';
import { v4 as uuidv4 } from 'uuid';
import { environment } from '../../../environments/environment';

let ogEnvironmentThemes;

describe('MetadataRepresentation decorator function', () => {
  const type1 = 'TestType';
  const type2 = 'TestType2';
  const type3 = 'TestType3';
  const type4 = 'RandomType';
  const typeAncestor = 'TestTypeAncestor';
  const typeUnthemed = 'TestTypeUnthemed';
  let prefix;

  class Test1PlainText {
  }

  class Test1Authority {
  }

  class Test2Item {
  }

  class Test2ItemSubmission {
  }

  class Test3ItemSubmission {
  }

  class TestAncestorComponent {
  }

  class TestUnthemedComponent {
  }

  /* eslint-enable max-classes-per-file */

  beforeEach(() => {
    prefix = uuidv4();
    init(prefix);
  });

  function init(key) {
    metadataRepresentationComponent(key + type1, MetadataRepresentationType.PlainText)(Test1PlainText);
    metadataRepresentationComponent(key + type1, MetadataRepresentationType.AuthorityControlled)(Test1Authority);

    metadataRepresentationComponent(key + type2, MetadataRepresentationType.Item)(Test2Item);
    metadataRepresentationComponent(key + type2, MetadataRepresentationType.Item, Context.Workspace)(Test2ItemSubmission);

    metadataRepresentationComponent(key + type3, MetadataRepresentationType.Item, Context.Workspace)(Test3ItemSubmission);

    // Register a metadata representation in the 'ancestor' theme
    metadataRepresentationComponent(key + typeAncestor, MetadataRepresentationType.Item, Context.Any, 'ancestor')(TestAncestorComponent);
    metadataRepresentationComponent(key + typeUnthemed, MetadataRepresentationType.Item, Context.Any)(TestUnthemedComponent);

    ogEnvironmentThemes = environment.themes;
  }

  afterEach(() => {
    environment.themes = ogEnvironmentThemes;
  });

  describe('If there\'s an exact match', () => {
    it('should return the matching class', () => {
      const component = getMetadataRepresentationComponent(prefix + type3, MetadataRepresentationType.Item, Context.Workspace);
      expect(component).toEqual(Test3ItemSubmission);
    });
  });

  describe('If there isn\'nt an exact match', () => {
    describe('If there is a match for the entity type and representation type', () => {
      it('should return the class with the matching entity type and representation type and default context', () => {
        const component = getMetadataRepresentationComponent(prefix + type1, MetadataRepresentationType.AuthorityControlled, Context.Workspace);
        expect(component).toEqual(Test1Authority);
      });
    });
    describe('If there isn\'t a match for the representation type', () => {
      it('should return the class with the matching entity type and the default representation type and default context', () => {
        const component = getMetadataRepresentationComponent(prefix + type1, MetadataRepresentationType.Item);
        expect(component).toEqual(Test1PlainText);
      });
      describe('If there isn\'t a match for the entity type', () => {
        it('should return the class with the default entity type and the default representation type and default context', () => {
          const defaultComponent = getMetadataRepresentationComponent(DEFAULT_ENTITY_TYPE, DEFAULT_REPRESENTATION_TYPE);
          const component = getMetadataRepresentationComponent(prefix + type4, MetadataRepresentationType.AuthorityControlled);
          expect(component).toEqual(defaultComponent);
        });
      });
    });
  });

  describe('With theme extensions', () => {
    // We're only interested in the cases that the requested theme doesn't match the requested entityType,
    // as the cases where it does are already covered by the tests above
    describe('If requested theme has no match', () => {
      beforeEach(() => {
        environment.themes = [
          {
            name: 'requested',        // Doesn't match any entityType
            extends: 'intermediate',
          },
          {
            name: 'intermediate',     // Doesn't match any entityType
            extends: 'ancestor',
          },
          {
            name: 'ancestor',         // Matches typeAncestor, but not typeUnthemed
          }
        ];
      });

      it('should return component from the first ancestor theme that matches its entityType', () => {
        const component = getMetadataRepresentationComponent(prefix + typeAncestor, MetadataRepresentationType.Item, Context.Any, 'requested');
        expect(component).toEqual(TestAncestorComponent);
      });

      it('should return default component if none of the ancestor themes match its entityType', () => {
        const component = getMetadataRepresentationComponent(prefix + typeUnthemed, MetadataRepresentationType.Item, Context.Any, 'requested');
        expect(component).toEqual(TestUnthemedComponent);
      });
    });

    describe('If there is a theme extension cycle', () => {
      beforeEach(() => {
        environment.themes = [
          { name: 'extension-cycle', extends: 'broken1' },
          { name: 'broken1', extends: 'broken2' },
          { name: 'broken2', extends: 'broken3' },
          { name: 'broken3', extends: 'broken1' },
        ];
      });

      it('should throw an error', () => {
        expect(() => {
          getMetadataRepresentationComponent(prefix + typeAncestor, MetadataRepresentationType.Item, Context.Any, 'extension-cycle');
        }).toThrowError(
          'Theme extension cycle detected: extension-cycle -> broken1 -> broken2 -> broken3 -> broken1'
        );
      });
    });
  });
});
