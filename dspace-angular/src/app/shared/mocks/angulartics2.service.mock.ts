import { Injectable } from '@angular/core';

/* eslint-disable no-empty, @typescript-eslint/no-empty-function */
@Injectable()
export class Angulartics2Mock {
  public eventTrack = {
    next: (param: any): void => {}
  };
}
