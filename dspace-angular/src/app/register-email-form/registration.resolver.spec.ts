import { RegistrationResolver } from './registration.resolver';
import { EpersonRegistrationService } from '../core/data/eperson-registration.service';
import { Registration } from '../core/shared/registration.model';
import { first } from 'rxjs/operators';
import { createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';

describe('RegistrationResolver', () => {
  let resolver: RegistrationResolver;
  let epersonRegistrationService: EpersonRegistrationService;

  const token = 'test-token';
  const registration = Object.assign(new Registration(), {email: 'test@email.org', token: token, user:'user-uuid'});

  beforeEach(() => {
    epersonRegistrationService = jasmine.createSpyObj('epersonRegistrationService', {
      searchByToken: createSuccessfulRemoteDataObject$(registration)
    });
    resolver = new RegistrationResolver(epersonRegistrationService);
  });
  describe('resolve', () => {
    it('should resolve a registration based on the token', (done) => {
      resolver.resolve({params: {token: token}} as any, undefined)
        .pipe(first())
        .subscribe(
          (resolved) => {
            expect(resolved.payload.token).toEqual(token);
            expect(resolved.payload.email).toEqual('test@email.org');
            expect(resolved.payload.user).toEqual('user-uuid');
            done();
          }
        );
    });
  });
});
