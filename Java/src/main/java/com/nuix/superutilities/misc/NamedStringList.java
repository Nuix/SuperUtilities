package com.nuix.superutilities.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class NamedStringList {
	private String name = "";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	private List<String> values = new ArrayList<String>();

	public void forEach(Consumer<? super String> action) {
		values.forEach(action);
	}

	public int size() {
		return values.size();
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public boolean contains(Object o) {
		return values.contains(o);
	}

	public Iterator<String> iterator() {
		return values.iterator();
	}

	public Object[] toArray() {
		return values.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return values.toArray(a);
	}

	public boolean add(String e) {
		return values.add(e);
	}

	public boolean remove(Object o) {
		return values.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return values.containsAll(c);
	}

	public boolean addAll(Collection<? extends String> c) {
		return values.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends String> c) {
		return values.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return values.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return values.retainAll(c);
	}

	public void replaceAll(UnaryOperator<String> operator) {
		values.replaceAll(operator);
	}

	public boolean removeIf(Predicate<? super String> filter) {
		return values.removeIf(filter);
	}

	public void sort(Comparator<? super String> c) {
		values.sort(c);
	}

	public void clear() {
		values.clear();
	}

	public boolean equals(Object o) {
		return values.equals(o);
	}

	public int hashCode() {
		return values.hashCode();
	}

	public String get(int index) {
		return values.get(index);
	}

	public String set(int index, String element) {
		return values.set(index, element);
	}

	public void add(int index, String element) {
		values.add(index, element);
	}

	public Stream<String> stream() {
		return values.stream();
	}

	public String remove(int index) {
		return values.remove(index);
	}

	public Stream<String> parallelStream() {
		return values.parallelStream();
	}

	public int indexOf(Object o) {
		return values.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return values.lastIndexOf(o);
	}

	public ListIterator<String> listIterator() {
		return values.listIterator();
	}

	public ListIterator<String> listIterator(int index) {
		return values.listIterator(index);
	}

	public List<String> subList(int fromIndex, int toIndex) {
		return values.subList(fromIndex, toIndex);
	}

	public Spliterator<String> spliterator() {
		return values.spliterator();
	}
	
	
}
