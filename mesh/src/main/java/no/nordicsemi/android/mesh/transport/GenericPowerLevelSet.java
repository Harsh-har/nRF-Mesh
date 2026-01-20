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
//public class GenericPowerLevelSet extends ApplicationMessage {
//
//    private static final String TAG = GenericPowerLevelSet.class.getSimpleName();
//    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_SET;
//
//    private static final int REGULAR_PARAMS_LENGTH = 4; // cmd(1) + power(2) + tid(1)
//    private static final int MAX_DATA_LENGTH = 8;
//
//    // Long command: command + length + 8 data + tid = 11 bytes
//    private static final int LONG_COMMAND_LENGTH = 1 + 1 + MAX_DATA_LENGTH + 1;
//
//    private final int mCommand;
//    private final boolean mIsLongCommand;
//
//    // Long command
//    private final int mLength;          // 1–8
//    private final int[] mDataArray;     // always 8 bytes
//
//    // Regular command
//    private final int mPowerLevel;
//    private final int mTid;
//
//    /* ------------------------------------------------------------
//     * REGULAR COMMAND
//     * ------------------------------------------------------------ */
//    public GenericPowerLevelSet(@NonNull final ApplicationKey appKey,
//                                final int command,
//                                final int powerLevel,
//                                final int tid) {
//        super(appKey);
//
//        validateRange("Command", command, 0, 255);
//        validateRange("Transaction ID", tid, 0, 255);
//
//        if (powerLevel < 0 || powerLevel > 0xFFFF) {
//            throw new IllegalArgumentException(
//                    "Power level must be between 0 and 65535");
//        }
//
//        this.mCommand = command;
//        this.mPowerLevel = powerLevel;
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
//     * LONG COMMAND (FIXED TO MATCH GenericOnOffSet FORMAT)
//     * ------------------------------------------------------------ */
//    public GenericPowerLevelSet(@NonNull final ApplicationKey appKey,
//                                final int command,
//                                final int length,
//                                @NonNull final int[] dataArray,
//                                final int tid) {
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
//        this.mPowerLevel = 0; // Not used in long command
//
//        assembleMessageParameters();
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
//     * PARAMETER ASSEMBLY (MAIN FIX - NOW CONSISTENT WITH GenericOnOffSet)
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
//            MeshLogger.verbose(TAG, "Assembling LONG Power Level command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", Length=" + mLength + ", TID=" + mTid);
//
//            final ByteBuffer buffer = ByteBuffer
//                    .allocate(LONG_COMMAND_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);   // command
//            buffer.put((byte) mLength);    // length
//
//            // Always send 8 bytes (consistent with GenericOnOffSet)
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
//            // Regular command format: [command][power(LSB)][power(MSB)][tid]
//            MeshLogger.verbose(TAG, "Assembling REGULAR Power Level command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", PowerLevel=" + mPowerLevel + ", TID=" + mTid);
//
//            final ByteBuffer buffer = ByteBuffer
//                    .allocate(REGULAR_PARAMS_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);
//            buffer.putShort((short) (mPowerLevel & 0xFFFF));
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
//    public int getPowerLevel() { return mPowerLevel; }
//    public int getTid() { return mTid; }
//
//    /* ------------------------------------------------------------
//     * TO STRING
//     * ------------------------------------------------------------ */
//    @Override
//    public String toString() {
//        if (mIsLongCommand) {
//            return "GenericPowerLevelSet{" +
//                    "type=LONG" +
//                    ", command=" + mCommand +
//                    ", length=" + mLength +
//                    ", data=" + Arrays.toString(mDataArray) +
//                    ", tid=" + mTid +
//                    '}';
//        }
//        return "GenericPowerLevelSet{" +
//                "type=REGULAR" +
//                ", command=" + mCommand +
//                ", powerLevel=" + mPowerLevel +
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
//        private int powerLevel = 0;
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
//        public Builder withPowerLevel(int powerLevel) {
//            this.powerLevel = powerLevel;
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
//        public GenericPowerLevelSet build() {
//            if (isLongCommand) {
//                return new GenericPowerLevelSet(
//                        appKey, command, length, dataArray, tid);
//            }
//            return new GenericPowerLevelSet(
//                    appKey, command, powerLevel, tid);
//        }
//    }
//}