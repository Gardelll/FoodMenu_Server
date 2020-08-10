package cn.sunxinao.menu.server;

import cn.sunxinao.menu.proto.ResponseOuterClass;
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

@WebServlet(name = "Comment", urlPatterns = "/comment.php")
public class Comment extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getContentType().equals("application/protobuf")) {
            response.sendError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Expect Content-Type: application/protobuf");
            return;
        }
        final var in = request.getInputStream();
        final var addCommentRequest = cn.sunxinao.menu.proto.Request.AddCommentRequest.parseFrom(in);
        final var userId = addCommentRequest.getUserId();
        final var token = addCommentRequest.getToken();
        final long foodId = addCommentRequest.getFoodId();
        final int score = addCommentRequest.getScore();
        final String comment = addCommentRequest.getComment();
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
            if (token == null || token.isEmpty()) throw new IllegalArgumentException("token is null or empty");
            if (comment == null || comment.isEmpty()) throw new IllegalArgumentException("comment can not be empty");
            if (foodId == 0) throw new IllegalArgumentException("foodId == 0");
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
                statement.close();
                statement = DBConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM `menu` WHERE `menu`.food_id = ? LIMIT 1");
                statement.setLong(1, foodId);
                resultSet.close();
                resultSet = statement.executeQuery();
                if (!resultSet.first() || resultSet.getInt(1) < 1)
                    throw new IllegalArgumentException("food_id not found");
                statement.close();
                statement = DBConnection.getConnection().prepareStatement("INSERT INTO `comment` (`cid`, `uid`, `to_food_id`, `score`, `comment`, `time`) VALUES (NULL, ?, ?, ?, ?, ?)");
                statement.setLong(1, userId);
                statement.setLong(2, foodId);
                statement.setShort(3, (short) score);
                statement.setString(4, comment);
                statement.setLong(5, System.currentTimeMillis());
                statement.execute();
                responseBuilder.setCode(0)
                        .setMsg("Success");

                statement.close();
                statement = DBConnection.getConnection().prepareStatement("UPDATE `menu` SET `menu`.`score` = " +
                        "( SELECT AVG(`comment`.`score`) AS `aver` FROM `comment` WHERE `comment`.`to_food_id` = ?) WHERE `menu`.`food_id` = ?");
                statement.setLong(1, foodId);
                statement.setLong(2, foodId);
                statement.execute();
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
            var getMenuResponse = responseBuilder.build();
            response.setContentType("application/protobuf");
            getMenuResponse.writeTo(response.getOutputStream());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
            final var userId = Long.parseLong(request.getParameter("user_id"));
            final var token = request.getParameter("token");
            final long foodId = Long.parseLong(request.getParameter("food_id"));
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
                if (foodId != 0) {
                    statement.close();
                    statement = DBConnection.getConnection().prepareStatement("SELECT `comment`.*, `users`.`nick_name`, `users`.`avatar` FROM `comment`, `users` WHERE `comment`.`to_food_id` = ? AND `users`.`uid` = `comment`.`uid`");
                    statement.setLong(1, foodId);
                } else {
                    statement = DBConnection.getConnection().prepareStatement("SELECT `comment`.*, `users`.`nick_name`, `users`.`avatar` FROM `comment`, `users` WHERE `users`.`uid` = `comment`.`uid` ORDER BY `comment`.`time` DESC LIMIT 30");
                }
                resultSet.close();
                resultSet = statement.executeQuery();
                var commentList = ResponseOuterClass.Response.CommentResponse.newBuilder();
                while (resultSet.next()) {
                    var comment = ResponseOuterClass.Response.CommentResponse.Comment.newBuilder()
                            .setCommentId(resultSet.getLong("cid"))
                            .setUserId(resultSet.getLong("uid"))
                            .setNickName(resultSet.getString("nick_name"))
                            .setFoodId(resultSet.getLong("to_food_id"))
                            .setScore(resultSet.getShort("score"))
                            .setComment(resultSet.getString("comment"))
                            .setTime(resultSet.getLong("time"))
                            .setUserProfilePhoto(Optional.ofNullable(resultSet.getString("avatar")).orElse(""));
                    commentList.addCommentList(comment.build());
                }
                responseBuilder.setCode(0)
                        .setMsg("Success")
                        .setDetails(Any.pack(commentList.build()));
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
            var addCommentResponse = responseBuilder.build();
            response.setContentType("application/protobuf");
            addCommentResponse.writeTo(response.getOutputStream());
        }
    }
}
