/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jcarvalho.jbrpersistencia.utilitario;

import com.jcarvalho.jbrpersistencia.encryption.Encryption;
import java.awt.Component;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author pc_android
 */
public class Adaptador {

    private final Encryption encryption;

    public Adaptador() {
        this.encryption = new Encryption();
    }

    private String getValor(Object object, String nome) {
        try {
            Field field = object.getClass().getDeclaredField(nome);
            field.setAccessible(true);
            Object obj = field.get(object);
            if (obj == null) {
                return "";
            } else {
                return field.get(object).toString();
            }
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(Adaptador.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    private String removerCondicao(String string) {
        if (string.contains(":pk") || string.contains(":fk")) {
            string = string.substring(0, string.length() - 3);
        }
        return string;
    }

    public void setValores(Object object, JPanel jPanel) {
        Component[] components = jPanel.getComponents();
        for (Component component : components) {
            String nomeC = component.getName();
            if (nomeC != null && !nomeC.isEmpty()) {
                String nome = component.getClass().getSimpleName().toLowerCase();
                switch (nome) {
                    case "jtextfield":
                    case "jformattedtextfield":
                        JTextField jtf = (JTextField) component;
                        jtf.setText(getValor(object, removerCondicao(nomeC)));
                        break;
                    case "jpasswordfield":
                        JPasswordField jpf = (JPasswordField) component;
                        String valro = "" + getValor(object, removerCondicao(nomeC));
                        jpf.setText(encryption.decrypt(valro));
                        break;
                    case "jtextarea":
                        JTextArea jta = (JTextArea) component;
                        jta.setText(getValor(object, removerCondicao(nomeC)));
                        break;
                    case "jcombobox":
                        JComboBox jbx = (JComboBox) component;
                        jbx.setSelectedItem(getValor(object, removerCondicao(nomeC)));
                        break;
                }
            }
        }
    }

    public void setValores(JComboBox jComboBox, final List<?> objects, final String nome) {
        jComboBox.removeAllItems();
        for (Object object : objects) {
            jComboBox.addItem(getValor(object, nome));
        }
    }

    public void setValores(JTable jTable, final List<?> objects, final String[] colunas) {
        DefaultTableModel defaultTableModel = (DefaultTableModel) jTable.getModel();
        defaultTableModel.setNumRows(0);
        try {
            if (objects != null && colunas != null && colunas.length > 0) {
                int itens = objects.size();
                int columnCount = jTable.getColumnCount();
                int totalColunas = colunas.length;
                int linha = 0;
                if (totalColunas == columnCount) {
                    defaultTableModel.setNumRows(itens);
                    for (Object object : objects) {
                        for (int coluna = 0; coluna < columnCount; coluna++) {
                            defaultTableModel.setValueAt(getValor(object, colunas[coluna]), linha, coluna);
                        }
                        linha++;
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(Adaptador.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("Erro: " + e.getMessage());
        }
        jTable.setModel(defaultTableModel);
    }

    public void setValores(JTable jTable, ResultSet resultSet, final String[] colunas) {
        DefaultTableModel defaultTableModel = (DefaultTableModel) jTable.getModel();
        defaultTableModel.setNumRows(0);
        try {
            if (resultSet != null && colunas != null && colunas.length > 0) {
                int itens = 0;
                if (resultSet.first()) {
                    if (resultSet.last()) {
                        itens = resultSet.getRow();
                    }
                    resultSet.first();
                    int columnCount = jTable.getColumnCount();
                    int totalColunas = colunas.length;
                    int linha = 0;
                    if (totalColunas == columnCount) {
                        defaultTableModel.setNumRows(itens);
                        do {
                            for (int coluna = 0; coluna < columnCount; coluna++) {
                                defaultTableModel.setValueAt(resultSet.getObject(colunas[coluna]), linha, coluna);
                            }
                            linha++;
                        } while (resultSet.next());
                    } else {
                        System.out.println("Erro: Quantidade de Coluna do Jtable e diferente do String[] colunas..."
                                + "  JTable = " + columnCount + " String[] colunas = " + totalColunas);
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(Adaptador.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("Erro: " + e.getMessage());
            jTable.setModel(defaultTableModel);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(Adaptador.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        jTable.setModel(defaultTableModel);
    }

}
