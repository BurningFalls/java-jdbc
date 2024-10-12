package com.techcourse.service;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.interface21.dao.DataAccessException;
import com.interface21.jdbc.datasource.DataSourceUtils;
import com.interface21.transaction.support.TransactionSynchronizationManager;
import com.techcourse.config.DataSourceConfig;
import com.techcourse.domain.User;

public class TxUserService implements UserService {

    private final UserService appUserService;
    private final DataSource dataSource;

    public TxUserService(UserService userService) {
        this.appUserService = userService;
        this.dataSource = DataSourceConfig.getInstance();
    }

    @Override
    public User findById(long id) {
        return appUserService.findById(id);
    }

    @Override
    public void save(User user) {
        appUserService.save(user);
    }

    @Override
    public void changePassword(final long id, final String newPassword, final String createdBy) {
        try {
            final Connection conn = DataSourceUtils.getConnection(dataSource);
            conn.setAutoCommit(false);

            changePasswordWithTransaction(id, newPassword, createdBy, conn);
        } catch (SQLException e) {
            throw new DataAccessException("Connection failed", e);
        }
    }

    private void changePasswordWithTransaction(final long id, final String newPassword, final String createBy, final Connection conn) throws SQLException {
        try {
            appUserService.changePassword(id, newPassword, createBy);
            conn.commit();
        } catch (DataAccessException e) {
            handleRollback(conn);
            throw new DataAccessException("Transaction failed and rolled back", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
            TransactionSynchronizationManager.unbindResource(dataSource);
        }
    }

    private void handleRollback(final Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException rollbackException) {
            throw new DataAccessException("Rollback Failed", rollbackException);
        }
    }
}

