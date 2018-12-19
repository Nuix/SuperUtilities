package com.nuix.superutilities.reporting;

import java.util.function.BiFunction;

import nuix.Case;

public class ScriptedColumnValueGenerator extends ColumnValueGenerator {
	BiFunction<Case,String,Object> expression = null;
	
	public ScriptedColumnValueGenerator(String label, BiFunction<Case,String,Object> expression) {
		this.label = label;
		this.expression = expression;
	}

	@Override
	public Object generateValue(Case nuixCase, String query) {
		return expression.apply(nuixCase,query);
	}
}
