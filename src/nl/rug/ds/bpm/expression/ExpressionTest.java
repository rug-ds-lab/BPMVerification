package nl.rug.ds.bpm.expression;

public class ExpressionTest {

	public static void main(String[] args) {
		Expression<String> e = new Expression<String>("X", ExpressionType.EQ, "Bla");
		Expression<String> e2 = new Expression<String>("X", ExpressionType.NEQ, "Bla");
		
		Expression<Integer> e3 = new Expression<Integer>("X", ExpressionType.EQ, 5);
		Expression<Double> e4 = new Expression<Double>("X", ExpressionType.EQ, 5.0);
		
		System.out.println(e.accepts("Bo"));
		System.out.println(e.accepts("Bla"));
		System.out.println();
		
		System.out.println(e2.accepts("Bo"));
		System.out.println(e2.accepts("Bla"));
		System.out.println();
		
		System.out.println(e.contradicts(e2));		
		System.out.println();
		
		System.out.println(e3.getValue());
		System.out.println(e4.getValue());
		System.out.println(e3.contradicts(e4));
	}

}
