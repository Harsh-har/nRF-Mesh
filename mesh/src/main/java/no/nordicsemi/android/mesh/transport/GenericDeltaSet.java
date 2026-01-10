package no.nordicsemi.android.mesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.logger.MeshLogger;
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.mesh.utils.SecureUtils;

/**
 * Generic Level Delta Set (UNACKED style wrapper)
 *
 * Parameters:
 *  - Delta (int16)
 *  - TID (uint8)
 *  - Command (uint8)  <-- custom
 */
@SuppressWarnings("unused")
public class GenericDeltaSet extends ApplicationMessage {

    private static final String TAG = GenericDeltaSet.class.getSimpleName();
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_DELTA_SET;

    // delta(2) + tid(1) + command(1) = 4 bytes
    private static final int PARAMS_LENGTH = 4;

    private final int mState;
    private final int mTid;
    private final int mCommand;

    /**
     * Constructor (NO transition params)
     *
     * @param appKey   {@link ApplicationKey}
     * @param state    Level delta value
     * @param tId      Transaction ID
     * @param command  Custom command ID
     */
    public GenericDeltaSet(@NonNull final ApplicationKey appKey,
                           final int state,
                           final int tId,
                           final int command) {

        super(appKey);
        this.mState = state;
        this.mTid = tId;
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

        MeshLogger.verbose(TAG, "Delta: " + mState);
        MeshLogger.verbose(TAG, "TID: " + mTid);
        MeshLogger.verbose(TAG, "Command: " + mCommand);

        final ByteBuffer buffer = ByteBuffer
                .allocate(PARAMS_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short) mState);   // int16
        buffer.put((byte) mTid);            // uint8
        buffer.put((byte) mCommand);        // uint8

        mParameters = buffer.array();
    }
}
