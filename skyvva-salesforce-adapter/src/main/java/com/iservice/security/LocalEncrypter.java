package com.iservice.security;

import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;

import com.iservice.helper.FileHelper;

public class LocalEncrypter {

   private static String algorithm = "DESede";

   private static Key key = null;

   private static Cipher cipher = null;

   public static void setUp() throws Exception {
      // key = KeyGenerator.getInstance(algorithm).generateKey();
      if (key==null) {
         key = (Key) FileHelper.readObject(LocalEncrypter.class.getResourceAsStream("integration.key"));
      }
      if (cipher==null) {
         cipher = Cipher.getInstance(algorithm);
      }
   }

   public static void main(String[] args) throws Exception {
      setUp();
      /*
       * if (args.length !=1) { System.out.println( "USAGE: java LocalEncrypter " +
       * "[String]"); System.exit(1); }
       */
      byte[] encryptionBytes = null;

      //
      //String input = "00:0c:29:29:b8:20";// args[0];
      String input = "00-0C-29-29-B8-20";// args[0];
      
      //  00:30:48:71:98:C0
      //String input = "00-D0-B7-3C-ED-96";// args[0];00-13-02-55-97-C5

      System.out.println("Entered: " + input);
      encryptionBytes = encrypt(input);

      System.out.println("crypted: " + Base64.getEncoder().encode(encryptionBytes));

      System.out.println("Recovered: " + decrypt(Base64.getEncoder().encode(encryptionBytes)));
   }

   public static byte[] encrypt(String input) throws Exception {
      setUp();
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] inputBytes = input.getBytes();
      return cipher.doFinal(inputBytes);
   }

   public static String decrypt(String encryptionString) throws Exception {
      
      return decrypt(Base64.getDecoder().decode(encryptionString));
   }

   public static String decrypt(byte[] encryptionBytes) throws Exception {
      setUp();
      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] recoveredBytes = cipher.doFinal(encryptionBytes);
      String recovered = new String(recoveredBytes);
      return recovered;
   }
}