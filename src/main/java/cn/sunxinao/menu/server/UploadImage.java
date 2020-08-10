package cn.sunxinao.menu.server;

import cn.sunxinao.menu.server.utils.CryptUtil;
import cn.sunxinao.menu.server.utils.Settings;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.image.BufferedImage;
import java.io.*;

@WebServlet(name = "UploadImage", urlPatterns = "/upload-image")
@MultipartConfig
public class UploadImage extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        File savePath = null;
        int code = 0;
        String errorMessage = "";
        try {
            Part part = request.getPart("image");
            BufferedImage image;
            try (var imageStream = part.getInputStream()) {
                image = ImageIO.read(imageStream);
            }
            if (image == null) throw new IOException("Not a image file");
            File tmp = File.createTempFile("image_upload_", ".png");
            if (ImageIO.write(image, "PNG", tmp)) {
                var fis = new FileInputStream(tmp);
                String fileMD5 = CryptUtil.getStreamDigestHex("MD5", fis);
                fis.close();
                savePath = new File(Settings.UPLOAD_DIR + "/img/" + fileMD5 + ".png");
                FileOutputStream outputStream = new FileOutputStream(savePath);
                FileInputStream inputStream = new FileInputStream(tmp);
                inputStream.transferTo(outputStream);
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            }
            if (!tmp.delete()) log("tmp file: `" + tmp.getAbsolutePath() + "' can't delete");
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
            code = 104;
        } finally {
            JSONObject output = new JSONObject();
            output.put("code", code);
            if (code == 0 && savePath != null) {
                StringBuffer requestURL = request.getRequestURL();
                output.put("url", requestURL.substring(0, requestURL.lastIndexOf("/")) + "/img/" + savePath.getName());
            } else {
                output.put("msg", errorMessage);
            }
            response.setContentType("application/json");
            try {
                output.write(response.getWriter());
            } catch (IOException e) {
                log("Error", e);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("Unicode");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=");
        out.println(resp.getCharacterEncoding());
        out.println("\" />");
        out.println("<title>图片上传测试</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<form");
        out.println("action=\"");
        out.println(req.getRequestURI());
        out.println("\" enctype=\"multipart/form-data\" method=\"POST\">");
        out.println("<input type=\"file\" name=\"image\" /> <input");
        out.println("type=\"submit\" value=\"提交\"> <input type=\"reset\" value=\"重置\">");
        out.println("</form>");
        out.println("</body>");
        out.println("</html>");
    }
}
