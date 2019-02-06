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

package com.alibaba.fescar.rm.datasource.undo.mysql;

import com.alibaba.fescar.rm.datasource.undo.AbstractUndoLogManager;
import com.alibaba.fescar.rm.datasource.undo.UndoLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Undo log manager.
 */
public final class MySQLUndoLogManager extends AbstractUndoLogManager {

    private static String UNDO_LOG_TABLE_NAME = "undo_log";

    private static String INSERT_UNDO_LOG_SQL = "INSERT INTO " + UNDO_LOG_TABLE_NAME + "\n" +
        "\t(branch_id, xid, rollback_info, log_status, log_created, log_modified)\n" +
        "VALUES (?, ?, ?, 0, now(), now())";

    private static class MySQLUndoLogManagerHolder {
        private static MySQLUndoLogManager instance = new MySQLUndoLogManager();
    }

    private MySQLUndoLogManager() {

    }

    public static UndoLogManager instance() {
        return MySQLUndoLogManagerHolder.instance;
    }

    @Override
    protected String getInsertUndoLogSql() {
        return INSERT_UNDO_LOG_SQL;
    }
}
