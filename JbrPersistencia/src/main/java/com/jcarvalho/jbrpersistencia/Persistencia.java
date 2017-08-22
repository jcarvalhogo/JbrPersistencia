package com.jcarvalho.jbrpersistencia;

import com.jcarvalho.jbrpersistencia.encryption.Encryption;
import com.jcarvalho.jbrpersistencia.conexao.ServicoConexao;
import java.awt.Component;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Persistencia {

    private final Encryption encryption;

    private Statement stm;
    private final VerificarTipo vrt;

    private String campos = "";
    private String camposFk = "";
    private String valoresFk = "";
    private String valores = "";
    private String camposValores = "";
    private String campoCodValor = "";
    private String campoValorUpdate = "";

    private String nomeCampoPk = "";

    public Persistencia() {
        this.encryption = new Encryption();
        vrt = new VerificarTipo();
    }

    private String getNomeTabela(Object obj) {
        String nome = obj.getClass().getSimpleName();
        Tabela tabela = obj.getClass().getAnnotation(Tabela.class);
        if (tabela.nome().isEmpty()) {
            return nome;
        } else {
            return tabela.nome();
        }
    }

    private void fecharStm() {
        try {
            if ((stm != null) && !stm.isClosed()) {
                stm.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
            ServicoConexao.setStatusCon(false);
        }
    }

    public boolean insertDetalhe(Object obj) {
        boolean retorno = false;
        int id = executarInsert(obj);
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        if (id > 0) {
            for (Field field : declaredFields) {
                if (vrt.verificarSeDetalhe(field)) {
                    field.setAccessible(true);
                    try {
                        List<Object> lista = (List<Object>) field.get(obj);
                        for (Object object : lista) {
                            Field[] fieds = object.getClass().getDeclaredFields();
                            for (Field campo_fk : fieds) {
                                if (vrt.verificarSeFK(campo_fk)) {
                                    try {
                                        campo_fk.setAccessible(true);
                                        campo_fk.set(object, id);
                                        insert(object);
                                        break;
                                    } catch (SecurityException ex) {
                                        Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                        }
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                }
            }
            retorno = true;
        }
        return retorno;
    }

    public boolean deleteDetalhe(Object obj) {
        boolean retorno = false;
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (vrt.verificarSeDetalhe(field)) {
                field.setAccessible(true);
                try {
                    List<Object> lista = (List<Object>) field.get(obj);
                    for (Object object : lista) {
                        delete(object);
                    }
                    retorno = delete(obj);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }
            retorno = true;
        }
        return retorno;
    }

    public boolean updateDetalhe(Object obj) {
        boolean retorno;
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        retorno = update(obj);
        if (retorno) {
            for (Field field : declaredFields) {
                if (vrt.verificarSeDetalhe(field)) {
                    field.setAccessible(true);
                    try {
                        List<Object> lista = (List<Object>) field.get(obj);
                        for (Object object : lista) {
                            update(object);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                }
            }
            retorno = true;
        }
        return retorno;
    }

    public boolean insert(Object obj) {
        return executarInsert(obj) > 0;
    }

    private int executarInsert(Object obj) {
        int re = 0;
        try {
            if (ServicoConexao.isStatusCon()) {
                valores = "";
                campos = "";
                setCamposValoresInsert(obj);
                String sql = "insert into " + getNomeTabela(obj) + "("
                        + campos + ") values(" + valores + ");";

                //System.out.println("SQL: " + sql);
                stm = ServicoConexao.getConnection().createStatement();

                if (stm != null && !stm.isClosed()) {
                    int r = stm.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                    if (r > 0) {
                        ResultSet generatedKeys = stm.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            re = generatedKeys.getInt(1);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro insert: " + e.getMessage());
        } finally {
            fecharStm();
        }
        return re;
    }

    public boolean insertOrUpdateList(List<Object> listaObj) {
        int c = 0;
        for (Object object : listaObj) {
            if (!insertOrUpdate(object)) {
                c++;
                System.out.println("Erro ao inserir item index = " + c);
                return false;
            }
        }
        return true;
    }

    public boolean insertList(List<Object> listaObj) {
        int c = 0;
        for (Object object : listaObj) {
            if (!insert(object)) {
                c++;
                System.out.println("Erro ao inserir item index = " + c);
                return false;
            }
        }
        return true;
    }

    public boolean deleteList(List<Object> listaobj) {
        int c = 0;
        for (Object object : listaobj) {
            if (!delete(object)) {
                c++;
                System.out.println("Erro ao inserir item index" + c);
                return false;
            }
        }
        return true;
    }

    public boolean updateList(List<Object> listaobj) {
        int c = 0;
        for (Object object : listaobj) {
            if (!update(object)) {
                c++;
                System.out.println("Erro ao update no item index" + c);
                return false;
            }
        }
        return true;
    }

    public boolean insertOrUpdate(Object obj) {
        try {
            if (ServicoConexao.isStatusCon()) {
                campos = "";
                valores = "";

                stm = ServicoConexao.getConnection().createStatement();

                if ((!stm.isClosed()) && (stm != null)) {
                    getCampoCodigo(obj);
                    String sql = "select " + campos + " from "
                            + getNomeTabela(obj) + " where "
                            + campos + " = " + valores;
                    ResultSet resultado = stm.executeQuery(sql);
                    if (resultado.first()) {
                        return update(obj);
                    } else {
                        return insert(obj);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro sql: " + e.getMessage());
            return false;
        } finally {
            fecharStm();
        }
        return false;
    }

    public boolean delete(Object obj) {
        try {
            if (ServicoConexao.isStatusCon()) {
                campos = "";
                valores = "";

                stm = ServicoConexao.getConnection().createStatement();
                if ((!stm.isClosed()) && (stm != null)) {
                    getCampoCodigo(obj);
                    String sql = "delete from "
                            + getNomeTabela(obj) + " where "
                            + campos + " = " + valores;
                    return !stm.execute(sql);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro sql: " + e.getMessage());
            return false;
        } finally {
            fecharStm();
        }
        return false;
    }

    public boolean update(Object obj) {
        try {
            if (ServicoConexao.isStatusCon()) {
                campoCodValor = "";
                camposValores = "";

                stm = ServicoConexao.getConnection().createStatement();
                setCamposValoresUpdate(obj);
                String sql = "update "
                        + getNomeTabela(obj) + " set " + camposValores
                        + " where " + campoCodValor;

                //System.out.println("Update SQL: " + sql);
                if ((!stm.isClosed()) && (stm != null)) {
                    return !stm.execute(sql);
                }
            }
        } catch (SQLException e) {
            System.out.println("Erro: " + e.getMessage());
            return false;
        } finally {
            fecharStm();
        }
        return false;
    }

    private void setCamposValoresInsert(Object obj) {
        boolean verAuto = true;
        boolean negou = false;
        int contador = 0;
        campos = "";
        valores = "";
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        //System.out.println("" + declaredFields.length);
        for (Field field : declaredFields) {
            field.setAccessible(true);

            if (negou) {
                negou = false;
            } else if (vrt.verificarSeDetalhe(field)) {
                negou = true;
            } else {
                if ((declaredFields.length > 1) && (contador != 1)) {
                    if ((contador < declaredFields.length) && (contador > 1)) {
                        valores += ",";
                        campos += ",";
                    }
                } else if (declaredFields.length > 1) {
                    valores += ",";
                    campos += ",";
                }
            }

            if (vrt.verificarSeNega(field) || negou) {
                negou = true;
            } else {
                if (verAuto) {
                    if (vrt.verificarSeCodigo(field)) {
                        if (vrt.verificarSeAuto(field)) {
                            verAuto = false;
                            negou = true;
                        } else {
                            campos += vrt.getNomeCampo(field);
                            valores += vrt.getValorField(field, obj);
                            verAuto = false;
                        }
                    } else {
                        campos += vrt.getNomeCampo(field);
                        valores += vrt.getValorField(field, obj);
                    }
                } else {
                    campos += vrt.getNomeCampo(field);
                    valores += vrt.getValorField(field, obj);
                }

            }

            contador++;
        }
    }

    private void setCamposValoresUpdate(Object obj) {
        boolean verAuto = true;
        boolean negou = false;
        int contador = 0;
        campos = "";
        valores = "";
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        //System.out.println("" + declaredFields.length);
        for (Field field : declaredFields) {
            field.setAccessible(true);

            if (negou) {
                negou = false;
            } else if (vrt.verificarSeDetalhe(field)) {
                negou = true;
            } else {
                if ((declaredFields.length > 1) && (contador != 1)) {
                    if ((contador < declaredFields.length) && (contador > 1)) {
                        camposValores += ",";
                    }
                } else if (declaredFields.length > 1) {
                    camposValores += ",";
                }
            }

            if (vrt.verificarSeNega(field) || negou) {
                negou = true;
            } else {
                if (verAuto) {
                    if (vrt.verificarSeCodigo(field)) {
                        campoCodValor += vrt.getNomeCampo(field) + " = ";
                        campoCodValor += vrt.getValorField(field, obj);
                        if (vrt.verificarSeAuto(field)) {
                            verAuto = false;
                            negou = true;
                        } else {
                            camposValores += vrt.getNomeCampo(field) + " = ";
                            camposValores += vrt.getValorField(field, obj);
                            verAuto = false;
                        }
                    } else {
                        camposValores += vrt.getNomeCampo(field) + " = ";
                        camposValores += vrt.getValorField(field, obj);
                    }
                } else {
                    camposValores += vrt.getNomeCampo(field) + " = ";
                    camposValores += vrt.getValorField(field, obj);
                }
            }
            contador++;
        }
    }

    private void setCamposValoresRelacaoSimple(Object obj) {
        boolean negou = false;
        boolean eCodigo = false;
        int contador = 0;
        campos = "";
        valores = "";
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        //System.out.println("" + declaredFields.length);
        for (Field field : declaredFields) {
            field.setAccessible(true);

            if (negou) {
                negou = false;
            } else {
                if ((contador > 0) && (!eCodigo) && !negou) {
                    valores += " and ";
                    campos += ",";
                }
            }

            if (vrt.verificarSeNega(field)) {
                negou = true;
            } else {
                if (!vrt.verificarSeCodigo(field)) {
                    campos += vrt.getNomeCampo(field);
                    valores += vrt.getNomeCampo(field) + " = " + vrt.getValorField(field, obj);
                    eCodigo = false;
                } else {
                    nomeCampoPk = vrt.getNomeCampo(field);
                    if (contador > 1) {
                        campos += vrt.getNomeCampo(field);
                    } else {
                        campos += vrt.getNomeCampo(field) + ",";
                    }
                    eCodigo = true;
                }
            }
            contador++;
        }
    }

//    public boolean executarSql(String sql) {
//        try {
//            if (ServicoConexao.isStatusCon()) {
//                stm = ServicoConexao.getConnection().createStatement();
//                this.stm.execute(sql);
//                return true;
//            }
//        } catch (SQLException ex) {
//            Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            fecharStm();
//        }
//        return false;
//    }
    private void getCampoCodigo(Object obj) {
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        campos = "";
        valores = "";
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if (vrt.verificarSeCodigo(field)) {
                campos = field.getName();
                valores = vrt.getValorField(field, obj);
            }
        }
    }

    private boolean capturarValores(JPanel jp, Object obj, boolean insert) {
        campos = "";
        valores = "";
        String nomeC;
        String conteudo = "";
        int c = 0;
        Component[] components = jp.getComponents();
        Field[] fields = obj.getClass().getDeclaredFields();
        int t = 0;
        for (Field field : fields) {
            field.setAccessible(true);
            for (Component com : components) {
                nomeC = com.getName();
                if (nomeC != null && !nomeC.isEmpty() && !nomeC.contains(":fk")) {
                    if (nomeC.contains(":pk")) {
                        if (vrt.verificarAuto(field)) {
                            break;
                        } else {
                            nomeC = nomeC.substring(0, (nomeC.length() - 3));
                        }
                    }

                    switch (com.getClass().getSimpleName().toLowerCase()) {
                        case "jtextfield":
                            JTextField jt = (JTextField) com;
                            conteudo = jt.getText();
                            t++;
                            break;
                        case "jcombobox":
                            JComboBox jComboBox = (JComboBox) com;
                            conteudo = jComboBox.getSelectedItem().toString();
                            t++;
                            break;
                        case "jpasswordfield":
                            JTextField jt2 = (JTextField) com;
                            conteudo = encryption.encrypt(jt2.getText());
                            t++;
                            break;
                        case "jformattedtextfield":
                            JTextField jt3 = (JTextField) com;
                            conteudo = jt3.getText();
                            t++;
                            break;
                        case "jtextarea":
                            JTextArea jta = (JTextArea) com;
                            conteudo = jta.getText();
                            t++;
                            break;
                    }
                    if (vrt.getNomeCampo(field).equals(nomeC)) {
                        try {
                            if (c > 0 && c < t) {
                                if (insert) {
                                    valores += ", ";
                                    campos += ", ";
                                } else {
                                    campoValorUpdate += ", ";
                                }
                            }
                            String tipo = field.getType().getSimpleName().toLowerCase();
                            if ((tipo.equals("double")) || (tipo.equals("int"))
                                    || (tipo.equals("long")) || (tipo.equals("short"))) {
                                if (insert) {
                                    campos += vrt.getNomeCampo(field);
                                    valores += conteudo;
                                } else {
                                    campoValorUpdate += vrt.getNomeCampo(field) + " = " + conteudo;
                                }
                                c++;
                                break;
                            } else {
                                if (insert) {
                                    campos += vrt.getNomeCampo(field);
                                    valores += "'" + conteudo + "'";
                                } else {
                                    campoValorUpdate += vrt.getNomeCampo(field) + " = '" + conteudo + "'";
                                }
                                c++;
                                break;
                            }
                        } catch (IllegalArgumentException ex) {
                            Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean insertConteiner(JPanel jp, Object obj) {
        if (capturarValores(jp, obj, true)) {
            if (ServicoConexao.isStatusCon()) {
                String sql = "insert into " + getNomeTabela(obj);
                //System.out.println("SQL = " + sql);

                sql += "(" + campos + ") values (" + valores + ");";

                try {
                    stm = ServicoConexao.getConnection().createStatement();
                    if ((!stm.isClosed()) && (stm != null)) {
                        return !stm.execute(sql);
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    fecharStm();
                }
            }
        }
        return false;
    }

    public boolean updateConteiner(JPanel jp, Object obj) {
        campoValorUpdate = "";
        String nome_pk = "";
        String valor_pk = "";
        boolean pk_ok = false;
        Field[] declaredFields = obj.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (vrt.verificarSePk(field)) {
                nome_pk = vrt.getNomeCampo(field);
                valor_pk = vrt.getValorField(field, obj);
                pk_ok = true;
                break;
            }
        }
        if (pk_ok) {
            if (capturarValores(jp, obj, false)) {
                if (ServicoConexao.isStatusCon()) {
                    String sql = "update " + getNomeTabela(obj) + " set "
                            + campoValorUpdate + " where " + nome_pk + " = " + valor_pk;

                    System.out.println("SQL = " + sql);

                    try {
                        stm = ServicoConexao.getConnection().createStatement();
                        if ((!stm.isClosed()) && (stm != null)) {
                            return !stm.execute(sql);
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        fecharStm();
                    }
                }
            }
        }
        return false;
    }

    private boolean getIdMaster(Object object) {
        String retorno = "";
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Id.class)) {
                try {
                    Id id = field.getAnnotation(Id.class);
                    if (id.nomeCampo().isEmpty()) {
                        camposFk += "," + field.getName();
                        valoresFk += "," + field.get(object).toString();
                        return true;
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return false;
    }

    public boolean insertConteiner(JPanel jp, Object master, Object detalhe) {
        camposFk = "";
        valoresFk = "";
        if (capturarValores(jp, detalhe, true)) {
            if (ServicoConexao.isStatusCon()) {
                String sql = "insert into " + getNomeTabela(detalhe);
                if (getIdMaster(master)) {

                    sql += "(" + campos + camposFk + ") "
                            + "values (" + valores + valoresFk + ");";

                    try {

                        stm = ServicoConexao.getConnection().createStatement();
                        if ((!stm.isClosed()) && (stm != null)) {
                            return !stm.execute(sql);
                        }

                    } catch (SQLException ex) {
                        Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        fecharStm();
                    }
                }
            }
        }
        return false;
    }

}
