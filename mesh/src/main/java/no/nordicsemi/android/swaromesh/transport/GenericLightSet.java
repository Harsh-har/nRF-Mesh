package no.nordicsemi.android.swaromesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import no.nordicsemi.android.swaromesh.ApplicationKey;
import no.nordicsemi.android.swaromesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.swaromesh.utils.SecureUtils;
import no.nordicsemi.android.swaromesh.logger.MeshLogger;

public class GenericLightSet extends ApplicationMessage {

    private static final String TAG = GenericLightSet.class.getSimpleName();
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_LIGHT_CONTROL_OPCODE;

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 8;
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 255;

    private final int length;
    private final int command;
    private final int[] brightness; // valid brightness only
    private final int tid;

    /**
     * Message Structure (VARIABLE LENGTH):
     *
     * Byte 0  : length
     * Byte 1  : command
     * Byte 2+ : brightness[length]
     * Last    : tid
     *
     * Total size = 1 + 1 + length + 1
     */
    public GenericLightSet(@NonNull final ApplicationKey appKey,
                           final int length,
                           final int command,
                           @NonNull final int[] brightness,
                           final int tid) {
        super(appKey);

        validate(length, command, brightness, tid);

        this.length = length;
        this.command = command;
        this.tid = tid;
        this.brightness = Arrays.copyOf(brightness, length);

        assembleMessageParameters();
    }

    private void validate(final int length,
                          final int command,
                          final int[] brightness,
                          final int tid) {

        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }

        if (command < MIN_VALUE || command > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid command: " + command);
        }

        if (brightness == null || brightness.length < length) {
            throw new IllegalArgumentException("Brightness array too short");
        }

        for (int i = 0; i < length; i++) {
            if (brightness[i] < MIN_VALUE || brightness[i] > MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Invalid brightness[" + i + "] = " + brightness[i]);
            }
        }

        if (tid < MIN_VALUE || tid > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid TID: " + tid);
        }
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    public int getLength() {
        return length;
    }

    public int getCommand() {
        return command;
    }

    public int[] getBrightness() {
        return Arrays.copyOf(brightness, length);
    }

    public int getTid() {
        return tid;
    }

    public int getMessageSize() {
        return 1 + 1 + length + 1;
    }

    @Override
    void assembleMessageParameters() {

        mAid = SecureUtils.calculateK4(mAppKey.getKey());

        final ByteBuffer buffer = ByteBuffer
                .allocate(getMessageSize())
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.put((byte) length);
        buffer.put((byte) command);

        for (int i = 0; i < length; i++) {
            buffer.put((byte) brightness[i]);
        }

        buffer.put((byte) tid);

        mParameters = buffer.array();

        logMessageParameters();
    }

    private void logMessageParameters() {
        MeshLogger.verbose(TAG,
                "Len=" + length +
                        ", Cmd=" + command +
                        " (0x" + String.format("%02X", command) + ")" +
                        ", Brightness=" + Arrays.toString(brightness) +
                        ", TID=" + tid +
                        " (0x" + String.format("%02X", tid) + ")"
        );
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(mParameters, mParameters.length);
    }

    public static GenericLightSet fromByteArray(@NonNull ApplicationKey appKey,
                                                @NonNull byte[] data) {

        if (data.length < 4) {
            throw new IllegalArgumentException("Invalid data length");
        }

        int length = data[0] & 0xFF;
        int command = data[1] & 0xFF;

        if (data.length != 1 + 1 + length + 1) {
            throw new IllegalArgumentException("Length mismatch");
        }

        int[] brightness = new int[length];
        for (int i = 0; i < length; i++) {
            brightness[i] = data[2 + i] & 0xFF;
        }

        int tid = data[data.length - 1] & 0xFF;

        return new GenericLightSet(appKey, length, command, brightness, tid);
    }

    @Override
    public String toString() {
        return "GenericLightSet{" +
                "length=" + length +
                ", command=" + command +
                ", brightness=" + Arrays.toString(brightness) +
                ", tid=" + tid +
                '}';
    }
}
