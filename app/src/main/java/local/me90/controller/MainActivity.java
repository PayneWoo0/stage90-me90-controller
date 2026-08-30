package local.me90.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Offline editor for a directly connected processor. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(17, 12, 31);
    private static final int SURFACE = Color.rgb(37, 24, 61);
    private static final int SURFACE_ALT = Color.rgb(50, 33, 79);
    private static final int PURPLE = Color.rgb(154, 80, 255);
    private static final int GREEN = Color.rgb(157, 255, 65);
    private static final int TEXT = Color.rgb(246, 244, 255);
    private static final int MUTED = Color.rgb(190, 177, 210);
    private static final int OFF = Color.rgb(83, 75, 97);
    private static final int[] BLOCK_COLORS = { Color.rgb(55, 188, 232), Color.rgb(237, 160, 42), Color.rgb(214, 195, 62), Color.rgb(112, 180, 80), Color.rgb(83, 183, 230), Color.rgb(118, 199, 71), Color.rgb(230, 230, 238), Color.rgb(30, 214, 190), Color.rgb(245, 88, 187), Color.rgb(220, 85, 95), Color.rgb(30, 214, 190), Color.rgb(142, 104, 202) };

    private static final String[] COMP_MAIN = { "COMP", "T.WAH UP", "SLOW GEAR", "DEFRETTER", "OCTAVE", "HUMANIZER", "FEEDBACKER", "AC SIM", "SOLO", "TUNE DOWN", "SELECTABLE" };
    private static final String[] COMP_ALT = { "S-BEND", "D-COMP", "RING MOD", "POLY OCTAVE", "T.WAH DOWN", "Single > Hum", "Hum > Single" };
    private static final String[] OD_MAIN = { "BOOST", "OVERDRIVE", "T-SCREAM", "CENTA OD", "BLUES OD", "DISTORTION", "TURBO DS", "RAT DS", "METAL DS", "CORE", "SELECTABLE" };
    private static final String[] OD_ALT = { "MUFF FUZZ", "CLEAN BOOST", "TREBLE BOOST", "OD-1", "TURBO OD", "GUV DS", "'60S FUZZ", "OCT FUZZ" };
    private static final String[] PRE_MAIN = { "NATURAL", "X-CRUNCH", "X-HI GAIN", "MAXIMUM", "JUGGERNAUT", "X-MODDED", "TWIN COMBO", "TWEED COMBO", "DIAMOND", "BRIT STACK", "SELECTABLE" };
    private static final String[] PRE_ALT = { "RECTI STACK", "TRANSPARENT", "BOUTIQUE", "SUPREME", "JC-120", "DELUXE COMBO" };
    private static final String[] MOD_MAIN = { "PHASER", "FLANGER", "TREMOLO", "CHORUS", "VIBRATO", "PITCH SHIFT", "HARMONIST", "ROTARY", "UNI-V", "DELAY", "SELECTABLE" };
    private static final String[] MOD_ALT = { "OVERTONE", "SCRIPT PHASER", "STEREO CHORUS", "CE-1 CHORUS", "AUTO WAH" };
    private static final String[] FX_MAIN = { "PHASER", "TREM/PAN", "BOOST", "DELAY", "CHORUS", "SELECTABLE" };
    private static final String[] FX_ALT = { "EQ", "FLANGER", "VIBRATO", "PITCH SHIFT", "HARMONIST", "ROTARY", "UNI-V", "OVERTONE" };
    private static final String[] DLY_MAIN = { "STANDARD", "ANALOG", "TAPE", "WARM", "MODULATE", "REVERSE", "TEMPO", "TERA ECHO", "SHIMMER", "PHRASE LOOP", "SELECTABLE" };
    private static final String[] DLY_ALT = { "+REVERB", "CHO+DELAY", "WARP", "TWIST", "GLITCH" };
    private static final String[] REV_TYPES = { "ROOM", "HALL", "SPRING" };
    private static final String[] PEDAL_TYPES = { "WAH", "VOICE", "+1 OCT", "+2 OCT", "-1 OCT", "-2 OCT", "FREEZE", "OSC DELAY", "OD/DS", "MOD RATE", "DELAY LEVEL" };
    private static final String[] CAB_TYPES = { "ORIGINAL", "IR-1", "IR-2", "IR-3" };
    private static final String[] OUTPUT_TYPES = { "TUBE COMBO 212 INPUT", "TUBE COMBO 212 RETURN", "TUBE COMBO 112 INPUT", "TUBE COMBO 112 RETURN", "TUBE STACK 412 INPUT", "TUBE STACK 412 RETURN", "JC-120 INPUT", "JC-120 RETURN", "KATANA-100/212 INPUT", "KATANA-100/212 RETURN", "KATANA-100 INPUT", "KATANA-100 RETURN" };

    private final State state = new State();
    private final List<Button> chainButtons = new ArrayList<>();
    private UsbMidiClient midi;
    private LinearLayout chainRow, editorPanel;
    private TextView status, patchTitle, selectedTitle, tunerNote, tunerPitch, livePatchTitle, livePatchName;
    private TunerMeter tunerMeter;
    private Block selectedBlock;
    private boolean chinese = true, building, refreshQueued, liveMode, sliderTracking;
    private int connectionPhase, pendingPatchSync = -1;
    private PatchDial bankDial, slotDial;

    private static final Block[] BLOCKS = {
            new Block("COMP", "COMP / FX1", 0x20, 0x00, 0x10, true, COMP_MAIN, COMP_ALT, 3, 3),
            new Block("DRIVE", "OD", 0x20, 0x00, 0x20, true, OD_MAIN, OD_ALT, 3, 3),
            new Block("AMP", "AMP", 0x20, 0x00, 0x30, true, PRE_MAIN, PRE_ALT, 5, 3),
            new Block("CAB", "IR", 0x20, 0x00, 0x30, false, null, null, 0, 0),
            new Block("MOD", "MOD", 0x20, 0x00, 0x40, true, MOD_MAIN, MOD_ALT, 3, 5),
            new Block("FX2", "EQ / FX2", 0x20, 0x00, 0x50, true, FX_MAIN, FX_ALT, 4, 3),
            new Block("DLY", "DLY", 0x20, 0x00, 0x60, true, DLY_MAIN, DLY_ALT, 3, 5),
            new Block("REV", "REV", 0x20, 0x00, 0x70, true, REV_TYPES, null, 1, 2),
            new Block("PEDAL", "PEDAL", 0x20, 0x01, 0x00, true, PEDAL_TYPES, null, 0, 0),
            new Block("NS", "NS", 0x20, 0x01, 0x10, false, null, null, 1, 0),
            new Block("S/R", "S/R", 0x20, 0x01, 0x30, true, null, null, 0, 0),
            new Block("AIRD", "AIRD OUTPUT", 0x10, 0x00, 0x00, false, null, null, 0, 0)
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Arrays.fill(state.enabled, true);
        midi = new UsbMidiClient(this, new UsbMidiClient.Listener() {
            @Override public void onConnected(UsbDevice device) { runOnUiThread(() -> { setStatus(t("已连接，正在验证", "Connected · preparing"), false); connectionPhase = 1; try { midi.requestIdentity(); } catch (RuntimeException ignored) { beginHandshake(); } status.postDelayed(() -> { if (connectionPhase == 1) beginHandshake(); }, 600); }); }
            @Override public void onError(String message) { runOnUiThread(() -> setStatus(t("连接失败", "Connection failed"), true)); }
            @Override public void onSysEx(byte[] message) { handleMessage(message); }
        });
        setContentView(buildEditorView());
    }

    @Override protected void onDestroy() { midi.close(); super.onDestroy(); }

    private View buildEditorView() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        liveMode = false;
        chainButtons.clear();
        LinearLayout root = column(); root.setBackgroundColor(BG);
        TextView brand = text("STAGE 90", 23, GREEN, true); brand.setPadding(dp(14), dp(12), dp(14), dp(3));
        root.addView(brand, match());
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); header.setPadding(dp(14), dp(4), dp(14), dp(7));
        Button live = button("LIVE", false); live.setOnClickListener(v -> setContentView(buildLiveView())); header.addView(live);
        Button tuner = button(t("调音", "Tune"), true); tuner.setOnClickListener(v -> showTuner()); header.addView(tuner);
        Button connect = button(midi.isConnected() ? t("已连接", "Connected") : t("连接", "Connect"), true); connect.setOnClickListener(v -> connect()); header.addView(connect);
        Button settings = button("⚙", true); settings.setContentDescription(t("设置", "Settings")); settings.setOnClickListener(v -> showSettings()); header.addView(settings);
        root.addView(header, match());

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout page = column(); page.setPadding(dp(14), 0, dp(14), dp(24)); scroll.addView(page); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        status = text(midi.isConnected() ? t("已连接", "Connected") : t("等待连接", "Ready"), 13, MUTED, false); status.setPadding(dp(12), dp(8), dp(12), dp(8)); status.setBackground(background(SURFACE_ALT, 12)); page.addView(status, match());

        LinearLayout patch = new LinearLayout(this); patch.setGravity(Gravity.CENTER_VERTICAL); patch.setPadding(dp(12), dp(9), dp(12), dp(9)); patch.setBackground(background(SURFACE, 12));
        patchTitle = text(patchText(), 14, TEXT, true); patch.addView(patchTitle, new LinearLayout.LayoutParams(0, -2, 1));
        Button choose = button(t("音色", "Patch"), true); choose.setOnClickListener(v -> showPatchPicker()); patch.addView(choose);
        Button write = button(t("写入", "Write"), false); write.setOnClickListener(v -> showWriteDialog()); patch.addView(write); page.addView(patch, top(12));

        TextView chainLabel = text(t("效果链路", "EFFECT CHAIN"), 12, MUTED, true); chainLabel.setPadding(0, dp(18), 0, dp(7)); page.addView(chainLabel);
        HorizontalScrollView chainScroll = new HorizontalScrollView(this); chainScroll.setHorizontalScrollBarEnabled(false); chainRow = new LinearLayout(this); chainRow.setGravity(Gravity.CENTER_VERTICAL); chainScroll.addView(chainRow); page.addView(chainScroll, match());
        selectedTitle = text("", 18, TEXT, true); selectedTitle.setPadding(0, dp(22), 0, dp(6)); page.addView(selectedTitle);
        editorPanel = column(); editorPanel.setPadding(dp(14), dp(12), dp(14), dp(14)); editorPanel.setBackground(background(SURFACE, 14)); page.addView(editorPanel, match());
        if (selectedBlock == null) selectedBlock = BLOCKS[0];
        renderEditor();
        return root;
    }

    private View buildLiveView() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        liveMode = true;
        LinearLayout root = column(); root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(22), dp(12), dp(22), dp(12)); root.setBackgroundColor(BG);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("LIVE", 25, GREEN, true); top.addView(title, new LinearLayout.LayoutParams(dp(82), -2));
        LinearLayout liveStatus = column(); liveStatus.setGravity(Gravity.CENTER);
        TextView now = text(livePatchText(), 28, TEXT, true); now.setGravity(Gravity.CENTER); liveStatus.addView(now, match());
        TextView liveName = text(patchNameOrFallback(), 15, MUTED, true); liveName.setGravity(Gravity.CENTER); liveStatus.addView(liveName, match());
        livePatchTitle = now; livePatchName = liveName;
        top.addView(liveStatus, new LinearLayout.LayoutParams(0, -2, 1));
        Button exit = button("×", false); exit.setTextSize(25); exit.setOnClickListener(v -> exitLive()); top.addView(exit, new LinearLayout.LayoutParams(dp(60), dp(48))); root.addView(top, match());
        LinearLayout dials = new LinearLayout(this); dials.setGravity(Gravity.CENTER); dials.setOrientation(LinearLayout.HORIZONTAL);
        int userPatch = userPatchIndex();
        bankDial = new PatchDial(this, 9, userPatch / 4, PURPLE, value -> { int current = userPatchIndex(); selectPatch(value * 4 + current % 4); updateLiveLabels(); });
        slotDial = new PatchDial(this, 4, userPatch % 4, GREEN, value -> { int current = userPatchIndex(); selectPatch(current / 4 * 4 + value); updateLiveLabels(); });
        dials.addView(bankDial, new LinearLayout.LayoutParams(0, -1, 1)); dials.addView(slotDial, new LinearLayout.LayoutParams(0, -1, 1)); root.addView(dials, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }

    private void renderEditor() {
        if (liveMode || chainRow == null) return;
        chainRow.removeAllViews(); chainButtons.clear();
        List<Block> ordered = new ArrayList<>(Arrays.asList(BLOCKS));
        for (int i = 0; i < ordered.size(); i++) {
            Block block = ordered.get(i); Button item = button("", true); item.setTextSize(12); item.setMinWidth(dp(64));
            item.setOnClickListener(v -> { selectedBlock = block; buildPanel(); renderEditor(); }); item.setOnLongClickListener(v -> { toggle(block); return true; });
            FrameLayout ring = new FrameLayout(this); ring.setPadding(dp(2), dp(2), dp(2), dp(2)); ring.setBackground(background(block == selectedBlock ? lighten(BLOCK_COLORS[indexOf(block)]) : Color.TRANSPARENT, 12));
            ring.addView(item, new FrameLayout.LayoutParams(-2, dp(42))); chainButtons.add(item); chainRow.addView(ring, new LinearLayout.LayoutParams(-2, dp(46)));
            styleChain(item, block, block == selectedBlock);
            if (i + 1 < ordered.size()) { View line = new View(this); line.setBackgroundColor(Color.rgb(112, 92, 154)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(14), dp(2)); p.gravity = Gravity.CENTER_VERTICAL; chainRow.addView(line, p); }
        }
        buildPanel();
    }

    private void styleChain(Button item, Block block, boolean selected) {
        int i = indexOf(block); boolean on = !block.switchable || state.enabled[i]; int color = on ? BLOCK_COLORS[i] : OFF;
        item.setBackground(background(color, 10)); item.setTextColor(on ? textOn(color) : Color.rgb(205, 199, 212)); item.setText((on ? "● " : "○ ") + block.shortName);
    }

    private void buildPanel() {
        if (editorPanel == null || selectedBlock == null) return;
        building = true; editorPanel.removeAllViews(); selectedTitle.setText(selectedBlock.shortName);
        Block block = selectedBlock; int idx = indexOf(block);
        if (block.id.equals("CAB")) addSpinner(t("箱体 / IR", "SPEAKER / IR"), CAB_TYPES, state.cab, value -> { state.cab = value; send(block, 8, value); });
        else if (block.id.equals("AIRD")) addSpinner("AIRD OUTPUT", OUTPUT_TYPES, state.output, value -> { state.output = value; sendSystem(12, value); });
        else {
            if (block.switchable) addSwitch(block);
            if (block.main != null) addTypeSelectors(block);
            if (block.id.equals("S/R")) addSpinner(t("位置", "POSITION"), new String[] { t("前级前", "Before preamp"), t("前级后", "After preamp") }, state.sendReturnPost, value -> { state.sendReturnPost = value; sendSystem(8, value); renderEditor(); });
            addParameters(block);
        }
        building = false;
    }

    private void addTypeSelectors(Block block) {
        int idx = indexOf(block); addSpinner(t("类型", "TYPE"), block.main, state.type[idx], value -> {
            state.type[idx] = value; state.subtype[idx] = 0; send(block, 1, value + 1); if (block.alt != null && value == block.main.length - 1) send(block, 2, 0); renderEditor();
        });
        if (block.alt != null && state.type[idx] == block.main.length - 1) addSpinner(t("可选效果", "SELECTABLE"), block.alt, state.subtype[idx], value -> { state.subtype[idx] = value; send(block, 2, value); renderEditor(); });
    }

    private void addSwitch(Block block) {
        int idx = indexOf(block); LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(t("开关", "ON / OFF"), 13, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        Switch control = new Switch(this); control.setChecked(state.enabled[idx]); control.setTextColor(GREEN); control.setOnCheckedChangeListener((v, checked) -> { if (!building) { state.enabled[idx] = checked; send(block, 0, checked ? 1 : 0); renderEditor(); } }); row.addView(control); editorPanel.addView(row, match());
    }

    private void addParameters(Block block) {
        int idx = indexOf(block); int count = block.parameterCount;
        for (int i = 0; i < count; i++) {
            int offset = block.firstParam + i; int max = block.id.equals("REV") ? 49 : block.id.equals("NS") ? 50 : 99;
            String label = parameterName(block, i); int current = state.parameter[idx][offset];
            LinearLayout title = new LinearLayout(this); title.setPadding(0, dp(12), 0, 0); TextView value = text(String.valueOf(current), 13, GREEN, true); value.setGravity(Gravity.END);
            title.addView(text(label, 12, MUTED, true), new LinearLayout.LayoutParams(0, -2, 1)); title.addView(value, new LinearLayout.LayoutParams(dp(50), -2)); editorPanel.addView(title, match());
            SeekBar slider = new SeekBar(this); slider.setMax(max); slider.setProgress(Math.min(max, current)); slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seek, int progress, boolean user) { value.setText(String.valueOf(progress)); if (user) { state.parameter[idx][offset] = progress; send(block, offset, progress); } }
                @Override public void onStartTrackingTouch(SeekBar seek) { sliderTracking = true; }
                @Override public void onStopTrackingTouch(SeekBar seek) { sliderTracking = false; queueRefresh(); }
            }); editorPanel.addView(slider, match());
        }
    }

    private String parameterName(Block block, int parameter) {
        return chinese ? "参数" + (parameter + 1) : "PARAM " + (parameter + 1);
    }

    private void connect() {
        List<UsbDevice> devices = midi.listOutputDevices();
        if (devices.isEmpty()) { setStatus(t("未找到设备", "No device found"), true); return; }
        String[] choices = new String[devices.size()]; for (int i = 0; i < choices.length; i++) choices[i] = t("设备 ", "Device ") + (i + 1);
        new AlertDialog.Builder(this).setTitle(t("选择设备", "Select device")).setItems(choices, (d, which) -> { setStatus(t("正在连接", "Connecting"), false); midi.requestConnection(devices.get(which)); }).show();
    }

    private void requestFullSync() {
        new Thread(() -> {
            int[][] reads = { { 0x10,0,0,0,13 }, { 0x20,0,0,0,16 }, { 0x20,0,0x10,0,6 }, { 0x20,0,0x20,0,6 }, { 0x20,0,0x30,0,9 }, { 0x20,0,0x40,0,8 }, { 0x20,0,0x50,0,7 }, { 0x20,0,0x60,0,8 }, { 0x20,0,0x70,0,3 }, { 0x20,1,0,0,2 }, { 0x20,1,0x10,0,1 }, { 0x20,1,0x30,0,1 }, { 0,1,0,0,2 }, { 0x7F,0,2,6,1 } };
            for (int[] read : reads) { try { midi.requestRange(read[0], read[1], read[2], read[3], read[4]); Thread.sleep(22); } catch (Exception ignored) { } }
            runOnUiThread(() -> { setStatus(t("同步完成", "Synced"), false); queueRefresh(); });
        }, "Stage90-sync").start();
    }

    private void handleMessage(byte[] message) {
        if (message.length >= 6 && (message[0] & 255) == 0xF0 && (message[1] & 255) == 0x7E && (message[3] & 255) == 0x06 && (message[4] & 255) == 0x02) {
            midi.setDeviceId(message[2] & 127); if (connectionPhase == 1) beginHandshake(); return;
        }
        if (message.length < 14 || (message[0] & 255) != 0xF0 || (message[1] & 255) != 0x41 || (message[6] & 255) != 0x12) return;
        int a = message[7] & 127, b = message[8] & 127, c = message[9] & 127, d = message[10] & 127;
        int end = message.length - 2; if ((message[message.length - 1] & 255) != 0xF7) return;
        int checksum = 0; for (int i = 7; i < message.length - 1; i++) checksum += message[i] & 127; if (checksum % 128 != 0) return;
        int[] data = new int[Math.max(0, end - 11)]; for (int i = 0; i < data.length; i++) data[i] = message[11 + i] & 127;
        if (advanceHandshake(a, b, c, d, data)) return;
        applyInbound(a, b, c, d, data); queueRefresh();
    }

    private void beginHandshake() {
        if (connectionPhase > 2) return;
        connectionPhase = 2;
        status.postDelayed(() -> { if (connectionPhase < 5) beginFallbackSync(); }, 1800);
        new Thread(() -> { try { midi.requestRange(0x7F, 0, 0, 0, 1); } catch (RuntimeException ignored) { beginFallbackSync(); } }, "Stage90-handshake").start();
    }

    private boolean advanceHandshake(int a, int b, int c, int d, int[] data) {
        if (a != 0x7F || b != 0 || c != 0 || data.length == 0) return false;
        if (d == 0 && connectionPhase == 2) { connectionPhase = 3; sendHandshake(1, 0); return true; }
        if (d == 1 && connectionPhase == 3 && data[0] == 0) { connectionPhase = 4; sendHandshake(1, 1); return true; }
        if (d == 3 && connectionPhase == 4) { connectionPhase = 5; requestFullSync(); return true; }
        return false;
    }

    private void sendHandshake(int offset, int value) {
        new Thread(() -> { try { midi.sendParameter(0x7F, 0, 0, offset, value); Thread.sleep(45); midi.requestRange(0x7F, 0, 0, offset == 1 && value == 1 ? 3 : 1, 1); } catch (Exception ignored) { beginFallbackSync(); } }, "Stage90-handshake-step").start();
    }

    private void beginFallbackSync() { if (connectionPhase < 5) { connectionPhase = 5; requestFullSync(); } }

    private void applyInbound(int a, int b, int c, int d, int[] data) {
        if (a == 0x7F && b == 0 && c == 2 && d == 0 && data.length >= 2) { state.tunerNote = data[0]; state.tunerPitch = data[1]; updateTuner(); return; }
        if (a == 0x7F && b == 0 && c == 2 && d == 6 && data.length >= 1) { state.manualMode = data[0] != 0; return; }
        // The processor uses this 4-bit packed address when a physical footswitch changes memory.
        if (a == 0 && b == 0 && c == 0 && d == 0 && data.length >= 2) { acceptDevicePatch((data[0] << 4) | data[1]); return; }
        // This is the two-7-bit representation returned by the explicit current-patch query.
        if (a == 0 && b == 1 && c == 0 && d == 0 && data.length >= 2) { acceptDevicePatch((data[0] << 7) | data[1]); return; }
        // This command is echoed after an app-initiated change and can also arrive after a device-side change.
        if (a == 0x7F && b == 0 && c == 1 && d == 0 && data.length >= 2) { acceptDevicePatch(data[1]); return; }
        for (int i = 0; i < data.length; i++) applyValue(a, b, c, d + i, data[i]);
    }

    private void applyValue(int a, int b, int c, int offset, int value) {
        if (a == 0x10 && b == 0 && c == 0) { if (offset == 3) state.tunerMute = value; else if (offset == 4) state.tunerPitchRef = value; else if (offset == 8) state.sendReturnPost = value; else if (offset == 12) state.output = value; return; }
        if (a == 0x20 && b == 0 && c == 0 && offset < 16) { state.patchNameBytes[offset] = value; return; }
        if (a == 0x20 && b == 0 && c == 0x30 && offset == 8) { state.cab = value; return; }
        for (Block block : BLOCKS) {
            if (a != block.a || b != block.b || c != block.c) continue;
            int idx = indexOf(block);
            if (block.id.equals("CAB") && offset == 8) { state.cab = value; return; }
            if (offset == 0 && block.switchable) state.enabled[idx] = value != 0;
            else if (offset == 1 && block.main != null) state.type[idx] = clamp(value - 1, 0, block.main.length - 1);
            else if (offset == 2 && block.alt != null) state.subtype[idx] = clamp(value, 0, block.alt.length - 1);
            else if (offset >= 0 && offset < state.parameter[idx].length) state.parameter[idx][offset] = value;
            return;
        }
    }

    private void queueRefresh() {
        runOnUiThread(() -> { if (refreshQueued) return; refreshQueued = true; status.postDelayed(() -> { refreshQueued = false; if (liveMode) { int userPatch = userPatchIndex(); if (bankDial != null) { bankDial.setValue(userPatch / 4); slotDial.setValue(userPatch % 4); } updateLiveLabels(); } else { if (patchTitle != null) patchTitle.setText(patchText()); if (!sliderTracking) renderEditor(); } }, 45); });
    }

    private void toggle(Block block) { int idx = indexOf(block); if (!block.switchable) return; state.enabled[idx] = !state.enabled[idx]; send(block, 0, state.enabled[idx] ? 1 : 0); renderEditor(); }
    private void send(Block block, int offset, int value) { try { midi.sendParameter(block.a, block.b, block.c, offset, value); setStatus(t("已更新", "Updated"), false); } catch (RuntimeException e) { setStatus(t("操作失败", "Operation failed"), true); } }
    private void sendSystem(int offset, int value) { try { midi.sendParameter(0x10, 0, 0, offset, value); setStatus(t("已更新", "Updated"), false); } catch (RuntimeException e) { setStatus(t("操作失败", "Operation failed"), true); } }

    private void showPatchPicker() {
        String[] items = new String[72]; for (int i = 0; i < 36; i++) items[i] = String.format("USER %02d", i + 1); for (int i = 0; i < 36; i++) items[36 + i] = String.format("PRESET %02d", i + 1);
        new AlertDialog.Builder(this).setTitle(t("选择音色", "Select patch")).setItems(items, (d, value) -> selectPatch(value)).show();
    }

    private void selectPatch(int patch) { state.patch = clamp(patch, 0, 71); pendingPatchSync = state.patch; try { midi.sendDt1(0x7F, 0, 1, 0, new int[] { 0, state.patch }); setStatus(t("正在切换", "Changing"), false); status.postDelayed(() -> { if (pendingPatchSync >= 0) { pendingPatchSync = -1; requestFullSync(); } }, 700); } catch (RuntimeException e) { pendingPatchSync = -1; setStatus(t("操作失败", "Operation failed"), true); } queueRefresh(); }
    private void acceptDevicePatch(int value) {
        int patch = clamp(value, 0, 71);
        // A patch command is acknowledged by the requested number. Do not let a late report from
        // the preceding patch complete a new selection or overwrite the Live display.
        if (pendingPatchSync >= 0 && patch != pendingPatchSync) return;
        boolean changed = state.patch != patch; state.patch = patch;
        if (pendingPatchSync >= 0) completePatchChange(); else if (changed) requestFullSync();
    }
    private void completePatchChange() { if (pendingPatchSync >= 0) { pendingPatchSync = -1; requestFullSync(); } }
    private void updateLiveLabels() { if (livePatchTitle != null) livePatchTitle.setText(livePatchText()); if (livePatchName != null) livePatchName.setText(patchNameOrFallback()); }
    private void exitLive() { liveMode = false; livePatchTitle = null; livePatchName = null; bankDial = null; slotDial = null; setContentView(buildEditorView()); status.postDelayed(this::requestFullSync, 100); }
    private void showWriteDialog() {
        LinearLayout body = column(); body.setPadding(dp(20), 0, dp(20), 0); EditText name = new EditText(this); name.setHint(t("音色名称", "Patch name")); name.setText(patchName()); body.addView(name, match());
        Spinner slot = simpleSpinner(userSlots()); body.addView(slot, match()); new AlertDialog.Builder(this).setTitle(t("写入当前音色", "Write current patch")).setView(body).setNegativeButton(t("取消", "Cancel"), null).setPositiveButton(t("写入", "Write"), (d, x) -> { try { midi.sendDt1(0x20, 0, 0, 0, asciiName(name.getText().toString())); midi.sendDt1(0x7F, 0, 1, 4, new int[] { 0, slot.getSelectedItemPosition() }); setStatus(t("已写入", "Written"), false); } catch (RuntimeException e) { setStatus(e.getMessage(), true); } }).show();
    }

    private void showSettings() {
        String[] actions = { t("同步状态", "Sync state"), t("调音器设置", "Tuner settings"), t("语言", "Language"), t("关于", "About") };
        new AlertDialog.Builder(this).setTitle(t("全局设置", "Settings")).setItems(actions, (d, which) -> { if (which == 0) requestFullSync(); else if (which == 1) showTunerSettings(); else if (which == 2) showLanguage(); else showAbout(); }).show();
    }

    private void showAbout() { new AlertDialog.Builder(this).setTitle(t("关于", "About")).setMessage(t("版本号：1.0\n功能说明：用于 ME-90 的本地连接、音色与效果控制。", "Version: 1.0\nFunction: Local patch and effect control for ME-90.")).setPositiveButton("OK", null).show(); }

    private void showLanguage() { new AlertDialog.Builder(this).setTitle(t("语言", "Language")).setSingleChoiceItems(new String[] { "中文", "English" }, chinese ? 0 : 1, (d, value) -> { chinese = value == 0; setContentView(liveMode ? buildLiveView() : buildEditorView()); d.dismiss(); }).show(); }

    private void showTunerSettings() {
        LinearLayout body = column(); body.setPadding(dp(20), 0, dp(20), 0); Spinner output = simpleSpinner(new String[] { "MUTE", "THRU" }); output.setSelection(clamp(state.tunerMute, 0, 1)); Spinner pitch = simpleSpinner(new String[] { "435 Hz", "436 Hz", "437 Hz", "438 Hz", "439 Hz", "440 Hz", "441 Hz", "442 Hz", "443 Hz", "444 Hz", "445 Hz" }); pitch.setSelection(clamp(state.tunerPitchRef, 0, 10)); body.addView(text(t("输出", "OUTPUT"), 12, MUTED, true)); body.addView(output, match()); body.addView(text(t("基准音高", "REFERENCE PITCH"), 12, MUTED, true)); body.addView(pitch, match());
        new AlertDialog.Builder(this).setTitle(t("调音器设置", "Tuner settings")).setView(body).setNegativeButton(t("取消", "Cancel"), null).setPositiveButton(t("应用", "Apply"), (d, x) -> { state.tunerMute = output.getSelectedItemPosition(); state.tunerPitchRef = pitch.getSelectedItemPosition(); sendSystem(3, state.tunerMute); sendSystem(4, state.tunerPitchRef); }).show();
    }

    private void showTuner() {
        LinearLayout body = column(); body.setGravity(Gravity.CENTER_HORIZONTAL); body.setPadding(dp(24), dp(12), dp(24), dp(14));
        tunerNote = text(noteName(state.tunerNote), 52, GREEN, true); tunerNote.setGravity(Gravity.CENTER); body.addView(tunerNote, match());
        tunerMeter = new TunerMeter(this); body.addView(tunerMeter, new LinearLayout.LayoutParams(-1, dp(118)));
        LinearLayout direction = new LinearLayout(this); direction.setGravity(Gravity.CENTER_VERTICAL);
        TextView low = text(t("♭ 偏低", "♭ LOWER"), 12, MUTED, true); direction.addView(low, new LinearLayout.LayoutParams(0, -2, 1));
        tunerPitch = text(tunerText(), 18, TEXT, true); tunerPitch.setGravity(Gravity.CENTER); direction.addView(tunerPitch, new LinearLayout.LayoutParams(dp(100), -2));
        TextView high = text(t("偏高 ♯", "HIGHER ♯"), 12, MUTED, true); high.setGravity(Gravity.RIGHT); direction.addView(high, new LinearLayout.LayoutParams(0, -2, 1)); body.addView(direction, match());
        tunerMeter.setPitch(state.tunerPitch);
        try { midi.sendParameter(0x7F, 0, 0, 2, 2); } catch (RuntimeException e) { setStatus(e.getMessage(), true); }
        new AlertDialog.Builder(this).setTitle(t("调音器", "Tuner")).setView(body).setNegativeButton(t("关闭", "Close"), (d, x) -> { try { midi.sendParameter(0x7F, 0, 0, 2, 1); } catch (RuntimeException ignored) { } tunerNote = null; tunerPitch = null; tunerMeter = null; }).show();
    }

    private void updateTuner() { runOnUiThread(() -> { if (tunerNote != null) tunerNote.setText(noteName(state.tunerNote)); if (tunerPitch != null) tunerPitch.setText(tunerText()); if (tunerMeter != null) tunerMeter.setPitch(state.tunerPitch); }); }
    private String tunerText() { int delta = state.tunerPitch - 50; return delta == 0 ? t("音准准确", "IN TUNE") : delta < 0 ? t("偏低 ", "LOWER ") + Math.abs(delta) : t("偏高 ", "HIGHER ") + Math.abs(delta); }
    private static String noteName(int n) { String[] names = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" }; return n == 12 ? "–" : names[Math.abs(n) % 12]; }

    private int indexOf(Block block) { for (int i = 0; i < BLOCKS.length; i++) if (BLOCKS[i] == block) return i; return 0; }
    private String patchText() { return (state.patch < 36 ? "USER " : "PRESET ") + String.format("%02d", state.patch % 36 + 1) + (patchName().isEmpty() ? "" : " · " + patchName()); }
    private int userPatchIndex() { return state.patch >= 0 && state.patch < 36 ? state.patch : 0; }
    private String livePatchText() { int userPatch = userPatchIndex(); return "USER " + (userPatch / 4 + 1) + "-" + (userPatch % 4 + 1); }
    private String patchName() { return new String(toBytes(state.patchNameBytes), StandardCharsets.US_ASCII).trim(); }
    private String patchNameOrFallback() { String name = patchName(); return name.isEmpty() ? "—" : name; }
    private static byte[] toBytes(int[] values) { byte[] r = new byte[values.length]; for (int i = 0; i < values.length; i++) r[i] = (byte) values[i]; return r; }
    private static int[] asciiName(String name) { byte[] raw = name.getBytes(StandardCharsets.US_ASCII); int[] result = new int[16]; Arrays.fill(result, 0x20); for (int i = 0; i < Math.min(16, raw.length); i++) result[i] = raw[i] & 127; return result; }
    private String[] userSlots() { String[] slots = new String[36]; for (int i = 0; i < 36; i++) slots[i] = String.format("USER %02d", i + 1); return slots; }

    private Spinner addSpinner(String label, String[] options, int selection, Choice listener) { TextView title = text(label, 12, MUTED, true); title.setPadding(0, dp(12), 0, dp(3)); editorPanel.addView(title, match()); Spinner spinner = simpleSpinner(options); spinner.setSelection(clamp(selection, 0, options.length - 1), false); spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() { @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int value, long id) { if (!building) listener.pick(value); } @Override public void onNothingSelected(android.widget.AdapterView<?> p) { } }); editorPanel.addView(spinner, match()); return spinner; }
    private Spinner simpleSpinner(String[] options) { Spinner spinner = new Spinner(this); ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) { @Override public View getView(int p, View v, ViewGroup parent) { return style(super.getView(p, v, parent)); } @Override public View getDropDownView(int p, View v, ViewGroup parent) { return style(super.getDropDownView(p, v, parent)); } }; adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter); spinner.setBackground(background(SURFACE_ALT, 8)); return spinner; }
    private View style(View view) { if (view instanceof TextView) { ((TextView) view).setTextColor(TEXT); ((TextView) view).setTextSize(14); ((TextView) view).setPadding(dp(12), dp(8), dp(12), dp(8)); } return view; }
    private LinearLayout column() { LinearLayout result = new LinearLayout(this); result.setOrientation(LinearLayout.VERTICAL); return result; }
    private TextView text(String value, int size, int color, boolean strong) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); if (strong) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return view; }
    private Button button(String value, boolean compact) { Button b = new Button(this); b.setText(value); b.setTextColor(TEXT); b.setTextSize(compact ? 12 : 13); b.setAllCaps(false); b.setMinHeight(0); b.setMinWidth(0); b.setPadding(dp(compact ? 9 : 14), dp(6), dp(compact ? 9 : 14), dp(6)); b.setBackground(background(compact ? SURFACE_ALT : PURPLE, 10)); return b; }
    private GradientDrawable background(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams top(int margin) { LinearLayout.LayoutParams p = match(); p.topMargin = dp(margin); return p; }
    private int dp(int px) { return (int) (px * getResources().getDisplayMetrics().density + .5f); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int darken(int color) { return Color.rgb((int) (Color.red(color) * .68f), (int) (Color.green(color) * .68f), (int) (Color.blue(color) * .68f)); }
    private static int lighten(int color) { return Color.rgb(Math.min(255, Color.red(color) + 65), Math.min(255, Color.green(color) + 65), Math.min(255, Color.blue(color) + 65)); }
    private static int textOn(int color) { return Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114 > 145000 ? Color.rgb(25, 22, 32) : Color.WHITE; }
    private String t(String zh, String en) { return chinese ? zh : en; }
    private void setStatus(String message, boolean error) { if (status != null) status.setText(message); }

    private interface Choice { void pick(int value); }
    private static final class Block { final String id, shortName; final int a, b, c; final boolean switchable; final String[] main, alt; final int parameterCount, firstParam; Block(String id, String shortName, int a, int b, int c, boolean switchable, String[] main, String[] alt, int parameterCount, int firstParam) { this.id = id; this.shortName = shortName; this.a = a; this.b = b; this.c = c; this.switchable = switchable; this.main = main; this.alt = alt; this.parameterCount = parameterCount; this.firstParam = firstParam; } }
    private static final class State { final boolean[] enabled = new boolean[12]; final int[] type = new int[12], subtype = new int[12]; final int[][] parameter = new int[12][10]; final int[] patchNameBytes = new int[16]; boolean manualMode; int cab, output, sendReturnPost, tunerMute, tunerPitchRef = 5, patch, tunerNote = 12, tunerPitch = 50; }

    /** Dial-style tuning indicator; the needle points left when flat and right when sharp. */
    private static final class TunerMeter extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); private int pitch = 50;
        TunerMeter(Activity context) { super(context); }
        void setPitch(int value) { pitch = clamp(value, 0, 100); invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h * .92f, r = Math.min(w * .39f, h * .82f);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeWidth(5); paint.setColor(Color.rgb(79, 68, 98));
            canvas.drawArc(new RectF(cx-r, cy-r, cx+r, cy+r), 200, 140, false, paint);
            for (int i = 0; i <= 16; i++) { float degrees = 200 + 140 * i / 16f; double rad = Math.toRadians(degrees); float inner = r - (i == 8 ? 17 : 10); float x1 = (float) (cx + Math.cos(rad) * inner), y1 = (float) (cy + Math.sin(rad) * inner); float x2 = (float) (cx + Math.cos(rad) * (r + 3)), y2 = (float) (cy + Math.sin(rad) * (r + 3)); paint.setStrokeWidth(i == 8 ? 4 : 2); paint.setColor(i == 8 ? Color.rgb(157, 255, 65) : Color.rgb(137, 119, 166)); canvas.drawLine(x1, y1, x2, y2, paint); }
            float delta = (pitch - 50) / 50f; double needle = Math.toRadians(270 + delta * 70); paint.setStrokeWidth(6); paint.setColor(Math.abs(delta) < .06f ? Color.rgb(157, 255, 65) : Color.rgb(154, 80, 255)); canvas.drawLine(cx, cy, (float) (cx + Math.cos(needle) * (r - 24)), (float) (cy + Math.sin(needle) * (r - 24)), paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE); canvas.drawCircle(cx, cy, 8, paint); paint.setColor(Math.abs(delta) < .06f ? Color.rgb(157, 255, 65) : Color.rgb(154, 80, 255)); canvas.drawCircle(cx, cy, 4, paint);
        }
    }

    /** A forgiving angular selector: values change only after crossing a tick midpoint. */
    private static final class PatchDial extends View {
        interface Change { void changed(int value); }
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); private final int count, color; private final Change change; private int value;
        PatchDial(Activity context, int count, int value, int color, Change change) { super(context); this.count = count; this.value = Math.max(0, Math.min(count - 1, value)); this.color = color; this.change = change; setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        void setValue(int next) { value = Math.max(0, Math.min(count - 1, next)); invalidate(); }
        @Override protected void onDraw(Canvas canvas) { super.onDraw(canvas); float w = getWidth(), h = getHeight(), r = Math.min(w, h) * .30f, cx = w / 2, cy = h / 2; paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(39, 26, 63)); canvas.drawCircle(cx, cy, r + 20, paint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(12); paint.setColor(color); canvas.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -90, 360, false, paint); paint.setStrokeWidth(3); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT_BOLD); for (int i = 0; i < count; i++) { double a = -Math.PI / 2 + 2 * Math.PI * i / count; float x1 = (float) (cx + Math.cos(a) * r * .86), y1 = (float) (cy + Math.sin(a) * r * .86), x2 = (float) (cx + Math.cos(a) * r * 1.07), y2 = (float) (cy + Math.sin(a) * r * 1.07); paint.setColor(color); canvas.drawLine(x1, y1, x2, y2, paint); paint.setStyle(Paint.Style.FILL); paint.setTextSize(count > 6 ? 13 : 16); canvas.drawText(String.valueOf(i + 1), (float) (cx + Math.cos(a) * r * 1.30), (float) (cy + Math.sin(a) * r * 1.30 + 5), paint); paint.setStyle(Paint.Style.STROKE); } paint.setStrokeWidth(6); paint.setColor(Color.WHITE); double angle = -Math.PI / 2 + 2 * Math.PI * value / count; canvas.drawLine(cx, cy, (float) (cx + Math.cos(angle) * r * .72), (float) (cy + Math.sin(angle) * r * .72), paint); paint.setStyle(Paint.Style.FILL); paint.setTextSize(28); paint.setColor(Color.WHITE); canvas.drawText(String.valueOf(value + 1), cx, cy + 10, paint); }
        @Override public boolean onTouchEvent(MotionEvent event) { if (event.getAction() != MotionEvent.ACTION_DOWN && event.getAction() != MotionEvent.ACTION_MOVE) return true; float x = event.getX() - getWidth() / 2f, y = event.getY() - getHeight() / 2f; double angle = Math.atan2(y, x) + Math.PI / 2; if (angle < 0) angle += Math.PI * 2; int next = (int) Math.floor((angle + Math.PI / count) / (2 * Math.PI / count)) % count; if (next != value) { value = next; invalidate(); change.changed(value); } return true; }
    }
}
