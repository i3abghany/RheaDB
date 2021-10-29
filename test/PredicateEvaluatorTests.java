import QueryParser.PredicateParser.PredicateParser;
import RheaDB.AttributeType;
import RheaDB.PredicateEvaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class PredicateEvaluatorTests {
    @Test
    void testBasicPredicate() throws Exception {
        var p = new PredicateParser("a > 2").parse();
        Assertions.assertNotNull(p);

        var values = new HashMap<String, PredicateEvaluator.IdentifierValue>();
        values.put("a", new PredicateEvaluator.IdentifierValue(1, AttributeType.INT));

        Assertions.assertFalse(new PredicateEvaluator(p, values).Evaluate());
    }

    @Test
    void testCompoundPredicate() throws Exception {
        var p = new PredicateParser("a > 2 && (b != 3)").parse();
        Assertions.assertNotNull(p);

        var values = new HashMap<String, PredicateEvaluator.IdentifierValue>();
        values.put("a", new PredicateEvaluator.IdentifierValue(1, AttributeType.INT));
        values.put("b", new PredicateEvaluator.IdentifierValue(3, AttributeType.INT));

        Assertions.assertFalse(new PredicateEvaluator(p, values).Evaluate());
    }
}
