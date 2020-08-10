package cn.sunxinao.menu.server.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadUtil {
    private static final ExecutorService pool;

    static {
        pool = Executors.newCachedThreadPool();
    }

    public static ExecutorService getPool() {
        return pool;
    }
}
