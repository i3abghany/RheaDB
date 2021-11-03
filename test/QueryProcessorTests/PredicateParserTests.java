package QueryProcessorTests;

import QueryParser.PredicateParser.*;
import QueryParser.TokenKind;
import RheaDB.DBError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PredicateParserTests {

    @Test
    void parseCompoundPredicate() throws Exception {
        String predicate = "(a > 10 && b <= 12) || a != 2";
        PredicateAST ast = new PredicateParser(predicate).parse();
        Assertions.assertTrue(ast.root() instanceof BinaryLogicalExpression);

        var left = ((BinaryLogicalExpression) ast.root()).getLhs();
        var operator = ((BinaryLogicalExpression) ast.root()).getOperatorToken();
        var right = ((BinaryLogicalExpression) ast.root()).getRhs();

        Assertions.assertTrue(left instanceof ParenthesizedExpression);
        Assertions.assertEquals(operator.getKind(), TokenKind.BarBarToken);
        Assertions.assertTrue(right instanceof BinaryLogicalExpression);

        var enclosedExpression = ((ParenthesizedExpression) left).getExpression();
        Assertions.assertTrue(enclosedExpression instanceof BinaryLogicalExpression);

        left = ((BinaryLogicalExpression) enclosedExpression).getLhs();
        operator = ((BinaryLogicalExpression) enclosedExpression).getOperatorToken();
        right = ((BinaryLogicalExpression) enclosedExpression).getRhs();

        Assertions.assertTrue(left instanceof BinaryLogicalExpression);
        Assertions.assertEquals(operator.getKind(), TokenKind.AmpersandAmpersandToken);
        Assertions.assertTrue(right instanceof BinaryLogicalExpression);

        var aGreaterThan10 = (BinaryLogicalExpression) left;
        Assertions.assertNotNull(aGreaterThan10);
        var aIdentifier = aGreaterThan10.getLhs();
        var greaterOperator = aGreaterThan10.getOperatorToken();
        var tenLiteral = aGreaterThan10.getRhs();

        Assertions.assertTrue(aIdentifier instanceof IdentifierExpression);
        Assertions.assertEquals(((IdentifierExpression) aIdentifier).getIdentifierToken().getValue(), "a");
        Assertions.assertEquals(greaterOperator.getKind(), TokenKind.GreaterToken);
        Assertions.assertTrue(tenLiteral instanceof LiteralExpression);
        Assertions.assertEquals(((LiteralExpression) tenLiteral).getValueToken().getValue(), 10);

        var bLessEqualsThan12 = (BinaryLogicalExpression) right;
        Assertions.assertNotNull(bLessEqualsThan12);

        var bIdentifier = bLessEqualsThan12.getLhs();
        var lessEquals = bLessEqualsThan12.getOperatorToken();
        var twelveLiteral = bLessEqualsThan12.getRhs();

        Assertions.assertTrue(bIdentifier instanceof IdentifierExpression);
        Assertions.assertEquals(((IdentifierExpression) bIdentifier).getIdentifierToken().getValue(), "b");
        Assertions.assertEquals(lessEquals.getKind(), TokenKind.LessEqualsToken);
        Assertions.assertTrue(twelveLiteral instanceof LiteralExpression);
        Assertions.assertEquals(((LiteralExpression) twelveLiteral).getValueToken().getValue(), 12);
    }

    @Test
    void testIdentifierParsesAsNullPredicate() throws Exception {
        var result = new PredicateParser("identifierName").parse();
        Assertions.assertNull(result);
    }

    @Test
    void testLiteralsParseAsNullPredicate() throws Exception {
        var result = new PredicateParser("123").parse();
        Assertions.assertNull(result);

        result = new PredicateParser("\"HELLO\"").parse();
        Assertions.assertNull(result);

        result = new PredicateParser("3.1415296").parse();
        Assertions.assertNull(result);
    }

    @Test
    void testBadPredicate() {
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("a >").parse());
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("2 >").parse());
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("a >=").parse());
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("a >= 1 &&").parse());
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("a >= 5 && b >").parse());
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("b != 5 && a !=").parse());
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("b = \" && a !=").parse());
        Assertions.assertThrows(DBError.class, () -> new PredicateParser("b = \" && a != 6").parse());
    }
}
