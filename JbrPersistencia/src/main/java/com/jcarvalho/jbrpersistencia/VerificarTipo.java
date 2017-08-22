/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jcarvalho.jbrpersistencia;

import java.lang.reflect.Field;

/**
 *
 * @author jcarvalho
 */
public class VerificarTipo {

    public String getValorField(Field field, Object obj) {
        String r = "";
        String tipo = field.getType().getSimpleName().toLowerCase();
        try {
            field.setAccessible(true);
            Object get = field.get(obj);
            if (get == null) {
                r = "";
            } else {
                if ((tipo.equals("double")) || (tipo.equals("int"))
                        || (tipo.equals("long"))) {
                    r = "" + get.toString();
                } else {
                    r = "'" + get.toString() + "'";
                }
            }
        } catch (IllegalAccessException e) {
            System.out.println("Field na Acessivel: " + e);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro no argumento: " + e);
        } catch (SecurityException e) {
            System.out.println("Erro de Seguraça: " + e);
        }

        return r;
    }

    public String getNomeCampo(Field field) {
        String rnome = "";
        if (field.isAnnotationPresent(Id.class)) {
            Id c = field.getAnnotation(Id.class);
            if (c.nomeCampo().equals("")) {
                rnome = field.getName();
            } else {
                rnome = c.nomeCampo();
            }
        } else if (field.isAnnotationPresent(Coluna.class)) {
            Coluna can = field.getAnnotation(Coluna.class);
            if (can.nome().equals("")) {
                rnome = field.getName();
            } else {
                rnome = can.nome();
            }
        } else if (field.isAnnotationPresent(Foreignkey.class)) {
            Foreignkey foriengKey = field.getAnnotation(Foreignkey.class);
            if (foriengKey.nome().isEmpty()) {
                rnome = field.getName();
            } else {
                rnome = foriengKey.nome();
            }

        }
        return rnome;
    }

    public boolean verificarAuto(Field field) {
        if (field.isAnnotationPresent(Id.class)) {
            Id c = field.getAnnotation(Id.class);
            return c.autoincrement();
        }
        return false;
    }

    public boolean verificarSeCodigo(Field field) {
        boolean auto = false;
        if (field.isAnnotationPresent(Id.class)) {
            Id c = field.getAnnotation(Id.class);
            auto = true;
        }
        return auto;
    }

    public boolean verificarSeDetalhe(Field field) {
        boolean auto = false;
        if (field.isAnnotationPresent(Detalhe.class)) {
            Detalhe d = field.getAnnotation(Detalhe.class);
            auto = true;
        }
        return auto;
    }

    public boolean verificarSeAuto(Field field) {
        boolean auto = false;
        if (field.isAnnotationPresent(Id.class)) {
            Id c = field.getAnnotation(Id.class);
            auto = c.autoincrement();
        }
        return auto;
    }

    public boolean verificarSePk(Field field) {
        boolean auto = false;
        if (field.isAnnotationPresent(Id.class)) {
            Id c = field.getAnnotation(Id.class);
            auto = c.primarykey();
        }
        return auto;
    }

    public boolean verificarSeFK(Field field) {
        boolean r = false;
        if (field.isAnnotationPresent(Foreignkey.class)) {
            r = true;
        }
        return r;
    }

    public boolean verificarSeNega(Field field) {
        boolean auto = false;
        if (field.isAnnotationPresent(Coluna.class)) {
            Coluna campo = field.getAnnotation(Coluna.class);
            auto = campo.negar();
        }
        return auto;
    }

}
