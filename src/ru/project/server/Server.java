package ru.project.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private final int PORT = 8189;
    private ServerSocket serv = null;
    private ArrayList<ClientHandler> list;

    public Server() {
        try {
            list = new ArrayList<>();
            serv = new ServerSocket(PORT);
            SQLHandler.connect();
            System.out.println("Ожидаем клиентов...");
            while (true) {
                Socket sock = serv.accept();
                System.out.println("Подключился новый клиент");
                ClientHandler ch = new ClientHandler(this, sock);
                list.add(ch);
                new Thread(ch).start();
            }
        } catch (IOException e) {
            System.out.println("Проблемы с сервером");
        } finally {
            try {
                serv.close();
                System.out.println("Сервер закрыт");
            } catch (IOException e) {
                System.out.println("Проблемы с закрытием сервера");
            }
        }
    }

    public synchronized void broadcastMsg(String nick, String msg) {
        for (ClientHandler o : list) {
            o.sendMsg(nick + ": " + msg);
        }
    }

    public synchronized void unsubscribe(ClientHandler o) {
        list.remove(o);
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler o : list) {
            if (o.getNick().equals(nick))
                return true;
        }
        return false;
    }

    public void personalMessage(ClientHandler from, String toNick, String msg) {
        for (ClientHandler o : list) {
            if (o.getNick().equals(toNick)) {
                o.sendMsg("from " + from.getNick() + ": " + msg);
                from.sendMsg("to " + toNick + ": " + msg);
                break;
            }
        }
    }
}
