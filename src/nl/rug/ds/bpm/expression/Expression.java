package nl.rug.ds.bpm.expression;

public class Expression<T> {
	private T value;
	private String name;
	private ExpressionType type;
	
	public Expression(String name, ExpressionType type, T value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	public T getValue() {
		return value;
	}
	
	public ExpressionType getExpressionType() {
		return type;
	}
	
	public Boolean accepts(T value) {
		switch (type) {
		case EQ:
			return (this.value.equals(value)); 
		case NEQ:
			return (!this.value.equals(value));
		case LT:
			if ((value instanceof Number) && (this.value instanceof Number)) {
				return ((double)value < (double)this.value);
			}
		case LEQ:
			if ((value instanceof Number) && (this.value instanceof Number)) {
				return ((double)value <= (double)this.value);
			}
		case GT:
			if ((value instanceof Number) && (this.value instanceof Number)) {
				return ((double)value > (double)this.value);
			}
		case GEQ:
			if ((value instanceof Number) && (this.value instanceof Number)) {
				return ((double)value >= (double)this.value);
			}
		}
		return false;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Boolean contradicts(Expression other) {
		if ((this.getClass().equals(other.getClass())) && (this.getName().equals(other.getName()))) {
			switch (type) {
			case EQ:
				return (!other.accepts(this.value)); 
			case NEQ:
				return ((other.getExpressionType() == ExpressionType.NEQ) && (!other.getValue().equals(this.value)));
			case LT:
				if ((other.value instanceof Number) && (this.value instanceof Number)) {
					if (other.getExpressionType() == ExpressionType.GT) {
						return ((double)this.value + 2 < (double)other.value);
					}
					else if (other.getExpressionType() == ExpressionType.GEQ) {
						return ((double)this.value + 1 < (double)other.value);
					}
					else {
						return false;
					}
				}
			case LEQ:
				if ((other.value instanceof Number) && (this.value instanceof Number)) {
					if (other.getExpressionType() == ExpressionType.GT) {
						return ((double)this.value + 1 < (double)other.value);
					}
					else if (other.getExpressionType() == ExpressionType.GEQ) {
						return ((double)this.value < (double)other.value);
					}
					else {
						return false;
					}
				}
			case GT:
				if ((other.value instanceof Number) && (this.value instanceof Number)) {
					if ((other.getExpressionType() == ExpressionType.GT) || (other.getExpressionType() == ExpressionType.GEQ)) {
						return false;
					}
					else {
						return other.contradicts(this);
					}
				}
			case GEQ:
				if ((other.value instanceof Number) && (this.value instanceof Number)) {
					if ((other.getExpressionType() == ExpressionType.GT) || (other.getExpressionType() == ExpressionType.GEQ)) {
						return false;
					}
					else {
						return other.contradicts(this);
					}
				}
			}
		}
		
		return true;
	}
	
	public String getName() {
		return name;
	}
}
