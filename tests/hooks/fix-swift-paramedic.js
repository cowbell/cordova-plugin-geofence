var child_process = require('child_process'),
    fs = require('fs'),
    path = require('path');

module.exports = function(context) {
    var COMMENT_KEY = /_comment$/,
        CORDOVA_VERSION = process.env.CORDOVA_VERSION;

    if (!process.env.FIX_PARAMEDIC) {
        return;
    }

    run();

    function run() {
        var cordova_util = context.requireCordovaModule('cordova-lib/src/cordova/util'),
            ConfigParser = CORDOVA_VERSION >= 6.0
                ? context.requireCordovaModule('cordova-common').ConfigParser
                : context.requireCordovaModule('cordova-lib/src/configparser/ConfigParser'),
            projectRoot = cordova_util.isCordova(),
            platform_ios,
            xml = cordova_util.projectConfig(projectRoot),
            cfg = new ConfigParser(xml),
            projectName = cfg.name(),
            platform_ios = CORDOVA_VERSION < 5.0
                ? context.requireCordovaModule('cordova-lib/src/plugman/platforms')['ios']
                : context.requireCordovaModule('cordova-lib/src/plugman/platforms/ios'),
            iosPlatformPath = path.join(projectRoot, 'platforms', 'ios'),
            iosProjectFilesPath = path.join(iosPlatformPath, projectName),
            xcconfigPath = path.join(iosPlatformPath, 'cordova', 'build.xcconfig'),
            xcconfigContent,
            projectFile,
            xcodeProject,
            bridgingHeaderPath;

        projectFile = platform_ios.parseProjectFile(iosPlatformPath);
        xcodeProject = projectFile.xcode;


        var configurations = nonComments(xcodeProject.pbxXCBuildConfigurationSection()),
        config, buildSettings;

        for (config in configurations) {
            buildSettings = configurations[config].buildSettings;
            buildSettings['SWIFT_VERSION'] = '2.3'
        }
        console.log('IOS project Swift Version changed to 2.3 ...');

        projectFile.write();
    }

    function nonComments(obj) {
        var keys = Object.keys(obj),
            newObj = {},
            i = 0;

        for (i; i < keys.length; i++) {
            if (!COMMENT_KEY.test(keys[i])) {
                newObj[keys[i]] = obj[keys[i]];
            }
        }

        return newObj;
    }

    function unquote(str) {
        if (str) return str.replace(/^"(.*)"$/, "$1");
    }
}