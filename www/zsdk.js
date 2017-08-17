/*global cordova, module*/

module.exports = {
    discover: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ZsdkPlugin", "discover", []);
    },
    printImage: function(macAddress, imageData, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ZsdkPlugin", "printImage", [macAddress, imageData]);
    }
};
