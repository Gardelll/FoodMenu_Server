package cn.sunxinao.menu.server.utils;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import static cn.sunxinao.menu.server.utils.Settings.DB_PROPERTIES;

public class DBConnection {

    private static final DataSource dataSource;

    private static Connection connection = null;

    static {
        try {
            var pooledDataSource = new ComboPooledDataSource();
            pooledDataSource.setDriverClass(DB_PROPERTIES.getProperty("c3p0.driverClass"));
            pooledDataSource.setJdbcUrl(DB_PROPERTIES.getProperty("c3p0.jdbcUrl"));
            pooledDataSource.setUser(DB_PROPERTIES.getProperty("user"));
            pooledDataSource.setPassword(DB_PROPERTIES.getProperty("password"));
            pooledDataSource.setMinPoolSize(5);
            pooledDataSource.setAcquireIncrement(5);
            pooledDataSource.setMaxPoolSize(20);
            pooledDataSource.setProperties(DB_PROPERTIES);
            dataSource = pooledDataSource;
        } catch (PropertyVetoException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getConnection() {
        try {
            if (connection.isClosed()) {
                connection = dataSource.getConnection();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }
}
