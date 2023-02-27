import { renderStartsWithFor, StartsWithType } from './starts-with-decorator';

describe('BrowseByStartsWithDecorator', () => {
  const textDecorator = renderStartsWithFor(StartsWithType.text);
  const dateDecorator = renderStartsWithFor(StartsWithType.date);
  it('should have a decorator for both text and date', () => {
    expect(textDecorator.length).not.toBeNull();
    expect(dateDecorator.length).not.toBeNull();
  });
  it('should have 2 separate decorators for text and date', () => {
    expect(textDecorator).not.toEqual(dateDecorator);
  });
});
