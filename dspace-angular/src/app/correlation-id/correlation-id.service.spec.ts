import { CorrelationIdService } from './correlation-id.service';
import { CookieServiceMock } from '../shared/mocks/cookie.service.mock';
import { UUIDService } from '../core/shared/uuid.service';
import { MockStore } from '@ngrx/store/testing';
import { TestBed } from '@angular/core/testing';
import { Store, StoreModule } from '@ngrx/store';
import { appReducers, AppState, storeModuleConfig } from '../app.reducer';
import { SetCorrelationIdAction } from './correlation-id.actions';

describe('CorrelationIdService', () => {
  let service: CorrelationIdService;

  let cookieService;
  let uuidService;
  let store;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot(appReducers, storeModuleConfig),
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    cookieService = new CookieServiceMock();
    uuidService = new UUIDService();
    store = TestBed.inject(Store) as MockStore<AppState>;
    service = new CorrelationIdService(cookieService, uuidService, store);
  });

  describe('getCorrelationId', () => {
    it('should get from from store', () => {
      expect(service.getCorrelationId()).toBe(null);
      store.dispatch(new SetCorrelationIdAction('some value'));
      expect(service.getCorrelationId()).toBe('some value');
    });
  });


  describe('initCorrelationId', () => {
    const cookieCID = 'cookie CID';
    const storeCID = 'store CID';

    it('should set cookie and store values to a newly generated value if neither ex', () => {
      service.initCorrelationId();

      expect(cookieService.get('CORRELATION-ID')).toBeTruthy();
      expect(service.getCorrelationId()).toBeTruthy();
      expect(cookieService.get('CORRELATION-ID')).toEqual(service.getCorrelationId());
    });

    it('should set store value to cookie value if present', () => {
      expect(service.getCorrelationId()).toBe(null);

      cookieService.set('CORRELATION-ID', cookieCID);

      service.initCorrelationId();

      expect(cookieService.get('CORRELATION-ID')).toBe(cookieCID);
      expect(service.getCorrelationId()).toBe(cookieCID);
    });

    it('should set cookie value to store value if present', () => {
      store.dispatch(new SetCorrelationIdAction(storeCID));

      service.initCorrelationId();

      expect(cookieService.get('CORRELATION-ID')).toBe(storeCID);
      expect(service.getCorrelationId()).toBe(storeCID);
    });

    it('should set store value to cookie value if both are present', () => {
      cookieService.set('CORRELATION-ID', cookieCID);
      store.dispatch(new SetCorrelationIdAction(storeCID));

      service.initCorrelationId();

      expect(cookieService.get('CORRELATION-ID')).toBe(cookieCID);
      expect(service.getCorrelationId()).toBe(cookieCID);
    });
  });
});
