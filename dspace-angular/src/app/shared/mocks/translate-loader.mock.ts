import {of as observableOf,  Observable } from 'rxjs';
import { TranslateLoader } from '@ngx-translate/core';

export class TranslateLoaderMock implements TranslateLoader {
  getTranslation(lang: string): Observable<any> {
    return observableOf({});
  }
}
