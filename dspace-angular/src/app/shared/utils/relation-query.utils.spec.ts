import { getFilterByRelation, getQueryByRelations } from './relation-query.utils';

describe('Relation Query Utils', () => {
  const relationtype = 'isAuthorOfPublication';
  const itemUUID = 'a7939af0-36ad-430d-af09-7be8b0a4dadd';
  describe('getQueryByRelations', () => {
    it('Should return the correct query based on relationtype and uuid', () => {
      const result = getQueryByRelations(relationtype, itemUUID);
      expect(result).toEqual('query=relation.isAuthorOfPublication:"a7939af0-36ad-430d-af09-7be8b0a4dadd"');
    });
  });
  describe('getFilterByRelation', () => {
    it('Should return the correct query based on relationtype and uuid', () => {
      const result = getFilterByRelation(relationtype, itemUUID);
      expect(result).toEqual('f.isAuthorOfPublication=a7939af0-36ad-430d-af09-7be8b0a4dadd,equals');
    });
  });
});
