/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.gaussdb.util;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

/**
 * MD5-based utility function to obfuscate passwords before network transmission.
 *
 * @author Jeremy Wohl
 */
public class MD5Digest {

    private MD5Digest() {
    }

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Encodes user/password/salt information in the following way: MD5(MD5(password + user) + salt).
     *
     * @param user     The connecting user.
     * @param password The connecting user's password.
     * @param salt     A four-salt sent by the server.
     * @return A 35-byte array, comprising the string "md5" and an MD5 digest.
     */
    public static byte[] encode(byte[] user, byte[] password, byte[] salt) {
        MessageDigest md;
        byte[] temp_digest;
        byte[] pass_digest;
        byte[] hex_digest = new byte[35];

        try {
            md = MessageDigest.getInstance("MD5");

            md.update(password);
            md.update(user);
            temp_digest = md.digest();

            bytesToHex(temp_digest, hex_digest, 0, 16);
            md.update(hex_digest, 0, 32);
            md.update(salt);
            pass_digest = md.digest();

            bytesToHex(pass_digest, hex_digest, 3, 16);
            hex_digest[0] = (byte) 'm';
            hex_digest[1] = (byte) 'd';
            hex_digest[2] = (byte) '5';
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to encode password with MD5", e);
        }

        return hex_digest;
    }

    /*
     * Turn 16-byte stream into a human-readable 32-byte hex string
     */
    private static void bytesToHex(byte[] bytes, byte[] hex, int offset, int length) {
        final char[] lookup =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        int i;
        int c;
        int j;
        int pos = offset;

        for (i = 0; i < length; i++) {
            c = bytes[i] & 0xFF;
            j = c >> 4;
            hex[pos++] = (byte) lookup[j];
            j = (c & 0xF);
            hex[pos++] = (byte) lookup[j];
        }
    }

    public static byte[] SHA256_MD5encode(byte user[], byte password[], byte salt[]) {
        MessageDigest md, sha;
        byte[] temp_digest, pass_digest;
        byte[] hex_digest = new byte[70];
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(password);
            md.update(user);
            temp_digest = md.digest();
            bytesToHex(temp_digest, hex_digest, 0, 16);
            sha = MessageDigest.getInstance("SHA-256");
            sha.update(hex_digest, 0, 32);
            sha.update(salt);
            pass_digest = sha.digest();
            bytesToHex(pass_digest, hex_digest, 6, 32);
            hex_digest[0] = (byte) 's';
            hex_digest[1] = (byte) 'h';
            hex_digest[2] = (byte) 'a';
            hex_digest[3] = (byte) '2';
            hex_digest[4] = (byte) '5';
            hex_digest[5] = (byte) '6';
        } catch (Exception e) {
            throw new IllegalArgumentException("SHA256_MD5encode failed. ", e);
        }
        return hex_digest;
    }

    private static byte[] sha256(byte[] str) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("SHA256_MD5encode failed. ", e);
        }

        if (md == null) {
            return new byte[0];
        }

        md.update(str);
        return md.digest();
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return new byte[0];
        }
        hexString = hexString.toUpperCase(Locale.ENGLISH);
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte[] generateKFromPBKDF2(String password, String random64code, int server_iteration) {
        char[] chars = password.toCharArray();
        byte[] random32code = hexStringToBytes(random64code);
        PBEKeySpec spec = new PBEKeySpec(chars, random32code, server_iteration, 32 * 8);
        SecretKeyFactory skf = null;
        try {
            skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("no algorithm: PBKDF2WithHmacSHA1. ", e);
        }

        byte[] hash = null;
        try {
            hash = skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("mothod 'generateSecret' error. Invalid key. ", e);
        }
        return hash;
    }

    private static byte[] generateKFromPBKDF2(String password, String random64code) {
        return generateKFromPBKDF2(password, random64code, 2048);
    }

    private static byte[] getKeyFromHmac(byte[] key, byte[] data) {
        SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA256_ALGORITHM);
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("no algorithm: HMAC_SHA256_ALGORITHM. ", e);
        }

        try {
            mac.init(signingKey);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("method 'init' error. Invalid key. ", e);
        }
        return mac.doFinal(data);
    }

    private static byte[] XOR_between_password(byte[] password1, byte[] password2, int length) {
        byte[] temp = new byte[length];
        for (int i = 0; i < length; i++) {
            temp[i] = (byte) (password1[i] ^ password2[i]);
        }
        return temp;
    }

    public static byte[] MD5_SHA256encode(String password, String random64code, byte salt[]) {
        MessageDigest md;
        byte[] pass_digest;
        byte[] hex_digest = new byte[35];
        try {
            StringBuilder stringBuilder = new StringBuilder();
            byte[] K = MD5Digest.generateKFromPBKDF2(password, random64code);
            byte[] server_key = MD5Digest.getKeyFromHmac(K, "Sever Key".getBytes("UTF-8"));
            byte[] client_key = MD5Digest.getKeyFromHmac(K, "Client Key".getBytes("UTF-8"));
            byte[] stored_key = MD5Digest.sha256(client_key);
            stringBuilder.append(random64code);
            stringBuilder.append(MD5Digest.bytesToHexString(server_key));
            stringBuilder.append(MD5Digest.bytesToHexString(stored_key));
            String EncryptString = stringBuilder.toString();
            md = MessageDigest.getInstance("MD5");
            md.update(EncryptString.getBytes(StandardCharsets.UTF_8));
            md.update(salt);
            pass_digest = md.digest();
            bytesToHex(pass_digest, hex_digest, 3, 16);
            hex_digest[0] = (byte) 'm';
            hex_digest[1] = (byte) 'd';
            hex_digest[2] = (byte) '5';
        } catch (Exception e) {
            throw new IllegalArgumentException("MD5_SHA256encode failed. ", e);
        }
        return hex_digest;
    }

    public static byte[] RFC5802Algorithm(String password, String random64code, String token) {
        int server_iteration_350 = 2048;
        return RFC5802Algorithm(password, random64code, token, null, server_iteration_350);
    }

    public static byte[] RFC5802Algorithm(
        String password, String random64code, String token, String server_signature, int server_iteration) {
        byte[] hValue = null;
        byte[] result = null;
        try {
            byte[] K = generateKFromPBKDF2(password, random64code, server_iteration);
            byte[] server_key = getKeyFromHmac(K, "Sever Key".getBytes(StandardCharsets.UTF_8));
            byte[] clientKey = getKeyFromHmac(K, "Client Key".getBytes(StandardCharsets.UTF_8));
            byte[] storedKey = sha256(clientKey);
            byte[] tokenbyte = hexStringToBytes(token);
            byte[] client_signature = getKeyFromHmac(server_key, tokenbyte);
          if (server_signature != null && !server_signature.equals(bytesToHexString(client_signature))) {
            return new byte[0];
          }
            byte[] hmac_result = getKeyFromHmac(storedKey, tokenbyte);
            hValue = XOR_between_password(hmac_result, clientKey, clientKey.length);
            result = new byte[hValue.length * 2];
            bytesToHex(hValue, result, 0, hValue.length);
        } catch (Exception e) {
            throw new IllegalArgumentException("RFC5802Algorithm failed. ", e);
        }
        return result;
    }

    public static byte[] RFC5802Algorithm(String password, String random64code, String token, int server_iteration) {
        return RFC5802Algorithm(password, random64code, token, null, server_iteration);
    }

}
