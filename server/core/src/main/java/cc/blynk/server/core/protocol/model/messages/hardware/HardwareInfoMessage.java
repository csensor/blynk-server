package cc.blynk.server.core.protocol.model.messages.hardware;

import cc.blynk.server.core.protocol.model.messages.StringMessage;

import static cc.blynk.server.core.protocol.enums.Command.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class HardwareInfoMessage extends StringMessage {

    public HardwareInfoMessage(int messageId, String body) {
        super(messageId, HARDWARE_INFO, body.length(), body);
    }

    @Override
    public String toString() {
        return "HardwareInfoMessage{" + super.toString() + "}";
    }
}
