/*
 * (c) Copyright 2007 by Volker Bergmann. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted under the terms of the
 * GNU General Public License.
 *
 * For redistributing this software or a derivative work under a license other
 * than the GPL-compatible Free Software License as defined by the Free
 * Software Foundation or approved by OSI, you must first obtain a commercial
 * license to this software product from Volker Bergmann.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * WITHOUT A WARRANTY OF ANY KIND. ALL EXPRESS OR IMPLIED CONDITIONS,
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE
 * HEREBY EXCLUDED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.databene.platform.db;

import org.databene.id.IdProvider;
import org.databene.id.IdProviderFactory;
import org.databene.id.IdProviderId;
import org.databene.id.IdStrategy;
import org.databene.platform.db.model.jdbc.JDBCDBImporter;
import org.databene.platform.db.model.*;
import org.databene.commons.*;
import org.databene.commons.bean.ArrayPropertyExtractor;
import org.databene.commons.converter.AnyConverter;
import org.databene.commons.converter.ConvertingIterable;
import org.databene.model.data.*;
import org.databene.model.depend.DependencyModel;
import org.databene.model.storage.StorageSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * RDBMS implementation of the {@link StorageSystem} interface.<br/>
 * <br/>
 * Created: 27.06.2007 23:04:19
 * @since 0.3
 * @author Volker Bergmann
 */
public class DBSystem implements StorageSystem, IdProviderFactory {
    
    // constants -------------------------------------------------------------------------------------------------------
    
    protected static final ArrayPropertyExtractor<String> nameExtractor
            = new ArrayPropertyExtractor<String>("name", String.class);
    
    public static final IdStrategy<Long>   SEQHILO   = new IdStrategy<Long>("seqhilo", Long.class);
    public static final IdStrategy<Long>   SEQUENCE  = new IdStrategy<Long>("sequence", Long.class);
    public static final IdStrategy<Object> QUERY     = new IdStrategy<Object>("query", Object.class);

    private static final IdStrategy[] ID_STRATEGIES = {
        SEQHILO, SEQUENCE, QUERY
    };
    
    // attributes ------------------------------------------------------------------------------------------------------
    
    private String id;
    private String url;
    private String user;
    private String password;
    private String driver;
    private String schema;
    private boolean batch;
    
    private int fetchSize;

    private Database database;

    private Map<Thread, ThreadContext> contexts;
    private Map<String, TypeDescriptor> typeDescriptors;
    
    private TypeMapper<Class<? extends Object>> driverTypeMapper;
    private DatabaseDialect dialect;
    
    // constructors ----------------------------------------------------------------------------------------------------

    public DBSystem() {
        this(null, null, null, null, null);
    }

    public DBSystem(String id, String url, String driver, String user, String password) {
        super();
        this.id = id;
        this.url = url;
        this.user = user;
        this.password = password;
        this.driver = driver;
        this.schema = null;
        this.fetchSize = 100;
        this.batch = false;
        this.contexts = new HashMap<Thread, ThreadContext>();
        this.driverTypeMapper = driverTypeMapper();
    }

    // properties ------------------------------------------------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    /**
     * @return the batch
     */
    public boolean isBatch() {
        return batch;
    }

    /**
     * @param batch the batch to set
     */
    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    /**
     * @return the fetchSize
     */
    public int getFetchSize() {
        return fetchSize;
    }

    /**
     * @param fetchSize the fetchSize to set
     */
    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    // DescriptorProvider interface ------------------------------------------------------------------------------------

    public TypeDescriptor[] getTypeDescriptors() {
        if (logger.isDebugEnabled())
            logger.debug("getTypeDescriptors()");
        if (typeDescriptors == null)
            parseMetaData();
        return CollectionUtil.toArray(typeDescriptors.values(), TypeDescriptor.class);
    }

    public TypeDescriptor getTypeDescriptor(String tableName) {
        if (logger.isDebugEnabled())
            logger.debug("getTypeDescriptor(" + tableName + ")");
        if (typeDescriptors == null)
            parseMetaData();
        TypeDescriptor entityDescriptor = typeDescriptors.get(tableName);
        if (entityDescriptor == null)
            for (TypeDescriptor candidate : typeDescriptors.values())
                if (candidate.getName().equalsIgnoreCase(tableName)) {
                    entityDescriptor = candidate;
                    break;
                }
        return entityDescriptor;
    }

    // StorageSystem interface -----------------------------------------------------------------------------------------

