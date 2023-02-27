import { autoserialize, deserialize } from 'cerialize';
import { HALLink } from '../shared/hal-link.model';
import { HALResource } from '../shared/hal-resource.model';
import { DSpaceSerializer } from './dspace.serializer';

class TestModel implements HALResource {
  @autoserialize
  id: string;

  @autoserialize
  name: string;

  @deserialize
  _links: {
    self: HALLink;
    parents: HALLink;
  };
}

const testModels = [
  {
    id: 'd4466d54-d73b-4d8f-b73f-c702020baa14',
    name: 'Model 1',
    _links: {
      self: {
        href: '/testmodels/9e32a2e2-6b91-4236-a361-995ccdc14c60'
      },
      parents: {
        href: '/testmodels/9e32a2e2-6b91-4236-a361-995ccdc14c60/parents'
      }
    }
  },
  {
    id: '752a1250-949a-46ad-9bea-fbc45f0b656d',
    name: 'Model 2',
    _links: {
      self: {
        href: '/testmodels/598ce822-c357-46f3-ab70-63724d02d6ad'
      },
      parents: {
        href: '/testmodels/598ce822-c357-46f3-ab70-63724d02d6ad/parents'
      }
    }
  }
];

const testResponses = [
  {
    _links: {
      self: {
        href: '/testmodels/9e32a2e2-6b91-4236-a361-995ccdc14c60'
      },
      parents: {
        href: '/testmodels/9e32a2e2-6b91-4236-a361-995ccdc14c60/parents'
      }
    },
    id: '9e32a2e2-6b91-4236-a361-995ccdc14c60',
    type: 'testModels',
    name: 'A Test Model'
  },
  {
    _links: {
      self: {
        href: '/testmodels/598ce822-c357-46f3-ab70-63724d02d6ad'
      },
      parents: {
        href: '/testmodels/598ce822-c357-46f3-ab70-63724d02d6ad/parents'
      }
    },
    id: '598ce822-c357-46f3-ab70-63724d02d6ad',
    type: 'testModels',
    name: 'Another Test Model'
  }
];

describe('DSpaceSerializer', () => {

  describe('serialize', () => {

    it('should turn a model in to a valid document', () => {
      const serializer = new DSpaceSerializer(TestModel);
      const doc = serializer.serialize(testModels[0]);
      expect(doc.id).toBe(testModels[0].id);
      expect(doc.name).toBe(testModels[0].name);
      expect(doc._links).toBeUndefined();
    });

  });

  describe('serializeArray', () => {

    it('should turn an array of models in to a valid document', () => {
      const serializer = new DSpaceSerializer(TestModel);
      const doc = serializer.serializeArray(testModels);

      expect(doc[0].id).toBe(testModels[0].id);
      expect(doc[0].name).toBe(testModels[0].name);
      expect(doc[0]._links).toBeUndefined();
      expect(doc[1].id).toBe(testModels[1].id);
      expect(doc[1].name).toBe(testModels[1].name);
      expect(doc[1]._links).toBeUndefined();
    });

  });

  describe('deserialize', () => {

    it('should turn a valid document describing a single entity in to a valid model', () => {
      const serializer = new DSpaceSerializer(TestModel);
      const model = serializer.deserialize(testResponses[0]);

      expect(model.id).toBe(testResponses[0].id);
      expect(model.name).toBe(testResponses[0].name);
    });

    it('should throw an error when dealing with a document describing an array', () => {
      const serializer = new DSpaceSerializer(TestModel);
      expect(() => {
        serializer.deserialize(testResponses);
      }).toThrow();
    });

  });

  describe('deserializeArray', () => {

    it('should throw an error when dealing with a document describing a single model', () => {
      const serializer = new DSpaceSerializer(TestModel);
      const doc = {
        _embedded: testResponses[0]
      };

      expect(() => {
        serializer.deserializeArray(doc);
      }).toThrow();
    });

    it('should turn an array of responses in to valid models', () => {
      const serializer = new DSpaceSerializer(TestModel);
      const output = serializer.deserializeArray(testResponses);

      expect(testResponses[0].id).toBe(output[0].id);
      expect(testResponses[0].name).toBe(output[0].name);
      expect(testResponses[0]._links.self.href).toBe(output[0]._links.self.href);
      expect(testResponses[0]._links.parents.href).toBe(output[0]._links.parents.href);
      expect(testResponses[1].id).toBe(output[1].id);
      expect(testResponses[1].name).toBe(output[1].name);
      expect(testResponses[1]._links.self.href).toBe(output[1]._links.self.href);
      expect(testResponses[1]._links.parents.href).toBe(output[1]._links.parents.href);
    });

  });

});
