package com.smartelderly.config;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 开发环境启动时拉一次连接，便于确认数据源配置是否正确。
 */
@Component
@Profile("dev")
@Order(0)
public class DevDatabaseConnectivityCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDatabaseConnectivityCheck.class);

    private final DataSource dataSource;

    public DevDatabaseConnectivityCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            String catalog = connection.getCatalog();
            log.info("[dev] 数据库连接成功: url={}, catalog={}", url, catalog);
        }
    }
}