    public void store(Entity entity) {
        if (logger.isDebugEnabled())
            logger.debug("Storing " + entity);
        ColumnInfo[] writeColumnInfos = writeColumnInfos(entity);
        try {
            String tableName = entity.getName();
            PreparedStatement insertStatement = getInsertStatement(entity.getDescriptor(), writeColumnInfos);
            for (int i = 0; i < writeColumnInfos.length; i++) {
                String columnName = writeColumnInfos[i].name;
                Object componentValue = entity.getComponent(columnName);
                Class<? extends Object> type = writeColumnInfos[i].type;
                Object jdbcValue = AnyConverter.convert(componentValue, type);
                try {
                    if (jdbcValue != null)
                        insertStatement.setObject(i + 1, jdbcValue);
                    else
                        insertStatement.setNull(i + 1, writeColumnInfos[i].sqlType);
                } catch (SQLException e) {
                    throw new RuntimeException("error setting column " + tableName + '.' + columnName, e);
                }
            }
            if (batch)
                insertStatement.addBatch();
            else
                insertStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error in persisting " + entity, e);
        }
    }

    public void flush() {
        if (logger.isDebugEnabled())
            logger.debug("flush()");
    	for (ThreadContext threadContext : contexts.values())
    		threadContext.commit();
    }

    public void close() {
        if (logger.isDebugEnabled())
            logger.debug("close()");
        flush();
        for (IdProvider<? extends Object> idProvider : idProviders.values())
            idProvider.close();
        Iterator<ThreadContext> iterator = contexts.values().iterator();
        while (iterator.hasNext()) {
            iterator.next().close();
            iterator.remove();
        }
    }

    public TypedIterable<Entity> queryEntities(String type, String selector) {
        if (logger.isDebugEnabled())
            logger.debug("getEntities(" + type + ")");
    	Connection connection = getThreadContext().connection;
        String sql = "select * from " + type + (StringUtil.isEmpty(selector) ? "" : " WHERE " + selector);
        Iterable<ResultSet> iterable = new QueryIterable(connection, sql, fetchSize);
        return new EntityResultSetIterable(iterable, (ComplexTypeDescriptor) getTypeDescriptor(type));
    }

    public long countEntities(String tableName) {
        if (logger.isDebugEnabled())
            logger.debug("countEntities(" + tableName + ")");
        String sql = "select count(*) from " + tableName;
        try {
            Connection connection = getThreadContext().connection;
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            long count = resultSet.getLong(1);
            return count;
        } catch (SQLException e) {
            throw new RuntimeException("Error in counting rows of table " + tableName + ". SQL = " + sql, e);
        }
    }

    public <T> TypedIterable<T> queryEntityIds(String tableName, String selector) {
        if (logger.isDebugEnabled())
            logger.debug("getIds(" + tableName + ", " + selector + ")");
        
        DBTable table = getTable(tableName);
        DBPrimaryKeyConstraint pkConstraint = table.getPrimaryKeyConstraint();
        DBColumn[] columns = pkConstraint.getColumns();
        String[] pkColumnNames = ArrayPropertyExtractor.convert(columns, "name", String.class);
        String query = "select " + ArrayFormat.format(pkColumnNames) + " from " + tableName;
        if (selector != null)
            query += " where " + selector;
        return query(query);
    }

    public <T> TypedIterable<T> query(String query) {
        if (logger.isDebugEnabled())
            logger.debug("getBySelector(" + query + ")");
    	Connection connection = getThreadContext().connection;
        QueryIterable resultSetIterable = new QueryIterable(connection, query);
        return (TypedIterable<T>)new ConvertingIterable<ResultSet, Object>(resultSetIterable, new ResultSetConverter(true));
    }
      
    // IdProviderFactory interface -------------------------------------------------------------------------------------
    
    private Map<IdProviderId, IdProvider> idProviders = new HashMap<IdProviderId, IdProvider>();
    
    public IdStrategy<? extends Object>[] getIdStrategies() {
        return ID_STRATEGIES;
    }

    public <T> IdProvider<T> idProvider(
            IdStrategy<T> strategy, String param, String scope) {
        IdProviderId pId = new IdProviderId(strategy.getName(), param, scope, this.getId());
        IdProvider<T> provider = idProviders.get(pId);
        if (provider == null) {
            if (SEQHILO.equals(strategy))
                provider = (IdProvider<T>) new SeqHiLoIdProvider(getConnection(), dialect.sequenceAccessorSql(param), 100);
            else if (SEQUENCE.equals(strategy))
                provider = (IdProvider<T>) new LongQueryIdProvider(getConnection(), dialect.sequenceAccessorSql(param));
            else if (QUERY.equals(strategy))
                provider = (IdProvider<T>) new QueryIdProvider(getConnection(), param);
            else 
                throw new IllegalArgumentException("unknown id generation strategy: " + strategy);
            idProviders.put(pId, provider);
        }
        return provider;
    }

