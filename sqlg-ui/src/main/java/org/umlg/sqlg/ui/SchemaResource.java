package org.umlg.sqlg.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.structure.topology.*;
import org.umlg.sqlg.ui.util.ObjectMapperFactory;
import org.umlg.sqlg.ui.util.SlickLazyTree;
import org.umlg.sqlg.ui.util.SlickLazyTreeContainer;
import spark.Request;

import java.util.List;
import java.util.Optional;

public class SchemaResource {

    public static ObjectNode retrieveGraph() {
        ObjectMapper objectMapper = ObjectMapperFactory.INSTANCE.getObjectMapper();
        SqlgGraph sqlgGraph = SqlgUI.INSTANCE.getSqlgGraph();
        Configuration configuration = sqlgGraph.configuration();
        String jdbc = configuration.getString("jdbc.url");
        String username = configuration.getString("jdbc.username");
        return objectMapper.createObjectNode()
                .put("jdbcUrl", jdbc)
                .put("username", username);
    }

    public static ArrayNode retrieveSchemas() {
        Pair<ListOrderedSet<SlickLazyTree>, ListOrderedSet<SlickLazyTree>> rootNodes = SchemaTreeBuilder.retrieveRootNodes();
        ListOrderedSet<SlickLazyTree> roots = rootNodes.getRight();
        ListOrderedSet<SlickLazyTree> initialEntries = rootNodes.getLeft();
        SlickLazyTreeContainer treeContainer = new SlickLazyTreeContainer(roots);
        @SuppressWarnings("UnnecessaryLocalVariable")
        ArrayNode result = treeContainer.complete(new SchemaTreeBuilder.SchemaTreeSlickLazyTreeHelper(initialEntries));
        return result;
    }

    public static ObjectNode retrieveSchemaDetails(Request req) {
        SqlgGraph sqlgGraph = SqlgUI.INSTANCE.getSqlgGraph();
        String schemaName = req.params("schemaName");
        Optional<Schema> schemaOptional = sqlgGraph.getTopology().getSchema(schemaName);
        if (schemaOptional.isPresent()) {
            Schema schema = schemaOptional.get();
            List<Vertex> schemaVertices = sqlgGraph.topology().V()
                    .hasLabel(Topology.SQLG_SCHEMA + "." + Topology.SQLG_SCHEMA_SCHEMA)
                    .has(Topology.SQLG_SCHEMA_SCHEMA_NAME, schema.getName())
                    .toList();
            Preconditions.checkState(schemaVertices.size() == 1);
            Vertex schemaVertex = schemaVertices.get(0);
            ObjectMapper objectMapper = ObjectMapperFactory.INSTANCE.getObjectMapper();
            ObjectNode result = objectMapper.createObjectNode();
            ObjectNode schemaObjectNode = objectMapper.createObjectNode();
            result.set("schema", schemaObjectNode);
            schemaObjectNode.put("label", "Schema")
                    .put("name", schema.getName())
                    .put("ID", schemaVertex.id().toString())
                    .put("createdOn", schemaVertex.value("createdOn").toString());
            return result;
        } else {
            throw new IllegalStateException(String.format("Unknown schema '%s'", schemaName));
        }
    }

