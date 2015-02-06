package cc.blynk.integration;

import cc.blynk.integration.model.ClientPair;
import cc.blynk.server.Server;
import cc.blynk.server.utils.FileManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static cc.blynk.common.enums.Response.OK;
import static cc.blynk.common.model.messages.MessageFactory.produce;
import static org.mockito.Mockito.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/2/2015.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MainWorkflowTest extends IntegrationBase {

    private Server server;

    @Before
    public void init() throws Exception {
        FileManager fileManager = new FileManager();
        FileUtils.deleteDirectory(fileManager.getDataDir().toFile());

        server = new Server(TEST_PORT);
        new Thread(server).start();

        //wait util server start.
        Thread.sleep(500);
    }

    @After
    public void shutdown() {
        server.stop();
    }

    @Test
    public void testConnectAppAndHardware() throws Exception {
        initAppAndHardPair("localhost", TEST_PORT);
    }

    @Test
    public void testConnectAppAndHardwareAndSendCommands() throws Exception {
        ClientPair clientPair = initAppAndHardPair("localhost", TEST_PORT);



        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            clientPair.appClient.sendNoSleep("hardware 1 1");
        }
        System.out.println("Time : " + (System.currentTimeMillis() - start));

        sleep(500);

        for (int i = 1; i <= 100; i++) {
            verify(clientPair.appClient.responseMock).channelRead(any(), eq(produce(i, OK)));
        }
        verify(clientPair.hardwareClient.responseMock, times(100)).channelRead(any(), any());
    }

}