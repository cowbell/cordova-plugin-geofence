exports.defineAutoTests = function () {
    var fail = function (done, reason) {
            if (reason) {
                console.log(reason);
            }
            expect(true).toBe(false);
            done();
        },
        succeed = function (done) {
            expect(true).toBe(true);
            done();
        },
        geofence = {
            id: '1',
            latitude: 50,
            longitude: 50,
            radius: 1000,
            transitionType: 1
        },
        geofence2 = {
            id: '2',
            latitude: 55,
            longitude: 55,
            radius: 1000,
            transitionType: 1
        };

    describe('Geofence plugin object', function () {
        it('should be defined in window object', function () {
            expect(window.geofence).toBeDefined();
        });

        it('should contain addOrUpdate function', function () {
            expect(window.geofence.addOrUpdate).toBeDefined();
            expect(typeof window.geofence.addOrUpdate).toBe('function');
        });

        it('should contain initialize function', function () {
            expect(window.geofence.initialize).toBeDefined();
            expect(typeof window.geofence.initialize).toBe('function');
        });

        it('should contain remove function', function () {
            expect(window.geofence.remove).toBeDefined();
            expect(typeof window.geofence.remove).toBe('function');
        });

        it('should contain removeAll function', function () {
            expect(window.geofence.removeAll).toBeDefined();
            expect(typeof window.geofence.removeAll).toBe('function');
        });

        it('should contain getWatched function', function () {
            expect(window.geofence.getWatched).toBeDefined();
            expect(typeof window.geofence.getWatched).toBe('function');
        });
    });

    describe('removeAll function', function () {
        it('should remove all stored geofences', function (done) {
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

    describe('addOrUpdate function', function () {
        beforeEach(function () {

        });

        afterEach(function () {
            window.geofence.removeAll();
        });

        it('should add single geofence', function (done) {
            window.geofence
                .addOrUpdate(geofence)
                .then(window.geofence.getWatched)
                .then(function (geofencesJson) {
                    var geofences = JSON.parse(geofencesJson);
                    expect(geofences[0].id).toBe('1');
                    done();
                })
                .catch(fail.bind(this, done));
        });

        it('should add array of geofences', function (done) {
            window.geofence
                .addOrUpdate([geofence, geofence2])
                .then(window.geofence.getWatched)
                .then(function (geofencesJson) {
                    var geofences = JSON.parse(geofencesJson);
                    expect(geofences.length).toBe(2);
                    expect(geofences.filter(filterById('1'))[0].id).toBe('1');
                    expect(geofences.filter(filterById('2'))[0].id).toBe('2');
                    done();
                })
                .catch(fail.bind(this, done));
        });

    });

    function filterById(id) {
        return function (item) {
            return item.id === id;
        };
    }
};

exports.defineManualTests = function (contentEl, createActionButton) {
    createActionButton('Test button', function () {
        console.log('Test button clicked');
    });
};
