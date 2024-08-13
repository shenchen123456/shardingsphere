/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.postgresql.ingest;

import lombok.SneakyThrows;
import org.apache.shardingsphere.data.pipeline.postgresql.ingest.wal.WALPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.postgresql.replication.LogSequenceNumber;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostgreSQLIngestPositionManagerTest {
    
    private static final String POSTGRESQL_96_LSN = "0/14EFDB8";
    
    private static final String POSTGRESQL_10_LSN = "0/1634520";
    
    @Mock(extraInterfaces = AutoCloseable.class)
    private DataSource dataSource;
    
    @Mock
    private Connection connection;
    
    @Mock
    private DatabaseMetaData databaseMetaData;
    
    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getCatalog()).thenReturn("sharding_db");
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        PreparedStatement lsn96PreparedStatement = mockPostgreSQL96LSN();
        when(connection.prepareStatement("SELECT * FROM pg_create_logical_replication_slot(?, ?)")).thenReturn(mock(PreparedStatement.class));
        when(connection.prepareStatement("SELECT PG_CURRENT_XLOG_LOCATION()")).thenReturn(lsn96PreparedStatement);
        PreparedStatement lsn10PreparedStatement = mockPostgreSQL10LSN();
        when(connection.prepareStatement("SELECT PG_CURRENT_WAL_LSN()")).thenReturn(lsn10PreparedStatement);
    }
    
    @Test
    void assertGetCurrentPositionOnPostgreSQL96() throws SQLException {
        mockSlotExistsOrNot(false);
        when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(9);
        when(databaseMetaData.getDatabaseMinorVersion()).thenReturn(6);
        WALPosition actual = new PostgreSQLIngestPositionManager().init(dataSource, "");
        assertThat(actual.getLogSequenceNumber().get(), is(LogSequenceNumber.valueOf(POSTGRESQL_96_LSN)));
    }
    
    @Test
    void assertGetCurrentPositionOnPostgreSQL10() throws SQLException {
        mockSlotExistsOrNot(false);
        when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(10);
        WALPosition actual = new PostgreSQLIngestPositionManager().init(dataSource, "");
        assertThat(actual.getLogSequenceNumber().get(), is(LogSequenceNumber.valueOf(POSTGRESQL_10_LSN)));
    }
    
    @Test
    void assertGetCurrentPositionThrowException() throws SQLException {
        mockSlotExistsOrNot(false);
        when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(9);
        when(databaseMetaData.getDatabaseMinorVersion()).thenReturn(4);
        assertThrows(RuntimeException.class, () -> new PostgreSQLIngestPositionManager().init(dataSource, ""));
    }
    
    @SneakyThrows(SQLException.class)
    private PreparedStatement mockPostgreSQL96LSN() {
        PreparedStatement result = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(result.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn(POSTGRESQL_96_LSN);
        return result;
    }
    
    @SneakyThrows(SQLException.class)
    private PreparedStatement mockPostgreSQL10LSN() {
        PreparedStatement result = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(result.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn(POSTGRESQL_10_LSN);
        return result;
    }
    
    @SneakyThrows(SQLException.class)
    private void mockSlotExistsOrNot(final boolean exists) {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT slot_name FROM pg_replication_slots WHERE slot_name=? AND plugin=?")).thenReturn(preparedStatement);
        ResultSet resultSet = mock(ResultSet.class);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(exists);
    }
    
    @Test
    void assertDestroyWhenSlotExists() throws SQLException {
        mockSlotExistsOrNot(true);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT pg_drop_replication_slot(?)")).thenReturn(preparedStatement);
        new PostgreSQLIngestPositionManager().destroy(dataSource, "");
        verify(preparedStatement).execute();
    }
}
