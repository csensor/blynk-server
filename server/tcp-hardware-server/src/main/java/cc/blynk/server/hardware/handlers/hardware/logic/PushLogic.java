package cc.blynk.server.hardware.handlers.hardware.logic;

import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.widgets.notifications.Notification;
import cc.blynk.server.core.protocol.exceptions.NotificationBodyInvalidException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.HardwareStateHolder;
import cc.blynk.server.hardware.exceptions.NotifNotAuthorizedException;
import cc.blynk.server.notifications.push.GCMWrapper;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Response.NO_ACTIVE_DASHBOARD;
import static cc.blynk.utils.ByteBufUtil.makeResponse;
import static cc.blynk.utils.ByteBufUtil.ok;

/**
 * Handler sends push notifications to Applications. Initiation is on hardware side.
 * Sends both to iOS and Android via Google Cloud Messaging service.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public class PushLogic extends NotificationBase {

    private static final Logger log = LogManager.getLogger(PushLogic.class);

    private final BlockingIOProcessor blockingIOProcessor;
    private final GCMWrapper gcmWrapper;

    public PushLogic(BlockingIOProcessor blockingIOProcessor, GCMWrapper gcmWrapper, long notificationQuotaLimit) {
        super(notificationQuotaLimit);
        this.blockingIOProcessor = blockingIOProcessor;
        this.gcmWrapper = gcmWrapper;
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        if (Notification.isWrongBody(message.body)) {
            throw new NotificationBodyInvalidException();
        }

        DashBoard dash = state.user.profile.getDashByIdOrThrow(state.dashId);

        if (!dash.isActive) {
            log.debug("No active dashboard.");
            ctx.writeAndFlush(makeResponse(message.id, NO_ACTIVE_DASHBOARD), ctx.voidPromise());
            return;
        }

        Notification widget = dash.getWidgetByType(Notification.class);

        if (widget == null || widget.hasNoToken()) {
            throw new NotifNotAuthorizedException("User has no access token provided.");
        }

        checkIfNotificationQuotaLimitIsNotReached();

        log.trace("Sending push for user {}, with message : '{}'.", state.user.name, message.body);
        blockingIOProcessor.push(gcmWrapper, state.user.name, widget, message.body, state.dashId);
        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
