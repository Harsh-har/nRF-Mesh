package no.nordicsemi.android.mesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.logger.MeshLogger;
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.mesh.utils.SecureUtils;

/**
 * Generic On Off Set Unacknowledged with support for both regular and long commands
 *
 * Regular Command Parameters:
 *  - Command (uint8)
 *  - State (uint8)
 *  - TID (uint8)
 *
 * Long Command Parameters:
 *  - Command (uint8)
 *  - TID (uint8)
 *  - Length (uint8)
 *  - Data (uint8 array, size = length)
 */
public class GenericOnOffSetUnacknowledged extends ApplicationMessage {

    private static final String TAG = GenericOnOffSetUnacknowledged.class.getSimpleName();
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_ON_OFF_SET_UNACKNOWLEDGED;
    private static final int REGULAR_PARAMS_LENGTH = 3; // command(1) + state(1) + tid(1)
    private static final int MAX_DATA_LENGTH = 8;

    private final int mCommand;
    private final boolean mIsLongCommand;
    private final int mLength; // For long command: 1-8
    private final int[] mDataArray; // For long command: data array
    private final int mState;
    private final int mTid;

    /**
     * Constructor for regular command
     *
     * @param appKey {@link ApplicationKey} key for this message
     * @param command Command ID (0-255)
     * @param state State value (0-255)
     * @param tid Transaction ID (0-255)
     */
    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
                                         final int command,
                                         final int state,
                                         final int tid) {
        super(appKey);

        validateRange("Command", command, 0, 255);
        validateRange("State", state, 0, 255);
        validateRange("Transaction ID", tid, 0, 255);

        this.mCommand = command;
        this.mIsLongCommand = false;
        this.mLength = 0;
        this.mDataArray = null;
        this.mState = state;
        this.mTid = tid;

        assembleMessageParameters();
    }

    /**
     * Constructor for long command with data array
     *
     * @param appKey {@link ApplicationKey} key for this message
     * @param command Command ID (0-255)
     * @param length Data length (1-8)
     * @param dataArray Data array of specified length, each element 0-255
     * @param tid Transaction ID (0-255)
     */
    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
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
        this.mState = 0; // Not used in long command

        assembleMessageParameters();
    }

    /**
     * Legacy constructor for backward compatibility
     */
    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
                                         final int state,
                                         final int tid) {
        this(appKey, 1, state, tid);
    }

    /**
     * Legacy constructor with transition parameters (ignored for backward compatibility)
     */
    public GenericOnOffSetUnacknowledged(@NonNull final ApplicationKey appKey,
                                         final int state,
                                         final int tid,
                                         final Integer transitionSteps,
                                         final Integer transitionResolution,
                                         final Integer delay) {
        this(appKey, 1, state, tid);
        if (transitionSteps != null || transitionResolution != null || delay != null) {
            MeshLogger.warn(TAG, "Transition parameters are ignored in GenericOnOffSetUnacknowledged");
        }
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
            // Regular command structure: [command, state, tid]
            MeshLogger.verbose(TAG, "Assembling Regular Command:");
            MeshLogger.verbose(TAG, "  Command: " + mCommand);
            MeshLogger.verbose(TAG, "  State: " + mState);
            MeshLogger.verbose(TAG, "  TID: " + mTid);

            final ByteBuffer buffer = ByteBuffer.allocate(REGULAR_PARAMS_LENGTH)
                    .order(ByteOrder.LITTLE_ENDIAN);

            buffer.put((byte) mCommand);
            buffer.put((byte) mState);
            buffer.put((byte) mTid);

            mParameters = buffer.array();
        }
    }

    // Getters
    public int getCommand() { return mCommand; }
    public boolean isLongCommand() { return mIsLongCommand; }
    public int getLength() { return mLength; }
    public int[] getDataArray() { return mDataArray; }
    public int getState() { return mState; }
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

            return "GenericOnOffSetUnacknowledged{" +
                    "type=LONG_COMMAND" +
                    ", command=" + mCommand +
                    ", length=" + mLength +
                    ", data=" + dataStr +
                    ", tid=" + mTid +
                    '}';
        } else {
            return "GenericOnOffSetUnacknowledged{" +
                    "type=REGULAR_COMMAND" +
                    ", command=" + mCommand +
                    ", state=" + mState +
                    ", tid=" + mTid +
                    '}';
        }
    }

    /**
     * Builder class for GenericOnOffSetUnacknowledged to support both regular and long commands.
     */
    public static class Builder {
        private ApplicationKey appKey;
        private int command = 1;
        private boolean isLongCommand = false;
        private int length = 0;
        private int[] dataArray = null;
        private int state = 0;
        private int tid = 0;

        public Builder(@NonNull ApplicationKey appKey) {
            this.appKey = appKey;
        }

        public Builder withCommand(int command) {
            this.command = command;
            return this;
        }

        public Builder withState(int state) {
            this.state = state;
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

        public GenericOnOffSetUnacknowledged build() {
            if (isLongCommand) {
                return new GenericOnOffSetUnacknowledged(appKey, command, length, dataArray, tid);
            } else {
                return new GenericOnOffSetUnacknowledged(appKey, command, state, tid);
            }
        }
    }
}