#!/usr/bin/env node

module.exports = function(context) {

    var IOS_DEPLOYMENT_TARGET = '7.0';

    var fs = require("fs"),
        path = require("path"),
        shell = require("shelljs"),
        xcode = require('xcode'),
        xml = require("node-xml-lite"),
        glob = require("glob"),
        readline = require("readline"),
        COMMENT_KEY = /_comment$/,
        projectRoot = process.argv[2];
    //if run for plugin projectRoot initialy is platform
    projectRoot = path.join(projectRoot,'..');

    run(projectRoot);
    /*
      This is our runner function. It sets up the project paths,
      parses the project file using xcode and delegates to our updateDeploymentTarget
      that does the actual work.
    */

    function run(projectRoot) {
        var projectName = getProjectName(projectRoot),
            xcodeProjectName = projectName + '.xcodeproj',
            iosPlatformPath = path.join(projectRoot, 'platforms', 'ios'),
            iosProjectFilesPath = path.join(iosPlatformPath, projectName),
            xcodeProjectPath = path.join(iosPlatformPath, xcodeProjectName, 'project.pbxproj'),
            bridgingHeaderPath;

        if (!fs.existsSync(xcodeProjectPath)) {
            return;
        }

        xcodeProject = xcode.project(xcodeProjectPath);

        shell.echo("Adjusting iOS deployment target for " + projectName + " to: [" + IOS_DEPLOYMENT_TARGET + "] ...");

        xcodeProject.parse(function(err) {
            if (err) {
                shell.echo('An error occured during parsing of [' + xcodeProjectPath + ']: ' + JSON.stringify(err));
            } else {
                bridgingHeaderPath = getBridgingHeader(xcodeProject);
                if(bridgingHeaderPath) {
                    bridgingHeaderPath = path.join(iosPlatformPath, bridgingHeaderPath);
                } else {
                    bridgingHeaderPath = createBridgingHeader(xcodeProject, projectName, iosProjectFilesPath);
                }
                importBridgingHeaders(bridgingHeaderPath, getPluginsBridgingHeaders(iosProjectFilesPath));
                var configurations = nonComments(xcodeProject.pbxXCBuildConfigurationSection()),
                    config, buildSettings;

                for (config in configurations) {
                    buildSettings = configurations[config].buildSettings;
                    buildSettings['IPHONEOS_DEPLOYMENT_TARGET'] = IOS_DEPLOYMENT_TARGET;
                    buildSettings['EMBEDDED_CONTENT_CONTAINS_SWIFT'] = "YES";
                    buildSettings['LD_RUNPATH_SEARCH_PATHS'] = '"@executable_path/Frameworks"'
                }
                shell.echo('[' + xcodeProjectPath + '] now has deployment target set as:[' + IOS_DEPLOYMENT_TARGET + '] ...');
                shell.echo('[' + xcodeProjectPath + '] option EMBEDDED_CONTENT_CONTAINS_SWIFT set as:[YES] ...');
                shell.echo('[' + xcodeProjectPath + '] swift_objc Bridging-Header set to:[' + bridgingHeaderPath + '] ...');
                shell.echo('[' + xcodeProjectPath + '] Runpath Search Paths set to: @executable_path/Frameworks ...');
                shell.echo('[' + xcodeProjectPath + '] Adding libsqlite3...');
                xcodeProject.addFramework("libsqlite3.dylib");
                fs.writeFileSync(xcodeProjectPath, xcodeProject.writeSync(), 'utf-8');
            }
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
        shell.echo('Creating new Bridging-Header.h at path: ', newBHPath);
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

    function getPluginsBridgingHeaders(xcodeProjectRootPath) {
        var searchPath = path.join(xcodeProjectRootPath, 'Plugins');

        return glob.sync("**/*Bridging-Header*.h", { cwd: searchPath })
            .map(function(filePath) {
                return path.basename(path.join(searchPath, filePath));
            })
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
                shell.echo('Importing ' + header + ' into main bridging-header at: ' + mainBridgingHeader);
            }
        });
        fs.writeFileSync(mainBridgingHeader, content, 'utf-8');
    }

    function getProjectName(protoPath) {
        var cordovaConfigPath = path.join(protoPath, 'config.xml'),
            xmlObject = xml.parseFileSync(cordovaConfigPath)
            //content = fs.readFileSync(cordovaConfigPath, 'utf-8'),
            //use better lib

        return xmlObject.childs[0].childs[0]
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
