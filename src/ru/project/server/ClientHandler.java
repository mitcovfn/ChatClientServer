package ru.project.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket sock;
    private Server owner;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;

    public ClientHandler(Server owner, Socket sock) {
        this.owner = owner;
        this.sock = sock;
        this.nick = "";
        try {
            in = new DataInputStream(sock.getInputStream());
            out = new DataOutputStream(sock.getOutputStream());
        } catch (IOException e) {
            System.out.println("Проблема с созданием обработчиков потоков in и out(неизвестно у кого)");
        }
    }

    public String getNick() {
        return nick;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String str = in.readUTF();
                if (str != null && str.startsWith("/auth")) {
                    String login = str.split(" ")[1];
                    String pass = str.split(" ")[2];
                    String user = SQLHandler.getNickByLoginPass(login, pass);
                    if (user != null) {
                        if (!owner.isNickBusy(user)) {
                            nick = user;
                            sendMsg("/authok " + nick);
                            owner.broadcastMsg("Сервер", nick + " подключился к чату");
                            break;
                        } else sendMsg("...Такой ник уже занят");
                    } else sendMsg("...Неверный логин/пароль");
                }
            }
            while (true) {
                String str = in.readUTF();
                if (str != null) {
                    if (str.startsWith("/")) {
                        if (str.equals("/end")) {
                            sendMsg("Вы вышли из чата");
                            sendMsg("/endsession");
                            break;
                        }
                        if (str.startsWith("/changenick")) {
                            String newNick = str.split(" ")[1];
                            if (newNick.length() > 2 && SQLHandler.tryToChangeNick(nick, newNick)) {
                                sendMsg("/nickchanged " + newNick);
                                owner.broadcastMsg("Сервер", "Пользователь " + nick + " сменил ник на " + newNick);
                                this.nick = newNick;
                            } else {
                                sendMsg("Невозможно поменять ник");
                            }
                        }
                        if (str.startsWith("/pm")) { // /pm geekbrains hello java
                            String sto = str.split(" ")[1];
                            String getmsg = str.substring(sto.length() + 5);
                            owner.personalMessage(this, sto, getmsg);
                            System.out.println("pm from " + this.getNick() + " to " + sto + ": " + getmsg);
                        }
                    } else {
                        System.out.println(nick + ": " + str);
                        owner.broadcastMsg(nick, str);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Обрыв соединения с клиентом");
        } finally {
            try {
                owner.broadcastMsg("Сервер", nick + " вышел из чата");
                owner.unsubscribe(this);
                sock.close();
            } catch (IOException e) {
                System.out.println("Проблема с закрытием сокета");
            }
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            System.out.println("Невозможно отослать сообщение клиенту: " + nick);
        }
    }
}
