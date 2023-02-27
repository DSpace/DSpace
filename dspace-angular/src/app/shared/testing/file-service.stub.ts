import { of as observableOf } from 'rxjs';

export class FileServiceStub {
  retrieveFileDownloadLink() {
    return observableOf(null);
  }
}
