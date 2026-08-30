package local.me90.controller;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** USB transport and SysEx framing for the connected processor. */
public final class UsbMidiClient implements AutoCloseable {
    public interface Listener {
        void onConnected(UsbDevice device);
        void onError(String message);
        void onSysEx(byte[] message);
    }

    private static final String USB_PERMISSION = "local.me90.controller.USB_PERMISSION";
    private final Context appContext;
    private final UsbManager usbManager;
    private final Listener listener;
    private final BroadcastReceiver permissionReceiver;
    private final ByteArrayOutputStream sysExBuffer = new ByteArrayOutputStream();

    private volatile UsbDeviceConnection connection;
    private volatile UsbInterface claimedInterface;
    private volatile UsbInterface claimedInputInterface;
    private volatile UsbEndpoint outEndpoint;
    private volatile UsbEndpoint inEndpoint;
    private volatile boolean receiving;
    private volatile UsbDevice pendingPermissionDevice;
    private volatile int deviceId = 0x10;

    public UsbMidiClient(Context context, Listener listener) {
        appContext = context.getApplicationContext();
        usbManager = (UsbManager) appContext.getSystemService(Context.USB_SERVICE);
        this.listener = listener;
        permissionReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!USB_PERMISSION.equals(intent.getAction())) return;
                UsbDevice device = Build.VERSION.SDK_INT >= 33
                        ? intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class)
                        : intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device == null) device = pendingPermissionDevice;
                pendingPermissionDevice = null;
                if (device == null) { listener.onError("Connection request expired"); return; }
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) || usbManager.hasPermission(device)) open(device);
                else listener.onError("Connection permission was not granted");
            }
        };
        IntentFilter filter = new IntentFilter(USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= 33) appContext.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else appContext.registerReceiver(permissionReceiver, filter);
    }

    public List<UsbDevice> listOutputDevices() {
        List<UsbDevice> result = new ArrayList<>();
        for (UsbDevice device : usbManager.getDeviceList().values()) if (findInterface(device) != null) result.add(device);
        return result;
    }

    public void requestConnection(UsbDevice device) {
        if (device == null) { listener.onError("No compatible device found"); return; }
        if (usbManager.hasPermission(device)) { open(device); return; }
        pendingPermissionDevice = device;
        Intent intent = new Intent(USB_PERMISSION);
        PendingIntent pending = PendingIntent.getBroadcast(appContext, device.getDeviceId(), intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, pending);
    }

    private void open(UsbDevice device) {
        closeConnection();
        UsbInterface usbInterface = findInterface(device);
        if (usbInterface == null) { listener.onError("Selected device cannot be used"); return; }
        UsbEndpoint output = findEndpoint(usbInterface, UsbConstants.USB_DIR_OUT);
        UsbInterface inputInterface = usbInterface;
        UsbEndpoint input = findEndpoint(inputInterface, UsbConstants.USB_DIR_IN);
        if (input == null) {
            inputInterface = findInputInterface(device);
            input = inputInterface == null ? null : findEndpoint(inputInterface, UsbConstants.USB_DIR_IN);
        }
        UsbDeviceConnection opened = usbManager.openDevice(device);
        if (opened == null || !opened.claimInterface(usbInterface, true)
                || (inputInterface != usbInterface && !opened.claimInterface(inputInterface, true))) {
            if (opened != null) opened.close();
            listener.onError("Could not open the selected device");
            return;
        }
        connection = opened;
        claimedInterface = usbInterface;
        claimedInputInterface = inputInterface;
        outEndpoint = output;
        inEndpoint = input;
        startReceiver();
        listener.onConnected(device);
    }

    private static UsbInterface findInterface(UsbDevice device) {
        UsbInterface fallback = null;
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface candidate = device.getInterface(i);
            if (findEndpoint(candidate, UsbConstants.USB_DIR_OUT) == null) continue;
            if (candidate.getInterfaceClass() == UsbConstants.USB_CLASS_AUDIO && candidate.getInterfaceSubclass() == 3) return candidate;
            if (fallback == null) fallback = candidate;
        }
        return fallback;
    }

    /** Some Android USB stacks expose MIDI IN and OUT on different streaming interfaces. */
    private static UsbInterface findInputInterface(UsbDevice device) {
        UsbInterface fallback = null;
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface candidate = device.getInterface(i);
            if (findEndpoint(candidate, UsbConstants.USB_DIR_IN) == null) continue;
            if (candidate.getInterfaceClass() == UsbConstants.USB_CLASS_AUDIO && candidate.getInterfaceSubclass() == 3) return candidate;
            if (fallback == null) fallback = candidate;
        }
        return fallback;
    }

    private static UsbEndpoint findEndpoint(UsbInterface usbInterface, int direction) {
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint endpoint = usbInterface.getEndpoint(i);
            int type = endpoint.getType();
            if (endpoint.getDirection() == direction && (type == UsbConstants.USB_ENDPOINT_XFER_BULK || type == UsbConstants.USB_ENDPOINT_XFER_INT)) return endpoint;
        }
        return null;
    }

    private void startReceiver() {
        if (inEndpoint == null) return;
        receiving = true;
        new Thread(() -> {
            byte[] buffer = new byte[Math.max(64, inEndpoint.getMaxPacketSize() * 8)];
            while (receiving) {
                UsbDeviceConnection active = connection;
                UsbEndpoint input = inEndpoint;
                if (active == null || input == null) return;
                int read = active.bulkTransfer(input, buffer, buffer.length, 250);
                if (read > 0) consumeUsbPackets(buffer, read);
            }
        }, "Stage90-receiver").start();
    }

    private synchronized void consumeUsbPackets(byte[] buffer, int length) {
        for (int offset = 0; offset + 3 < length; offset += 4) {
            int cin = buffer[offset] & 0x0F;
            if (cin == 0) continue;
            int count = cin == 0x05 ? 1 : cin == 0x06 ? 2 : 3;
            for (int i = 0; i < count; i++) consumeByte(buffer[offset + 1 + i]);
        }
    }

    private void consumeByte(byte value) {
        int unsigned = value & 0xFF;
        if (unsigned == 0xF0) { sysExBuffer.reset(); sysExBuffer.write(unsigned); return; }
        if (sysExBuffer.size() == 0) return;
        sysExBuffer.write(unsigned);
        if (unsigned == 0xF7) {
            byte[] complete = sysExBuffer.toByteArray();
            sysExBuffer.reset();
            listener.onSysEx(complete);
        }
    }

    public boolean isConnected() { return connection != null && outEndpoint != null; }
    public void setDeviceId(int value) { if (value >= 0 && value <= 0x7F) deviceId = value; }
    public void requestIdentity() { sendSysEx(new byte[] { (byte) 0xF0, 0x7E, 0x7F, 0x06, 0x01, (byte) 0xF7 }); }
    public void sendParameter(int a, int b, int c, int d, int value) { sendDt1(a, b, c, d, new int[] { value }); }

    public void sendDt1(int a, int b, int c, int d, int[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("Missing value");
        int[] checked = new int[4 + data.length];
        checked[0] = checked7(a); checked[1] = checked7(b); checked[2] = checked7(c); checked[3] = checked7(d);
        byte[] message = new byte[13 + data.length];
        message[0] = (byte) 0xF0; message[1] = 0x41; message[2] = (byte) deviceId; message[3] = 0x01;
        message[4] = 0x05; message[5] = 0x03; message[6] = 0x12;
        message[7] = (byte) a; message[8] = (byte) b; message[9] = (byte) c; message[10] = (byte) d;
        for (int i = 0; i < data.length; i++) { checked[4 + i] = checked7(data[i]); message[11 + i] = (byte) data[i]; }
        message[message.length - 2] = (byte) rolandChecksum(checked);
        message[message.length - 1] = (byte) 0xF7;
        sendSysEx(message);
    }

    public void requestRange(int a, int b, int c, int d, int size) {
        if (size < 1 || size > 0x0FFFFF) throw new IllegalArgumentException("Invalid request size");
        int s1 = (size >> 21) & 0x7F, s2 = (size >> 14) & 0x7F, s3 = (size >> 7) & 0x7F, s4 = size & 0x7F;
        int checksum = rolandChecksum(checked7(a), checked7(b), checked7(c), checked7(d), s1, s2, s3, s4);
        sendSysEx(new byte[] { (byte) 0xF0, 0x41, (byte) deviceId, 0x01, 0x05, 0x03, 0x11,
                (byte) a, (byte) b, (byte) c, (byte) d, (byte) s1, (byte) s2, (byte) s3, (byte) s4,
                (byte) checksum, (byte) 0xF7 });
    }

    private synchronized void sendSysEx(byte[] data) {
        if (!isConnected()) throw new IllegalStateException("Not connected");
        byte[] packets = toUsbMidiPackets(data);
        int written = connection.bulkTransfer(outEndpoint, packets, packets.length, 1000);
        if (written != packets.length) throw new IllegalStateException("Transfer did not complete");
    }

    static int rolandChecksum(int... values) { int sum = 0; for (int value : values) sum += value & 0x7F; return (128 - sum % 128) % 128; }
    static byte[] toUsbMidiPackets(byte[] message) {
        if (message.length < 2 || message[0] != (byte) 0xF0 || message[message.length - 1] != (byte) 0xF7) throw new IllegalArgumentException("Invalid message");
        byte[] packets = new byte[((message.length + 2) / 3) * 4];
        int from = 0, to = 0;
        while (from < message.length) {
            int count = Math.min(3, message.length - from);
            boolean last = from + count == message.length;
            packets[to++] = (byte) (!last ? 0x04 : count == 3 ? 0x07 : count == 2 ? 0x06 : 0x05);
            packets[to++] = message[from++]; packets[to++] = count > 1 ? message[from++] : 0; packets[to++] = count > 2 ? message[from++] : 0;
        }
        return packets;
    }
    private static int checked7(int value) { if (value < 0 || value > 127) throw new IllegalArgumentException("Value outside supported range"); return value; }

    private void closeConnection() {
        receiving = false;
        UsbDeviceConnection active = connection; UsbInterface usbInterface = claimedInterface; UsbInterface inputInterface = claimedInputInterface;
        connection = null; claimedInterface = null; claimedInputInterface = null; outEndpoint = null; inEndpoint = null;
        if (active != null && usbInterface != null) active.releaseInterface(usbInterface);
        if (active != null && inputInterface != null && inputInterface != usbInterface) active.releaseInterface(inputInterface);
        if (active != null) active.close();
    }
    @Override public void close() { closeConnection(); try { appContext.unregisterReceiver(permissionReceiver); } catch (IllegalArgumentException ignored) { } }
}
