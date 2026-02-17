package no.nordicsemi.android.swaromesh.transport;

import android.util.Log;
import androidx.annotation.NonNull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import no.nordicsemi.android.swaromesh.ApplicationKey;
import no.nordicsemi.android.swaromesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.swaromesh.utils.SecureUtils;

/**
 * GenericStatureSet — 2-byte mesh message (NO TID).
 *
 * ┌─────────────────────────────────────────────┐
 * │  BYTE 0 (Control)                           │
 * │  bit 7 : isIncrement  (1=incr, 0=decr)      │
 * │  bit 6 : isDeviceUpdate (1=device update)   │
 * │  bits 5-0 : deviceCategory (0–63)           │
 * ├─────────────────────────────────────────────┤
 * │  BYTE 1 : step value (0–255)                │
 * └─────────────────────────────────────────────┘
 */
public class GenericStatureSet extends ApplicationMessage {

    private static final String TAG = "GenericStatureSet";

    private static final int OP_CODE      = ApplicationMessageOpCodes.GENERIC_ENCODER_OPCODE_STATUS;
    private static final int MESSAGE_SIZE = 2;

    // Bit masks for Byte 0
    private static final int MASK_INCREMENT     = 0x80; // bit 7
    private static final int MASK_DEVICE_UPDATE = 0x40; // bit 6
    private static final int MASK_CATEGORY      = 0x3F; // bits 5-0

    // Validation limits
    private static final int MAX_CATEGORY = 63;  // 6 bits
    private static final int MAX_STEP     = 255; // 8 bits

    private final boolean isIncrement;
    private final boolean isDeviceUpdate;
    private final int deviceCategory;
    private final int step;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Main constructor — pass each field individually.
     *
     * @param appKey         Bound application key
     * @param isIncrement    true = increment, false = decrement
     * @param isDeviceUpdate true = device/category update operation
     * @param deviceCategory Device category (0–63)
     * @param step           Step value (0–255)
     */
    public GenericStatureSet(@NonNull ApplicationKey appKey,
                             boolean isIncrement,
                             boolean isDeviceUpdate,
                             int deviceCategory,
                             int step) {
        super(appKey);

        validate(deviceCategory, step);

        this.isIncrement    = isIncrement;
        this.isDeviceUpdate = isDeviceUpdate;
        this.deviceCategory = deviceCategory;
        this.step           = step;

        assembleMessageParameters();
    }

    /**
     * Packed constructor — pass Byte 0 as a single int (0–255).
     * Bits are auto-extracted: bit7=increment, bit6=deviceUpdate, bits5-0=category.
     *
     * @param appKey      Bound application key
     * @param controlByte Packed first byte (0–255)
     * @param step        Step value (0–255)
     */
    public GenericStatureSet(@NonNull ApplicationKey appKey,
                             int controlByte,
                             int step) {
        super(appKey);

        this.isIncrement    = (controlByte & MASK_INCREMENT)     != 0;
        this.isDeviceUpdate = (controlByte & MASK_DEVICE_UPDATE) != 0;
        this.deviceCategory =  controlByte & MASK_CATEGORY;
        this.step           = step;

        validate(deviceCategory, step);
        assembleMessageParameters();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core Methods
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    protected void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());

        // Build Byte 0: combine flags + category
        int controlByte = 0;
        if (isIncrement)    controlByte |= MASK_INCREMENT;
        if (isDeviceUpdate) controlByte |= MASK_DEVICE_UPDATE;
        controlByte |= (deviceCategory & MASK_CATEGORY);

        // Pack into 2-byte buffer
        mParameters = ByteBuffer.allocate(MESSAGE_SIZE)
                .order(ByteOrder.BIG_ENDIAN)
                .put((byte) controlByte)
                .put((byte) step)
                .array();

        Log.d(TAG, toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private void validate(int deviceCategory, int step) {
        if (deviceCategory < 0 || deviceCategory > MAX_CATEGORY) {
            throw new IllegalArgumentException(
                    "deviceCategory must be 0–" + MAX_CATEGORY + " (got " + deviceCategory + ")");
        }
        if (step < 0 || step > MAX_STEP) {
            throw new IllegalArgumentException(
                    "step must be 0–" + MAX_STEP + " (got " + step + ")");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory Methods (convenience shortcuts)
    // ─────────────────────────────────────────────────────────────────────────

    /** Create an INCREMENT message */
    public static GenericStatureSet createIncrement(@NonNull ApplicationKey appKey,
                                                    int deviceCategory, int step) {
        return new GenericStatureSet(appKey, true, false, deviceCategory, step);
    }

    /** Create a DECREMENT message */
    public static GenericStatureSet createDecrement(@NonNull ApplicationKey appKey,
                                                    int deviceCategory, int step) {
        return new GenericStatureSet(appKey, false, false, deviceCategory, step);
    }

    /** Create a DEVICE UPDATE message */
    public static GenericStatureSet createDeviceUpdate(@NonNull ApplicationKey appKey,
                                                       boolean isIncrement,
                                                       int deviceCategory, int step) {
        return new GenericStatureSet(appKey, isIncrement, true, deviceCategory, step);
    }

    /** Create from raw 2-byte array */
    public static GenericStatureSet fromByteArray(@NonNull ApplicationKey appKey,
                                                  @NonNull byte[] data) {
        if (data.length != MESSAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Expected " + MESSAGE_SIZE + " bytes, got " + data.length);
        }
        return new GenericStatureSet(appKey, data[0] & 0xFF, data[1] & 0xFF);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isIncrement()    { return isIncrement;    }
    public boolean isDeviceUpdate() { return isDeviceUpdate; }
    public int getDeviceCategory()  { return deviceCategory; }
    public int getStep()            { return step;           }

    /** Returns the packed control byte (Byte 0) */
    public int getControlByte() {
        int b = 0;
        if (isIncrement)    b |= MASK_INCREMENT;
        if (isDeviceUpdate) b |= MASK_DEVICE_UPDATE;
        b |= (deviceCategory & MASK_CATEGORY);
        return b;
    }

    /** Returns raw message bytes */
    public byte[] toByteArray() {
        return mParameters != null ? mParameters.clone() : null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toString
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
                "GenericStatureSet { op=%s, incr=%b, update=%b, cat=%d, step=%d, bytes=[%02X %02X] }",
                isIncrement ? "INCREMENT" : "DECREMENT",
                isIncrement, isDeviceUpdate,
                deviceCategory, step,
                getControlByte(), step
        );
    }
}