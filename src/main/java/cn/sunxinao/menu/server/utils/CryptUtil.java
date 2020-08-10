package cn.sunxinao.menu.server.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptUtil {
    /**
     * 获取字符串的摘要(十六进制)
     * 如果传入的值为空，则返回空白字符串
     *
     * @param str 加密内容
     * @return 十六进制哈希
     */
    @NotNull
    public static String getStringDigestHex(@NotNull String algorithm, String str) {
        if (str == null || str.isEmpty()) return "";
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(str.getBytes());
            byte[] dist = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : dist) {
                sb.append(Integer.toHexString((0x000000ff & b) | 0xffffff00).substring(6));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    @NotNull
    public static String getStreamDigestHex(@NotNull String algorithm, InputStream inputStream) {
        try {
            if (inputStream == null || inputStream.available() == 0) return "";
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[4096];
            int n;
            while ((n = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, n);
            }
            byte[] dist = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : dist) {
                sb.append(Integer.toHexString((0x000000ff & b) | 0xffffff00).substring(6));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
