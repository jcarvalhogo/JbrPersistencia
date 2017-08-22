/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jcarvalho.jbrpersistencia.conexao;

import com.jcarvalho.jbrpersistencia.Acesso;
import com.jcarvalho.jbrpersistencia.Servidor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author android
 */
public class ServicoConexao {

    public static Connection connection = null;
    private final Object objConf;

    private static boolean statusCon = false;
    private boolean monitorAtivo = true;

    private int intervalo = 3000;

    private String erro = "";

    private static String USUARIO = "";
    private static String SENHA = "";
    private static String CLASSFORNAME = "";
    private static String URL = "";

    public ServicoConexao(Object objConf) {
        this.objConf = objConf;
        autoStartMonitor();
    }

    private void autoStartMonitor() {
        new MonitorConexao().start();
    }

    private boolean setConexao() {
        try {
            setConfigConexao(objConf);
            Class.forName(CLASSFORNAME).newInstance();
            ServicoConexao.connection = DriverManager.getConnection(URL, USUARIO, SENHA);
            return true;
        } catch (IllegalAccessException e) {
            mensagem("Acesso Ilegal: " + e.getMessage(), "Erro de Acesso");
        } catch (InstantiationException e) {
            mensagem("Não foi possivel criar Instância: " + e.getMessage(), "Erro de Instancia");
        } catch (ClassNotFoundException e) {
            mensagem("Driver não Encontrado: " + e.getMessage(), "Erro Driver");
        } catch (SQLException e) {
            mensagem("Não foi passivel a conexão com o banco: " + e, "Erro de Conexao");
        }
        return false;
    }

    public void mensagem(String msg, String titulo) {
        erro = "Erro: " + titulo + " --> " + msg;
        System.out.println(erro);
    }

    private boolean setConfigConexao(Object obj) {
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
                    SENHA = ac.senha();
                    USUARIO = ac.usuario();
                } else if (field.isAnnotationPresent(Servidor.class)) {
                    Servidor ser = field.getAnnotation(Servidor.class);
                    porta = ser.porta();
                    local = ser.local();
                    sgdb = ser.sgdb();
                    CLASSFORNAME = ser.classForName();
                }
            }

            URL = "jdbc:" + sgdb + "://" + local + ":" + porta + "/" + nomeBanco;
            return true;
        } catch (SecurityException e) {
            mensagem("Erro ao Configurar Banco: " + e.getMessage(), "Erro Configuração");
        }
        return false;
    }

    private class MonitorConexao extends Thread {

        @Override
        public void run() {
            while (monitorAtivo) {
                if (!statusCon) {
                    if (setConfigConexao(objConf)) {
                        if (setConexao()) {
                            statusCon = true;
                        }
                    }
                } else {
                    try {
                        connection.createStatement().executeQuery("show databases");
                    } catch (SQLException ex) {
                        statusCon = false;
                        mensagem("Erro Conexão: " + ex.getMessage(), "Queda de Conexão");
                        try {
                            connection.close();
                        } catch (SQLException ex1) {
                            mensagem("Erro: " + ex1.getMessage(), "Erro ao Fechar Conexão");
                        }
                    }
                }
                try {
                    sleep(intervalo);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServicoConexao.class.getName()).log(Level.SEVERE, null, ex);
                    mensagem("Erro: " + ex.getMessage(), "Erro Monitor");
                }
                System.out.println("Estatus Server: " + statusCon);
            }
            monitorAtivo = false;
        }

    }

    //-------------------------------Gets---------------------------------------
    public static Connection getConnection() {
        return connection;
    }

    public static boolean isStatusCon() {
        return statusCon;
    }

    public boolean isMonitorAtivo() {
        return monitorAtivo;
    }

    public int getIntervalo() {
        return intervalo;
    }

    public String getErro() {
        return erro;
    }

    //-------------------------------Sets---------------------------------------
    public static void setStatusCon(boolean statusCon) {
        ServicoConexao.statusCon = statusCon;
    }

    public void setMonitorAtivo(boolean monitorAtivo) {
        this.monitorAtivo = monitorAtivo;
    }

    public void setIntervalo(int intervalo) {
        this.intervalo = intervalo;
    }

    //-------------------------------Metodos Disposiveis------------------------
    public void startMonitor() {
        new MonitorConexao().start();
    }

}
