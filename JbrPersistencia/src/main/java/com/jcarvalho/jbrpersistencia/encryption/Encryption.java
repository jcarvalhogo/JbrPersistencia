/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jcarvalho.jbrpersistencia.encryption;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Android
 */
public class Encryption {

    protected final String key = "#cjld!evr&sop#owet@jug*gtrwkyhprteqpgxt"
            + "b%tyepoajqtlkeptjudtgralyfhardgpoetdphsjqufladgfkdroabasdasd&";

    public String encrypt(String password) {
        String string = password + key;
        try {
            return new String(Base64.encodeBase64(string.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public String decrypt(String password) {
        try {
            String decode = new String(Base64.decodeBase64(password.getBytes("UTF-8")));
            int index = decode.indexOf(key);
            return decode.substring(0, index);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Encryption.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

}
