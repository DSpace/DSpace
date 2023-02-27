import { StoreDevtoolsModule } from '@ngrx/store-devtools';

export const StoreDevModules = [
  // This enables Redux DevTools by default.
  // Note: this is overridden in production by devtools.prod.ts
  StoreDevtoolsModule.instrument({
    maxAge: 1000,
    logOnly: false,
  })
];