    public Connection createConnection() {
		try {
            Class.forName(driver);
            Connection connection = new PooledConnection(DriverManager.getConnection(url, user, password));
            connection.setAutoCommit(false);
            return connection;
        } catch (ClassNotFoundException e) {
            throw new ConfigurationError("JDBC driver not found: " + driver, e);
        } catch (SQLException e) {
            throw new RuntimeException("Connecting the database failed. URL: " + url, e);
        }
	}
	
	// java.lang.Object overrides ------------------------------------------------------------------
	
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + user + '@' + url + ']';
    }

    // private helpers ------------------------------------------------------------------------------

    private PreparedStatement getInsertStatement(ComplexTypeDescriptor descriptor, ColumnInfo[] columnInfos) throws SQLException {
        ThreadContext context = getThreadContext();
        return context.getInsertStatement(descriptor, columnInfos);
    }

	private void parseMetaData() {
        logger.debug("parsing metadata...");
        try {
            this.typeDescriptors = new OrderedMap<String, TypeDescriptor>();
            //this.tableColumnIndexes = new HashMap<String, Map<String, Integer>>();
            JDBCDBImporter importer = new JDBCDBImporter(url, driver, user, password, schema, false);
            database = importer.importDatabase();
            String productName = importer.getProductName();
            mapStrategy(productName);
            List<DBTable> tables = dependencyOrderedTables(database);
            for (DBTable table : tables)
                parseTable(table);
        } catch (ImportFailedException e) {
            throw new ConfigurationError("Unexpected failure of database meta data import. ", e);
        }
    }

    private void mapStrategy(String productName) { 
        String filename = "org/databene/platform/db/databene.db_dialect.properties";
        try {
            Map<String, String> mappings = IOUtil.readProperties(filename);
            for (Map.Entry<String, String> entry : mappings.entrySet())
                if (productName.toLowerCase().contains((String)entry.getKey())) {
                    dialect = (DatabaseDialect) BeanUtil.newInstance((String)entry.getValue());
                    return;
                }
            dialect = new UnknownDialect(productName);
        } catch (IOException e) {
            throw new ConfigurationError("Database dialect mapping not found: " + filename, e);
        }
    }

    private static List<DBTable> dependencyOrderedTables(Database database) {
        DependencyModel<DBTable> model = new DependencyModel<DBTable>();
        for (DBCatalog catalog : database.getCatalogs())
            for (DBTable table : catalog.getTables())
                model.addNode(table);
        for (DBSchema schema : database.getSchemas())
            for (DBTable table : schema.getTables())
                model.addNode(table);
        List<DBTable> tables = model.dependencyOrderedObjects(true);
        return tables;
    }

    private void parseTable(DBTable table) {
        if (logger.isDebugEnabled())
            logger.debug("Parsing table " + table);
        String tableName = table.getName();
        if (tableName.startsWith("BIN$"))
            return;
        ComplexTypeDescriptor td = new ComplexTypeDescriptor(tableName);
        // process foreign keys
        for (DBForeignKeyConstraint constraint : table.getForeignKeyConstraints()) {
            List<DBForeignKeyColumn> foreignKeyColumns = constraint.getForeignKeyColumns();
            if (foreignKeyColumns.size() == 1) {
                DBForeignKeyColumn foreignKeyColumn = foreignKeyColumns.get(0);
                DBColumn targetColumn = foreignKeyColumn.getTargetColumn();
                DBTable targetTable = targetColumn.getTable();
                String fkColumnName = foreignKeyColumn.getForeignKeyColumn().getName();
                DBColumnType concreteType = foreignKeyColumn.getForeignKeyColumn().getType();
                String abstractType = JdbcMetaTypeMapper.abstractType(concreteType);
                ReferenceDescriptor descriptor = new ReferenceDescriptor(
                        fkColumnName, 
                        abstractType,
                        targetTable.getName());
                descriptor.getLocalType(false).setSource(id);
                td.addComponent(descriptor); // overwrite attribute descriptor
                logger.debug("Parsed reference " + table.getName() + '.' + descriptor);
            } else {
                logger.error("Not implemented: Don't know how to handle composite foreign keys");
            }
        }
        // process normal columns
        for (DBColumn column : table.getColumns()) {
            if (logger.isDebugEnabled())
                logger.debug("parsing column: " + column);
            String columnName = column.getName();
            if (td.getComponent(columnName) != null)
                continue; // skip columns that were already parsed (fk)
            String columnId = table.getName() + '.' + columnName;
            if (column.isVersionColumn()) {
                logger.debug("Leaving out version column " + columnId);
                continue;
            }
            DBColumnType columnType = column.getType();
            String type = JdbcMetaTypeMapper.abstractType(columnType);
            String defaultValue = column.getDefaultValue();
            SimpleTypeDescriptor typeDescriptor = new SimpleTypeDescriptor(columnId, type);
            if (defaultValue != null)
                typeDescriptor.setDetailValue("values", defaultValue);
            if (column.getSize() != null)
                typeDescriptor.setMaxLength(column.getSize());
            if (column.getFractionDigits() != null)
                typeDescriptor.setPrecision(precision(column.getFractionDigits()));
            //typeDescriptors.put(typeDescriptor.getName(), typeDescriptor);
            PartDescriptor descriptor = new PartDescriptor(columnName);
            descriptor.setLocalType(typeDescriptor);
            descriptor.setMinCount(1L);
            descriptor.setMaxCount(1L);
            descriptor.setNullable(column.getNotNullConstraint() == null);
            List<DBConstraint> ukConstraints = column.getUkConstraints();
            for (DBConstraint constraint : ukConstraints) {
                if (constraint.getColumns().length == 1) {
                    assert constraint.getColumns()[0].equals(column); // consistence check
                    descriptor.setUnique(true);
                } else {
                    logger.error("Uniqueness assurance on multiple columns is not supported yet: " + constraint);
                    // TODO v0.6 support uniqueness constraints on combination of columns
                }
            }
            logger.debug("parsed attribute " + columnId + ": " + descriptor);
            td.addComponent(descriptor);
        }

        typeDescriptors.put(td.getName(), td);
    }

    private String createSQLInsert(String tableName, ColumnInfo[] columnInfos) {
        StringBuilder builder = new StringBuilder("insert into ").append(tableName).append("(");
        if (columnInfos.length > 0)
            builder.append(columnInfos[0].name);
        for (int i = 1; i < columnInfos.length; i++)
            builder.append(',').append(columnInfos[i].name);
        builder.append(") values (");
        if (columnInfos.length> 0)
            builder.append("?");
        for (int i = 1; i < columnInfos.length; i++)
            builder.append(",?");
        builder.append(")");
        String sql = builder.toString();
        logger.debug("built SQL statement: " + sql);
        return sql;
    }
