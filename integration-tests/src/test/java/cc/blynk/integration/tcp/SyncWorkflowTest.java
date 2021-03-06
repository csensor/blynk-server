package cc.blynk.integration.tcp;

import cc.blynk.integration.IntegrationBase;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.server.application.AppServer;
import cc.blynk.server.core.BaseServer;
import cc.blynk.server.core.dao.UserKey;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.controls.Timer;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.sharing.SyncMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.hardware.HardwareServer;
import cc.blynk.utils.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.time.ZoneOffset;
import java.util.List;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.server.core.protocol.enums.Response.*;
import static cc.blynk.server.core.protocol.model.messages.MessageFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncWorkflowTest extends IntegrationBase {

    private BaseServer appServer;
    private BaseServer hardwareServer;
    private ClientPair clientPair;

    @Before
    public void init() throws Exception {
        this.hardwareServer = new HardwareServer(holder).start(transportTypeHolder);
        this.appServer = new AppServer(holder).start(transportTypeHolder);

        this.clientPair = initAppAndHardPair();
    }

    @After
    public void shutdown() {
        this.appServer.close();
        this.hardwareServer.close();
        this.clientPair.stop();
    }


    //todo more tests for that case
    @Test
    public void testHardSyncReturnHardwareCommands() throws Exception {
        clientPair.hardwareClient.send("hardsync");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(8)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
    }

    @Test
    public void testHardSyncReturnNothingNoWidgetOnPin() throws Exception {
        clientPair.hardwareClient.send("hardsync vr 22");
        verify(clientPair.hardwareClient.responseMock, after(400).never()).channelRead(any(), any());
    }

    @Test
    public void testHardSyncReturnValueForNoWidgetOnVirtualPin() throws Exception {
        clientPair.hardwareClient.send("hardware vw 67 100");

        clientPair.hardwareClient.send("hardsync vr 67");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("vw 67 100"))));

        clientPair.hardwareClient.reset();

        clientPair.hardwareClient.send("hardsync");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(9)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 67 100"))));
    }

    @Test
    public void testHardSyncReturnValueForNoWidgetOnAnalogPin() throws Exception {
        clientPair.hardwareClient.send("hardware aw 66 100");

        clientPair.hardwareClient.send("hardsync ar 66");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("aw 66 100"))));

        clientPair.hardwareClient.reset();

        clientPair.hardwareClient.send("hardsync");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(9)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 66 100"))));
    }

    @Test
    public void testHardSyncReturn1HardwareCommand() throws Exception {
        clientPair.hardwareClient.send("hardsync vr 4");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
    }

    @Test
    public void testLCDOnActivateSendsCorrectBodySimpleMode() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"type\":\"LCD\",\"id\":1923810267,\"x\":0,\"y\":6,\"color\":600084223,\"width\":8,\"height\":2,\"tabId\":0,\"" +
                "pins\":[" +
                "{\"pin\":10,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"10\"}," +
                "{\"pin\":11,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023, \"value\":\"11\"}]," +
                "\"advancedMode\":false,\"textLight\":false,\"textLightOn\":false,\"frequency\":1000}");

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.appClient.reset();
        clientPair.appClient.send("activate 1");

        verify(clientPair.appClient.responseMock, timeout(500).times(13)).channelRead(any(), any());

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 10 10"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 11 11"))));


        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 1 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 2 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 3 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 5 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 4 244"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 7 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 30 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 0 89.888037459418"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 1 -58.74774244674501"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 13 60 143 158"))));
    }

    @Test
    public void testLCDOnActivateSendsCorrectBodyAdvancedMode() throws Exception {
        clientPair.appClient.send("createWidget 1\0{\"type\":\"LCD\",\"id\":1923810267,\"x\":0,\"y\":6,\"color\":600084223,\"width\":8,\"height\":2,\"tabId\":0,\"" +
                "pins\":[" +
                "{\"pin\":10,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023}," +
                "{\"pin\":11,\"pinType\":\"VIRTUAL\",\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":1023}]," +
                "\"advancedMode\":true,\"textLight\":false,\"textLightOn\":false,\"frequency\":1000}");

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardware vw 10 p x y 10");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new HardwareMessage(1, b("1 vw 10 p x y 10"))));

        clientPair.appClient.reset();
        clientPair.appClient.send("activate 1");

        verify(clientPair.appClient.responseMock, timeout(500).times(12)).channelRead(any(), any());

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 10 p x y 10"))));


        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 1 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 2 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 3 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 5 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 4 244"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 7 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 30 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 0 89.888037459418"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 1 -58.74774244674501"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 13 60 143 158"))));
    }

    @Test
    public void testHardSyncReturnRTCWithoutTimezone() throws Exception {
        clientPair.hardwareClient.send("hardsync vr 9");

        long expectedTS = System.currentTimeMillis() / 1000;

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(15, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 9")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezone() throws Exception {
        ZoneOffset offset = ZoneOffset.of("+00:00");

        clientPair.appClient.send(("createWidget 1\0{\"type\":\"RTC\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":0,\"height\":0," +
                "\"timezone\":\"TZ\"}").replace("TZ", offset.toString()));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardsync " + b("vr 99"));

        long expectedTS = System.currentTimeMillis() / 1000 + offset.getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(16, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 99")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezonePlus3() throws Exception {
        ZoneOffset offset = ZoneOffset.of("+03:00");

        clientPair.appClient.send(("createWidget 1\0{\"type\":\"RTC\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":0,\"height\":0," +
                "\"timezone\":\"TZ\"}").replace("TZ", offset.toString()));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardsync vr 99");

        long expectedTS = System.currentTimeMillis() / 1000 + offset.getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(16, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 99")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testHardSyncReturnRTCWithUTCTimezoneMinus3() throws Exception {
        ZoneOffset offset = ZoneOffset.of("-03:00");

        clientPair.appClient.send(("createWidget 1\0{\"type\":\"RTC\",\"id\":99, \"pin\":99, \"pinType\":\"VIRTUAL\", " +
                "\"x\":0,\"y\":0,\"width\":0,\"height\":0," +
                "\"timezone\":\"TZ\"}").replace("TZ", offset.toString()));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        clientPair.hardwareClient.send("hardsync vr 99");

        long expectedTS = System.currentTimeMillis() / 1000 + offset.getTotalSeconds();

        ArgumentCaptor<StringMessage> objectArgumentCaptor = ArgumentCaptor.forClass(StringMessage.class);
        verify(clientPair.hardwareClient.responseMock, timeout(500).times(1)).channelRead(any(), objectArgumentCaptor.capture());

        List<StringMessage> arguments = objectArgumentCaptor.getAllValues();
        StringMessage hardMessage = arguments.get(0);
        assertEquals(1, hardMessage.id);
        assertEquals(HARDWARE, hardMessage.command);
        assertEquals(16, hardMessage.length);
        assertTrue(hardMessage.body.startsWith(b("vw 99")));
        String tsString = hardMessage.body.split("\0")[2];
        long ts = Long.valueOf(tsString);

        assertEquals(expectedTS, ts, 2);
    }

    @Test
    public void testSyncForTimer() throws Exception {
        User user = holder.userDao.getUsers().get(new UserKey("dima@mail.ua", "Blynk"));
        Widget widget = user.profile.dashBoards[0].findWidgetByPin((byte) 5, PinType.DIGITAL);
        Timer timer = (Timer) widget;
        timer.value = b("dw 5 100500");

        clientPair.hardwareClient.send("hardsync dr 5");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 100500"))));

        Thread thread = new Thread(() -> {
            timer.value = b("dw 5 200300");
        });

        thread.start();
        thread.join();

        clientPair.hardwareClient.send("hardsync dr 5");
        verify(clientPair.hardwareClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("dw 5 200300"))));

        clientPair.hardwareClient.reset();

        clientPair.hardwareClient.send("hardsync");
        verify(clientPair.hardwareClient.responseMock, timeout(1000).times(8)).channelRead(any(), any());
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 1 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 2 1"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("dw 5 200300"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 3 0"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 4 244"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 7 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("aw 30 3"))));
        verify(clientPair.hardwareClient.responseMock, timeout(100)).channelRead(any(), eq(produce(1, HARDWARE, b("vw 13 60 143 158"))));
    }



    @Test
    public void testTerminalSendsSyncOnActivate() throws Exception {
        clientPair.appClient.send("loadProfileGzipped");
        Profile profile = JsonParser.parseProfile(clientPair.appClient.getBody());
        assertEquals(15, profile.dashBoards[0].widgets.length);

        clientPair.appClient.send("getEnergy");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, GET_ENERGY, "7900")));

        clientPair.appClient.send("createWidget 1\0{\"id\":102, \"x\":5, \"y\":0, \"tabId\":0, \"label\":\"Some Text\", \"type\":\"TERMINAL\", \"pinType\":\"VIRTUAL\", \"pin\":17}");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(3, OK)));

        clientPair.hardwareClient.send("hardware vw 17 a");
        clientPair.hardwareClient.send("hardware vw 17 b");
        clientPair.hardwareClient.send("hardware vw 17 c");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(1, HARDWARE, b("1 vw 17 a"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(2, HARDWARE, b("1 vw 17 b"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(produce(3, HARDWARE, b("1 vw 17 c"))));

        clientPair.appClient.send("deactivate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(4, OK)));

        clientPair.appClient.send("activate 1");
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(5, OK)));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 17 a"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 17 b"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 17 c"))));
    }



    @Test
    public void testActivateAndGetSync() throws Exception {
        clientPair.appClient.send("activate 1");

        verify(clientPair.appClient.responseMock, timeout(500).times(11)).channelRead(any(), any());

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new ResponseMessage(1, OK)));

        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 1 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 2 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 3 0"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 dw 5 1"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 4 244"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 7 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 aw 30 3"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 0 89.888037459418"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 1 -58.74774244674501"))));
        verify(clientPair.appClient.responseMock, timeout(500)).channelRead(any(), eq(new SyncMessage(1111, b("1 vw 13 60 143 158"))));
    }

    @Override
    public String getDataFolder() {
         try {
             return Files.createTempDirectory("blynk_test_", new FileAttribute[0]).toString();
         } catch (IOException e) {
             throw new RuntimeException("Unable create temp dir.", e);
         }
    }
}
