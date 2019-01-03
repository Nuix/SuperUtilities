package com.nuix.superutilities.reporting;

import java.util.function.BiFunction;

import nuix.Case;

/***
 * A {@link ColumnValueGenerator} which uses the provided BiFunction expression to calculate its value.
 * @author Jason Wells
 *
 */
public class ScriptedColumnValueGenerator extends ColumnValueGenerator {
	BiFunction<Case,String,Object> expression = null;
	
	/***
	 * Creates a new instance which will used the specified expression.
	 * @param label The label to use when generating a report
	 * @param expression The expression used to calculate this column's value.  Expression will be provided a Nuix case object and query String and is expected to return a value such as a string or integer.
	 */
	public ScriptedColumnValueGenerator(String label, BiFunction<Case,String,Object> expression) {
		this.label = label;
		this.expression = expression;
	}

	@Override
	public Object generateValue(Case nuixCase, String query) {
		return expression.apply(nuixCase,query);
	}
}
