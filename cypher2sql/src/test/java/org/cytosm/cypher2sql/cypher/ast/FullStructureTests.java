package org.cytosm.cypher2sql.cypher.ast;

import org.cytosm.cypher2sql.cypher.ast.clause.Clause;
import org.cytosm.cypher2sql.cypher.ast.clause.match.Match;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.NodePattern;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipChain;
import org.cytosm.cypher2sql.cypher.ast.clause.match.pattern.RelationshipPattern;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.Return;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.ReturnItem;
import org.cytosm.cypher2sql.cypher.ast.clause.projection.With;
import org.cytosm.cypher2sql.cypher.ast.expression.*;
import org.cytosm.cypher2sql.cypher.parser.ASTBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

/**
 * Those tests take a long time to write but
 * they check the entire structure to make sure everything is correct.
 */
public class FullStructureTests {

    // This test would be hard to debug but it test all the features
    // together and so might be more likely to catch any bug introduced.
    @Test
    public void testFullExampleWithoutRelationships() {
        String cypher = "MATCH (a:Person {id: 'test'}) WHERE a.x > (12 + 4 / 1)\n" +
                "WITH a AS foobar RETURN 234, foobar.test ORDER BY {} SKIP 23 LIMIT 42";
        Statement st = ASTBuilder.parse(cypher);
        List<Clause> clauses = ((SingleQuery) st.query.part).clauses;
        Assertions.assertEquals(3, clauses.size());
        Assertions.assertInstanceOf(Match.class, clauses.get(0));
        Assertions.assertInstanceOf(With.class, clauses.get(1));
        Assertions.assertInstanceOf(Return.class, clauses.get(2));

        Match m = (Match) clauses.get(0);
        With w = (With) clauses.get(1);
        Return r = (Return) clauses.get(2);

        // Match
        Assertions.assertFalse(m.optional);
        Assertions.assertNotNull(m.pattern);
        Assertions.assertTrue(m.where.isPresent());

        Assertions.assertEquals(m.pattern.patternParts.size(), 1);
        Assertions.assertInstanceOf(NodePattern.class, m.pattern.patternParts.get(0).element);
        NodePattern np = (NodePattern) m.pattern.patternParts.get(0).element;
        Assertions.assertEquals(np.labels.size(), 1);
        Assertions.assertEquals(np.labels.get(0).name, "Person");
        Assertions.assertTrue(np.variable.isPresent());
        Assertions.assertEquals(np.variable.get().name, "a");

        Assertions.assertTrue(np.properties.isPresent());
        MapExpression props = np.properties.get();

        Assertions.assertEquals(props.props.get(0).getKey().name, "id");
        Assertions.assertEquals(((Literal.StringLiteral) props.props.get(0).getValue()).value, "test");

        Binary.GreaterThan whereExpr = (Binary.GreaterThan) m.where.get().expression;
        Property lhs = (Property)  whereExpr.lhs;
        Assertions.assertEquals(lhs.propertyKey.name, "x");
        Assertions.assertEquals(((Variable) lhs.map).name, "a");

        Binary.Add rhs = (Binary.Add) whereExpr.rhs;
        Assertions.assertEquals(((Literal.Integer) rhs.lhs).value, 12L);
        Assertions.assertEquals(((Literal.Integer) ((Binary.Divide) rhs.rhs).lhs).value, 4L);
        Assertions.assertEquals(((Literal.Integer) ((Binary.Divide) rhs.rhs).rhs).value, 1L);

        // With
        Assertions.assertEquals(w.returnItems.size(), 1);
        Assertions.assertInstanceOf(ReturnItem.Aliased.class, w.returnItems.get(0));
        Assertions.assertEquals(((ReturnItem.Aliased) w.returnItems.get(0)).alias.name, "foobar");
        Assertions.assertEquals(((Variable) w.returnItems.get(0).expression).name, "a");

        // Return
        Assertions.assertEquals(r.returnItems.size(), 2);
        Assertions.assertInstanceOf(ReturnItem.Unaliased.class, r.returnItems.get(0));
        Assertions.assertInstanceOf(ReturnItem.Unaliased.class, r.returnItems.get(1));
        Assertions.assertEquals(((ReturnItem.Unaliased) r.returnItems.get(1)).name, "foobar.test");
        Assertions.assertTrue(r.orderBy.isPresent());
        Assertions.assertEquals(r.orderBy.get().sortItems.size(), 1);
        Assertions.assertTrue(r.skip.isPresent());
        Assertions.assertEquals(((Literal.Integer) r.skip.get().expression).value, 23);
        Assertions.assertTrue(r.limit.isPresent());
        Assertions.assertEquals(((Literal.Integer) r.limit.get().expression).value, 42);
    }

    @Test
    public void testRelationshipChains1() {
        String cypher = "MATCH (a)-[r:TEST]-(c)";
        Statement st = ASTBuilder.parse(cypher);
        List<Clause> clauses = ((SingleQuery) st.query.part).clauses;
        Match m = (Match) clauses.get(0);

        Assertions.assertInstanceOf(RelationshipChain.class, m.pattern.patternParts.get(0).element);

        RelationshipChain rc = (RelationshipChain) m.pattern.patternParts.get(0).element;
        Assertions.assertInstanceOf(NodePattern.class, rc.element);
        Assertions.assertTrue(((NodePattern) rc.element).variable.isPresent());
        Assertions.assertTrue(rc.rightNode.variable.isPresent());
        Assertions.assertEquals(((NodePattern) rc.element).variable.get().name, "a");
        Assertions.assertEquals(rc.rightNode.variable.get().name, "c");
        Assertions.assertEquals(rc.relationship.direction, RelationshipPattern.SemanticDirection.BOTH);
        Assertions.assertEquals(rc.relationship.length, Optional.empty());
        Assertions.assertEquals(rc.relationship.types.size(), 1);
        Assertions.assertEquals(rc.relationship.types.get(0).name, "TEST");
    }

    @Test
    public void testListExpressions() {
        String cypher = "MATCH (a) WHERE a.lastName IN ['foo','bar'] RETURN a.firstName";
        Statement st = ASTBuilder.parse(cypher);
        List<Clause> clauses = ((SingleQuery) st.query.part).clauses;
        Match m = (Match) clauses.get(0);

        Assertions.assertTrue(m.where.isPresent());
        Assertions.assertInstanceOf(Binary.In.class, m.where.get().expression);

        Binary.In in = (Binary.In) m.where.get().expression;

        Assertions.assertInstanceOf(ListExpression.class, in.rhs);

        ListExpression list = (ListExpression) in.rhs;

        Assertions.assertEquals(2, list.exprs.size());
        Assertions.assertEquals("foo", ((Literal.StringLiteral) list.exprs.get(0)).value);
        Assertions.assertEquals("bar", ((Literal.StringLiteral) list.exprs.get(1)).value);
    }
}