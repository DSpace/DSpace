import { RestRequestMethod } from '../app/core/data/rest-request-method';
import { NotificationAnimationsType } from '../app/shared/notifications/models/notification-animations-type';
import { AppConfig } from './app-config.interface';
import { AuthConfig } from './auth-config.interfaces';
import { BrowseByConfig } from './browse-by-config.interface';
import { CacheConfig } from './cache-config.interface';
import { CollectionPageConfig } from './collection-page-config.interface';
import { FormConfig } from './form-config.interfaces';
import { ItemConfig } from './item-config.interface';
import { LangConfig } from './lang-config.interface';
import { MediaViewerConfig } from './media-viewer-config.interface';
import { INotificationBoardOptions } from './notifications-config.interfaces';
import { ServerConfig } from './server-config.interface';
import { SubmissionConfig } from './submission-config.interface';
import { ThemeConfig } from './theme.model';
import { UIServerConfig } from './ui-server-config.interface';
import { BundleConfig } from './bundle-config.interface';
import { ActuatorsConfig } from './actuators.config';
import { InfoConfig } from './info-config.interface';
import { CommunityListConfig } from './community-list-config.interface';
import { HomeConfig } from './homepage-config.interface';
import { MarkdownConfig } from './markdown-config.interface';
import { FilterVocabularyConfig } from './filter-vocabulary-config';

export class DefaultAppConfig implements AppConfig {
  production = false;

  // NOTE: will log all redux actions and transfers in console
  debug = false;

  // Angular Universal server settings
  // NOTE: these must be 'synced' with the 'dspace.ui.url' setting in your backend's local.cfg.
  ui: UIServerConfig = {
    ssl: false,
    host: 'localhost',
    port: 4000,
    // NOTE: Space is capitalized because 'namespace' is a reserved string in TypeScript
    nameSpace: '/',

    // The rateLimiter settings limit each IP to a 'max' of 500 requests per 'windowMs' (1 minute).
    rateLimiter: {
      windowMs: 1 * 60 * 1000, // 1 minute
      max: 500 // limit each IP to 500 requests per windowMs
    },

    // Trust X-FORWARDED-* headers from proxies
    useProxies: true,
  };

  // The REST API server settings
  // NOTE: these must be 'synced' with the 'dspace.server.url' setting in your backend's local.cfg.
  rest: ServerConfig = {
    ssl: false,
    host: 'localhost',
    port: 8080,
    // NOTE: Space is capitalized because 'namespace' is a reserved string in TypeScript
    nameSpace: '/',
  };

  actuators: ActuatorsConfig = {
    endpointPath: '/actuator/health'
  };

  // Caching settings
  cache: CacheConfig = {
    // NOTE: how long should objects be cached for by default
    msToLive: {
      default: 15 * 60 * 1000 // 15 minutes
    },
    // Cache-Control HTTP Header
    control: 'max-age=604800', // revalidate browser
    autoSync: {
      defaultTime: 0,
      maxBufferSize: 100,
      timePerMethod: { [RestRequestMethod.PATCH]: 3 } as any // time in seconds
    },
    // In-memory cache of server-side rendered content
    serverSide: {
      debug: false,
      // Cache specific to known bots.  Allows you to serve cached contents to bots only.
      // Defaults to caching 1,000 pages. Each page expires after 1 day
      botCache: {
        // Maximum number of pages (rendered via SSR) to cache. Setting max=0 disables the cache.
        max: 1000,
        // Amount of time after which cached pages are considered stale (in ms)
        timeToLive: 24 * 60 * 60 * 1000, // 1 day
        allowStale: true,
      },
      // Cache specific to anonymous users. Allows you to serve cached content to non-authenticated users.
      // Defaults to caching 0 pages. But, when enabled, each page expires after 10 seconds (to minimize anonymous users seeing out-of-date content)
      anonymousCache: {
        // Maximum number of pages (rendered via SSR) to cache. Setting max=0 disables the cache.
        max: 0, // disabled by default
        // Amount of time after which cached pages are considered stale (in ms)
        timeToLive: 10 * 1000, // 10 seconds
        allowStale: true,
      }
    }
  };

  // Authentication settings
  auth: AuthConfig = {
    // Authentication UI settings
    ui: {
      // the amount of time before the idle warning is shown
      timeUntilIdle: 15 * 60 * 1000, // 15 minutes
      // the amount of time the user has to react after the idle warning is shown before they are logged out.
      idleGracePeriod: 5 * 60 * 1000 // 5 minutes
    },
    // Authentication REST settings
    rest: {
      // If the rest token expires in less than this amount of time, it will be refreshed automatically.
      // This is independent from the idle warning.
      timeLeftBeforeTokenRefresh: 2 * 60 * 1000 // 2 minutes
    }
  };

