package com.bird2fish.birdtalksdk.net;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ClientWebSocketListener extends WebSocketListener {

    private WebSocketClient ws = null;

    public ClientWebSocketListener(WebSocketClient client){
        super();
        this.ws = client;

    }
    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        //LogHelper.d("open websocket");
        Log.d("net", "open websocket");
        Session.INSTANCE.updateState(Session.SessionState.CONNECTED);
        Session.INSTANCE.startHello();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        //LogHelper.d( "Received message: {0}", text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {

        //LogHelper.d( "Received message: {0}", bytes.hex());
        Session.INSTANCE.dispatchMsg(bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        //LogHelper.d( "WebSocket closing: {0} / {1}", new Object[]{code, reason});
        // 判断关闭是否是对方触发
        if (reason != null && !reason.isEmpty()) {
            this.ws.setNotRunning();
            this.ws.attemptReconnect();
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        //LogHelper.d( "WebSocket error: ", t);
        System.out.println("WebSocket error: " + t);
        //this.ws.setNotRunning();
        //this.ws.attemptReconnect();
    }
}
