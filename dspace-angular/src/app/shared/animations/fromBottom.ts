import { animate, state, style, transition, trigger } from '@angular/animations';

export const fromBottomInState = state('fromBottomIn', style({opacity: 1, transform: 'translateY(0)'}));
export const fromBottomEnter = transition('* => fromBottomIn', [
  style({opacity: 0, transform: 'translateY(5%)'}),
  animate('400ms ease-in-out')
]);

export const fromBottomOutState = state('fromBottomOut', style({opacity: 0, transform: 'translateY(-5%)'}));
export const fromBottomLeave = transition('fromBottomIn => fromBottomOut', [
  style({opacity: 1, transform: 'translateY(0)'}),
  animate('300ms ease-in-out')
]);

export const fromBottomIn = trigger('fromBottomIn', [
  fromBottomEnter
]);

export const fromBottomOut = trigger('fromBottomOut', [
  fromBottomLeave
]);

export const fromBottomInOut = trigger('fromBottomInOut', [
  fromBottomEnter,
  fromBottomLeave
]);
