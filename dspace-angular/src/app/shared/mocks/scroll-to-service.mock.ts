import { ScrollToService } from '@nicky-lenaers/ngx-scroll-to';

/**
 * Mock for [[ScrollToService]]
 */
export function getMockScrollToService(): ScrollToService {
  return jasmine.createSpyObj('scrollToService', {
    scrollTo: jasmine.createSpy('scrollTo')
  });
}
