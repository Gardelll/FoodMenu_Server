package cn.sunxinao.menu.server.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.jetbrains.annotations.NotNull;

public class AESUtil {
    public final static byte[] AES_SECRET = {-1, -127, 109, -6, 27, -103, 51, -126, -29, -16, 126, 44, -47, -124, 14, -111};
    private static SecretKeySpec secretKey;

    private static void setKey(byte[] myKey) {
        secretKey = new SecretKeySpec(myKey, "AES");
    }

    public static String encrypt(@NotNull String strToEncrypt, @NotNull byte[] secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(@NotNull String strToDecrypt, @NotNull byte[] secret) {
        try {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
