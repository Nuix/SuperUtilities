package com.nuix.superutilities.query;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;

public class QueryHelper {
	/***
	 * Builds an item-date Nuix range query for items with a date occurring within the specified year.
	 * @param year The year the range query should constrain to.  Value should be a valid 4 digit year (2019 not 19).
	 * @return A Nuix range query for item dates that occurred within the given year, i.e. item-date:[20190101 TO 20191231]
	 */
	public static String yearRangeQuery(int year) {
		// Unlikely someone would intentionally provide year in this range, this is here
		// mostly to catch accidentally odd values
		if (year < 1000 || year > 9999) {
			throw new IllegalArgumentException(String.format("Year %s is a non-sensical value",year));
		}
		
		return String.format("item-date:[%s0101 TO %s1231]", year, year);
	}
	
	/***
	 * Builds an item-date range query for items with a date occurring within the specified year and month.  For example
	 * a year input of 2019 and month input of 6 should yield the query: item-date:[20190601 TO 20190630]
	 * @param year The year the range query should constrain to.  Value should be a valid 4 digit year (2019 not 19).
	 * @param month The month of the year, valid values are 1 through 12
	 * @return
	 */
	public static String yearMonthRangeQuery(int year, int month) {
		// Unlikely someone would intentionally provide year in this range, this is here
		// mostly to catch accidentally odd values
		if (year < 1000 || year > 9999) {
			throw new IllegalArgumentException(String.format("Year %s is a non-sensical value",year));
		}
		
		// Make sure a valid month number was provided
		if(month < 1 || month > 12) {
			throw new IllegalArgumentException(String.format("Month %s does fall in valid range of 1-12",month));
		}
		
		// We need to get a date time which exists within the given month and year
		DateTime instant = new DateTime(year,month,1,1,1);
		// Determine the last day of the given month on the given year
		int lastDayOfMonth = instant.dayOfMonth().getMaximumValue();
		// Build a query for item dates within the same year/month
		return String.format("item-date:[%d%02d01 TO %d%02d%02d]", year,month,year,month,lastDayOfMonth);
	}
	
	/***
	 * Joins a series of expressions by " AND ", so expressions "cat", "dog" would become "cat AND dog".  Nil/null values and values containing only
	 * whitespace characters in the provided expressions collection are ignored.
	 * @param expressions Collection of expressions to join together.
	 * @return The expressions AND'ed together.
	 */
	public static String joinByAnd(Collection<String> expressions) {
		List<String> updatedExpressionsCollection = expressions	.stream()
				.filter(e -> e != null && e.trim().isEmpty() == false)
				.collect(Collectors.toList()); 
		return String.join(" AND ", updatedExpressionsCollection);
	}
	
	/***
	 * Joins a series of expressions by " AND " after wrapping each expression in parentheses.
	 * So expressions "cat", "dog" would become "(cat) AND (dog)".  Nil/null values and values containing only
	 * whitespace characters in the provided expressions collection are ignored.
	 * @param expressions Collection of expressions to join together.
	 * @return The expressions surrounded by parentheses and AND'ed together.
	 */
	public static String parenThenJoinByAnd(Collection<String> expressions) {
		List<String> updatedExpressionsCollection = expressions.stream()
				.filter(e -> e != null && e.trim().isEmpty() == false)
				.map(e -> String.format("(%s)", e))
				.collect(Collectors.toList()); 
		return String.join(" AND ", updatedExpressionsCollection);
	}
	
	/***
	 * Joins a series of expressions by " OR ", so expressions "cat", "dog" would become "cat OR dog".  Nil/null values and values containing only
	 * whitespace characters in the provided expressions collection are ignored.
	 * @param expressions Collection of expressions to join together.
	 * @return The expressions OR'ed together.
	 */
	public static String joinByOr(Collection<String> expressions) {
		List<String> updatedExpressionsCollection = expressions	.stream()
				.filter(e -> e != null && e.trim().isEmpty() == false)
				.collect(Collectors.toList()); 
		return String.join(" OR ", updatedExpressionsCollection);
	}
	
	/***
	 * Joins a series of expressions by " OR " after wrapping each expression in parentheses.
	 * So expressions "cat", "dog" would become "(cat) OR (dog)".  Nil/null values and values containing only
	 * whitespace characters in the provided expressions collection are ignored.
	 * @param expressions Collection of expressions to join together.
	 * @return The expressions surrounded by parentheses and OR'ed together.
	 */
	public static String parenThenJoinByOr(Collection<String> expressions) {
		List<String> updatedExpressionsCollection = expressions.stream()
				.filter(e -> e != null && e.trim().isEmpty() == false)
				.map(e -> String.format("(%s)", e))
				.collect(Collectors.toList()); 
		return String.join(" OR ", updatedExpressionsCollection);
	}
}
