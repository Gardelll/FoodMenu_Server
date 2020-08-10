package cn.sunxinao.menu.server;

import cn.sunxinao.menu.proto.Request;
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
import java.util.Objects;

@WebServlet(name = "Register", urlPatterns = "/register.php")
public class Register extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getContentType().equals("application/protobuf")) {
            log(request.getContentType());
            response.sendError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Expect Content-Type: application/protobuf");
            return;
        }

        final var in = request.getInputStream();
        final var registerRequest = Request.RegisterRequest.parseFrom(in);
        final var userName = registerRequest.getUserName();
        final var token = CryptUtil.getStringDigestHex("MD5", AESUtil.decrypt(registerRequest.getUserPass(), AESUtil.AES_SECRET));
        final long expire = System.currentTimeMillis() + 2592000000L;
        final var schoolNum = registerRequest.getSchoolNum();
        final var nickName = registerRequest.getNickName();
        final var phone = registerRequest.getPhone();
        final var email = registerRequest.getEmail();
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
            if (userName == null || userName.isEmpty() || token.isEmpty())
                throw new IllegalArgumentException("用户名或密码为空");
            if (userName.length() < 3) throw new IllegalArgumentException("用户名不得少于三个字符");
            if (Objects.requireNonNull(AESUtil.decrypt(registerRequest.getUserPass(), AESUtil.AES_SECRET)).length() < 6)
                throw new IllegalArgumentException("密码不得少于六个字符");
            if (email == null || email.isEmpty() || !email.matches("^(\\w){3,}(\\.\\w+)*@(\\w){2,}((\\.\\w+)+)$"))
                throw new IllegalArgumentException("邮箱格式不正确");
            if (phone == null || phone.isEmpty() || !phone.matches("^\\d{11}"))
                throw new IllegalArgumentException("电话号码格式错误");

            var statement = DBConnection.getConnection().prepareStatement("SELECT COUNT(`uid`) FROM `users` WHERE `user_name` = ?");
            statement.setString(1, userName);
            var resultSet = statement.executeQuery();
            if (resultSet.first() && resultSet.getInt(1) > 0) {
                responseBuilder.setCode(103)
                        .setMsg("用户已存在");
            } else {
                statement = DBConnection.getConnection().prepareStatement("INSERT INTO `users` VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)");
                statement.setString(1, userName);
                statement.setString(2, "******");
                statement.setString(3, token);
                statement.setTimestamp(4, new java.sql.Timestamp(expire));
                statement.setString(5, nickName);
                statement.setInt(6, schoolNum);
                statement.setString(7, phone);
                statement.setInt(8, 0);
                statement.setString(9, email);
                statement.setInt(10, 0);
                statement.execute();
                statement = DBConnection.getConnection().prepareStatement("SELECT `uid` FROM `users` WHERE `user_name` = ? LIMIT 1");
                statement.setString(1, userName);
                resultSet = statement.executeQuery();
                if (resultSet.first()) {
                    responseBuilder.setCode(0)
                            .setMsg("注册成功")
                            .setDetails(Any.pack(ResponseOuterClass.Response.LoginResponse.newBuilder()
                                    .setUserId(resultSet.getInt(1))
                                    .setUserName(userName)
                                    .setToken(token)
                                    .setEmail(email)
                                    .setExpire(expire)
                                    .setNickName(nickName)
                                    .setPhone(phone)
                                    .setSchoolNum(schoolNum)
                                    .setProfilePhoto("")
                                    .build()));
                } else {
                    responseBuilder.setCode(102)
                            .setMsg("Unknown ERROR");
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
            var pbResponse = responseBuilder.build();
            response.setContentType("application/protobuf");
            pbResponse.writeTo(response.getOutputStream());
        }
        in.close();
    }
}
