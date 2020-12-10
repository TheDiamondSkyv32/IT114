package client;
public interface Event {
    void onClientConnect(String clientName, String message);

    void onClientDisconnect(String clientName, String message);

    void onGetRoom(String roomname);

    void onMessageReceive(String clientName, String message);

    void onChangeRoom();
}
