import { animate, state, style, transition, trigger } from '@angular/animations';

export const rotateInState = state('rotateIn', style({ opacity: 1, transform: 'rotate(0deg)' }));
export const rotateEnter = transition('* => rotateIn', [
  style({ opacity: 0, transform: 'rotate(5deg)' }),
  animate('400ms ease-in-out')
]);

export const rotateOutState = state('rotateOut', style({ opacity: 0, transform: 'rotate(5deg)' }));
export const rotateLeave = transition('rotateIn => rotateOut', [
  style({ opacity: 1, transform: 'rotate(0deg)' }),
  animate('400ms ease-in-out')
]);

export const rotateIn = trigger('rotateIn', [
  rotateEnter
]);

export const rotateOut = trigger('rotateOut', [
  rotateLeave
]);

export const rotateInOut = trigger('rotateInOut', [
  rotateEnter,
  rotateLeave
]);

const expandedStyle = { transform: 'rotate(90deg)' };
const collapsedStyle = { transform: 'rotate(0deg)' };

export const rotate = trigger('rotate',
  [
    state('expanded', style(expandedStyle)),
    state('collapsed', style(collapsedStyle)),
    transition('expanded <=> collapsed', [
      animate('200ms')
    ])

  ]);
