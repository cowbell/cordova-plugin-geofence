exports.defineAutoTests = function () {
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
};

exports.defineManualTests = function (contentEl, createActionButton) {
    createActionButton('Test button', function () {
        console.log('Test button clicked');
    });
};
