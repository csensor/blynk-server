package cc.blynk.server.hardware.handlers.hardware;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.dao.SessionDao;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.Session;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.enums.Command;
import cc.blynk.server.core.protocol.model.messages.ResponseWithBodyMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.notifications.push.GCMWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.DEVICE_WENT_OFFLINE;
import static cc.blynk.utils.StateHolderUtil.getHardState;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/20/2015.
 *
 * Removes channel from session in case it became inactive (closed from client side).
 */
@ChannelHandler.Sharable
public class HardwareChannelStateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(HardwareChannelStateHandler.class);

    private final SessionDao sessionDao;
    private final BlockingIOProcessor blockingIOProcessor;
    private final GCMWrapper gcmWrapper;

    public HardwareChannelStateHandler(SessionDao sessionDao, BlockingIOProcessor blockingIOProcessor, GCMWrapper gcmWrapper) {
        this.sessionDao = sessionDao;
        this.blockingIOProcessor = blockingIOProcessor;
        this.gcmWrapper = gcmWrapper;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        HardwareStateHolder state = getHardState(ctx.channel());
        if (state != null) {
            Session session = sessionDao.userSession.get(state.user);
            if (session != null) {
                session.removeHardChannel(ctx.channel());
                log.trace("Hardware channel disconnect.");
                sentOfflineMessage(state);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            log.trace("Hardware timeout disconnect.");
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    private void sentOfflineMessage(HardwareStateHolder state) {
        DashBoard dashBoard = state.user.profile.getDashByIdOrThrow(state.dashId);
        if (dashBoard.isActive) {
            Notification notification = dashBoard.getWidgetByType(Notification.class);
            if (notification == null || !notification.notifyWhenOffline) {
                Session session = sessionDao.userSession.get(state.user);
                if (session.getAppChannels().size() > 0) {
                    for (Channel appChannel : session.getAppChannels()) {
                        appChannel.writeAndFlush(
                                new ResponseWithBodyMessage(
                                        0, Command.RESPONSE, DEVICE_WENT_OFFLINE, state.dashId
                                ),
                                appChannel.voidPromise()
                        );
                    }
                }
            } else {
                String boardType = dashBoard.boardType;
                String dashName = dashBoard.name;
                dashName = dashName == null ? "" : dashName;
                blockingIOProcessor.push(gcmWrapper, state.user.name, notification,
                        String.format("Your %s went offline. \"%s\" project is disconnected.", boardType, dashName),
                        state.dashId
                );
            }
        }
    }

}
