package no.nordicsemi.android.mesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.logger.MeshLogger;
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.mesh.utils.SecureUtils;

@SuppressWarnings("unused")
public class GenericPowerLevelSet extends ApplicationMessage {

    private static final String TAG = GenericPowerLevelSet.class.getSimpleName();
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_POWER_LEVEL_SET;
    private static final int REGULAR_PARAMS_LENGTH = 4; // command(1) + powerLevel(2) + tid(1)
    private static final int MAX_DATA_LENGTH = 8;

    private final int mCommand;
    private final boolean mIsLongCommand;
    private final int mLength; // For long command: 1-8
    private final int[] mDataArray; // For long command: data array
    private final int mPowerLevel;
    private final int mTid;

    /**
     * Constructor for regular command
     */
    public GenericPowerLevelSet(@NonNull final ApplicationKey appKey,
                                final int command,
                                final int powerLevel,
                                final int tid) {
        super(appKey);

        validateRange("Command", command, 0, 255);
        validateRange("Transaction ID", tid, 0, 255);

        if (powerLevel < 0 || powerLevel > 0xFFFF) {
            throw new IllegalArgumentException("Generic power level must be between 0 and 65535");
        }

        this.mCommand = command;
        this.mIsLongCommand = false;
        this.mLength = 0;
        this.mDataArray = null;
        this.mPowerLevel = powerLevel;
        this.mTid = tid;

        assembleMessageParameters();
    }

    /**
     * Constructor for long command with data array
     */
    public GenericPowerLevelSet(@NonNull final ApplicationKey appKey,
                                final int command,
                                final int length,
                                @NonNull final int[] dataArray,
                                final int tid) {
        super(appKey);

        // Validate length
        if (length < 1 || length > MAX_DATA_LENGTH) {
            throw new IllegalArgumentException("Length must be between 1 and " + MAX_DATA_LENGTH);
        }

        validateRange("Command", command, 0, 255);
        validateRange("Transaction ID", tid, 0, 255);

        // Validate data array - always use 8 elements internally
        if (dataArray == null || dataArray.length != MAX_DATA_LENGTH) {
            throw new IllegalArgumentException("Data array must have exactly " + MAX_DATA_LENGTH + " elements");
        }

        for (int i = 0; i < MAX_DATA_LENGTH; i++) {
            validateRange("Data element " + (i + 1), dataArray[i], 0, 255);
        }

        this.mCommand = command;
        this.mIsLongCommand = true;
        this.mLength = length;
        this.mDataArray = dataArray;
        this.mTid = tid;
        this.mPowerLevel = 0; // Not used in long command

        assembleMessageParameters();
    }

    /**
     * Validation helper method
     */
    private void validateRange(String paramName, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(paramName + " must be between " + min + " and " + max);
        }
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());

        if (mIsLongCommand) {
            // Long command structure: [command, tid, length, data1...dataN]
            // Total bytes: 1 (command) + 1 (tid) + 1 (length) + length (data)
            final int totalLength = 3 + mLength;

            MeshLogger.verbose(TAG, "Assembling Long Command:");
            MeshLogger.verbose(TAG, "  Command: " + mCommand);
            MeshLogger.verbose(TAG, "  TID: " + mTid);
            MeshLogger.verbose(TAG, "  Length: " + mLength);

            final ByteBuffer paramsBuffer = ByteBuffer.allocate(totalLength)
                    .order(ByteOrder.LITTLE_ENDIAN);

            // Add command
            paramsBuffer.put((byte) mCommand);

            // Add transaction ID
            paramsBuffer.put((byte) mTid);

            // Add length
            paramsBuffer.put((byte) mLength);

            // Add data (only first 'length' elements)
            for (int i = 0; i < mLength; i++) {
                MeshLogger.verbose(TAG, "  Data[" + i + "]: " + mDataArray[i]);
                paramsBuffer.put((byte) mDataArray[i]);
            }

            mParameters = paramsBuffer.array();

            MeshLogger.verbose(TAG, "Total parameters length: " + mParameters.length);

        } else {
            // Regular command structure: [command, powerLevel (2 bytes), tid]
            MeshLogger.verbose(TAG, "Assembling Regular Command:");
            MeshLogger.verbose(TAG, "  Command: " + mCommand);
            MeshLogger.verbose(TAG, "  Power Level: " + mPowerLevel);
            MeshLogger.verbose(TAG, "  TID: " + mTid);

            final ByteBuffer buffer = ByteBuffer.allocate(REGULAR_PARAMS_LENGTH)
                    .order(ByteOrder.LITTLE_ENDIAN);

            // Add command (uint8)
            buffer.put((byte) mCommand);

            // Add power level (uint16 - little endian)
            // Note: Using putShort with unsigned value
            buffer.putShort((short) (mPowerLevel & 0xFFFF));

            // Add transaction ID (uint8)
            buffer.put((byte) mTid);

            mParameters = buffer.array();
        }
    }

    // Getters remain the same...
    public int getCommand() { return mCommand; }
    public boolean isLongCommand() { return mIsLongCommand; }
    public int getLength() { return mLength; }
    public int[] getDataArray() { return mDataArray; }
    public int getPowerLevel() { return mPowerLevel; }
    public int getTid() { return mTid; }

    @Override
    public String toString() {
        if (mIsLongCommand) {
            StringBuilder dataStr = new StringBuilder("[");
            for (int i = 0; i < mLength; i++) {
                dataStr.append(mDataArray[i]);
                if (i < mLength - 1) dataStr.append(", ");
            }
            dataStr.append("]");

            return "GenericPowerLevelSet{" +
                    "type=LONG_COMMAND" +
                    ", command=" + mCommand +
                    ", length=" + mLength +
                    ", data=" + dataStr +
                    ", tid=" + mTid +
                    '}';
        } else {
            return "GenericPowerLevelSet{" +
                    "type=REGULAR_COMMAND" +
                    ", command=" + mCommand +
                    ", powerLevel=" + mPowerLevel +
                    ", tid=" + mTid +
                    '}';
        }
    }

    /**
     * Fixed Builder class
     */
    public static class Builder {
        private ApplicationKey appKey;
        private int command = 1;
        private boolean isLongCommand = false;
        private int length = 0;
        private int[] dataArray = null;
        private int powerLevel = 0;
        private int tid = 0;

        public Builder(@NonNull ApplicationKey appKey) {
            this.appKey = appKey;
        }

        public Builder withCommand(int command) {
            this.command = command;
            return this;
        }

        public Builder withPowerLevel(int powerLevel) {
            this.powerLevel = powerLevel;
            return this;
        }

        public Builder withTid(int tid) {
            this.tid = tid;
            return this;
        }

        public Builder asLongCommand(int length, int[] dataArray) {
            this.isLongCommand = true;
            this.length = length;
            // Ensure we always have 8 elements
            this.dataArray = new int[MAX_DATA_LENGTH];
            if (dataArray != null) {
                System.arraycopy(dataArray, 0, this.dataArray, 0,
                        Math.min(length, dataArray.length));
                // Fill remaining with 0
                for (int i = length; i < MAX_DATA_LENGTH; i++) {
                    this.dataArray[i] = 0;
                }
            }
            return this;
        }

        public GenericPowerLevelSet build() {
            if (isLongCommand) {
                return new GenericPowerLevelSet(appKey, command, length, dataArray, tid);
            } else {
                return new GenericPowerLevelSet(appKey, command, powerLevel, tid);
            }
        }
    }
}