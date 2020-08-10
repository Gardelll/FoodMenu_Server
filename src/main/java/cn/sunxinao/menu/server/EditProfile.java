package cn.sunxinao.menu.server;

import cn.sunxinao.menu.proto.ResponseOuterClass;
import cn.sunxinao.menu.server.utils.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;

@WebServlet(name = "EditProfile", urlPatterns = "/edit-profile.php")
public class EditProfile extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getContentType().equals("application/protobuf")) {
            response.sendError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Expect Content-Type: application/protobuf");
            return;
        }
        final var in = request.getInputStream();
        final var editProfileRequest = cn.sunxinao.menu.proto.Request.EditProfileRequest.parseFrom(in);
        final var userId = editProfileRequest.getUserId();
        final var token = editProfileRequest.getToken();
        final int schoolNum = editProfileRequest.getSchoolNum();
        final String nickname = editProfileRequest.getNickName();
        final String phone = editProfileRequest.getPhone();
        final String email = editProfileRequest.getEmail();
        final String avatar = editProfileRequest.getAvatar();
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
            if (token == null || token.isEmpty()) throw new IllegalArgumentException("token is null or empty");
            if (nickname != null && !nickname.isEmpty() && nickname.length() < 3)
                throw new IllegalArgumentException("用户名不得少于三个字符");
            if (email != null && !email.isEmpty() && !email.matches("^(\\w){3,}(\\.\\w+)*@(\\w){2,}((\\.\\w+)+)$"))
                throw new IllegalArgumentException("邮箱格式不正确");
            if (phone != null && !phone.isEmpty() && !phone.matches("^\\d{11}"))
                throw new IllegalArgumentException("电话号码格式错误");
            var statement = DBConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM `users` WHERE `uid` = ? AND `token` = ?");
            statement.setLong(1, userId);
            statement.setString(2, token);
            var resultSet = statement.executeQuery();
            if (!resultSet.first()) {
                responseBuilder.setCode(0xffff)
                        .setMsg("未知错误");
            } else if (resultSet.getInt(1) == 0) {
                responseBuilder.setCode(100)
                        .setMsg("用户名或密码错误");
            } else {
                if (schoolNum != 0) {
                    statement.close();
                    statement = DBConnection.getConnection().prepareStatement("UPDATE `users` SET `users`.`school_num` = ? WHERE `users`.`uid` = ?");
                    statement.setInt(1, schoolNum);
                    statement.setLong(2, userId);
                    statement.execute();
                }
                if (nickname != null && !nickname.isEmpty()) {
                    statement.close();
                    statement = DBConnection.getConnection().prepareStatement("UPDATE `users` SET `users`.`nick_name` = ? WHERE `users`.`uid` = ?");
                    statement.setString(1, nickname);
                    statement.setLong(2, userId);
                    statement.execute();
                }
                if (phone != null && !phone.isEmpty()) {
                    statement.close();
                    statement = DBConnection.getConnection().prepareStatement("UPDATE `users` SET `users`.`phone` = ? WHERE `users`.`uid` = ?");
                    statement.setString(1, phone);
                    statement.setLong(2, userId);
                    statement.execute();
                }
                if (email != null && !email.isEmpty()) {
                    statement.close();
                    statement = DBConnection.getConnection().prepareStatement("UPDATE `users` SET `users`.`email` = ? WHERE `users`.`uid` = ?");
                    statement.setString(1, email);
                    statement.setLong(2, userId);
                    statement.execute();
                }
                if (avatar != null && !avatar.isEmpty()) {
                    statement.close();
                    statement = DBConnection.getConnection().prepareStatement("UPDATE `users` SET `users`.`avatar` = ? WHERE `users`.`uid` = ?");
                    statement.setString(1, avatar);
                    statement.setLong(2, userId);
                    statement.execute();
                }
                statement.execute();
                responseBuilder.setCode(0)
                        .setMsg("Success");
            }
            statement.close();
            resultSet.close();
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            responseBuilder.setCode(100)
                    .setMsg(e.getMessage() == null ? "???" : e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            responseBuilder.setCode(102)
                    .setMsg(e.getMessage());
        } finally {
            var getMenuResponse = responseBuilder.build();
            response.setContentType("application/protobuf");
            getMenuResponse.writeTo(response.getOutputStream());
        }
        in.close();
    }
}
