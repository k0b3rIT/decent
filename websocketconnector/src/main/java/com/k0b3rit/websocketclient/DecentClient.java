package com.k0b3rit.websocketclient;

import com.k0b3rit.websocketclient.model.WSClientMessage;
import org.apache.logging.log4j.util.TriConsumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.function.Consumer;

public class DecentClient extends WebSocketClient {

    private final Consumer<WSClientMessage> messageConsumer;
    private final Consumer<WSClientMessage> controlMessageProcessor;
    private final TriConsumer<Integer, String, Boolean> onClose;
    private final Runnable onOpen;
    private static final Logger LOGGER = LoggerFactory.getLogger(DecentClient.class);

    public DecentClient(URI serverUri, Consumer<WSClientMessage> messageConsumer, Consumer<WSClientMessage> controlMessageProcessor, TriConsumer<Integer, String, Boolean> onClose, Runnable onOpen) {
        super(serverUri);
        this.messageConsumer = messageConsumer;
        this.controlMessageProcessor = controlMessageProcessor;
        this.onClose = onClose;
        this.onOpen = onOpen;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
       this.onOpen.run();
    }

    @Override
    public void onMessage(String message) {
        JSONObject jsonMessage = new JSONObject(message);
        if (isControlMessage(jsonMessage)) {
            controlMessageProcessor.accept(new WSClientMessage(WSClientMessage.Type.MESSAGE, jsonMessage));
        } else {
            messageConsumer.accept(new WSClientMessage(WSClientMessage.Type.MESSAGE, jsonMessage));
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        this.onClose.accept(code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.error("Websocket error: [%s]".formatted(ex.getMessage()), ex);

    }

    private boolean isControlMessage(JSONObject message) {
        return message.has("op");
    }
}
