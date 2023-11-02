package org.cytosm.cypher2sql.lowering;

import org.cytosm.cypher2sql.PassAvailables;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Rendering tests don't assert anything. Instead they
 * pass if and only if the code doesn't rise any exception.
 * They are sort of end 2 end tests.
 * Finally this is also a convenient place to visualize
 * the SQL result to get a feeling of the result being generated
 * on simple examples.
 */
public class RenderingTests extends BaseLDBCTests {

    @Test
    @Disabled
    public void testBasicCypherToSQL() throws Exception {
        String cypher = """
                MATCH (a:Person {firstName: 'Richard', lastName: 'Smith'})-[:KNOWS]-(b:Person)
                MATCH (a {id: 0})-[:KNOWS]-(c:Person)-[:KNOWS]-(b)
                RETURN b.firstName""";
        cypher2sql(cypher);
    }

    @Test
    public void testOrderByOnReturnUsingAnAliasVar() throws Exception {
        String cypher = "" +
                "MATCH (a:Person)\n" +
                "RETURN a.firstName AS foo ORDER BY foo DESC";
        cypher2sql(cypher);
    }

    @Test
    public void testCountFunction() throws Exception {
        String cypher = """
                MATCH (a:Message)
                WITH count(a) AS messageCount
                RETURN messageCount""";
        cypher2sql(cypher);
    }

    @Test
    public void testAliasVarInWith() throws Exception {
        String cypher = """
                MATCH (a:Person)
                WITH a AS b
                MATCH (b)-[:KNOWS]-(c:Person) WHERE c.firstName = b.firstName
                RETURN b.firstName, c.age""";
        cypher2sql(cypher);
    }

    @Test
    public void testDisjointMatch() throws Exception {
        // This is example should trigger the second MATCH
        // to increase the cardinality of the final result.
        String cypher = """
                MATCH (a:Person {id:0})-[:KNOWS]-(b:Person)
                MATCH (c:Person)
                RETURN 42""";
        cypher2sql(cypher);
    }

    @Test
    public void testMultipleMatchOnNode() throws Exception {
        String cypher = """
                MATCH (a:Message)
                RETURN a.length ORDER BY a.length DESC
                LIMIT 10""";
        cypher2sql(cypher);
    }

    @Test
    public void testHops() throws Exception {
        String cypher = "MATCH (a:Person)-[:KNOWS*1..2]-(b:Person) RETURN a.id";
        System.out.println(PassAvailables.cypher2sql(getGTopInterface(), cypher));
    }

    @Test
    public void testMergeExpandCyphers() throws Exception {
        String cypher = """
                MATCH (a)
                RETURN a.id
                LIMIT 10""";
        System.out.println(PassAvailables.cypher2sql(getGTopInterface(), cypher));
    }

    @Test
    public void testIndirectDependency() throws Exception {
        String cypher = """
                MATCH (a:Person)-[:KNOWS]-(b:Person)
                MATCH (b)-[:KNOWS]-(c:Person)
                MATCH (a)-[:KNOWS]-(d:Person)
                RETURN 42""";
        cypher2sql(cypher);
    }

    @Test
    public void testFullExample() throws Exception {
        String cypher = """
                MATCH (test:Person {id:2199023259437})-[:KNOWS]->(friend:Person)
                MATCH (:Person)-[:KNOWS]->(other_friend:Person)
                MATCH (friend)-[:STUDY_AT]->(:University)-[:IS_LOCATED_IN]->(:City)
                RETURN friend.id""";
        cypher2sql(cypher);
    }

    @Test
    public void testCircularRelationships() throws Exception {
        String cypher = """
                MATCH (a:Person)-[:KNOWS]-(b:Person)-[:KNOWS]-(c:Person)
                MATCH (a)-[:KNOWS]-(c)
                RETURN a.id, b.id, c.id""";
        cypher2sql(cypher);
    }

    @Test
    public void testJoinsExpandedIntoProblematicUnions() throws Exception {
        String cypher = """
                MATCH (a:Message)
                MATCH (a)<-[:LIKES]-(b:Person)
                RETURN a.id, b.id""";
        cypher2sql(cypher);
    }

    @Test
    public void testAliasVar() throws Exception {
        String cypher = """
                MATCH (a:Person)-[:KNOWS]-(b:Person)
                WITH a, {c:{b:b}} AS d
                RETURN a.firstName, d.c.b.firstName""";
        cypher2sql(cypher);
    }

    @Test
    public void testInExpression() throws Exception {
        String cypher = "" +
                "MATCH (a:Person) WHERE a.firstName IN ['foo', 'bar']\n" +
                "RETURN a.firstName";
        cypher2sql(cypher);
    }

    @Test
    public void testAliasVarInUnions() throws Exception {
        String cypher = """
                MATCH (a:Message)
                WITH a, {c:{b:a}} AS d
                RETURN a.firstName, d.c.b.firstName""";
        cypher2sql(cypher);
    }

    private void cypher2sql(String cypher) throws Exception {
        System.out.println(PassAvailables.cypher2sqlOnExpandedPaths(getGTopInterface(), cypher).toSQLString());
    }
}
