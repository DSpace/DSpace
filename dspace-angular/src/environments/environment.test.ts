// This configuration is only used for unit tests, end-to-end tests use environment.production.ts
import { BuildConfig } from 'src/config/build-config.interface';
import { RestRequestMethod } from '../app/core/data/rest-request-method';
import { NotificationAnimationsType } from '../app/shared/notifications/models/notification-animations-type';

export const environment: BuildConfig = {
  production: false,

  // Angular Universal settings
  universal: {
    preboot: true,
    async: true,
    time: false
  },

  // Angular Universal server settings.
  ui: {
    ssl: false,
    host: 'dspace.com',
    port: 80,
    // NOTE: Space is capitalized because 'namespace' is a reserved string in TypeScript
    nameSpace: '/angular-dspace',
    baseUrl: 'http://dspace.com/angular-dspace',
    // The rateLimiter settings limit each IP to a 'max' of 500 requests per 'windowMs' (1 minute).
    rateLimiter: {
      windowMs: 1 * 60 * 1000, // 1 minute
      max: 500 // limit each IP to 500 requests per windowMs
    },
    useProxies: true,
  },

  // The REST API server settings.
  rest: {
    ssl: true,
    host: 'rest.com',
    port: 443,
    // NOTE: Space is capitalized because 'namespace' is a reserved string in TypeScript
    nameSpace: '/api',
    baseUrl: 'https://rest.com/api'
  },

  actuators: {
    endpointPath: '/actuator/health'
  },

  // Caching settings
  cache: {
    // NOTE: how long should objects be cached for by default
    msToLive: {
      default: 15 * 60 * 1000, // 15 minutes
    },
    // msToLive: 1000, // 15 minutes
    control: 'max-age=60',
    autoSync: {
      defaultTime: 0,
      maxBufferSize: 100,
      timePerMethod: { [RestRequestMethod.PATCH]: 3 } as any // time in seconds
    },
    // In-memory cache of server-side rendered pages. Disabled in test environment (max=0)
    serverSide: {
      debug: false,
      botCache: {
        max: 0,
        timeToLive: 24 * 60 * 60 * 1000, // 1 day
        allowStale: true,
      },
      anonymousCache: {
        max: 0,
        timeToLive: 10 * 1000, // 10 seconds
        allowStale: true,
      }
    }
  },

  // Authentication settings
  auth: {
    // Authentication UI settings
    ui: {
      // the amount of time before the idle warning is shown
      timeUntilIdle: 20000,

      // the amount of time the user has to react after the idle warning is shown before they are logged out.
      idleGracePeriod: 20000, // 20 sec
    },
    // Authentication REST settings
    rest: {
      // If the rest token expires in less than this amount of time, it will be refreshed automatically.
      // This is independent from the idle warning.
      timeLeftBeforeTokenRefresh: 20000, // 20 sec
    },
  },

  // Form settings
  form: {
    spellCheck: true,
    // NOTE: Map server-side validators to comparative Angular form validators
    validatorMap: {
      required: 'required',
      regex: 'pattern'
    }
  },

  // Notifications
  notifications: {
    rtl: false,
    position: ['top', 'right'],
    maxStack: 8,
    // NOTE: after how many seconds notification is closed automatically. If set to zero notifications are not closed automatically
    timeOut: 5000,
    clickToClose: true,
    // NOTE: 'fade' | 'fromTop' | 'fromRight' | 'fromBottom' | 'fromLeft' | 'rotate' | 'scale'
    animate: NotificationAnimationsType.Scale
  },

  // Submission settings
  submission: {
    autosave: {
      // NOTE: which metadata trigger an autosave
      metadata: ['dc.title', 'dc.identifier.doi', 'dc.identifier.pmid', 'dc.identifier.arxiv'],
      // NOTE: every how many minutes submission is saved automatically
      timer: 5
    },
    typeBind: {
      field: 'dc.type'
    },
    icons: {
      metadata: [
        {
          name: 'mainField',
          style: 'fas fa-user'
        },
        {
          name: 'relatedField',
          style: 'fas fa-university'
        },
        {
          name: 'otherRelatedField',
          style: 'fas fa-circle'
        },
        {
          name: 'default',
          style: ''
        }
      ],
      authority: {
        confidence: [
          {
            value: 600,
            style: 'text-success'
          },
          {
            value: 500,
            style: 'text-info'
          },
          {
            value: 400,
            style: 'text-warning'
          },
          {
            value: 'default',
            style: 'text-muted'
          },
        ]
      }
    }
  },

  // NOTE: will log all redux actions and transfers in console
  debug: false,

  // Default Language in which the UI will be rendered if the user's browser language is not an active language
  defaultLanguage: 'en',

  // Languages. DSpace Angular holds a message catalog for each of the following languages.
  // When set to active, users will be able to switch to the use of this language in the user interface.
  languages: [{
    code: 'en',
    label: 'English',
    active: true,
  }, {
    code: 'de',
    label: 'Deutsch',
    active: true,
  }, {
    code: 'cs',
    label: 'Čeština',
    active: true,
  }, {
    code: 'nl',
    label: 'Nederlands',
    active: true,
  }, {
    code: 'pt',
    label: 'Português',
    active: true,
  }, {
    code: 'fr',
    label: 'Français',
    active: true,
  }, {
    code: 'lv',
    label: 'Latviešu',
    active: true,
  }, {
    code: 'bn',
    label: 'বাংলা',
    active: true,
  }, {
    code: 'el',
    label: 'Ελληνικά',
    active: true,
  }],

  // Browse-By Pages
  browseBy: {
    // Amount of years to display using jumps of one year (current year - oneYearLimit)
    oneYearLimit: 10,
    // Limit for years to display using jumps of five years (current year - fiveYearLimit)
    fiveYearLimit: 30,
    // The absolute lowest year to display in the dropdown (only used when no lowest date can be found for all items)
    defaultLowerLimit: 1900,
    // Whether to add item thumbnail images to BOTH browse and search result lists.
    showThumbnails: true,
    // The number of entries in a paginated browse results list.
    // Rounded to the nearest size in the list of selectable sizes on the
    // settings menu.  See pageSizeOptions in 'pagination-component-options.model.ts'.
    pageSize: 20,
  },
  communityList: {
    pageSize: 20
  },
  homePage: {
    recentSubmissions: {
      pageSize: 5,
      //sort record of recent submission
      sortField: 'dc.date.accessioned',
    },
    topLevelCommunityList: {
      pageSize: 5
    }
  },
  item: {
    edit: {
      undoTimeout: 10000 // 10 seconds
    },
    // Show the item access status label in items lists
    showAccessStatuses: false,
    bitstream: {
      // Number of entries in the bitstream list in the item view page.
      // Rounded to the nearest size in the list of selectable sizes on the
      // settings menu.  See pageSizeOptions in 'pagination-component-options.model.ts'.
      pageSize: 5
    }
  },
  collection: {
    edit: {
      undoTimeout: 10000 // 10 seconds
    }
  },
  themes: [
    {
      name: 'full-item-page-theme',
      regex: 'items/aa6c6c83-3a83-4953-95d1-2bc2e67854d2/full'
    },
    {
      name: 'error-theme',
      regex: 'collections/aaaa.*'
    },
    {
      name: 'handle-theme',
      handle: '10673/1233'
    },
    {
      name: 'regex-theme',
      regex: 'collections\/e8043bc2.*'
    },
    {
      name: 'uuid-theme',
      uuid: '0958c910-2037-42a9-81c7-dca80e3892b4'
    },
    {
      name: 'base',
    },
  ],
  bundle: {
    standardBundles: ['ORIGINAL', 'THUMBNAIL', 'LICENSE'],
  },
  mediaViewer: {
    image: true,
    video: true
  },
  info: {
    enableEndUserAgreement: true,
    enablePrivacyStatement: true,
  },
  markdown: {
    enabled: false,
    mathjax: false,
  },

  vocabularies: [
    {
      filter: 'subject',
      vocabulary: 'srsc',
      enabled: true
    }
  ]
};