  // Form settings
  form: FormConfig = {
    spellCheck: true,
    // NOTE: Map server-side validators to comparative Angular form validators
    validatorMap: {
      required: 'required',
      regex: 'pattern'
    }
  };

  // Notifications
  notifications: INotificationBoardOptions = {
    rtl: false,
    position: ['top', 'right'],
    maxStack: 8,
    // NOTE: after how many seconds notification is closed automatically. If set to zero notifications are not closed automatically
    timeOut: 5000, // 5 second
    clickToClose: true,
    // NOTE: 'fade' | 'fromTop' | 'fromRight' | 'fromBottom' | 'fromLeft' | 'rotate' | 'scale'
    animate: NotificationAnimationsType.Scale
  };

  // Submission settings
  submission: SubmissionConfig = {
    autosave: {
      // NOTE: which metadata trigger an autosave
      metadata: [],
      /**
       * NOTE: after how many time (milliseconds) submission is saved automatically
       * eg. timer: 5 * (1000 * 60); // 5 minutes
       */
      timer: 0
    },
    typeBind: {
      field: 'dc.type'
    },
    icons: {
      metadata: [
        /**
         * NOTE: example of configuration
         * {
         *    // NOTE: metadata name
         *    name: 'dc.author',
         *    // NOTE: fontawesome (v5.x) icon classes and bootstrap utility classes can be used
         *    style: 'fa-user'
         * }
         */
        {
          name: 'dc.author',
          style: 'fas fa-user'
        },
        // default configuration
        {
          name: 'default',
          style: ''
        }
      ],
      authority: {
        confidence: [
          /**
           * NOTE: example of configuration
           * {
           *    // NOTE: confidence value
           *    value: 'dc.author',
           *    // NOTE: fontawesome (v4.x) icon classes and bootstrap utility classes can be used
           *    style: 'fa-user'
           * }
           */
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
          // default configuration
          {
            value: 'default',
            style: 'text-muted'
          }

        ]
      }
    }
  };

  // Default Language in which the UI will be rendered if the user's browser language is not an active language
  defaultLanguage = 'en';

  // Languages. DSpace Angular holds a message catalog for each of the following languages.
  // When set to active, users will be able to switch to the use of this language in the user interface.
  languages: LangConfig[] = [
    { code: 'en', label: 'English', active: true },
    { code: 'ca', label: 'Català', active: true },
    { code: 'cs', label: 'Čeština', active: true },
    { code: 'de', label: 'Deutsch', active: true },
    { code: 'es', label: 'Español', active: true },
    { code: 'fr', label: 'Français', active: true },
    { code: 'gd', label: 'Gàidhlig', active: true },
    { code: 'lv', label: 'Latviešu', active: true },
    { code: 'hu', label: 'Magyar', active: true },
    { code: 'nl', label: 'Nederlands', active: true },
    { code: 'pl', label: 'Polski', active: true },
    { code: 'pt-PT', label: 'Português', active: true },
    { code: 'pt-BR', label: 'Português do Brasil', active: true },
    { code: 'fi', label: 'Suomi', active: true },
    { code: 'sv', label: 'Svenska', active: true },
    { code: 'tr', label: 'Türkçe', active: true },
    { code: 'kk', label: 'Қазақ', active: true },
    { code: 'bn', label: 'বাংলা', active: true },
    { code: 'hi', label: 'हिंदी', active: true},
    { code: 'el', label: 'Ελληνικά', active: true },
    { code: 'uk', label: 'Yкраї́нська', active: true}
  ];

  // Browse-By Pages
  browseBy: BrowseByConfig = {
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
    pageSize: 20
  };

  communityList: CommunityListConfig = {
    pageSize: 20
  };

  homePage: HomeConfig = {
    recentSubmissions: {
      //The number of item showing in recent submission components
      pageSize: 5,
      //sort record of recent submission
      sortField: 'dc.date.accessioned',
    },
    topLevelCommunityList: {
      pageSize: 5
    }
  };

  // Item Config
  item: ItemConfig = {
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
  };

  // Collection Page Config
  collection: CollectionPageConfig = {
    edit: {
      undoTimeout: 10000 // 10 seconds
    }
  };

