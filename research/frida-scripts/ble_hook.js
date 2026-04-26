/**
 * ble_hook.js — Frida hook for TVS Connect BLE traffic interception
 *
 * Usage:
 *   frida -U -f com.tvsm.connect -l ble_hook.js --no-pause
 *
 * Or with objection (non-rooted):
 *   objection patchapk -s tvs_connect.apk
 *   adb install tvs_connect.objection.apk
 *   frida -U -f com.tvsm.connect -l ble_hook.js --no-pause
 *
 * What this captures:
 *   [WRITE >>]  — bytes the app sends TO the bike
 *   [NOTIF <<]  — bytes the bike sends TO the app
 *   [CIPHER]    — plaintext before/after AES encryption (if any)
 *   [KEY]       — AES key bytes when loaded
 */

Java.perform(function () {

    // ─── BLE Write (Phone → Bike) ───────────────────────────────────────────
    var Gatt = Java.use('android.bluetooth.BluetoothGatt');

    // Android < 13
    Gatt.writeCharacteristic
        .overload('android.bluetooth.BluetoothGattCharacteristic')
        .implementation = function (characteristic) {
            console.log('[WRITE >>] uuid=' + characteristic.getUuid()
                + '  val=' + bytesToHex(characteristic.getValue()));
            return this.writeCharacteristic(characteristic);
        };

    // Android 13+ overload
    try {
        Gatt.writeCharacteristic
            .overload('android.bluetooth.BluetoothGattCharacteristic', '[B', 'int')
            .implementation = function (characteristic, value, writeType) {
                console.log('[WRITE13>>] uuid=' + characteristic.getUuid()
                    + '  val=' + bytesToHex(value));
                return this.writeCharacteristic(characteristic, value, writeType);
            };
    } catch (e) { /* not available on this Android version */ }


    // ─── BLE Notifications (Bike → Phone) ───────────────────────────────────
    var GattCallback = Java.use('android.bluetooth.BluetoothGattCallback');

    // Android < 13
    GattCallback.onCharacteristicChanged
        .overload('android.bluetooth.BluetoothGatt',
                  'android.bluetooth.BluetoothGattCharacteristic')
        .implementation = function (gatt, characteristic) {
            console.log('[NOTIF <<] uuid=' + characteristic.getUuid()
                + '  val=' + bytesToHex(characteristic.getValue()));
            return this.onCharacteristicChanged(gatt, characteristic);
        };

    // Android 13+ overload
    try {
        GattCallback.onCharacteristicChanged
            .overload('android.bluetooth.BluetoothGatt',
                      'android.bluetooth.BluetoothGattCharacteristic', '[B')
            .implementation = function (gatt, characteristic, value) {
                console.log('[NOTIF13<<] uuid=' + characteristic.getUuid()
                    + '  val=' + bytesToHex(value));
                return this.onCharacteristicChanged(gatt, characteristic, value);
            };
    } catch (e) { /* not available on this Android version */ }


    // ─── AES Encryption (reveals plaintext if encrypted) ────────────────────
    var Cipher = Java.use('javax.crypto.Cipher');

    Cipher.doFinal.overload('[B').implementation = function (input) {
        var output = this.doFinal(input);
        console.log('[CIPHER] algo=' + this.getAlgorithm()
            + '\n         IN:  ' + bytesToHex(input)
            + '\n         OUT: ' + bytesToHex(output));
        return output;
    };

    // Also hook the no-arg doFinal (operates on internal buffer)
    Cipher.doFinal.overload().implementation = function () {
        var output = this.doFinal();
        console.log('[CIPHER-noarg] algo=' + this.getAlgorithm()
            + '  OUT: ' + bytesToHex(output));
        return output;
    };


    // ─── Key Loading (reveals AES key bytes) ────────────────────────────────
    var SecretKeySpec = Java.use('javax.crypto.spec.SecretKeySpec');

    SecretKeySpec.$init.overload('[B', 'java.lang.String')
        .implementation = function (keyBytes, algorithm) {
            console.log('[KEY LOADED] algo=' + algorithm
                + '  key=' + bytesToHex(keyBytes));
            return this.$init(keyBytes, algorithm);
        };

    SecretKeySpec.$init.overload('[B', 'int', 'int', 'java.lang.String')
        .implementation = function (keyBytes, offset, len, algorithm) {
            console.log('[KEY LOADED (slice)] algo=' + algorithm
                + '  key=' + bytesToHex(keyBytes));
            return this.$init(keyBytes, offset, len, algorithm);
        };


    // ─── Connection Events ───────────────────────────────────────────────────
    GattCallback.onConnectionStateChange
        .overload('android.bluetooth.BluetoothGatt', 'int', 'int')
        .implementation = function (gatt, status, newState) {
            var stateStr = newState === 2 ? 'CONNECTED' : newState === 0 ? 'DISCONNECTED' : newState;
            console.log('[CONN] status=' + status + '  newState=' + stateStr
                + '  device=' + gatt.getDevice().getAddress());
            return this.onConnectionStateChange(gatt, status, newState);
        };


    // ─── Utility ─────────────────────────────────────────────────────────────
    function bytesToHex(bytes) {
        if (!bytes) return 'null';
        var hex = '';
        for (var i = 0; i < bytes.length; i++) {
            var b = bytes[i] & 0xFF;
            hex += (b < 16 ? '0' : '') + b.toString(16).toUpperCase();
            if (i < bytes.length - 1) hex += ' ';
        }
        return hex;
    }

    console.log('[OpenTorq] BLE hook loaded. Waiting for TVS Connect to connect to NTorq...');
});
