if (typeof Object.assign != 'function') {
    Object.assign = function(target, varArgs) { // .length of function is 2
        'use strict';
        if (target == null) { // TypeError if undefined or null
            throw new TypeError('Cannot convert undefined or null to object');
        }

        var to = Object(target);

        for (var index = 1; index < arguments.length; index++) {
            var nextSource = arguments[index];

            if (nextSource != null) { // Skip over if undefined or null
                for (var nextKey in nextSource) {
                    // Avoid bugs when hasOwnProperty is shadowed
                    if (Object.prototype.hasOwnProperty.call(nextSource, nextKey)) {
                        to[nextKey] = nextSource[nextKey];
                    }
                }
            }
        }
        return to;
    };
}

exports.defineAutoTests = function () {
    var ANDROID_MAX_ALLOWED_GEOFENCES = 100;
    var IOS_MAX_ALLOWED_GEOFENCES = 20;
    var TESTS_TIMEOUT = 60000; // 1 min
    var MAX_ALLOWED_GEOFENCES = cordova.platformId === "android"
            ? ANDROID_MAX_ALLOWED_GEOFENCES
            : IOS_MAX_ALLOWED_GEOFENCES;

    var majorDeviceVersion = null;
    var versionRegex = /(\d)\..+/.exec(device.version);
    if (versionRegex !== null) {
        majorDeviceVersion = Number(versionRegex[1]);
    }
    // Starting from Android 6.0 there are confirmation dialog which prevents us from running auto tests in silent mode (user interaction needed)
    // Also, Android emulator doesn't provide geo fix without manual interactions or mocks
    var skipAndroid = cordova.platformId === "android" && device.isVirtual &&  majorDeviceVersion >= 6;

    var fail = function (done, reason) {
        if (reason) {
            console.log('Fail reason: ', reason);
        }
        expect(true).toBe(false);
        done();
    };

    jasmine.DEFAULT_TIMEOUT_INTERVAL = TESTS_TIMEOUT;

    describe("Geofence plugin", function () {
        beforeAll(function () {
            return window.geofence.initialize();
        });

        describe("object", function () {
            it("should be defined in window object", function () {
                expect(window.geofence).toBeDefined();
            });

            it("should contain addOrUpdate function", function () {
                expect(window.geofence.addOrUpdate).toBeDefined();
                expect(typeof window.geofence.addOrUpdate).toBe("function");
            });

            it("should contain initialize function", function () {
                expect(window.geofence.initialize).toBeDefined();
                expect(typeof window.geofence.initialize).toBe("function");
            });

            it("should contain remove function", function () {
                expect(window.geofence.remove).toBeDefined();
                expect(typeof window.geofence.remove).toBe("function");
            });

            it("should contain removeAll function", function () {
                expect(window.geofence.removeAll).toBeDefined();
                expect(typeof window.geofence.removeAll).toBe("function");
            });

            it("should contain getWatched function", function () {
                expect(window.geofence.getWatched).toBeDefined();
                expect(typeof window.geofence.getWatched).toBe("function");
            });
        });

        describe("removeAll function", function () {
            it("should remove all stored geofences", function (done) {
                if (skipAndroid) {
                    pending();
                }

                var geofence = {
                    id: "1",
                    latitude: 50,
                    longitude: 50,
                    radius: 1000,
                    transitionType: 1
                };

                window.geofence.addOrUpdate(geofence)
                    .then(window.geofence.removeAll)
                    .then(window.geofence.getWatched)
                    .then(function (geofencesJson) {
                        var geofences = JSON.parse(geofencesJson);
                        expect(geofences.length).toBe(0);
                        done();
                    })
                    .catch(fail.bind(this, done));
            });
        });

        describe("addOrUpdate function", function () {
            var geofence, geofence2;

            beforeEach(function () {
                geofence = {
                    id: "1",
                    latitude: 50,
                    longitude: 50,
                    radius: 1000,
                    transitionType: 1
                };
                geofence2 = {
                    id: "2",
                    latitude: 55,
                    longitude: 55,
                    radius: 1000,
                    transitionType: 1
                };
            });

            afterEach(function () {
                return window.geofence.removeAll();
            });

            it("should add single geofence", function (done) {
                if (skipAndroid) {
                    pending();
                }

                window.geofence
                    .addOrUpdate(geofence)
                    .then(window.geofence.getWatched)
                    .then(function (geofencesJson) {
                        var geofences = JSON.parse(geofencesJson);
                        expect(geofences[0].id).toBe("1");
                        done();
                    })
                    .catch(fail.bind(this, done));
            });

            it("should add array of geofences", function (done) {
                if (skipAndroid) {
                    pending();
                }

                window.geofence
                    .addOrUpdate([geofence, geofence2])
                    .then(window.geofence.getWatched)
                    .then(function (geofencesJson) {
                        var geofences = JSON.parse(geofencesJson);
                        expect(geofences.length).toBe(2);
                        expect(geofences.filter(filterById("1"))[0].id).toBe("1");
                        expect(geofences.filter(filterById("2"))[0].id).toBe("2");
                        done();
                    })
                    .catch(fail.bind(this, done));
            });

            describe("geofence limits", function () {
                var geofences;

                beforeEach(function (done) {
                    geofences = [];

                    for (var i = 0; i < MAX_ALLOWED_GEOFENCES; i++) {
                        geofences.push(Object.assign({}, geofence, { id: i.toString() }));
                    }
                    done();
                });

                it("should allow to add upper limit geofences", function (done) {
                    if (skipAndroid) {
                        pending();
                    }

                    window.geofence
                        .addOrUpdate(geofences)
                        .then(window.geofence.getWatched)
                        .then(function (geofencesJson) {
                            var geofences = JSON.parse(geofencesJson);
                            expect(geofences.length).toBe(MAX_ALLOWED_GEOFENCES);
                            done();
                        })
                        .catch(fail.bind(this, done));
                });

                it("should return error when trying to exceed limit", function (done) {
                    if (skipAndroid) {
                        pending();
                    }

                    geofences.push(Object.assign({}, geofence, { id: MAX_ALLOWED_GEOFENCES.toString() }));
                    window.geofence
                        .addOrUpdate(geofences)
                        .then(fail.bind(this, done))
                        .catch(function (error) {
                            expect(error.code).toEqual("GEOFENCE_LIMIT_EXCEEDED");
                            expect(error.message).toBeDefined();
                            done();
                        });
                });
            });

            describe("geofence object coercion", function () {
                it("should throw an error if id is not provided", function (done) {
                    geofence.id = undefined;

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrowError("Geofence id is not provided");
                    done();
                });

                it("should throw an error if latitude is not provided", function (done) {
                    geofence.latitude = undefined;

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Geofence latitude is not provided"));
                    done();
                });

                it("should throw an error if latitude can't be converted to number", function (done) {
                    geofence.latitude = "somestring";

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Cannot convert Geofence latitude to number"));
                    done();
                });

                it("should convert latitude to number if possible", function (done) {
                    if (skipAndroid) {
                        pending();
                    }

                    geofence.latitude = "50.5";
                    window.geofence.addOrUpdate(geofence)
                        .then(function () {
                            expect(geofence.latitude).toBe(50.5);
                            done();
                        });
                });

                it("should throw an error if longitude is not provided", function (done) {
                    geofence.longitude = undefined;

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Geofence longitude is not provided"));
                    done();
                });

                it("should throw an error if longitude can't be converted to number", function (done) {
                    geofence.longitude = "somestring";

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Cannot convert Geofence longitude to number"));
                    done();
                });

                it("should throw an error if radius is not provided", function (done) {
                    geofence.radius = undefined;

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Geofence radius is not provided"));
                    done();
                });

                it("should throw an error if radius can't be converted to number", function (done) {
                    geofence.radius = "somestring";

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Cannot convert Geofence radius to number"));
                    done();
                });

                it("should throw an error if transitionType is not provided", function (done) {
                    geofence.transitionType = undefined;

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Geofence transitionType is not provided"));
                    done();
                });

                it("should throw an error if transitionType can't be converted to number", function (done) {
                    geofence.transitionType = "somestring";

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Cannot convert Geofence transitionType to number"));
                    done();
                });

                it("should throw an error if notification.vibration is not an Array", function (done) {
                    geofence.notification = {
                        vibration: "somestring"
                    };

                    expect(function () {
                        window.geofence.addOrUpdate(geofence);
                    }).toThrow(new Error("Geofence notification.vibration is not an Array"));
                    done();
                });

            });
        });
    })

    function filterById(id) {
        return function (item) {
            return item.id === id;
        };
    }
};

exports.defineManualTests = function (contentEl, createActionButton) {
    // createActionButton("Test button", function () {
    //     console.log("Test button clicked");
    // });
};
