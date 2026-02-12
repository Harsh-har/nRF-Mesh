package no.nordicsemi.android.swaromesh.transport;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import no.nordicsemi.android.swaromesh.ApplicationKey;
import no.nordicsemi.android.swaromesh.opcodes.ApplicationMessageOpCodes;
import no.nordicsemi.android.swaromesh.utils.SecureUtils;
import no.nordicsemi.android.swaromesh.logger.MeshLogger;

public class GenericSceneSet extends ApplicationMessage {

    private static final String TAG = GenericSceneSet.class.getSimpleName();
    private static final int OP_CODE =
            ApplicationMessageOpCodes.GENERIC_BUTTON_OPCODE_STATUS;

    private static final int MESSAGE_SIZE = 4;

    public static final int PRESS_SINGLE  = 0;
    public static final int PRESS_DOUBLE  = 1;
    public static final int PRESS_LONG    = 2;
    public static final int PRESS_RELEASE = 3;

    private final int sceneId;
    private final int type;
    private final int press;
    private final int mode;
    private final int device;
    private final int sceneState;
    private final int tid;

    public GenericSceneSet(@NonNull ApplicationKey appKey,
                           int sceneId,
                           int type,
                           int press,
                           int mode,
                           int device,
                           int sceneState,
                           int tid) {

        super(appKey);

        this.sceneId = sceneId;
        this.type = type;
        this.press = press;
        this.mode = mode;
        this.device = device;
        this.sceneState = sceneState;
        this.tid = tid;

        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    protected void assembleMessageParameters() {

        mAid = SecureUtils.calculateK4(mAppKey.getKey());

        ByteBuffer buffer = ByteBuffer
                .allocate(MESSAGE_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.put((byte) sceneId);
        buffer.put((byte) ((type << 4) | press));
        buffer.put((byte) ((mode << 4) | device));
        buffer.put((byte) ((sceneState << 2) | tid));

        mParameters = buffer.array();

        MeshLogger.verbose(TAG,
                "SceneID=" + sceneId +
                        ", Type=" + type +
                        ", Press=" + press +
                        ", Mode=" + mode +
                        ", Device=" + device +
                        ", State=" + sceneState +
                        ", TID=" + tid
        );
    }

    public static int getPressTypeCode(@NonNull String press) {
        switch (press.toLowerCase()) {
            case "single":  return PRESS_SINGLE;
            case "double":  return PRESS_DOUBLE;
            case "long":    return PRESS_LONG;
            case "release": return PRESS_RELEASE;
            default:        return PRESS_SINGLE;
        }
    }
}