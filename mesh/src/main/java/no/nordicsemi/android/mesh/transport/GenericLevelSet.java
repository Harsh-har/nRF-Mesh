//package no.nordicsemi.android.mesh.transport;
//
//import androidx.annotation.NonNull;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.util.Arrays;
//import no.nordicsemi.android.mesh.ApplicationKey;
//import no.nordicsemi.android.mesh.logger.MeshLogger;
//import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
//import no.nordicsemi.android.mesh.utils.SecureUtils;
//
//@SuppressWarnings("unused")
//public class GenericLevelSet extends ApplicationMessage {
//
//    private static final String TAG = GenericLevelSet.class.getSimpleName();
//    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_LEVEL_SET;
//
//    private static final int REGULAR_PARAMS_LENGTH = 4; // command(1) + level(2) + tid(1)
//    private static final int MAX_DATA_LENGTH = 8;
//
//    // Long command: command + length + 8 data + tid = 11 bytes
//    private static final int LONG_COMMAND_LENGTH = 1 + 1 + MAX_DATA_LENGTH + 1;
//
//    private final int mCommand;
//    private final boolean mIsLongCommand;
//
//    // Long command fields
//    private final int mLength;        // 1–8
//    private final int[] mDataArray;   // Always 8 elements
//
//    // Regular command fields
//    private final int mLevel;
//    private final int mTid;
//
//    /* ------------------------------------------------------------
//     * REGULAR COMMAND CONSTRUCTOR
//     * ------------------------------------------------------------ */
//    public GenericLevelSet(@NonNull final ApplicationKey appKey,
//                           final int command,
//                           final int level,
//                           final int tid) {
//        super(appKey);
//
//        validateRange("Command", command);
//        validateRange("Transaction ID", tid);
//
//        if (level < Short.MIN_VALUE || level > Short.MAX_VALUE) {
//            throw new IllegalArgumentException(
//                    "Generic level value must be between -32768 and 32767");
//        }
//
//        this.mCommand = command;
//        this.mIsLongCommand = false;
//        this.mLevel = level;
//        this.mTid = tid;
//
//        this.mLength = 0;
//        this.mDataArray = null;
//
//        assembleMessageParameters();
//    }
//
//    /* ------------------------------------------------------------
//     * LONG COMMAND CONSTRUCTOR
//     * ------------------------------------------------------------ */
//    public GenericLevelSet(@NonNull final ApplicationKey appKey,
//                           final int command,
//                           final int length,
//                           @NonNull final int[] dataArray,
//                           final int tid) {
//        super(appKey);
//
//        validateRange("Command", command);
//        validateRange("Transaction ID", tid);
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
//            validateRange("Data[" + i + "]", dataArray[i]);
//        }
//
//        this.mCommand = command;
//        this.mIsLongCommand = true;
//        this.mLength = length;
//        this.mDataArray = dataArray;
//        this.mTid = tid;
//
//        this.mLevel = 0; // Not used in long command
//
//        assembleMessageParameters();
//    }
//
//    /* ------------------------------------------------------------
//     * VALIDATION
//     * ------------------------------------------------------------ */
//    private void validateRange(String name, int value) {
//        if (value < 0 || value > 255) {
//            throw new IllegalArgumentException(name + " must be 0–255");
//        }
//    }
//
//    /* ------------------------------------------------------------
//     * OPCODE
//     * ------------------------------------------------------------ */
//    @Override
//    public int getOpCode() {
//        return OP_CODE;
//    }
//
//    /* ------------------------------------------------------------
//     * PARAMETER ASSEMBLY (FIXED TO MATCH GenericOnOffSet FORMAT)
//     * ------------------------------------------------------------ */
//    @Override
//    void assembleMessageParameters() {
//        mAid = SecureUtils.calculateK4(mAppKey.getKey());
//
//        if (mIsLongCommand) {
//            // 🔥 FIXED: Changed to match GenericOnOffSet format
//            // Format: [command][length][data1..data8][tid]
//            // Total: 11 bytes (1 + 1 + 8 + 1)
//
//            MeshLogger.verbose(TAG, "Assembling LONG Generic Level command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", Length=" + mLength + ", TID=" + mTid);
//
//            final ByteBuffer buffer = ByteBuffer
//                    .allocate(LONG_COMMAND_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);   // command
//            buffer.put((byte) mLength);    // length
//
//            // Always send 8 bytes (consistent with other classes)
//            for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//                int value = (i < mLength) ? mDataArray[i] : 0;
//                buffer.put((byte) value);
//                MeshLogger.verbose(TAG, "Data[" + i + "]=" + value);
//            }
//
//            buffer.put((byte) mTid);       // tid
//
//            mParameters = buffer.array();
//
//            MeshLogger.verbose(TAG, "LONG CMD Params (" + mParameters.length + " bytes): "
//                    + bytesToHex(mParameters));
//
//        } else {
//            // Regular command format: [command][level(LSB)][level(MSB)][tid]
//            MeshLogger.verbose(TAG, "Assembling REGULAR Generic Level command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", Level=" + mLevel + ", TID=" + mTid);
//
//            final ByteBuffer buffer = ByteBuffer
//                    .allocate(REGULAR_PARAMS_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);
//            buffer.putShort((short) mLevel);
//            buffer.put((byte) mTid);
//
//            mParameters = buffer.array();
//
//            MeshLogger.verbose(TAG, "REGULAR CMD Params (" + mParameters.length + " bytes): "
//                    + bytesToHex(mParameters));
//        }
//    }
//
//    /* ------------------------------------------------------------
//     * HELPER: Convert bytes to hex string
//     * ------------------------------------------------------------ */
//    private String bytesToHex(byte[] bytes) {
//        StringBuilder sb = new StringBuilder();
//        for (byte b : bytes) {
//            sb.append(String.format("%02X ", b));
//        }
//        return sb.toString().trim();
//    }
//
//    /* ------------------------------------------------------------
//     * GETTERS
//     * ------------------------------------------------------------ */
//    public int getCommand() { return mCommand; }
//    public boolean isLongCommand() { return mIsLongCommand; }
//    public int getLength() { return mLength; }
//    public int[] getDataArray() { return mDataArray; }
//    public int getLevel() { return mLevel; }
//    public int getTid() { return mTid; }
//
//    /* ------------------------------------------------------------
//     * TO STRING
//     * ------------------------------------------------------------ */
//    @Override
//    public String toString() {
//        if (mIsLongCommand) {
//            return "GenericLevelSet{" +
//                    "type=LONG" +
//                    ", command=" + mCommand +
//                    ", length=" + mLength +
//                    ", data=" + Arrays.toString(mDataArray) +
//                    ", tid=" + mTid +
//                    '}';
//        }
//        return "GenericLevelSet{" +
//                "type=REGULAR" +
//                ", command=" + mCommand +
//                ", level=" + mLevel +
//                ", tid=" + mTid +
//                '}';
//    }
//
//    /* ------------------------------------------------------------
//     * BUILDER (UPDATED FOR CONSISTENCY)
//     * ------------------------------------------------------------ */
//    public static class Builder {
//
//        private final ApplicationKey appKey;
//
//        private int command = 1;
//        private int level = 0;
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
//            this.isLongCommand = true;
//            this.length = length;
//
//            // Create array with exactly 8 elements
//            this.dataArray = new int[MAX_DATA_LENGTH];
//            if (inputData != null) {
//                // Copy input data and zero-pad the rest
//                int copyLength = Math.min(inputData.length, MAX_DATA_LENGTH);
//                System.arraycopy(inputData, 0, this.dataArray, 0, copyLength);
//                // Remaining elements already 0
//            }
//            return this;
//        }
//
//        public GenericLevelSet build() {
//            if (isLongCommand) {
//                return new GenericLevelSet(
//                        appKey, command, length, dataArray, tid);
//            }
//            return new GenericLevelSet(
//                    appKey, command, level, tid);
//        }
//    }
//}