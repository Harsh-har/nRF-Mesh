//package no.nordicsemi.android.mesh.transport;
//
//import androidx.annotation.NonNull;
//
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.util.Arrays;
//
//import no.nordicsemi.android.mesh.ApplicationKey;
//import no.nordicsemi.android.mesh.logger.MeshLogger;
//import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
//import no.nordicsemi.android.mesh.utils.SecureUtils;
//
///**
// * Generic OnOff Set Unacknowledged
// *
// * REGULAR COMMAND:
// *  command(1) + state(1) + tid(1)
// *
// * LONG COMMAND (FIXED):
// *  command(1) + tid(1) + length(1) + data[8]
// */
//@SuppressWarnings("unused")
//public class GenericOnOffSetUnacknowledged extends ApplicationMessage {
//
//    private static final String TAG = GenericOnOffSetUnacknowledged.class.getSimpleName();
//    private static final int OP_CODE =
//            ApplicationMessageOpCodes.GENERIC_ON_OFF_SET_UNACKNOWLEDGED;
//
//    private static final int REGULAR_PARAMS_LENGTH = 3;
//    private static final int MAX_DATA_LENGTH = 8;
//
//    private final int mCommand;
//    private final boolean mIsLongCommand;
//
//    // Long command
//    private final int mLength;          // 1–8
//    private final int[] mDataArray;     // Always 8 bytes
//
//    // Regular command
//    private final int mState;
//
//    private final int mTid;
//
//    /* ------------------------------------------------------------
//     * REGULAR COMMAND CONSTRUCTOR
//     * ------------------------------------------------------------ */
//    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
//                                         final int command,
//                                         final int state,
//                                         final int tid) {
//        super(appKey);
//
//        validateRange("Command", command, 0, 255);
//        validateRange("State", state, 0, 255);
//        validateRange("Transaction ID", tid, 0, 255);
//
//        this.mCommand = command;
//        this.mState = state;
//        this.mTid = tid;
//
//        this.mIsLongCommand = false;
//        this.mLength = 0;
//        this.mDataArray = null;
//
//        assembleMessageParameters();
//    }
//
//    /* ------------------------------------------------------------
//     * LONG COMMAND CONSTRUCTOR
//     * ------------------------------------------------------------ */
//    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
//                                         final int command,
//                                         final int length,
//                                         @NonNull final int[] dataArray,
//                                         final int tid) {
//        super(appKey);
//
//        validateRange("Command", command, 0, 255);
//        validateRange("Transaction ID", tid, 0, 255);
//
//        if (length < 1 || length > MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException(
//                    "Length must be between 1 and " + MAX_DATA_LENGTH);
//        }
//
//        if (dataArray.length != MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException(
//                    "Data array must have exactly " + MAX_DATA_LENGTH + " elements");
//        }
//
//        for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//            validateRange("Data[" + i + "]", dataArray[i], 0, 255);
//        }
//
//        this.mCommand = command;
//        this.mIsLongCommand = true;
//        this.mLength = length;
//        this.mDataArray = dataArray;
//        this.mTid = tid;
//
//        this.mState = 0; // Not used in long command
//
//        assembleMessageParameters();
//    }
//
//    /* ------------------------------------------------------------
//     * LEGACY CONSTRUCTORS
//     * ------------------------------------------------------------ */
//    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
//                                         final int state,
//                                         final int tid) {
//        this(appKey, 1, state, tid);
//    }
//
//    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
//                                         final int state,
//                                         final int tid,
//                                         final Integer transitionSteps,
//                                         final Integer transitionResolution,
//                                         final Integer delay) {
//        this(appKey, 1, state, tid);
//        MeshLogger.warn(TAG,
//                "Transition parameters ignored in GenericOnOffSetUnacknowledged");
//    }
//
//    /* ------------------------------------------------------------
//     * VALIDATION
//     * ------------------------------------------------------------ */
//    private void validateRange(String name, int value, int min, int max) {
//        if (value < min || value > max) {
//            throw new IllegalArgumentException(
//                    name + " must be between " + min + " and " + max);
//        }
//    }
//
//    @Override
//    public int getOpCode() {
//        return OP_CODE;
//    }
//
//    /* ------------------------------------------------------------
//     * PARAMETER ASSEMBLY (🔥 MAIN FIX 🔥)
//     * ------------------------------------------------------------ */
//    @Override
//    void assembleMessageParameters() {
//        mAid = SecureUtils.calculateK4(mAppKey.getKey());
//
//        if (mIsLongCommand) {
//
//            // FIXED PAYLOAD SIZE:
//            // command + tid + length + 8 data bytes
//            final int totalLength = 3 + MAX_DATA_LENGTH;
//
//            final ByteBuffer buffer = ByteBuffer
//                    .allocate(totalLength)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);
//            buffer.put((byte) mTid);
//            buffer.put((byte) mLength);
//
//            // Always send 8 bytes
//            for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//                int value = (i < mLength) ? mDataArray[i] : 0;
//                buffer.put((byte) value);
//            }
//
//            mParameters = buffer.array();
//
//            MeshLogger.verbose(TAG,
//                    "LONG CMD Params (" + mParameters.length + "): "
//                            + Arrays.toString(mParameters));
//
//        } else {
//
//            final ByteBuffer buffer = ByteBuffer
//                    .allocate(REGULAR_PARAMS_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);
//            buffer.put((byte) mState);
//            buffer.put((byte) mTid);
//
//            mParameters = buffer.array();
//        }
//    }
//
//    /* ------------------------------------------------------------
//     * GETTERS
//     * ------------------------------------------------------------ */
//    public int getCommand() { return mCommand; }
//    public boolean isLongCommand() { return mIsLongCommand; }
//    public int getLength() { return mLength; }
//    public int[] getDataArray() { return mDataArray; }
//    public int getState() { return mState; }
//    public int getTid() { return mTid; }
//
//    /* ------------------------------------------------------------
//     * TO STRING
//     * ------------------------------------------------------------ */
//    @Override
//    public String toString() {
//        if (mIsLongCommand) {
//            return "GenericOnOffSetUnacknowledged{" +
//                    "type=LONG" +
//                    ", command=" + mCommand +
//                    ", length=" + mLength +
//                    ", data=" + Arrays.toString(mDataArray) +
//                    ", tid=" + mTid +
//                    '}';
//        }
//        return "GenericOnOffSetUnacknowledged{" +
//                "type=REGULAR" +
//                ", command=" + mCommand +
//                ", state=" + mState +
//                ", tid=" + mTid +
//                '}';
//    }
//
//    /* ------------------------------------------------------------
//     * BUILDER
//     * ------------------------------------------------------------ */
//    public static class Builder {
//
//        private final ApplicationKey appKey;
//
//        private int command = 1;
//        private int state = 0;
//        private int tid = 0;
//
//        private boolean isLongCommand = false;
//        private int length = 0;
//        private int[] dataArray = null;
//
//        public Builder(@NonNull ApplicationKey appKey) {
//            this.appKey = appKey;
//        }
//
//        public Builder withCommand(int command) {
//            this.command = command;
//            return this;
//        }
//
//        public Builder withState(int state) {
//            this.state = state;
//            return this;
//        }
//
//        public Builder withTid(int tid) {
//            this.tid = tid;
//            return this;
//        }
//
//        public Builder asLongCommand(int length, int[] inputData) {
//            this.isLongCommand = true;
//            this.length = length;
//
//            this.dataArray = new int[MAX_DATA_LENGTH];
//            if (inputData != null) {
//                System.arraycopy(
//                        inputData, 0,
//                        this.dataArray, 0,
//                        Math.min(inputData.length, MAX_DATA_LENGTH));
//            }
//            return this;
//        }
//
//        public GenericOnOffSetUnacknowledged build() {
//            if (isLongCommand) {
//                return new GenericOnOffSetUnacknowledged(
//                        appKey, command, length, dataArray, tid);
//            }
//            return new GenericOnOffSetUnacknowledged(
//                    appKey, command, state, tid);
//        }
//    }
//}
