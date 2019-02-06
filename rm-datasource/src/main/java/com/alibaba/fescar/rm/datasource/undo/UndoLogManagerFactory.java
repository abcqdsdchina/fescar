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

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.rm.datasource.undo.mysql.MySQLUndoLogManager;
import com.alibaba.fescar.rm.datasource.undo.oracle.OracleUndoLogManager;

/**
 * The type Undo log manager.
 */
public final class UndoLogManagerFactory {

    public static UndoLogManager getUndoLogManager(String dbType) {
        switch (dbType) {
            case JdbcConstants.MYSQL :
                return MySQLUndoLogManager.instance();
            case JdbcConstants.ORACLE :
                return OracleUndoLogManager.instance();
            default:
                throw new NotSupportYetException("DbType[" + dbType + "] is not support yet!");
        }
    }

}
