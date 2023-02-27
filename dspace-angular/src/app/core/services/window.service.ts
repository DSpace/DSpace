import { InjectionToken } from '@angular/core';

export const NativeWindowService = new InjectionToken('NativeWindowService');

export class NativeWindowRef {
  get nativeWindow(): any {
    if (typeof window !== 'undefined') {
      return window;
    } else {
      return false;
    }
  }
}

export function NativeWindowFactory() {
  return new NativeWindowRef();
}
