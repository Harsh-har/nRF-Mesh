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
//public class GenericLevelSetUnacknowledged extends ApplicationMessage {
//
//    private static final String TAG = GenericLevelSetUnacknowledged.class.getSimpleName();
//    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_LEVEL_SET_UNACKNOWLEDGED;
//
//    private static final int REGULAR_PARAMS_LENGTH = 4; // command + level(2) + tid
//    private static final int MAX_DATA_LENGTH = 8;
//
//    // 🔥 FIXED: command + length + 8 data + tid = 11 bytes
//    private static final int LONG_COMMAND_LENGTH = 1 + 1 + MAX_DATA_LENGTH + 1;
//
//    private final int mCommand;
//    private final boolean mIsLongCommand;
//    private final int mLength;
//    private final int[] mDataArray;
//    private final int mLevel;
//    private final int mTid;
//
//    /* ---------------- Regular constructor ---------------- */
//    public GenericLevelSetUnacknowledged(@NonNull ApplicationKey appKey,
//                                         int command,
//                                         int level,
//                                         int tid) {
//        super(appKey);
//
//        validateU8("Command", command);
//        validateU8("TID", tid);
//
//        if (level < Short.MIN_VALUE || level > Short.MAX_VALUE) {
//            throw new IllegalArgumentException(
//                    "Level must be between -32768 and 32767");
//        }
//
//        this.mCommand = command;
//        this.mLevel = level;
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
//    public GenericLevelSetUnacknowledged(@NonNull ApplicationKey appKey,
//                                         int command,
//                                         int length,
//                                         @NonNull int[] dataArray,
//                                         int tid) {
//        super(appKey);
//
//        if (length < 1 || length > MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException("Length must be 1–8");
//        }
//
//        if (dataArray.length != MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException(
//                    "Data array must contain exactly 8 elements");
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
//        this.mLevel = 0; // not used
//
//        assembleMessageParameters();
//    }
//
//    private void validateU8(String name, int value) {
//        if (value < 0 || value > 255) {
//            throw new IllegalArgumentException(
//                    name + " must be between 0 and 255");
//        }
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
//        mAid = SecureUtils.calculateK4(mAppKey.getKey());
//
//        if (mIsLongCommand) {
//            // 🔥 FIXED: Changed format to match GenericOnOffSet
//            // Format: [command][length][data1..data8][tid]
//            // Total: 11 bytes (1 + 1 + 8 + 1)
//
//            MeshLogger.verbose(TAG, "Assembling LONG LEVEL Unacknowledged command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", Length=" + mLength + ", TID=" + mTid);
//
//            ByteBuffer buffer = ByteBuffer
//                    .allocate(LONG_COMMAND_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);   // command
//            buffer.put((byte) mLength);    // length
//
//            // ALWAYS 8 data bytes
//            for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//                MeshLogger.verbose(TAG, "Data[" + i + "]=" + mDataArray[i]);
//                buffer.put((byte) mDataArray[i]);
//            }
//
//            buffer.put((byte) mTid);       // tid
//
//            mParameters = buffer.array();
//
//            MeshLogger.verbose(TAG, "Total params length=" + mParameters.length);
//
//            // 🔥 ADDED: Hex parameter logging for debugging
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : mParameters) {
//                hexString.append(String.format("%02X ", b));
//            }
//            MeshLogger.verbose(TAG, "Parameters hex: " + hexString.toString());
//
//        } else {
//            MeshLogger.verbose(TAG, "Assembling REGULAR LEVEL Unacknowledged command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", Level=" + mLevel + ", TID=" + mTid);
//
//            ByteBuffer buffer = ByteBuffer
//                    .allocate(REGULAR_PARAMS_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);
//            buffer.putShort((short) mLevel);
//            buffer.put((byte) mTid);
//
//            mParameters = buffer.array();
//
//            // 🔥 ADDED: Hex parameter logging for debugging
//            StringBuilder hexString = new StringBuilder();
//            for (byte b : mParameters) {
//                hexString.append(String.format("%02X ", b));
//            }
//            MeshLogger.verbose(TAG, "Parameters hex: " + hexString.toString());
//        }
//    }
//
//    /* ---------------- Builder ---------------- */
//    public static class Builder {
//
//        private final ApplicationKey appKey;
//        private int command = 1;
//        private int level = 0;
//        private int tid = 0;
//
//        private boolean isLong = false;
//        private int length;
//        private final int[] data = new int[MAX_DATA_LENGTH];
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
//        public Builder withLevel(int level) {
//            this.level = level;
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
//            // zero-padding
//            for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//                data[i] = (inputData != null && i < inputData.length)
//                        ? inputData[i] : 0;
//            }
//            return this;
//        }
//
//        public GenericLevelSetUnacknowledged build() {
//            if (isLong) {
//                return new GenericLevelSetUnacknowledged(
//                        appKey, command, length, data, tid);
//            }
//            return new GenericLevelSetUnacknowledged(
//                    appKey, command, level, tid);
//        }
//    }
//}