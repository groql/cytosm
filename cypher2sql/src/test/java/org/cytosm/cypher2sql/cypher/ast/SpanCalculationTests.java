package org.cytosm.cypher2sql.cypher.ast;

import org.cytosm.cypher2sql.cypher.parser.ASTBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
public class SpanCalculationTests {

    @Test
    public void testSpanCalculation1() {
        String cypher = "MATCH (a:Person {id: 'test'}) WHERE a.x > (12 + 4 / 1)\n" +
                "WITH a AS foobar RETURN 234, foobar.test ORDER BY {} SKIP 23 LIMIT 42";
        Statement st = ASTBuilder.parse(cypher);
        Assertions.assertEquals(0, st.span.lo);
        Assertions.assertEquals(124, st.span.hi);
    }
}
