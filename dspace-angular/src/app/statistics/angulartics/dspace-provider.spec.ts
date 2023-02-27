import { Angulartics2DSpace } from './dspace-provider';
import { Angulartics2 } from 'angulartics2';
import { StatisticsService } from '../statistics.service';
import { filter } from 'rxjs/operators';
import { of as observableOf } from 'rxjs';

describe('Angulartics2DSpace', () => {
  let provider: Angulartics2DSpace;
  let angulartics2: Angulartics2;
  let statisticsService: jasmine.SpyObj<StatisticsService>;

  beforeEach(() => {
    angulartics2 = {
      eventTrack: observableOf({action: 'page_view', properties: {object: 'mock-object'}}),
      filterDeveloperMode: () => filter(() => true)
    } as any;
    statisticsService = jasmine.createSpyObj('statisticsService', {trackViewEvent: null});
    provider = new Angulartics2DSpace(angulartics2, statisticsService);
  });

  it('should use the statisticsService', () => {
    provider.startTracking();
    expect(statisticsService.trackViewEvent).toHaveBeenCalledWith('mock-object' as any);
  });

});
