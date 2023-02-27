import { LookupGuard } from './lookup-guard';
import { NgModule } from '@angular/core';
import { RouterModule, UrlSegment } from '@angular/router';
import { isNotEmpty } from '../shared/empty.util';
import { ThemedObjectNotFoundComponent } from './objectnotfound/themed-objectnotfound.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        matcher: urlMatcher,
        canActivate: [LookupGuard],
        component: ThemedObjectNotFoundComponent  }
    ])
  ],
  providers: [
    LookupGuard
  ]
})

export class LookupRoutingModule {

}

export function urlMatcher(url) {
  // The expected path is :idType/:id
  const idType = url[0].path;
  // Allow for handles that are delimited with a forward slash.
  const id = url
    .slice(1)
    .map((us: UrlSegment) => us.path)
    .join('/');
  if (isNotEmpty(idType) && isNotEmpty(id)) {
    return {
      consumed: url,
      posParams: {
        idType: new UrlSegment(idType, {}),
        id: new UrlSegment(id, {})
      }
    };
  }
  return null;
}
