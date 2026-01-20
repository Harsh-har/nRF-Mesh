//package no.nordicsemi.android.mesh.transport;
//
//import androidx.annotation.NonNull;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import no.nordicsemi.android.mesh.ApplicationKey;
//import no.nordicsemi.android.mesh.logger.MeshLogger;
//import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
//import no.nordicsemi.android.mesh.utils.SecureUtils;
//
//@SuppressWarnings("unused")
//public class GenericDeltaSet extends ApplicationMessage {
//
//    private static final String TAG = GenericDeltaSet.class.getSimpleName();
//    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_DELTA_SET;
//
//    private static final int REGULAR_PARAMS_LENGTH = 4; // command + delta(2) + tid
//    private static final int MAX_DATA_LENGTH = 8;
//
//    // command + tid + length + 8 data bytes
//    private static final int LONG_COMMAND_LENGTH = 1 + 1 + 1 + MAX_DATA_LENGTH;
//
//    private final int mCommand;
//    private final boolean mIsLongCommand;
//    private final int mLength;
//    private final int[] mDataArray;
//    private final int mDelta;
//    private final int mTid;
//
//    /* ---------------- Regular constructor ---------------- */
//    public GenericDeltaSet(@NonNull ApplicationKey appKey,
//                           int command,
//
//                           int tid) {
//
//        super(appKey);
//
//        validateU8("Command", command);
//        validateU8("TID", tid);
//
//        if (delta < Short.MIN_VALUE || delta > Short.MAX_VALUE) {
//            throw new IllegalArgumentException("Delta must be -32768 to 32767");
//        }
//
//        this.mCommand = command;
//        this.mDelta = delta;
//        this.mTid = tid;
//
//        this.mIsLongCommand = false;
//        this.mLength = 0;
//        this.mDataArray = null;
//
//        assembleMessageParameters();
//    }
//
//    /* ---------------- Long constructor ---------------- */
//    public GenericDeltaSet(@NonNull ApplicationKey appKey,
//                           int command,
//                           int tid,
//                           int length,
//                           @NonNull int[] dataArray
//                           ) {
//
//        super(appKey);
//
//        if (length < 1 || length > MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException("Length must be 1–8");
//        }
//
//        if (dataArray.length != MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException("Data array must be exactly 8 bytes");
//        }
//
//        validateU8("Command", command);
//        validateU8("TID", tid);
//
//        for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//            validateU8("Data[" + i + "]", dataArray[i]);
//        }
//
//        this.mCommand = command;
//        this.mLength = length;
//        this.mDataArray = dataArray;
//        this.mTid = tid;
//
//        this.mIsLongCommand = true;
//        this.mDelta = 0;
//
//        assembleMessageParameters();
//    }
//
//    private void validateU8(String name, int value) {
//        if (value < 0 || value > 255) {
//            throw new IllegalArgumentException(name + " must be 0–255");
//        }
//    }
//
//    @Override
//    public int getOpCode() {
//        return OP_CODE;
//    }
//
//    // In GenericDeltaSet.java, update the assembleMessageParameters method:
//    @Override
//    void assembleMessageParameters() {
//        mAid = SecureUtils.calculateK4(mAppKey.getKey());
//
//        if (mIsLongCommand) {
//
//
//            MeshLogger.verbose(TAG, "Assembling LONG DELTA command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", TID=" + mTid + ", Length=" + mLength );
//
//            ByteBuffer buffer = ByteBuffer
//                    .allocate(LONG_COMMAND_LENGTH)  // Should be 11 bytes
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);   // command
//            buffer.put((byte) mTid);       // tid
//            buffer.put((byte) mLength);    // length
//
//            // ALWAYS 8 DATA BYTES
//            for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//                MeshLogger.verbose(TAG, "Data[" + i + "]=" + mDataArray[i]);
//                buffer.put((byte) mDataArray[i]);
//            }
//
//
//
//            mParameters = buffer.array();
//
//            MeshLogger.verbose(TAG, "Total params length=" + mParameters.length);
//
//            // Log hex representation
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : mParameters) {
//                hexString.append(String.format("%02X ", b));
//            }
//            MeshLogger.verbose(TAG, "Parameters hex: " + hexString.toString());
//
//        } else {
//            MeshLogger.verbose(TAG, "Assembling REGULAR DELTA command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", Delta=" + mDelta + ", TID=" + mTid);
//
//            ByteBuffer buffer = ByteBuffer
//                    .allocate(REGULAR_PARAMS_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);
//            buffer.putShort((short) mDelta);
//            buffer.put((byte) mTid);
//
//            mParameters = buffer.array();
//
//            // Log hex representation
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : mParameters) {
//                hexString.append(String.format("%02X ", b));
//            }
//            MeshLogger.verbose(TAG, "Parameters hex: " + hexString.toString());
//        }
//    }
//    /* ---------------- Builder ---------------- */
//    public static class Builder {
//
//        private final ApplicationKey appKey;
//        private int command = 1;
//        private int delta = 0;
//        private int tid = 0;
//
//        private boolean isLong = false;
//        private int length;
//        private int[] data = new int[MAX_DATA_LENGTH];
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
//        public Builder withDelta(int delta) {
//            this.delta = delta;
//            return this;
//        }
//
//        public Builder withTid(int tid) {
//            this.tid = tid;
//            return this;
//        }
//
//        public Builder asLongCommand(int length, int[] inputData) {
//            this.isLong = true;
//            this.length = length;
//
//            // zero padding
//            for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//                data[i] = (i < inputData.length) ? inputData[i] : 0;
//            }
//            return this;
//        }
//
//        public GenericDeltaSet build() {
//            if (isLong) {
//                return new GenericDeltaSet(appKey, command,tid, length, data);
//            }
//            return new GenericDeltaSet(appKey, command, delta, tid);
//        }
//    }
//}
