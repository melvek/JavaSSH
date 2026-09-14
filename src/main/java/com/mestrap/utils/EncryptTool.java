package com.mestrap.utils;

import org.jasypt.util.text.BasicTextEncryptor;

public class EncryptTool {
    public static final String SEC_KEY = "c7624159-ca0f-4078-9dc3-f4cd1da6a9f6";

    public static String encrypt(String val) {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        // Set a master key; do not write this key in the yaml
        encryptor.setPassword(SEC_KEY);
        return encryptor.encrypt(val);
    }

    public static String decrypt(String val) {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        // Set a master key; do not write this key in the yaml
        encryptor.setPassword(SEC_KEY);
        return encryptor.decrypt(val);
    }

    public static void main(String[] args) {
        // System.out.println(encrypt("Whsh@1234"));
        System.out.println(decrypt("Yi8AAYjFZ4rQhvCUY2BRrkup2M8KPV3n"));
    }
}