  // Theme Config
  themes: ThemeConfig[] = [
    // Add additional themes here. In the case where multiple themes match a route, the first one
    // in this list will get priority. It is advisable to always have a theme that matches
    // every route as the last one

    // {
    //   // A theme with a handle property will match the community, collection or item with the given
    //   // handle, and all collections and/or items within it
    //   name: 'custom',
    //   handle: '10673/1233'
    // },
    // {
    //   // A theme with a regex property will match the route using a regular expression. If it
    //   // matches the route for a community or collection it will also apply to all collections
    //   // and/or items within it
    //   name: 'custom',
    //   regex: 'collections\/e8043bc2.*'
    // },
    // {
    //   // A theme with a uuid property will match the community, collection or item with the given
    //   // ID, and all collections and/or items within it
    //   name: 'custom',
    //   uuid: '0958c910-2037-42a9-81c7-dca80e3892b4'
    // },
    // {
    //   // The extends property specifies an ancestor theme (by name). Whenever a themed component is not found
    //   // in the current theme, its ancestor theme(s) will be checked recursively before falling back to default.
    //   name: 'custom-A',
    //   extends: 'custom-B',
    //   // Any of the matching properties above can be used
    //   handle: '10673/34'
    // },
    // {
    //   name: 'custom-B',
    //   extends: 'custom',
    //   handle: '10673/12'
    // },
    // {
    //   // A theme with only a name will match every route
    //   name: 'custom'
    // },
    // {
    //   // This theme will use the default bootstrap styling for DSpace components
    //   name: BASE_THEME_NAME
    // },

    {
      // The default dspace theme
      name: 'dspace',
      // Whenever this theme is active, the following tags will be injected into the <head> of the page.
      // Example use case: set the favicon based on the active theme.
      headTags: [
        {
          // Insert <link rel="icon" href="assets/dspace/images/favicons/favicon.ico" sizes="any"/> into the <head> of the page.
          tagName: 'link',
          attributes: {
            'rel': 'icon',
            'href': 'assets/dspace/images/favicons/favicon.ico',
            'sizes': 'any',
          }
        },
        {
          // Insert <link rel="icon" href="assets/dspace/images/favicons/favicon.svg" type="image/svg+xml"/> into the <head> of the page.
          tagName: 'link',
          attributes: {
            'rel': 'icon',
            'href': 'assets/dspace/images/favicons/favicon.svg',
            'type': 'image/svg+xml',
          }
        },
        {
          // Insert <link rel="apple-touch-icon" href="assets/dspace/images/favicons/apple-touch-icon.png"/> into the <head> of the page.
          tagName: 'link',
          attributes: {
            'rel': 'apple-touch-icon',
            'href': 'assets/dspace/images/favicons/apple-touch-icon.png',
          }
        },
        {
          // Insert <link rel="manifest" href="assets/dspace/images/favicons/manifest.webmanifest"/> into the <head> of the page.
          tagName: 'link',
          attributes: {
            'rel': 'manifest',
            'href': 'assets/dspace/images/favicons/manifest.webmanifest',
          }
        },
      ]
    },
  ];
  // The default bundles that should always be displayed when you edit or add a bundle even when no bundle has been
  // added to the item yet.
  bundle: BundleConfig = {
    standardBundles: ['ORIGINAL', 'THUMBNAIL', 'LICENSE']
  };
  // Whether to enable media viewer for image and/or video Bitstreams (i.e. Bitstreams whose MIME type starts with "image" or "video").
  // For images, this enables a gallery viewer where you can zoom or page through images.
  // For videos, this enables embedded video streaming
  mediaViewer: MediaViewerConfig = {
    image: false,
    video: false
  };
  // Whether the end-user-agreement and privacy policy feature should be enabled or not.
  // Disabling the end user agreement feature will result in:
  // - Users no longer being forced to accept the end-user-agreement before they can access the repository
  // - A 404 page if you manually try to navigate to the end-user-agreement page at info/end-user-agreement
  // - All end-user-agreement related links and pages will be removed from the UI (e.g. in the footer)
  // Disabling the privacy policy feature will result in:
  // - A 404 page if you manually try to navigate to the privacy policy page at info/privacy
  // - All mentions of the privacy policy being removed from the UI (e.g. in the footer)
  info: InfoConfig = {
    enableEndUserAgreement: true,
    enablePrivacyStatement: true
  };

  // Whether to enable Markdown (https://commonmark.org/) and MathJax (https://www.mathjax.org/)
  // display in supported metadata fields. By default, only dc.description.abstract is supported.
  markdown: MarkdownConfig = {
    enabled: false,
    mathjax: false,
  };

  // Which vocabularies should be used for which search filters
  // and whether to show the filter in the search sidebar
  // Take a look at the filter-vocabulary-config.ts file for documentation on how the options are obtained
  vocabularies: FilterVocabularyConfig[] = [
    {
      filter: 'subject',
      vocabulary: 'srsc',
      enabled: false
    }
    ];
}
