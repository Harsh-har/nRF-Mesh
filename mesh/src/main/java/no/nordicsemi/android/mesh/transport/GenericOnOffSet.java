package no.nordicsemi.android.mesh.transport;

import no.nordicsemi.android.mesh.logger.MeshLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import no.nordicsemi.android.mesh.ApplicationKey;
import no.nordicsemi.android.mesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.mesh.utils.SecureUtils;

/**
 * To be used as a wrapper class when creating a GenericOnOffSet message.
 */
@SuppressWarnings("unused")
public class GenericOnOffSet extends ApplicationMessage {

    private static final String TAG = GenericOnOffSet.class.getSimpleName();
    private static final int OP_CODE = ApplicationMessageOpCodes.GENERIC_ON_OFF_SET;
    private static final int GENERIC_ON_OFF_SET_PARAMS_LENGTH = 4;

    private final int mCommand;
    private final int mTid;
    private final int mState;

    /**
     * Constructs GenericOnOffSet message with 4 integer parameters (legacy constructor for backward compatibility).
     *
     * @param appKey {@link ApplicationKey} key for this message
     * @param state  Boolean state of the GenericOnOffModel
     * @param tId    Transaction id
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public GenericOnOffSet(@NonNull final ApplicationKey appKey,
                           final boolean state,
                           final int tId) throws IllegalArgumentException {
        this(appKey, 1, tId, state ? 1 : 0);
    }

    /**
     * Constructs GenericOnOffSet message with 4 integer parameters (legacy constructor for backward compatibility).
     *
     * @param appKey               {@link ApplicationKey} key for this message
     * @param state                Boolean state of the GenericOnOffModel
     * @param tId                  Transaction id
     * @param transitionSteps      Transition steps for the level (ignored in new implementation)
     * @param transitionResolution Transition resolution for the level (ignored in new implementation)
     * @param delay                Delay for this message to be executed 0 - 1275 milliseconds (ignored in new implementation)
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public GenericOnOffSet(@NonNull final ApplicationKey appKey,
                           final boolean state,
                           final int tId,
                           @Nullable final Integer transitionSteps,
                           @Nullable final Integer transitionResolution,
                           @Nullable final Integer delay) {
        this(appKey, 1, tId, state ? 1 : 0);
        // Log warning about ignored parameters
        if (transitionSteps != null || transitionResolution != null || delay != null) {
            MeshLogger.warn(TAG, "Transition parameters are ignored in new implementation");
        }
    }

    /**
     * New constructor with 4 integer parameters.
     *
     * @param appKey   {@link ApplicationKey} key for this message
     * @param command  Command type (0-255)
     * @param tId      Transaction id (0-255)
     * @param state    State (0-255)
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public GenericOnOffSet(@NonNull final ApplicationKey appKey,
                           final int command,
                           final int tId,
                           final int state) throws IllegalArgumentException {
        super(appKey);

        // Validate parameter ranges
        if (command < 0 || command > 255) {
            throw new IllegalArgumentException("Command must be between 0 and 255");
        }
        if (tId < 0 || tId > 255) {
            throw new IllegalArgumentException("Transaction ID must be between 0 and 255");
        }
        if (state < 0 || state > 255) {
            throw new IllegalArgumentException("State must be between 0 and 255");
        }

        this.mCommand = command;
        this.mTid = tId;
        this.mState = state;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Gets the command value.
     * @return Command as integer (0-255)
     */
    public int getCommand() {
        return mCommand;
    }

    /**
     * Gets the transaction ID.
     * @return Transaction ID as integer (0-255)
     */
    public int getTid() {
        return mTid;
    }

    /**
     * Gets the state value.
     * @return State as integer (0-255)
     */
    public int getState() {
        return mState;
    }

    @Override
    void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());

        final ByteBuffer paramsBuffer = ByteBuffer.allocate(GENERIC_ON_OFF_SET_PARAMS_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);

        MeshLogger.verbose(TAG, "Command: " + mCommand);
        MeshLogger.verbose(TAG, "Transaction ID: " + mTid);
        MeshLogger.verbose(TAG, "State: " + mState);

        // Add all parameters as bytes
        paramsBuffer.put((byte) mCommand);
        paramsBuffer.put((byte) mTid);
        paramsBuffer.put((byte) mState);

        mParameters = paramsBuffer.array();
    }

    @Override
    public String toString() {
        return "GenericOnOffSet{" +
                "command=" + mCommand +
                ", tid=" + mTid +
                ", state=" + mState +
                '}';
    }
}