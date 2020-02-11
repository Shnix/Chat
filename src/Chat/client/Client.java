package Chat.client;

import Chat.Connection;
import Chat.ConsoleHelper;
import Chat.Message;
import Chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
            client.run();
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Wait has been interrupted.");
                return;
            }
        }
        if (clientConnected){
            System.out.println( "Соединение установлено.\n Для выхода наберите команду 'exit'.");
            while(clientConnected){
                String data = ConsoleHelper.readString();
                if (data.equals("exit")){
                    break;
                }
                if (shouldSendTextFromConsole()){
                    sendTextMessage(data);
                }
            }
        }
        else {
            System.out.println("Произошла ошибка во время работы клиента.");
        }
    }

    protected String getServerAddress(){
        System.out.println("Введите адрес сервера");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        System.out.println("Введите порт");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        System.out.println("Введите имя");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try{
            connection.send(new Message(MessageType.TEXT,text));
        } catch (IOException e) {
            System.out.println("Отправка не удалась");
            clientConnected = false;
        }
    }

    public class SocketThread extends Thread{

        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " вышел из чата");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while(true){
                Message message = connection.receive();
                if (message.getType() == (MessageType.NAME_REQUEST)){
                    connection.send(new Message(MessageType.USER_NAME,getUserName()));
                }
                else if (message.getType() == (MessageType.NAME_ACCEPTED)){
                    notifyConnectionStatusChanged(true);
                    return;
                }
                else {throw new IOException("Unexpected MessageType");}
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (true){
                Message message = connection.receive();
                if (message.getType() == (MessageType.TEXT)){
                    processIncomingMessage(message.getData());
                }
                else if (message.getType() == (MessageType.USER_ADDED)){
                    informAboutAddingNewUser(message.getData());
                }
                else if (message.getType() == (MessageType.USER_REMOVED)){
                    informAboutDeletingNewUser(message.getData());
                }
                else {throw new IOException("Unexpected MessageType");}
            }
        }

        public void run(){
            try {
                Socket socket = new Socket(getServerAddress(),getServerPort());
                Client.this.connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException ex) {
                notifyConnectionStatusChanged(false);
            }


        }

    }
}