/*
    private int getColumnIndex(String tableName, String columnName) {
        tableName = tableName.toLowerCase();
        columnName = columnName.toLowerCase();
        Map<String, Integer> columnIndexes = tableColumnIndexes.get(tableName);
        if (columnIndexes == null) {
            columnIndexes = new HashMap<String, Integer>();
            tableColumnIndexes.put(tableName, columnIndexes);
        }
        Integer index = columnIndexes.get(columnName);
        if (index == null) {
            String[] columnNames = StringUtil.toLowerCase(getColumnNames(tableName));
            index = ArrayUtil.indexOf(columnName, columnNames) + 1;
            if (index == 0)
                throw new IllegalArgumentException("Column not found: " + columnName);
            columnIndexes.put(columnName, index);
        }
        return index;
    }
*/
    
    private ColumnInfo[] writeColumnInfos(Entity entity) {
        String tableName = entity.getName();
        DBTable table = getTable(tableName);
        ComplexTypeDescriptor typeDescriptor = (ComplexTypeDescriptor) getTypeDescriptor(tableName);
        Collection<ComponentDescriptor> componentDescriptors = typeDescriptor.getComponents();
        ArrayBuilder<ColumnInfo> builder = new ArrayBuilder<ColumnInfo>(ColumnInfo.class, componentDescriptors.size());
        ComplexTypeDescriptor entityDescriptor = entity.getDescriptor();
        for (ComponentDescriptor dbCompDescriptor : componentDescriptors) {
            ComponentDescriptor enCompDescriptor = entityDescriptor.getComponent(dbCompDescriptor.getName());
            if (enCompDescriptor != null && enCompDescriptor.getMode() == Mode.ignored)
                continue;
            if (dbCompDescriptor.getMode() != Mode.ignored) {
                String name = dbCompDescriptor.getName();
                String primitiveType = ((SimpleTypeDescriptor) dbCompDescriptor.getType()).getPrimitiveType().getName();
                DBColumn column = table.getColumn(name);
                DBColumnType columnType = column.getType();
                int sqlType = columnType.getJdbcType();
                Class<? extends Object> javaType = driverTypeMapper.concreteType(primitiveType);
                builder.append(new ColumnInfo(name, sqlType, javaType));
            }
        }
        return builder.toArray();
    }

    private DBTable getTable(String tableName) {
        DBSchema dbSchema = database.getSchema(this.schema);
        if (dbSchema != null) {
            DBTable table = dbSchema.getTable(tableName);
            if (table != null)
                return table;
        }
        for (DBCatalog catalog : database.getCatalogs()) {
            DBTable table = catalog.getTable(tableName);
            if (table != null)
                return table;
        }
        for (DBSchema schema2 : database.getSchemas()) {
            DBTable table = schema2.getTable(tableName);
            if (table != null)
                return table;
        }
        throw new ObjectNotFoundException("Table " + tableName);
    }

    private synchronized ThreadContext getThreadContext() {
        Thread currentThread = Thread.currentThread();
        ThreadContext context = contexts.get(currentThread);
        if (context == null) {
            context = new ThreadContext();
            contexts.put(currentThread, context);
        }
        return context;
    }
    
    private Connection getConnection() {
        return getThreadContext().connection;
    }
    
    private class ThreadContext {
        
        private Connection connection;
        
        public Map<ComplexTypeDescriptor, PreparedStatement> insertStatements;
        
        public ThreadContext() {
            insertStatements = new HashMap<ComplexTypeDescriptor, PreparedStatement>();
            connection = createConnection();
        }
        
        void commit() {
            try {
                for (Map.Entry<ComplexTypeDescriptor, PreparedStatement> entry : insertStatements.entrySet()) {
                    PreparedStatement statement = entry.getValue();
                    if (statement != null) {
                        statement.executeBatch();            
                        // need to finish old statement
                        if (jdbcLogger.isDebugEnabled())
                            jdbcLogger.debug("Closing statement: " + statement);
                        DBUtil.close(statement);
                    }
                    entry.setValue(null);
                }
                if (jdbcLogger.isDebugEnabled())
                    jdbcLogger.debug("Committing connection: " + connection);
                connection.commit();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public PreparedStatement getInsertStatement(ComplexTypeDescriptor descriptor, ColumnInfo[] columnInfos) {
            try {
                PreparedStatement statement = insertStatements.get(descriptor);
                if (statement == null) {
                    String sql = createSQLInsert(descriptor.getName(), columnInfos);
                    if (jdbcLogger.isDebugEnabled())
                        jdbcLogger.debug("Creating prepared statement: " + sql);
                    statement = connection.prepareStatement(sql);
                    insertStatements.put(descriptor, statement);
                } else {
                    statement.clearParameters();
                }
                return statement;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        
        public void close() {
            commit();
            DBUtil.close(connection);
        }
    }

    private String precision(int scale) {
        if (scale == 0)
            return "1";
        StringBuilder builder = new StringBuilder("0.");
        for (int i = 1; i < scale; i++)
            builder.append('0');
        builder.append(1);
        return builder.toString();
    }

    private TypeMapper<Class<? extends Object>> driverTypeMapper() {
        return new TypeMapper<Class<? extends Object>>(
                "byte",        Byte.class,
                "short",       Short.class,
                "int",         Integer.class,
                "big_integer", BigInteger.class,
                "float",       Float.class,
                "double",      Double.class,
                "big_decimal", BigDecimal.class,
                
                "boolean",     Boolean.class,
                "char",        Character.class,
                "date",        java.sql.Date.class,
                "timestamp",   java.sql.Timestamp.class,
                
                "string",      java.sql.Clob.class,
                "string",      String.class,
                
                "binary",      Blob.class,
                "binary",      byte[].class
                
//              "object",      Object.class,
                
        );
    }

    private static final Log logger = LogFactory.getLog(DBSystem.class);
//    private static final Log sqlLogger = LogFactory.getLog("org.databene.benerator.SQL"); 
    private static final Log jdbcLogger = LogFactory.getLog("org.databene.benerator.JDBC"); 

}
