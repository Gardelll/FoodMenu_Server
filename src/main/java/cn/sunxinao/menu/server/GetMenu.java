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

@WebServlet(name = "GetMenu", urlPatterns = "/get-menu.php")
public class GetMenu extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getContentType().equals("application/protobuf")) {
            response.sendError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Expect Content-Type: application/protobuf");
            return;
        }
        final var in = request.getInputStream();
        final var getMenuRequest = cn.sunxinao.menu.proto.Request.GetMenuRequest.parseFrom(in);
        final var userId = getMenuRequest.getUserId();
        final var token = getMenuRequest.getToken();
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
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
                statement = DBConnection.getConnection().prepareStatement("SELECT * FROM `menu` ORDER BY `menu`.`hot` DESC ");
                resultSet.close();
                resultSet = statement.executeQuery();
                var menuList = ResponseOuterClass.Response.MenuResponse.newBuilder();
                while (resultSet.next()) {
                    var menu = ResponseOuterClass.Response.MenuResponse.Menu.newBuilder();
                    menu.setFoodId(resultSet.getLong("food_id"));
                    menu.setWindowId(resultSet.getInt("window_id"));
                    menu.setName(resultSet.getString("name"));
                    menu.setPrice(resultSet.getString("price"));
                    menu.setFoodTime(resultSet.getString("food_time"));
                    menu.setScore(resultSet.getShort("score"));
                    menu.setHot(resultSet.getLong("hot"));
                    String imgUrl = resultSet.getString("img_url");
                    menu.setImgUrl(imgUrl == null ? "" : imgUrl);
                    menuList.addMenuList(menu);
                }
                responseBuilder.setCode(0)
                        .setMsg("Success")
                        .setDetails(Any.pack(menuList.build()));
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
    }
}
