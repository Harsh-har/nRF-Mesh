//package no.nordicsemi.android.mesh.transport;
//
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//
//import androidx.annotation.NonNull;
//import no.nordicsemi.android.mesh.ApplicationKey;
//import no.nordicsemi.android.mesh.logger.MeshLogger;
//import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
//import no.nordicsemi.android.mesh.utils.SecureUtils;
//
//
//@SuppressWarnings("unused")
//public class GenericOnOffSet extends ApplicationMessage {
//
//    private static final String TAG = GenericOnOffSet.class.getSimpleName();
//    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_ON_OFF_SET;
//
//    private static final int GENERIC_ON_OFF_SET_PARAMS_LENGTH = 3;
//
//    private final int mState;
//    private final int tId;
//    private final int mCommand;
//
//    /**
//     * @param appKey  {@link ApplicationKey} key for this message
//     * @param state   value
//     * @param tId     Transaction ID
//     * @param command Command ID
//     * @param delay
//     */
//    public GenericOnOffSet(@NonNull final ApplicationKey appKey,
//                           boolean b, final int state,
//                           final int tId,
//                           final int command, Integer delay) {
//
//        super(appKey);
//        this.mState = state;
//        this.tId = tId;
//        this.mCommand = command;
//
//        assembleMessageParameters();
//    }
//
//    @Override
//    public int getOpCode() {
//        return OP_CODE;
//    }
//
//    @Override
//    void assembleMessageParameters() {
//
//        // AID calculation
//        mAid = SecureUtils.calculateK4(mAppKey.getKey());
//
//        MeshLogger.verbose(TAG, "State: " + (mState));
//        MeshLogger.verbose(TAG, "TID: " + tId);
//        MeshLogger.verbose(TAG, "Command: " + mCommand);
//
//        final ByteBuffer paramsBuffer =
//                ByteBuffer.allocate(GENERIC_ON_OFF_SET_PARAMS_LENGTH)
//                        .order(ByteOrder.LITTLE_ENDIAN);
//
//        paramsBuffer.put((byte) (mState));
//        paramsBuffer.put((byte) tId);
//        paramsBuffer.put((byte) mCommand);
//
//        mParameters = paramsBuffer.array();
//    }
//}
