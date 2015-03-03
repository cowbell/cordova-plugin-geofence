var child_process = require('child_process'),
    fs = require('fs'),
    path = require('path');

module.exports = function(context) {
    var IOS_DEPLOYMENT_TARGET = '7.0',
        COMMENT_KEY = /_comment$/,
        projectRoot = process.argv[2];

    //if run for plugin projectRoot initialy is platform
    projectRoot = path.resolve(path.join(projectRoot,'..'));

    run(projectRoot);

    function run(projectRoot) {
        var cordova_util = context.requireCordovaModule('cordova-lib/src/cordova/util'),
            ConfigParser = context.requireCordovaModule('cordova-lib/src/configparser/ConfigParser'),
            platforms = context.requireCordovaModule('cordova-lib/src/plugman/platforms'),
            xml = cordova_util.projectConfig(projectRoot),
            cfg = new ConfigParser(xml),
            projectName = cfg.name(),
            iosPlatformPath = path.join(projectRoot, 'platforms', 'ios'),
            iosProjectFilesPath = path.join(iosPlatformPath, projectName),
            projectFile = platforms['ios'].parseProjectFile(iosPlatformPath),
            xcodeProject = projectFile.xcode,
            bridgingHeaderPath;

        bridgingHeaderPath = getBridgingHeader(xcodeProject);
        if(bridgingHeaderPath) {
            bridgingHeaderPath = path.join(iosPlatformPath, bridgingHeaderPath);
        } else {
            bridgingHeaderPath = createBridgingHeader(xcodeProject, projectName, iosProjectFilesPath);
        }

        getExistingBridgingHeaders(iosProjectFilesPath, function (headers) {
            importBridgingHeaders(bridgingHeaderPath, headers);
            var configurations = nonComments(xcodeProject.pbxXCBuildConfigurationSection()),
            config, buildSettings;

            for (config in configurations) {
                buildSettings = configurations[config].buildSettings;
                buildSettings['IPHONEOS_DEPLOYMENT_TARGET'] = IOS_DEPLOYMENT_TARGET;
                buildSettings['EMBEDDED_CONTENT_CONTAINS_SWIFT'] = "YES";
                buildSettings['LD_RUNPATH_SEARCH_PATHS'] = '"@executable_path/Frameworks"'
            }
            console.log('IOS project now has deployment target set as:[' + IOS_DEPLOYMENT_TARGET + '] ...');
            console.log('IOS project option EMBEDDED_CONTENT_CONTAINS_SWIFT set as:[YES] ...');
            console.log('IOS project swift_objc Bridging-Header set to:[' + bridgingHeaderPath + '] ...');
            console.log('IOS project Runpath Search Paths set to: @executable_path/Frameworks ...');
            console.log('IOS project Adding libsqlite3...');
            xcodeProject.addFramework("libsqlite3.dylib");

            projectFile.write();
        });
    }

    function getBridgingHeader(xcodeProject) {
        var configurations = nonComments(xcodeProject.pbxXCBuildConfigurationSection()),
            config, buildSettings, bridgingHeader;

        for (config in configurations) {
            buildSettings = configurations[config].buildSettings;
            bridgingHeader = buildSettings['SWIFT_OBJC_BRIDGING_HEADER'];
            if (bridgingHeader) {
                return unquote(bridgingHeader);
            }
        }
    }

    function createBridgingHeader(xcodeProject, projectName, xcodeProjectRootPath) {
        var newBHPath = path.join(xcodeProjectRootPath, "Plugins", "Bridging-Header.h"),
            content = ["//",
            "//  Use this file to import your target's public headers that you would like to expose to Swift.",
            "//",
            "#import <Cordova/CDV.h>"]

        //fs.openSync(newBHPath, 'w');
        console.log('Creating new Bridging-Header.h at path: ', newBHPath);
        fs.writeFileSync(newBHPath, content.join("\n"), { encoding: 'utf-8', flag: 'w' });
        xcodeProject.addHeaderFile("Bridging-Header.h");
        setBridgingHeader(xcodeProject, path.join(projectName, "Plugins", "Bridging-Header.h"));
        return newBHPath;
    }

    function setBridgingHeader(xcodeProject, headerPath) {
        var configurations = nonComments(xcodeProject.pbxXCBuildConfigurationSection()),
            config, buildSettings, bridgingHeader;

        for (config in configurations) {
            buildSettings = configurations[config].buildSettings;
            buildSettings['SWIFT_OBJC_BRIDGING_HEADER'] = '"' + headerPath + '"';
        }
    }

    function getExistingBridgingHeaders(xcodeProjectRootPath, callback) {
        var searchPath = path.join(xcodeProjectRootPath, 'Plugins');

        child_process.exec('find . -name "*Bridging-Header*.h"', { cwd: searchPath }, function (error, stdout, stderr) {
            var headers = stdout.toString().split('\n').map(function (filePath) {
                return path.basename(filePath);
            });
            callback(headers);
        });
    }

    function importBridgingHeaders(mainBridgingHeader, headers) {
        var content = fs.readFileSync(mainBridgingHeader, 'utf-8'),
            mainHeaderName = path.basename(mainBridgingHeader);

        headers.forEach(function (header) {
            if(header !== mainHeaderName && content.indexOf(header) < 0) {
                if (content.charAt(content.length - 1) != '\n') {
                    content += "\n";
                }
                content += "#import \""+header+"\"\n"
                console.log('Importing ' + header + ' into main bridging-header at: ' + mainBridgingHeader);
            }
        });
        fs.writeFileSync(mainBridgingHeader, content, 'utf-8');
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
