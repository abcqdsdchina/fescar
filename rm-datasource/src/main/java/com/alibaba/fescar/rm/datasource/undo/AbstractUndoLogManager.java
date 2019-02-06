package com.alibaba.fescar.rm.datasource.undo;

import com.alibaba.fescar.common.util.BlobUtils;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.rm.datasource.ConnectionContext;
import com.alibaba.fescar.rm.datasource.ConnectionProxy;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMetaCache;
import com.alibaba.fescar.rm.datasource.undo.oracle.OracleUndoLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import static com.alibaba.fescar.core.exception.TransactionExceptionCode.BranchRollbackFailed_Retriable;

public abstract class AbstractUndoLogManager implements UndoLogManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleUndoLogManager.class);

    private static String UNDO_LOG_TABLE_NAME = "undo_log";

    private static String DELETE_UNDO_LOG_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME + "\n" +
            "\tWHERE branch_id = ? AND xid = ?";

    private static String SELECT_UNDO_LOG_SQL = "SELECT * FROM " + UNDO_LOG_TABLE_NAME + " WHERE log_status = 0 AND branch_id = ? AND xid = ? FOR UPDATE";

    /**
     * Flush undo logs.
     *
     * @param cp the cp
     * @throws SQLException the sql exception
     */
    public void flushUndoLogs(ConnectionProxy cp) throws SQLException {
        ConnectionContext connectionContext = cp.getContext();
        String xid = connectionContext.getXid();
        long branchID = connectionContext.getBranchId();

        BranchUndoLog branchUndoLog = new BranchUndoLog();
        branchUndoLog.setXid(xid);
        branchUndoLog.setBranchId(branchID);
        branchUndoLog.setSqlUndoLogs(connectionContext.getUndoItems());

        String undoLogContent = UndoLogParserFactory.getInstance().encode(branchUndoLog);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Flushing UNDO LOG: " + undoLogContent);
        }

        PreparedStatement pst = null;
        try {
            pst = cp.getTargetConnection().prepareStatement(getInsertUndoLogSql());
            pst.setLong(1, branchID);
            pst.setString(2, xid);
            pst.setBlob(3, BlobUtils.string2blob(undoLogContent));
            pst.executeUpdate();
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException(e);
            }
        } finally {
            if (pst != null) {
                pst.close();
            }
        }

    }

    /**
     * Undo.
     *
     * @param dataSourceProxy the data source proxy
     * @param xid             the xid
     * @param branchId        the branch id
     * @throws TransactionException the transaction exception
     */
    public void undo(DataSourceProxy dataSourceProxy, String xid, long branchId) throws TransactionException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement selectPST = null;
        try {
            conn = dataSourceProxy.getPlainConnection();

            // The entire undo process should run in a local transaction.
            conn.setAutoCommit(false);

            // Find UNDO LOG
            selectPST = conn.prepareStatement(SELECT_UNDO_LOG_SQL);
            selectPST.setLong(1, branchId);
            selectPST.setString(2, xid);
            rs = selectPST.executeQuery();

            while (rs.next()) {
                Blob b = rs.getBlob("rollback_info");
                String rollbackInfo = StringUtils.blob2string(b);
                BranchUndoLog branchUndoLog = UndoLogParserFactory.getInstance().decode(rollbackInfo);

                for (SQLUndoLog sqlUndoLog : branchUndoLog.getSqlUndoLogs()) {
                    TableMeta tableMeta = TableMetaCache.getTableMeta(dataSourceProxy, sqlUndoLog.getTableName());
                    sqlUndoLog.setTableMeta(tableMeta);
                    AbstractUndoExecutor undoExecutor = UndoExecutorFactory.getUndoExecutor(dataSourceProxy.getDbType(), sqlUndoLog);
                    undoExecutor.executeOn(conn);
                }

            }
            deleteUndoLog(xid, branchId, conn);

            conn.commit();

        } catch (Throwable e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.warn("Failed to close JDBC resource while undo ... ", rollbackEx);
                }
            }
            throw new TransactionException(BranchRollbackFailed_Retriable, String.format("%s/%s", branchId, xid), e);

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (selectPST != null) {
                    selectPST.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException closeEx) {
                LOGGER.warn("Failed to close JDBC resource while undo ... ", closeEx);
            }
        }

    }

    abstract protected String getInsertUndoLogSql();

    /**
     * Delete undo log.
     *
     * @param xid      the xid
     * @param branchId the branch id
     * @param conn     the conn
     * @throws SQLException the sql exception
     */
    public void deleteUndoLog(String xid, long branchId, Connection conn) throws SQLException {
        PreparedStatement deletePST = conn.prepareStatement(DELETE_UNDO_LOG_SQL);
        deletePST.setLong(1, branchId);
        deletePST.setString(2, xid);
        deletePST.executeUpdate();
    }

}
