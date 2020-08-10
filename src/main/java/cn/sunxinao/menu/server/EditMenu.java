package cn.sunxinao.menu.server;

import cn.sunxinao.menu.proto.ResponseOuterClass;
import cn.sunxinao.menu.server.utils.DBConnection;
import cn.sunxinao.menu.server.utils.ThreadUtil;
import org.json.JSONArray;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.stream.IntStream;

@WebServlet(name = "EditMenu", urlPatterns = "/edit-menu.php")
public class EditMenu extends HttpServlet {

    private final Object databaseLock = new Object();

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getContentType().equals("application/protobuf")) {
            response.sendError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Expect Content-Type: application/protobuf");
            return;
        }
        final var in = request.getInputStream();
        final var editMenuRequest = cn.sunxinao.menu.proto.Request.EditMenuRequest.parseFrom(in);
        final var userId = editMenuRequest.getUserId();
        final var token = editMenuRequest.getToken();
        final long foodId = editMenuRequest.getFoodId();
        final int windowId = editMenuRequest.getWindowId();
        final String name = editMenuRequest.getName();
        final String price = editMenuRequest.getPrice();
        final String foodTime = editMenuRequest.getFoodTime();
        final String imgUrl = editMenuRequest.getImgUrl();
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
            if (token == null || token.isEmpty()) throw new IllegalArgumentException("token is null or empty");
            if (name == null || name.isEmpty()) throw new IllegalArgumentException("name can not be empty");
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
            } else if ((foodId == 0) && (price == null || price.isEmpty())) {
                throw new IllegalArgumentException("food price can not be null while adding a new item");
            } else {
                statement.close();
                statement = DBConnection.getConnection().prepareStatement("INSERT INTO `menu_edit` (`edit_id`, `to_food_id`, `window_id`, `name`, `price`, `food_time`, `img_url`, `time`) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?)");
                statement.setLong(1, foodId);
                statement.setInt(2, windowId);
                statement.setString(3, name);
                statement.setString(4, price);
                statement.setString(5, foodTime);
                statement.setString(6, imgUrl);
                statement.setLong(7, System.currentTimeMillis());
                statement.execute();
                responseBuilder.setCode(0)
                        .setMsg("Success");

                ThreadUtil.getPool().submit(() -> {
                    synchronized (databaseLock) {
                        if (foodId != 0L) {
                            // 更新
                            try (var query = DBConnection.getConnection().prepareStatement("SELECT `img_url` FROM `menu_edit` WHERE `menu_edit`.`to_food_id` = ? ")) {
                                query.setLong(1, foodId);
                                var result = query.executeQuery();
                                final var imageUrlList = new JSONArray();
                                while (result.next()) {
                                    final var str = result.getString("img_url");
                                    if (str == null || str.isEmpty()) continue;
                                    final var imgUrlListPart = new JSONArray(str);
                                    IntStream.range(0, imgUrlListPart.length()).mapToObj(imgUrlListPart::getString).forEach(imageUrlList::put);
                                }
                                try (var query2 = DBConnection.getConnection().prepareStatement("UPDATE `menu` SET `window_id` = ?, `name` = ?, `price` = ?, `food_time` = ?, `img_url` = ? WHERE `menu`.`food_id` = ?")) {
                                    query2.setInt(1, windowId);
                                    query2.setString(2, name);
                                    query2.setString(3, price);
                                    query2.setString(4, foodTime);
                                    query2.setString(5, imageUrlList.toString());
                                    query2.setLong(6, foodId);
                                    query2.execute();
                                }
                                result.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                                log("更新数据库失败", e);
                            }
                        } else {
                            // 添加
                            try (var query = DBConnection.getConnection().prepareStatement("INSERT INTO `menu` (`food_id`, `window_id`, `name`, `price`, `food_time`, `img_url`) VALUES (NULL, ?, ?, ?, ?, ?)")) {
                                query.setInt(1, windowId);
                                query.setString(2, name);
                                query.setString(3, price);
                                query.setString(4, foodTime);
                                query.setString(5, imgUrl);
                                query.setLong(6, System.currentTimeMillis());
                                query.execute();
                            } catch (SQLException e) {
                                e.printStackTrace();
                                log("添加数据库失败", e);
                            }
                        }
                    }
                });
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
        in.close();
    }
}
