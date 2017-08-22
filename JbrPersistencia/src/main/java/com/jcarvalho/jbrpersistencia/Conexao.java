package com.jcarvalho.jbrpersistencia;

import java.lang.reflect.Field;
import java.sql.*;

public class Conexao {

    private static Connection connection = null;

    private static String usuario = "";
    private static String senha = "";
    private static String classforName = "";
    private static String url = "";

    public Conexao(Object objConf) {
        setConfigConexao(objConf);
    }

    public static Connection getConectar() {
        try {
            Class.forName(getClassforName()).newInstance();
            connection = DriverManager.getConnection(getUrl(), getUsuario(), getSenha());
        } catch (IllegalAccessException e) {
            
        } catch (InstantiationException e) {
            
        } catch (ClassNotFoundException e) {
            
        } catch (SQLException e) {
            
        }

        return connection;

    }

    public static void desConectar() {
        try {
            connection.close();
        } catch (SQLException e) {
            
        }
    }

    public static String getClassforName() {
        return classforName;
    }

    public static void setClassforName(String classforName) {
        Conexao.classforName = classforName;
    }

    public static String getUrl() {
        return url;
    }

    public static void setUrl(String url) {
        Conexao.url = url;
    }

    private static void setConfigConexao(Object obj) {
        String sgdb = "";
        String local = "";
        String porta = "";
        String nomeBanco = "";

        try {
            Field[] declaredFields = obj.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Acesso.class)) {
                    Acesso ac = field.getAnnotation(Acesso.class);
                    nomeBanco = ac.nomeBanco();
                    setSenha(ac.senha());
                    setUsuario(ac.usuario());
                } else if (field.isAnnotationPresent(Servidor.class)) {
                    Servidor ser = field.getAnnotation(Servidor.class);
                    porta = ser.porta();
                    local = ser.local();
                    sgdb = ser.sgdb();
                    setClassforName(ser.classForName());
                }
            }

            setUrl("jdbc:" + sgdb + "://" + local + ":" + porta + "/" + nomeBanco);
        } catch (SecurityException e) {
            
        }

    }

    public static String getUsuario() {
        return usuario;
    }

    public static void setUsuario(String usuario) {
        Conexao.usuario = usuario;
    }

    public static String getSenha() {
        return senha;
    }

    public static void setSenha(String senha) {
        Conexao.senha = senha;
    }

}
