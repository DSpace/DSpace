import { currentPath } from './route.utils';

describe('Route Utils', () => {
  const urlTree = {
    root: {
      children: {
        primary: {
          segments: [
            { path: 'test' },
            { path: 'path' }
          ]
        }

      }
    }
  };
  const router = { parseUrl: () => urlTree } as any;
  it('Should return the correct current path based on the router', () => {
    const result = currentPath(router);
    expect(result).toEqual('/test/path');
  });
});
