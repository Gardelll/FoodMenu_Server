package cn.sunxinao.menu.server;

import cn.sunxinao.menu.proto.ResponseOuterClass;
import cn.sunxinao.menu.server.utils.AESUtil;
import cn.sunxinao.menu.server.utils.CryptUtil;
import cn.sunxinao.menu.server.utils.DBConnection;
import com.google.protobuf.Any;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet(name = "Login", urlPatterns = "/login.php")
public class Login extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getContentType().equals("application/protobuf")) {
            getServletContext().log(req.getContentType());
            resp.sendError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Expect Content-Type: application/protobuf");
            return;
        }
        final var in = req.getInputStream();
        final var loginRequest = cn.sunxinao.menu.proto.Request.LoginRequest.parseFrom(in);
        final var userName = loginRequest.getUserName().trim();
        final var userPass = AESUtil.decrypt(loginRequest.getUserPass(), AESUtil.AES_SECRET);
        //log(loginRequest.getUserPass() + "->" + userPass);
        final var token = loginRequest.getToken();
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
            var statement = DBConnection.getConnection().prepareStatement("SELECT * FROM `users` WHERE `user_name` = ? OR `phone` = ? OR `email` = ?");
            statement.setString(1, userName);
            statement.setString(2, userName);
            statement.setString(3, userName);
            var resultSet = statement.executeQuery();
            if (!resultSet.first()) {
                responseBuilder.setCode(101)
                        .setMsg("需要注册");
            } else {
                String tokenInDB = resultSet.getString("token");
                long expire = resultSet.getDate("token_expire").getTime();
                if (tokenInDB.equals(token) || CryptUtil.getStringDigestHex("MD5", userPass).equals(tokenInDB)) {
                    responseBuilder.setCode(0)
                            .setMsg("登录成功")
                            .setDetails(Any.pack(ResponseOuterClass.Response.LoginResponse.newBuilder()
                                    .setUserId(resultSet.getInt("uid"))
                                    .setUserName(userName)
                                    .setToken(tokenInDB)
                                    .setEmail(resultSet.getString("email"))
                                    .setExpire(expire)
                                    .setNickName(resultSet.getString("nick_name"))
                                    .setPhone(resultSet.getString("phone"))
                                    .setSchoolNum(resultSet.getInt("school_num"))
                                    .setProfilePhoto(Optional.ofNullable(resultSet.getString("avatar")).orElse(""))
                                    .build()));
                } else {
                    responseBuilder.setCode(100)
                            .setMsg("密码错误");
                }
            }
            statement.close();
            resultSet.close();
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            responseBuilder.setCode(100)
                    .setMsg(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            responseBuilder.setCode(102)
                    .setMsg(e.getMessage());
        } finally {
            var response = responseBuilder.build();
            resp.setContentType("application/protobuf");
            response.writeTo(resp.getOutputStream());
        }
        in.close();
    }
}
