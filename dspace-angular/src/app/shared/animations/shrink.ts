import { animate, state, style, transition, trigger } from '@angular/animations';

export const shrinkInOut = trigger('shrinkInOut', [
  state('in', style({height: '100%', opacity: 1})),
  transition('* => void', [
    style({height: '!', opacity: 1}),
    animate(200, style({height: 0, opacity: 0}))
  ]),
  transition('void => *', [
    style({height: 0, opacity: 0}),
    animate(200, style({height: '*', opacity: 1}))
  ])
]);
