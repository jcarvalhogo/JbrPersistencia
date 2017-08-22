/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jcarvalho.jbrpersistencia;

import com.jcarvalho.jbrpersistencia.conexao.ServicoConexao;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jcarvalho
 */
public class Consultas {

    private Statement stm;
    private final VerificarTipo vt;

    private String campos;

    public Consultas() {
        this.vt = new VerificarTipo();
    }

    private void fecharStm() {
        try {
            if (stm != null) {
                stm.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public Date getDataServidor() {
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                ResultSet executeQuery = stm.executeQuery("SELECT current_date() as data_at");
                if (executeQuery.first()) {
                    return executeQuery.getDate("data_at");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
            ServicoConexao.setStatusCon(false);
        } finally {
            fecharStm();
        }
        return null;
    }

    public Time getHoraServidor() {
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                ResultSet executeQuery = stm.executeQuery("SELECT current_time() hora_at");
                if (executeQuery.first()) {
                    return executeQuery.getTime("hora_at");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return null;
    }

    public String getDataHoraServidor() {
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                ResultSet executeQuery = stm.executeQuery("SELECT current_timestamp() as data_hora");
                if (executeQuery.first()) {
                    return executeQuery.getString("data_hora");
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }

        return "";
    }

    private String getNomeTabela(Object obj) {
        String nome = obj.getClass().getSimpleName();
        Tabela tabela = obj.getClass().getAnnotation(Tabela.class);
        if (tabela != null) {
            if (tabela.nome().isEmpty()) {
                return nome;
            } else {
                return tabela.nome();
            }
        } else {
            return "";
        }
    }

    public ResultSet executarSql(String sql) {
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                ResultSet executeQuery = stm.executeQuery(sql);
                if (executeQuery.first()) {
                    return executeQuery;
                } else {
                    return null;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return null;
    }

    public List<?> listarTudo(Object obj, String filtro) {
        ArrayList<Object> lobj = new ArrayList<>();
        Object object;
        String sql = "select " + listarCampos(obj) + " from "
                + getNomeTabela(obj);

        if (filtro != null) {
            sql += " where " + filtro;
        }

        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                ResultSet retorno = stm.executeQuery(sql);
                Field[] fields = obj.getClass().getDeclaredFields();
                while (retorno.next()) {
                    object = new Object();
                    try {
                        object = obj.getClass().newInstance();
                    } catch (InstantiationException | IllegalAccessException ex) {
                        Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    for (Field field : fields) {
                        field.setAccessible(true);
                        try {
                            if (!vt.verificarSeDetalhe(field)) {
                                field.set(object, retorno.getObject(vt.getNomeCampo(field)));
                            }
                        } catch (IllegalArgumentException | IllegalAccessException ex) {
                            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    lobj.add(object);
                }
                return lobj;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return null;
    }

    public Object getObject(Object object, String filtro) {
        String sql = "select " + listarCampos(object) + " from "
                + getNomeTabela(object) + " where " + filtro;
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                ResultSet retorno = stm.executeQuery(sql);
                Field[] fields = object.getClass().getDeclaredFields();
                if (retorno.first()) {
                    for (Field field : fields) {
                        field.setAccessible(true);
                        try {
                            if (!vt.verificarSeDetalhe(field)) {
                                field.set(object, retorno.getObject(vt.getNomeCampo(field)));
                            }
                        } catch (IllegalArgumentException | IllegalAccessException ex) {
                            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                return object;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return object;
    }

    public Object getObjectDetalhe(Object object, String filtro) {
        boolean pegar_pk = false;
        boolean pegar_detalhe = false;

        String id_pk = "0";
        String campo_pk = "";
        Field fieldDetalhe = null;

        String sql = "select " + listarCampos(object) + " from "
                + getNomeTabela(object) + " where " + filtro;
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();

                ResultSet retorno = stm.executeQuery(sql);
                Field[] fields = object.getClass().getDeclaredFields();
                if (retorno.first()) {
                    for (Field field : fields) {
                        field.setAccessible(true);
                        if (!vt.verificarSeDetalhe(field)) {
                            field.set(object, retorno.getObject(vt.getNomeCampo(field)));
                        }
                        if (!pegar_pk && vt.verificarSePk(field)) {
                            campo_pk = vt.getNomeCampo(field);
                            id_pk = field.get(object).toString();
                            pegar_pk = true;
                        }
                        if (!pegar_detalhe && pegar_pk && vt.verificarSeDetalhe(field)) {
                            pegar_detalhe = true;
                            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                            Class<?> detalhe = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                            Field[] declaredFields = detalhe.getDeclaredFields();
                            Field f = declaredFields[0];
                            Object obb = f.getDeclaringClass().newInstance();
                            List<Object> listarTudo = (List<Object>) listarTudo(obb, campo_pk + " = " + id_pk);
                            field.set(object, listarTudo);
                        }

                    }
                }
                return object;
            }
        } catch (SQLException | IllegalArgumentException | IllegalAccessException | InstantiationException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return object;
    }

    public List<?> listarTudoSql(Object obj, String sql) {
        ArrayList<Object> lobj = new ArrayList<>();
        Object object;
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                ResultSet retorno = stm.executeQuery(sql);
                Field[] fields = obj.getClass().getDeclaredFields();
                while (retorno.next()) {
                    object = new Object();
                    try {
                        object = obj.getClass().newInstance();
                    } catch (InstantiationException | IllegalAccessException ex) {
                        Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    for (Field field : fields) {
                        field.setAccessible(true);
                        try {
                            if (!vt.verificarSeDetalhe(field)) {
                                field.set(object, retorno.getObject(vt.getNomeCampo(field)));
                            }
                        } catch (IllegalArgumentException | IllegalAccessException ex) {
                            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    lobj.add(object);
                }
                return lobj;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return null;
    }

    public ResultSet listar_Result(Object obj) {
        String sql = "select " + listarCampos(obj)
                + " from " + getNomeTabela(obj);
        System.out.println("SQL buscar Tudo: " + sql.toLowerCase());
        try {
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement();
                return stm.executeQuery(sql);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return null;
    }

    private String listarCampos(Object obj) {
        String rC = "";
        Field[] fields = obj.getClass().getDeclaredFields();
        int contador = 0;
        int nFields = fields.length;
        for (Field field : fields) {
            field.setAccessible(true);
            if (!vt.verificarSeDetalhe(field)) {
                if (contador > 0 && contador < nFields) {
                    rC += ", ";
                }
            }
            rC += vt.getNomeCampo(field);
            contador++;
        }
        return rC;
    }

    public ResultSet joinOnTabela(String filtro, Object... tabelas) {
        int contador = 0;
        int max = tabelas.length;

        String sql = "select";
        String n_campos = "";
        String join_on = "";
        String from = " from ";
        String abrev = "";

        String campos_ref = "";

        for (Object object : tabelas) {
            contador++;
            Tabela tabela = object.getClass().getAnnotation(Tabela.class);
            if (tabela != null) {
                abrev = tabela.abrev();
                String[] r_campos = tabela.r_campos();
                for (String campo : r_campos) {
                    n_campos += " " + abrev + "." + campo + ",";
                }
            }

            if (contador == 1) {
                Field[] fields_ant = object.getClass().getDeclaredFields();
                for (Field field : fields_ant) {
                    if (vt.verificarSePk(field)) {
                        join_on += getNomeTabela(object) + " " + abrev + " join ";
                        campos_ref = abrev + "." + vt.getNomeCampo(field);
                        break;
                    }
                }
            } else if (contador >= 2) {
                Field[] fields = object.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (vt.verificarSeFK(field)) {
                        join_on += getNomeTabela(object) + " " + abrev + " on " + campos_ref + " = " + abrev + "." + vt.getNomeCampo(field);
                    }
                }
                if (contador <= max) {
                    Field[] fields_ant = object.getClass().getDeclaredFields();
                    for (Field field : fields_ant) {
                        if (vt.verificarSePk(field)) {
                            join_on += " join ";
                            campos_ref = abrev + "." + vt.getNomeCampo(field);
                            break;
                        }
                    }
                }
            }
        }

        if (n_campos.length() > 0) {
            n_campos = n_campos.substring(0, (n_campos.length() - 1));
        }

        if (join_on.length() > 0) {
            join_on = join_on.substring(0, (join_on.length() - 6));
        }

        sql += n_campos + from + join_on;

        if (filtro != null) {
            sql += " where " + filtro;
        }

        try {
            System.out.println("Sql = " + sql);
            if (ServicoConexao.isStatusCon()) {
                stm = ServicoConexao.getConnection().createStatement(ResultSet.CONCUR_READ_ONLY, ResultSet.TYPE_FORWARD_ONLY);
                ResultSet executeQuery = stm.executeQuery(sql);

                if (executeQuery != null) {
                    executeQuery.first();
                    return executeQuery;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Consultas.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            fecharStm();
        }
        return null;
    }

}
