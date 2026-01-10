package no.nordicsemi.android.mesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.logger.MeshLogger;
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.mesh.utils.SecureUtils;

/**
 * Generic Level Set Unacknowledged
 *
 * Parameters:
 *  - Level (int16)
 *  - TID (uint8)
 *  - Command (uint8)
 */
@SuppressWarnings("unused")
public class GenericLevelSetUnacknowledged extends ApplicationMessage {

    private static final String TAG =
            GenericLevelSetUnacknowledged.class.getSimpleName();

    private static final int OP_CODE =
            ApplicationMessageOpCodes.GENERIC_LEVEL_SET_UNACKNOWLEDGED;

    // level(2) + tid(1) + command(1)
    private static final int PARAMS_LENGTH = 4;

    private final int mState;
    private final int mTid;
    private final int mCommand;

    /**
     * Constructor (NO transition parameters)
     *
     * @param appKey {@link ApplicationKey}
     * @param state
     * @param tid    Transaction ID
     * @param command    Transaction ID
     */
    public GenericLevelSetUnacknowledged(@NonNull final ApplicationKey appKey,
                                         final int state,
                                         final int tid,
                                         final int command) {

        super(appKey);

        if (state < Short.MIN_VALUE || state > Short.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Generic level must be between -32768 and 32767");
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

        buffer.putShort((short) mState); // int16
        buffer.put((byte) mTid);         // uint8
        buffer.put((byte) mCommand);     // uint8

        mParameters = buffer.array();
    }
}
