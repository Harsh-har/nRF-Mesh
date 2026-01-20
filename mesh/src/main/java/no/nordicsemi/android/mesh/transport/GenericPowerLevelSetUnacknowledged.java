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
//public class GenericPowerLevelSetUnacknowledged extends ApplicationMessage {
//
//    private static final String TAG = GenericPowerLevelSetUnacknowledged.class.getSimpleName();
//    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_SET_UNACKNOWLEDGED;
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
//    private final int mLength;
//    private final int[] mDataArray;
//
//    // Regular command
//    private final int mPowerLevel;
//    private final int mTid;
//
//    /* ------------------------------------------------------------
//     * REGULAR COMMAND CONSTRUCTOR
//     * ------------------------------------------------------------ */
//    public GenericPowerLevelSetUnacknowledged(@NonNull ApplicationKey appKey,
//                                              int command,
//                                              int powerLevel,
//                                              int tid) {
//        super(appKey);
//
//        validateRange("Command", command);
//        validateRange("TID", tid);
//
//        if (powerLevel < 0 || powerLevel > 0xFFFF) {
//            throw new IllegalArgumentException("PowerLevel must be 0–65535");
//        }
//
//        this.mCommand = command;
//        this.mIsLongCommand = false;
//        this.mPowerLevel = powerLevel;
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
//    // FIXED: Changed parameter order to be consistent with GenericOnOffSet
//    public GenericPowerLevelSetUnacknowledged(@NonNull ApplicationKey appKey,
//                                              int command,
//                                              int tid,
//                                              int length,
//                                              @NonNull int[] dataArray
//                                              ) {
//        super(appKey);
//
//        validateRange("Command", command);
//        validateRange("TID", tid);
//
//        if (length < 1 || length > MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException("Length must be 1–8");
//        }
//
//        if (dataArray.length != MAX_DATA_LENGTH) {
//            throw new IllegalArgumentException("Data array must be exactly 8 bytes");
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
//        this.mPowerLevel = 0;
//
//        assembleMessageParameters();
//    }
//
//    private void validateRange(String name, int value) {
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
//    /* ------------------------------------------------------------
//     * PARAMETER ASSEMBLY (UPDATED FOR CONSISTENCY)
//     * ------------------------------------------------------------ */
//    // In GenericPowerLevelSetUnacknowledged.java, update the assembleMessageParameters method:
//
//    @Override
//    void assembleMessageParameters() {
//        mAid = SecureUtils.calculateK4(mAppKey.getKey());
//
//        if (mIsLongCommand) {
//            // 🔥 CHANGED: Now matches GenericOnOffSet format
//            // Format: [command][length][data1..data8][tid]
//            // Total: 11 bytes (1 + 1 + 8 + 1)
//
//            MeshLogger.verbose(TAG, "Assembling LONG Power Level Unacknowledged command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand  + ", TID=" + mTid + ", Length=" + mLength);
//
//            ByteBuffer buffer = ByteBuffer
//                    .allocate(1 + 1 + MAX_DATA_LENGTH + 1) // command + length + 8 data + tid
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);   // command
//            buffer.put((byte) mTid);       // tid
//            buffer.put((byte) mLength);    // length
//
//            // Always write 8 data bytes
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
//            // Regular command format: [command][power(LSB)][power(MSB)][tid]
//            MeshLogger.verbose(TAG, "Assembling REGULAR Power Level Unacknowledged command");
//            MeshLogger.verbose(TAG, "Command=" + mCommand + ", PowerLevel=" + mPowerLevel + ", TID=" + mTid);
//
//            ByteBuffer buffer = ByteBuffer
//                    .allocate(REGULAR_PARAMS_LENGTH)
//                    .order(ByteOrder.LITTLE_ENDIAN);
//
//            buffer.put((byte) mCommand);
//            buffer.putShort((short) (mPowerLevel & 0xFFFF));
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
//     * BUILDER (UPDATED FOR CONSISTENCY)
//     * ------------------------------------------------------------ */
//    public static class Builder {
//
//        private final ApplicationKey appKey;
//        private int command = 1;
//        private int powerLevel = 0;
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
//            this.isLong = true;
//            this.length = length;
//
//
//            for (int i = 0; i < MAX_DATA_LENGTH; i++) {
//                data[i] = (i < inputData.length) ? inputData[i] : 0;
//            }
//            return this;
//        }
//
//        public GenericPowerLevelSetUnacknowledged build() {
//            if (isLong) {
//                return new GenericPowerLevelSetUnacknowledged(
//                        appKey, command,tid,length, data);
//            }
//            return new GenericPowerLevelSetUnacknowledged(
//                    appKey, command, powerLevel, tid);
//        }
//    }
//}