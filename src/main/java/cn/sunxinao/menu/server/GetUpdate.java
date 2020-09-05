package cn.sunxinao.menu.server;

import cn.sunxinao.menu.proto.ResponseOuterClass;
import cn.sunxinao.menu.server.utils.DBConnection;
import com.google.protobuf.Any;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "GetUpdate", urlPatterns = "/get-update")
public class GetUpdate extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getContentType().equals("application/protobuf")) {
            response.sendError(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Expect Content-Type: application/protobuf");
            return;
        }
        final var in = request.getInputStream();
        final var getUpdateRequest = cn.sunxinao.menu.proto.Request.GetUpdateRequest.parseFrom(in);
        final var userId = getUpdateRequest.getUserId();
        final var token = getUpdateRequest.getToken();
        final var clientVersion = getUpdateRequest.getCurrentVersion();
        final var responseBuilder = ResponseOuterClass.Response.newBuilder();
        try {
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
                statement.close();
                resultSet.close();
                resultSet = DBConnection.getConnection().createStatement().executeQuery("SELECT * FROM `version_info` ORDER BY `version_info`.`update_time` DESC LIMIT 1");
                if (!resultSet.first()) {
                    throw new NoNeedUpdateException("没有数据");
                }
                final var versionCode = resultSet.getInt("version_code");
                if (clientVersion >= versionCode) {
                    throw new NoNeedUpdateException("已是最新版");
                }
                final var updateTime = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss").format(new Date((long) resultSet.getInt("update_time") * 1000L));
                responseBuilder.setCode(0)
                        .setMsg("Success")
                        .setDetails(Any.pack(ResponseOuterClass.Response.GetUpdateResponse.newBuilder()
                                .setUpdateTime(updateTime)
                                .setVersionCode(versionCode)
                                .setVersionString(resultSet.getString("version_string"))
                                .setUpdateSummary(resultSet.getString("update_summary"))
                                .setDownloadUrl(resultSet.getString("download_url"))
                                .build()));
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
            responseBuilder.setCode(100)
                    .setMsg(e.getMessage() == null ? "???" : e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            responseBuilder.setCode(102)
                    .setMsg(e.getMessage());
        } catch (NoNeedUpdateException e) {
            responseBuilder.setCode(103)
                    .setMsg(e.getMessage());
        } finally {
            var getMenuResponse = responseBuilder.build();
            response.setContentType("application/protobuf");
            getMenuResponse.writeTo(response.getOutputStream());
        }
    }

    static class NoNeedUpdateException extends Exception {
        NoNeedUpdateException(String msg) {
            super(msg);
        }
    }
}
