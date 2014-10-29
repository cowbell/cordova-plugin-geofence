exports.defineAutoTests = function () {
    describe('Geofence plugin object', function () {
        it('should be defined in window object', function () {
            expect(window.geofence).toBeDefined();
        });
    });
};

exports.defineManualTests = function (contentEl, createActionButton) {
    createActionButton('Test button', function () {
        console.log('Test button clicked');
    });
};
