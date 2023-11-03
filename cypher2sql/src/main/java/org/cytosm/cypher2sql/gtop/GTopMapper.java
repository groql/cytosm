package org.cytosm.cypher2sql.gtop;

import org.cytosm.common.gtop.GTop;
import org.cytosm.common.gtop.abstraction.AbstractionEdge;
import org.cytosm.common.gtop.abstraction.AbstractionLevelGtop;
import org.cytosm.common.gtop.abstraction.AbstractionNode;
import org.cytosm.common.gtop.implementation.graphmetadata.GraphMetadata;
import org.cytosm.common.gtop.implementation.relational.Attribute;
import org.cytosm.common.gtop.implementation.relational.ImplementationEdge;
import org.cytosm.common.gtop.implementation.relational.ImplementationLevelGtop;
import org.cytosm.common.gtop.implementation.relational.ImplementationNode;
import org.cytosm.common.gtop.implementation.relational.NodeIdImplementation;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GTopMapper {
    private final Connection connection;

    public GTopMapper(Connection connection) {
        this.connection = connection;
    }

    public GTop mapSchema(String schemaPattern) {
        List<AbstractionNode> abstractionNodes = new ArrayList<>();
        List<AbstractionEdge> abstractionEdges = new ArrayList<>();
        List<ImplementationNode> implementationNodes = new ArrayList<>();
        List<ImplementationEdge> implementationEdges = new ArrayList<>();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tables = metaData.getTables(null, schemaPattern, null, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    Set<String> pks = collectPks(schemaPattern, metaData, tableName);
                    Set<String> fks = collectFks(schemaPattern, metaData, tableName);
                    try (ResultSet foreignKeys = metaData.getExportedKeys(null, schemaPattern, tableName)) {
                        while (foreignKeys.next()) {
                            String fkTableName = foreignKeys.getString("FKTABLE_NAME");
                            String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                            String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                            String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
                            // and also
                            // FKTABLE_CAT, PKTABLE_CAT, FKTABLE_SCHEM,
                            // PKTABLE_SCHEM, KEY_SEQ,
                            // FK_NAME, PK_NAME

                            // Create a unique type for each foreign key relationship
                            String edgeType = fkTableName + "_to_" + pkTableName;

                            List<String> edgeAttrs = !pkColumnName.equals(fkColumnName) ?
                                    List.of(fkColumnName, pkColumnName) : List.of(fkColumnName);
                            AbstractionEdge abstractionEdge = new AbstractionEdge(
                                    List.of(edgeType),
                                    edgeAttrs,
                                    List.of(fkTableName),
                                    List.of(pkTableName),
                                    true // TODO: determine a direction
                            );
                            abstractionEdges.add(abstractionEdge);
                            ImplementationEdge implementationEdge = new ImplementationEdge(List.of(edgeType), Collections.emptyList());
                            implementationEdges.add(implementationEdge);
                        }
                    }
                    try (ResultSet columns = metaData.getColumns(null, schemaPattern, tableName, null)) {
                        List<Attribute> attributes = new ArrayList<>();
                        List<NodeIdImplementation> ids = new ArrayList<>(); // multiple ids in case of composed key
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            String typeName = columns.getString("TYPE_NAME");
                            if (pks.contains(columnName)) {
                                int concatenationPosition = 1; // for composed keys should be something different
                                ids.add(new NodeIdImplementation(columnName, typeName, concatenationPosition));
                            } else if (!fks.contains(columnName)) {
                                String abstractionLevelName = columnName;
                                attributes.add(new Attribute(columnName, abstractionLevelName, typeName));
                            }
                        }
                        AbstractionNode abstractionNode = new AbstractionNode(
                                List.of(tableName),
                                attributes.stream().map(Attribute::getColumnName).toList()
                        );
                        abstractionNodes.add(abstractionNode);
                        ImplementationNode implementationNode = new ImplementationNode(
                                List.of(tableName),
                                tableName,
                                ids,
                                attributes,
                                Collections.emptyList()
                        );
                        implementationNodes.add(implementationNode);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new GTop(
                new AbstractionLevelGtop(abstractionNodes, abstractionEdges),
                new ImplementationLevelGtop(new GraphMetadata(), implementationNodes, implementationEdges)
        );
    }

    private static Set<String> collectFks(String schemaPattern, DatabaseMetaData metaData, String tableName) throws SQLException {
        Set<String> fks = new HashSet<>();
        try (ResultSet foreignKeys = metaData.getExportedKeys(null, schemaPattern, tableName)) {
            while (foreignKeys.next()) {
                String fkTableName = foreignKeys.getString("FKTABLE_NAME");
                String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
                fks.add(fkColumnName);
            }
        }
        return fks;
    }

    private static Set<String> collectPks(String schemaPattern, DatabaseMetaData metaData, String tableName) throws SQLException {
        Set<String> pks = new HashSet<>();
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, schemaPattern, tableName)) {
            while (primaryKeys.next()) {
                primaryKeys.getString("TABLE_CAT");
                primaryKeys.getString("TABLE_SCHEM");
                primaryKeys.getString("TABLE_NAME");
                primaryKeys.getString("COLUMN_NAME");
                primaryKeys.getInt("KEY_SEQ");
                primaryKeys.getString("PK_NAME");
                pks.add(primaryKeys.getString("COLUMN_NAME"));
            }
        }
        return pks;
    }
}