    public static ObjectNode retrieveVertexEdgeLabelDetails(Request req) {
        SqlgGraph sqlgGraph = SqlgUI.INSTANCE.getSqlgGraph();
        String schemaName = req.params("schemaName");
        String abstractLabel = req.params("abstractLabel");
        String vertexOrEdge = req.params("vertexOrEdge");
        Optional<Schema> schemaOptional = sqlgGraph.getTopology().getSchema(schemaName);
        if (schemaOptional.isPresent()) {
            Schema schema = schemaOptional.get();
            List<Vertex> schemaVertices = sqlgGraph.topology().V()
                    .hasLabel(Topology.SQLG_SCHEMA + "." + Topology.SQLG_SCHEMA_SCHEMA)
                    .has(Topology.SQLG_SCHEMA_SCHEMA_NAME, schema.getName())
                    .toList();
            Preconditions.checkState(schemaVertices.size() == 1);
            Vertex schemaVertex = schemaVertices.get(0);
            ObjectMapper objectMapper = ObjectMapperFactory.INSTANCE.getObjectMapper();
            ObjectNode result = objectMapper.createObjectNode();
            ObjectNode vertexLabelObjectNode = objectMapper.createObjectNode();
            result.set("vertexLabel", vertexLabelObjectNode);
            ObjectNode identifierData = objectMapper.createObjectNode();
            ArrayNode identifiers = objectMapper.createArrayNode();
            identifierData.set("identifiers", identifiers);
            vertexLabelObjectNode.set("identifierData", identifierData);
            ArrayNode propertyColumnsArrayNode = objectMapper.createArrayNode();
            vertexLabelObjectNode.set("propertyColumns", propertyColumnsArrayNode);
            ArrayNode indexArrayNode = objectMapper.createArrayNode();
            vertexLabelObjectNode.set("indexes", indexArrayNode);
            if (vertexOrEdge.equals("vertex")) {
                Optional<VertexLabel> vertexLabelOptional = schema.getVertexLabel(abstractLabel);
                if (vertexLabelOptional.isPresent()) {
                    VertexLabel vertexLabel = vertexLabelOptional.get();
                    vertexLabelObjectNode.put("label", "VertexLabel")
                            .put("name", vertexLabel.getName())
                            .put("ID", "TODO")
                            .put("createdOn", "TODO");
                    if (vertexLabel.hasIDPrimaryKey()) {
                        identifierData.put("userDefinedIdentifiers", false);
                    } else {
                        identifierData.put("userDefinedIdentifiers", true);
                        for (String identifier : vertexLabel.getIdentifiers()) {
                            identifiers.add(identifier);
                        }
                    }
                    for (PropertyColumn propertyColumn : vertexLabel.getProperties().values()) {
                        ObjectNode propertyColumnObjectNode = objectMapper.createObjectNode();
                        propertyColumnsArrayNode.add(propertyColumnObjectNode);
                        propertyColumnObjectNode.put("name", propertyColumn.getName());
                        propertyColumnObjectNode.put("type", propertyColumn.getPropertyType().name());
                    }
                    for (Index index: vertexLabel.getIndexes().values()) {
                        ObjectNode indexObjectNode = objectMapper.createObjectNode();
                        indexArrayNode.add(indexObjectNode);
                        indexObjectNode.put("name", index.getName());
                        indexObjectNode.put("type", index.getIndexType().getName());
                        ArrayNode indexPropertiesArrayNode = objectMapper.createArrayNode();
                        indexObjectNode.set("properties", indexPropertiesArrayNode);
                        for (PropertyColumn indexProperty : index.getProperties()) {
                            ObjectNode indexPropertyObjectNode = objectMapper.createObjectNode();
                            indexPropertiesArrayNode.add(indexPropertyObjectNode);
                            indexPropertyObjectNode.put("name", indexProperty.getName());
                        }
                    }
                } else {
                    throw new IllegalStateException(String.format("Unknown vertex label '%s'", abstractLabel));
                }
            } else {
                Optional<EdgeLabel> edgeLabelOptional = schema.getEdgeLabel(abstractLabel);
                if (edgeLabelOptional.isPresent()) {
                    EdgeLabel edgeLabel = edgeLabelOptional.get();
                    vertexLabelObjectNode.put("label", "EdgeLabel")
                            .put("name", edgeLabel.getName())
                            .put("ID", schemaVertex.id().toString())
                            .put("createdOn", schemaVertex.value("createdOn").toString());
                    if (edgeLabel.hasIDPrimaryKey()) {
                        identifierData.put("userDefinedIdentifiers", false);
                    } else {
                        identifierData.put("userDefinedIdentifiers", true);
                        for (String identifier : edgeLabel.getIdentifiers()) {
                            identifiers.add(identifier);
                        }
                    }
                    for (PropertyColumn propertyColumn : edgeLabel.getProperties().values()) {
                        ObjectNode propertyColumnObjectNode = objectMapper.createObjectNode();
                        propertyColumnsArrayNode.add(propertyColumnObjectNode);
                        propertyColumnObjectNode.put("name", propertyColumn.getName());
                        propertyColumnObjectNode.put("type", propertyColumn.getPropertyType().name());
                    }
                    for (Index index: edgeLabel.getIndexes().values()) {
                        ObjectNode indexObjectNode = objectMapper.createObjectNode();
                        indexArrayNode.add(indexObjectNode);
                        indexObjectNode.put("name", index.getName());
                        indexObjectNode.put("type", index.getIndexType().getName());
                        ArrayNode indexPropertiesArrayNode = objectMapper.createArrayNode();
                        indexObjectNode.set("properties", indexPropertiesArrayNode);
                        for (PropertyColumn indexProperty : index.getProperties()) {
                            ObjectNode indexPropertyObjectNode = objectMapper.createObjectNode();
                            indexPropertiesArrayNode.add(indexPropertyObjectNode);
                            indexPropertyObjectNode.put("name", indexProperty.getName());
                        }
                    }
                } else {
                    throw new IllegalStateException(String.format("Unknown vertex label '%s'", abstractLabel));
                }
            }
            return result;
        } else {
            throw new IllegalStateException(String.format("Unknown schema '%s'", schemaName));
        }
    }

}
