package ru.project.server;

import java.sql.*;

public class SQLHandler {
    private static Connection conn;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:users.db");
            stmt = conn.createStatement();
        } catch (Exception e) {
            System.out.println("Невозможно подключиться к БД");
        }
    }

    public static String getNickByLoginPass(String login, String pass) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT nick FROM test WHERE login = '" + login + "' AND pass = '" + pass + "'");
            if (rs.next())
                return rs.getString(1);
            else
                return null;
        } catch (SQLException e) {
            System.out.println("Проблема при проверки логина/пароля > " + login + " / " + pass);
        }
        return null;
    }

    public static boolean tryToChangeNick(String old, String _new) {
        try {
            int w = stmt.executeUpdate("UPDATE test SET nick = '" + _new + "' WHERE nick = '" + old + "';");
            return w == 1;
        } catch (SQLException e) {
            System.out.println("Проблемы с запросом");
        }
        return false;
    }

    public static void disconnect() {
        try {
            conn.close();
            System.out.println("Соединение с БД закрыто");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
