#!/usr/bin/env node

var IOS_DEPLOYMENT_TARGET = '7.0';

var fs = require("fs"),
    path = require("path"),
    shell = require("shelljs"),
    xcode = require('xcode'),
    xml = require("node-xml-lite"),
    COMMENT_KEY = /_comment$/,
    projectRoot = process.argv[2];

run(projectRoot);

/*
  This is our runner function. It sets up the project paths,
  parses the project file using xcode and delegates to our updateDeploymentTarget
  that does the actual work.
*/

function run(projectRoot) {
    var projectName = getProjectName(projectRoot),
        xcodeProjectName = projectName + '.xcodeproj',
        xcodeProjectPath = path.join(projectRoot, 'platforms', 'ios', xcodeProjectName, 'project.pbxproj'),
        xcodeProject,
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
            bridgingHeaderPath = path.join(projectName, "Classes", "Bridging-Header.h")
            var configurations = nonComments(xcodeProject.pbxXCBuildConfigurationSection()),
                config, buildSettings;

            for (config in configurations) {
                buildSettings = configurations[config].buildSettings;
                buildSettings['IPHONEOS_DEPLOYMENT_TARGET'] = IOS_DEPLOYMENT_TARGET;
                buildSettings['EMBEDDED_CONTENT_CONTAINS_SWIFT'] = "YES";
                buildSettings['SWIFT_OBJC_BRIDGING_HEADER'] = bridgingHeaderPath;
            }
            shell.echo('[' + xcodeProjectPath + '] now has deployment target set as:[' + IOS_DEPLOYMENT_TARGET + '] ...');
            shell.echo('[' + xcodeProjectPath + '] option EMBEDDED_CONTENT_CONTAINS_SWIFT set as:[YES] ...');
            shell.echo('[' + xcodeProjectPath + '] swift_objc Bridging-Header set to:[' + bridgingHeaderPath + '] ...');
            fs.writeFileSync(xcodeProjectPath, xcodeProject.writeSync(), 'utf-8');
        }
    });
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
