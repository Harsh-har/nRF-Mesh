package no.nordicsemi.android.mesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.logger.MeshLogger;
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.mesh.utils.SecureUtils;

public class GenericOnOffSetUnacknowledged extends ApplicationMessage {

    private static final String TAG =
            GenericOnOffSetUnacknowledged.class.getSimpleName();

    private static final int OP_CODE =
            ApplicationMessageOpCodes.GENERIC_ON_OFF_SET_UNACKNOWLEDGED;

    // State (1) + TID (1) + Command (1)
    private static final int PARAM_LENGTH = 3;

    private final int mState;
    private final int mTid;
    private final int mCommand;

    public GenericOnOffSetUnacknowledged(
            @NonNull ApplicationKey appKey,
            int state,
            int tid,
            int command
    ) {
        super(appKey);
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

        ByteBuffer buffer = ByteBuffer
                .allocate(PARAM_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.put((byte) mState);
        buffer.put((byte) mTid);
        buffer.put((byte) mCommand);

        mParameters = buffer.array();
    }
}
