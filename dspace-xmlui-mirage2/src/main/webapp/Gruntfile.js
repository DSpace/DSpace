/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
// Generated on 2013-11-09 using generator-webapp 0.4.3
'use strict';

module.exports = function (grunt) {
    // show elapsed time at the end
    require('time-grunt')(grunt);
    // load all grunt tasks
    require('load-grunt-tasks')(grunt);

    grunt.initConfig({
        copy: {
            classic_mirage_color_scheme: {
                files: [
                    {
                        src: ['styles/classic_mirage_color_scheme/_main.scss'],
                        dest: 'styles/main.scss'
                    }
                ]
            },
            bootstrap_color_scheme: {
                files: [
                    {
                        src: ['styles/bootstrap_color_scheme/_main.scss'],
                        dest: 'styles/main.scss'
                    }
                ]
            },
            scriptsxml: {
                files: [
                    {
                        src: ['scripts.xml'],
                        dest: 'scripts-dist.xml'
                    }
                ]
            }
        },
        compass: {
            prod: {
                options: {
                    config: 'config-prod.rb'
                }

            },
            dev: {
                options: {
                    config: 'config-dev.rb'
                }

            }
        },
        coffee: {
            glob_to_multiple: {
                expand: true,
                flatten: true,
                cwd: 'scripts',
                src: ['*.coffee'],
                dest: 'scripts',
                ext: '.js'
            }
        },
        handlebars: {
            compile: {
                options: {
                    namespace: "DSpace.templates",
                    processName: function(filePath) {
                        return filePath.replace(/^templates\//, '').replace(/\.handlebars$/, '').replace(/\.hbs$/, '');
                    }
                },
                files: {
                    "scripts/templates.js": ["templates/*.handlebars", "templates/*.hbs"]
                }
            }
        },
        useminPrepare:{
            src: ['scripts-dist.xml'],
            options: {
                // fool usemin in to putting theme.js straight into the scripts
                // folder, and not in a separate dist folder. And no, you can't
                // just use an empty string, I tried ;)
                dest: 'dist/../'
            }
        } ,
        usemin: {
            html:'scripts-dist.xml'
        }
    });


    grunt.registerTask('classic_mirage_color_scheme', [
        'copy:classic_mirage_color_scheme'
    ]);
    grunt.registerTask('bootstrap_color_scheme', [
        'copy:bootstrap_color_scheme'
    ]);
    grunt.registerTask('shared-steps', [
        'copy:scriptsxml', 'coffee', 'handlebars', 'useminPrepare','concat'
    ]);
    grunt.registerTask('no-compass-prod', [
        'shared-steps','uglify','usemin'
    ]);
    grunt.registerTask('no-compass-dev', [
        'shared-steps','uglify:generated'
    ]);
    grunt.registerTask('prod', [
        'compass:prod', 'no-compass-prod'
    ]);
    grunt.registerTask('dev', [
        'compass:dev', 'no-compass-dev'
    ]);
    grunt.registerTask('default', [
        'classic_mirage_color_scheme',
        'prod'
    ]);
};
