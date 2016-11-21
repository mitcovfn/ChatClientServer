package ru.project.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MyWindow extends JFrame {
    private final int PORT = 8189;
    private final String HOST = "localhost";
    private Socket sock = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private JTextField jtf = null;
    private JTextArea jta = null;
    private JPanel upperPanel;
    private JPanel bottomPanel;
    private String nick;

    public MyWindow() {
        setSize(400, 500);
        setLocationRelativeTo(null);
        setTitle("Client");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jta = new JTextArea();
        jta.setEditable(false);
        JScrollPane jsp = new JScrollPane(jta);
        add(jsp);
        bottomPanel = new JPanel(new BorderLayout());
        JButton jbSend = new JButton("Send");
        jtf = new JTextField();
        bottomPanel.add(jbSend, BorderLayout.EAST);
        bottomPanel.add(jtf, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        jbSend.addActionListener(e -> sendMsg());
        jtf.addActionListener(e -> sendMsg());
        jtf.requestFocus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                disconnect();
            }
        });

        upperPanel = new JPanel(new GridLayout(1, 3));
        add(upperPanel, BorderLayout.NORTH);
        JTextField jtfLogin = new JTextField();
        JPasswordField jtfPass = new JPasswordField();
        jtfPass.setEchoChar('•');
        JButton jbAuth = new JButton("Auth");
        upperPanel.add(jtfLogin);
        upperPanel.add(jtfPass);
        upperPanel.add(jbAuth);
        setNick("");

        jbAuth.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect();
                sendAuthMsg(jtfLogin.getText(), jtfPass.getText());
            }
        });

        JMenuBar jmb = new JMenuBar();
        setJMenuBar(jmb);
        JMenu jmHelp = new JMenu("Help");
        jmb.add(jmHelp);
        JMenuItem jmiAbout = new JMenuItem("About...");
        jmHelp.add(jmiAbout);
        JMenuItem jmiCmdList = new JMenuItem("cmdlist");
        jmHelp.add(jmiCmdList);
        jmiCmdList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Main commands:\nChange nick: /changenick *\n/pm nick msg...\nQuit: /end");
            }
        });

        setVisible(true);
    }

    public void sendMsg() {
        String str = jtf.getText();
        try {
            out.writeUTF(str);
            out.flush();
            jtf.setText("");
            jtf.requestFocus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Невозможно отослать сообщение. Проверьте сетевое подключение");
        }
    }

    public void sendAuthMsg(String login, String pass) {
        try {
            out.writeUTF("/auth " + login + " " + pass);
            out.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Невозможно произвести попытку авторизации. Проверьте сетевое подключение");
        }
    }

    public void enableAuthPanel(boolean x) {
        if (x) {
            upperPanel.setVisible(true);
            bottomPanel.setVisible(false);
        } else {
            upperPanel.setVisible(false);
            bottomPanel.setVisible(true);
        }
    }

    public void connect() {
        try {
            if (sock == null) {
                sock = new Socket(HOST, PORT);
                in = new DataInputStream(sock.getInputStream());
                out = new DataOutputStream(sock.getOutputStream());

                new Thread(() -> {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str != null) {
                                if (str.startsWith("/authok")) { // /authok geekbrains
                                    setNick(str.split(" ")[1]);
                                    break;
                                }
                                if (str.startsWith("...")) {
                                    jta.append(str);
                                    jta.append("\n");
                                    jta.setCaretPosition(jta.getDocument().getLength());
                                }
                            }
                        }

                        while (true) {
                            String str = in.readUTF();
                            if (str != null) {
                                if (str.startsWith("/")) {
                                    if (str.startsWith("/nickchanged")) {
                                        String newNick = str.split(" ")[1];
                                        setNick(newNick);
                                    }
                                    if (str.equals("/endsession")) {
                                        setNick("");
                                        break;
                                    }
                                } else {
                                    jta.append(str);
                                    jta.append("\n");
                                    jta.setCaretPosition(jta.getDocument().getLength());
                                }
                            }
                        }
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Обрыв соединения");
                        setNick("");
                    } finally {
                        try {
                            sock.close();
                            sock = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Невозможно подключиться к серверу");
        }
    }

    public void setNick(String nick) {
        this.nick = nick;
        if (!nick.isEmpty()) {
            this.setTitle("Клиент: " + nick);
            enableAuthPanel(false);
        } else {
            this.setTitle("Клиент: не авторизован");
            enableAuthPanel(true);
        }
    }

    public void disconnect() {
        try {
            sock.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Соединение закрыто");
        }
        setNick("");
        enableAuthPanel(true);
    }
}
