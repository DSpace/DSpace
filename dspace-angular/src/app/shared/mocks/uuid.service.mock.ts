import { UUIDService } from '../../core/shared/uuid.service';

export const defaultUUID = 'c4ce6905-290b-478f-979d-a333bbd7820f';

export function getMockUUIDService(uuid = defaultUUID): UUIDService {
  return jasmine.createSpyObj('uuidService', {
    generate: uuid,
  });
}
