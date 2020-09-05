package cn.sunxinao.menu.server;

import cn.sunxinao.menu.proto.ResponseOuterClass;
import cn.sunxinao.menu.server.utils.DBConnection;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "ClickMenu", urlPatterns = "/click")
public class ClickMenu extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
            final var userId = Long.parseLong(request.getParameter("user_id"));
            final var token = request.getParameter("token");
            final long foodId = Long.parseLong(request.getParameter("food_id"));
            if (token == null || token.isEmpty()) throw new IllegalArgumentException("token is null or empty");
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
                statement = DBConnection.getConnection().prepareStatement("SELECT `hot` FROM `menu` WHERE `menu`.food_id = ? LIMIT 1");
                statement.setLong(1, foodId);
                resultSet = statement.executeQuery();
                if (!resultSet.first()) throw new IllegalArgumentException("food_id not found");
                long hot = resultSet.getLong(1);
                statement = DBConnection.getConnection().prepareStatement("UPDATE `menu` SET `hot` = ? WHERE `menu`.`food_id` = ?");
                statement.setLong(1, ++hot);
                statement.setLong(2, foodId);
                if (statement.executeUpdate() != 1) throw new SQLException("更新数据库失败 affected rows != 1");
                responseBuilder.setCode(0)
                        .setMsg("Success");
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
}
