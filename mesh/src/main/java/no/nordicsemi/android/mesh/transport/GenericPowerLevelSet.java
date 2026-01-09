package no.nordicsemi.android.mesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.logger.MeshLogger;
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.mesh.utils.SecureUtils;

/**
 * Generic Power Level Set (Acknowledged)
 *
 * Parameters:
 *  - Power Level (uint16)
 *  - TID (uint8)
 *  - Command (uint8)
 */
@SuppressWarnings("unused")
public class GenericPowerLevelSet extends ApplicationMessage {

    private static final String TAG = GenericPowerLevelSet.class.getSimpleName();
    private static final int OP_CODE =
            ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_SET;

    // power(2) + tid(1) + command(1) = 4 bytes
    private static final int PARAMS_LENGTH = 4;

    private final int mState;
    private final int mTid;
    private final int mCommand;

    /**
     * Constructor (NO transition params)
     *
     * @param appKey     {@link ApplicationKey}
     * @param state Power Level (0–65535)
     * @param tid        Transaction ID
     * @param command    Custom command ID
     */
    public GenericPowerLevelSet(@NonNull final ApplicationKey appKey,
                                final int state,
                                final int tid,
                                final int command) {

        super(appKey);

        if (state < 0 || state > 0xFFFF) {
            throw new IllegalArgumentException(
                    "Generic power level must be between 0 and 65535");
        }

        this.mState = state;
        this.mTid = tid;
        this.mCommand = command;

        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());

        MeshLogger.verbose(TAG, "State: " + mState);
        MeshLogger.verbose(TAG, "TID: " + mTid);
        MeshLogger.verbose(TAG, "Command: " + mCommand);

        final ByteBuffer buffer = ByteBuffer
                .allocate(PARAMS_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short) mState); // uint16
        buffer.put((byte) mTid);              // uint8
        buffer.put((byte) mCommand);          // uint8

        mParameters = buffer.array();
    }
}
