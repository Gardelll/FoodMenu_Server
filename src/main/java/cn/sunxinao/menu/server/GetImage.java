package cn.sunxinao.menu.server;

import cn.sunxinao.menu.server.utils.Settings;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "GetImage", urlPatterns = "/img/*")
public class GetImage extends HttpServlet {
    private final List<String> supportedFormat = List.of(".png", ".jpg", ".jpeg");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String fullFileName = req.getRequestURI();
            String suffix = fullFileName.substring(fullFileName.lastIndexOf('.'));
            if (!supportedFormat.contains(suffix)) throw new ServletException("File not support");
            fullFileName = fullFileName.substring(fullFileName.lastIndexOf('/') + 1);
            String fileName = fullFileName.substring(0, fullFileName.lastIndexOf('.'));
            File filePath = null;
            for (String s : supportedFormat) {
                filePath = new File(Settings.UPLOAD_DIR + "/img/" + fileName + s);
                if (filePath.exists()) break;
                else filePath = null;
            }
            if (filePath == null) throw new FileNotFoundException(fullFileName);
            BufferedImage image = ImageIO.read(filePath);

            // 输出的宽和高
            String h = req.getParameter("h");
            String w = req.getParameter("w");
            int height;
            int width;
            if (h != null && w != null &&
                    (height = Integer.parseInt(h)) != image.getHeight() &&
                    (width = Integer.parseInt(w)) != image.getWidth()) {

                // 缩放后的图片
                BufferedImage resized = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);

                // 原始的宽高比
                double whScale = (double) image.getWidth() / image.getHeight();

                // 缩放后的宽高
                int innerW, innerH;
                if (Math.round(height * whScale) < width) {
                    innerH = height;
                    innerW = (int) Math.round(height * whScale);
                } else {
                    innerW = width;
                    innerH = (int) Math.round(width / whScale);
                }

                int paddingL = Math.abs(width - innerW) / 2; // 左右空白的一半
                int paddingT = Math.abs(height - innerH) / 2; // 上下空白的一半

                //log(String.format("oriW = %d, oriH = %d, w = %d, h = %d, W = %d, H = %d, L = %d, T = %d", image.getWidth(), image.getHeight(), width, height, innerW, innerH, paddingL, paddingT));
                // 获取画布
                Graphics2D graphics = resized.createGraphics();
                var scaledImage = image.getScaledInstance(innerW, innerH, Image.SCALE_SMOOTH);
                graphics.drawImage(scaledImage, paddingL, paddingT, null);

                if (paddingT != 0) {
                    // 取缩放后图片高度的10%作为填充
                    int repeatHeight = (int) (innerH * 0.1);
                    int[] pixels = new int[innerW * repeatHeight];
                    PixelGrabber pg = new PixelGrabber(scaledImage, 0, 0, innerW, repeatHeight, pixels, 0, innerW);
                    pg.grabPixels();
                    doBlur(pixels, innerW, repeatHeight, (int) (innerW * 0.1));
                    BufferedImage part = new BufferedImage(innerW, repeatHeight,
                            BufferedImage.TYPE_INT_ARGB);
                    part.setRGB(0, 0, innerW, repeatHeight, pixels, 0, innerW);
                    graphics.drawImage(part.getScaledInstance(width, paddingT + 1, Image.SCALE_SMOOTH), 0, 0, null);
                    pg = new PixelGrabber(scaledImage, 0, innerH - repeatHeight, innerW, repeatHeight, pixels, 0, innerW);
                    pg.grabPixels();
                    doBlur(pixels, innerW, repeatHeight, (int) (innerW * 0.1));
                    part.setRGB(0, 0, innerW, repeatHeight, pixels, 0, innerW);
                    graphics.drawImage(part.getScaledInstance(width, paddingT + 1, Image.SCALE_SMOOTH), 0, height - paddingT - 1, null);
                }

                if (paddingL != 0) {
                    // 取缩放后图片宽度的10%作为填充
                    int repeatWidth = (int) (innerW * 0.1);
                    int[] pixels = new int[innerH * repeatWidth];
                    PixelGrabber pg = new PixelGrabber(scaledImage, 0, 0, repeatWidth, innerH, pixels, 0, repeatWidth);
                    pg.grabPixels();
                    doBlur(pixels, repeatWidth, innerH, (int) (innerW * 0.1));
                    BufferedImage part = new BufferedImage(repeatWidth, innerH,
                            BufferedImage.TYPE_INT_ARGB);
                    part.setRGB(0, 0, repeatWidth, innerH, pixels, 0, repeatWidth);
                    graphics.drawImage(part.getScaledInstance(paddingL + 1, height, Image.SCALE_SMOOTH), 0, 0, null);
                    pg = new PixelGrabber(scaledImage, innerW - repeatWidth, 0, repeatWidth, innerH, pixels, 0, repeatWidth);
                    pg.grabPixels();
                    doBlur(pixels, repeatWidth, innerH, (int) (innerW * 0.1));
                    part.setRGB(0, 0, repeatWidth, innerH, pixels, 0, repeatWidth);
                    graphics.drawImage(part.getScaledInstance(paddingL + 1, height, Image.SCALE_SMOOTH), width - paddingL - 1, 0, null);
                }

                resp.addDateHeader("Last-Modified", System.currentTimeMillis());

                switch (suffix) {
                    case ".png":
                        resp.setContentType("image/png");
                        ImageIO.write(resized, "PNG", resp.getOutputStream());
                        break;
                    case ".jpg":
                    case ".jpeg":
                    default:
                        image = new BufferedImage(width, height,
                                BufferedImage.TYPE_INT_RGB);
                        image.getGraphics().drawImage(resized, 0, 0, null);
                        resp.setContentType("image/jpeg");
                        ImageIO.write(image, "JPEG", resp.getOutputStream());
                        break;
                }
            } else {
                resp.addDateHeader("Last-Modified", filePath.lastModified());
                resp.addHeader("ETag", fileName);
                long ifModifiedSince = req.getDateHeader("If-Modified-Since");
                String ifNoneMatch = req.getHeader("If-None-Match");
                if (ifModifiedSince >= filePath.lastModified() || fileName.equalsIgnoreCase(ifNoneMatch)) {
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
                // 没有裁剪参数，直接输出
                switch (suffix) {
                    case ".png":
                        resp.setContentType("image/png");
                        ImageIO.write(image, "PNG", resp.getOutputStream());
                        break;
                    case ".jpg":
                    case ".jpeg":
                    default:
                        resp.setContentType("image/jpeg");
                        ImageIO.write(image, "JPEG", resp.getOutputStream());
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            resp.sendError(404);
            //log(e.getMessage());
        } catch (InterruptedException | NumberFormatException | IOException e) {
            resp.sendError(500);
            log(e.getMessage(), e);
        }
    }

    // Copy来的高斯模糊算法，有时间学习一下
    private void doBlur(int[] pix, int w, int h, int radius) {
        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;
        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }
        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;
        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                sir = stack[i + radius];
                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
                        | (dv[gsum] << 8) | dv[bsum];
                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];
                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];
                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];
                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];
                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];
                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];
                yi += w;
            }
        }
    }
}
