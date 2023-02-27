import { of as observableOf } from 'rxjs';

export const mockTruncatableService: any = {
  /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
  isCollapsed: (id: string) => {
    if (id === '1') {
      return observableOf(true);
    } else {
      return observableOf(false);
    }
  },
  expand: (id: string) => {
  },
  collapse: (id: string) => {
  },
  toggle: (id: string) => {
  }
  /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
};
