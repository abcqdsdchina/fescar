/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.rm.datasource.undo;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.rm.datasource.ConnectionProxy;
import com.alibaba.fescar.rm.datasource.DataSourceProxy;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The type Undo log manager.
 */
public interface UndoLogManager {

    /**
     * Flush undo logs.
     *
     * @param cp the cp
     * @throws SQLException the sql exception
     */
    void flushUndoLogs(ConnectionProxy cp) throws SQLException;

    /**
     * Undo.
     *
     * @param dataSourceProxy the data source proxy
     * @param xid             the xid
     * @param branchId        the branch id
     * @throws TransactionException the transaction exception
     */
    void undo(DataSourceProxy dataSourceProxy, String xid, long branchId) throws TransactionException;

    /**
     * Delete undo log.
     *
     * @param xid      the xid
     * @param branchId the branch id
     * @param conn     the conn
     * @throws SQLException the sql exception
     */
    void deleteUndoLog(String xid, long branchId, Connection conn) throws SQLException;
}
