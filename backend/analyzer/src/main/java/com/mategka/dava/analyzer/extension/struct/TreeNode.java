package com.mategka.dava.analyzer.extension.struct;

import com.mategka.dava.analyzer.collections.Stack;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.traitlike.ParameterizedIterable;
import com.mategka.dava.analyzer.extension.traitlike.ParameterizedStreamable;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class TreeNode<T> implements ParameterizedIterable<TreeNode<T>, TreeOrder>, ParameterizedStreamable<TreeNode<T>, AnStream<TreeNode<T>>, TreeOrder> {

  @Accessors(fluent = true)
  @Getter(lazy = true)
  private final List<TreeNode<T>> children = new ArrayList<>();
  @Setter
  private T value;
  private TreeNode<T> parent = null;

  public TreeNode(T value) {
    this.value = value;
  }

  public void merge(@NotNull TreeNode<T> otherRoot, BiPredicate<TreeNode<T>, TreeNode<T>> equals) {
    for (TreeNode<T> otherChild : otherRoot.children()) {
      var match = children().stream().filter(c -> equals.test(c, otherChild)).findFirst();
      var thisChild = match.orElseGet(() -> addByValue(otherChild.value()));
      thisChild.merge(otherChild, equals);
    }
  }

  public void add(TreeNode<T> child) {
    children().add(child);
    child.parent = this;
  }

  public TreeNode<T> addByValue(T childValue) {
    var child = new TreeNode<>(childValue);
    add(child);
    return child;
  }

  public void addAll(Collection<TreeNode<T>> children) {
    children().addAll(children);
    children.forEach(child -> child.parent = this);
  }

  public Collection<TreeNode<T>> addAllByValue(Collection<? extends T> childValues) {
    var children = childValues.stream().<TreeNode<T>>map(TreeNode::new).toList();
    addAll(children);
    return children;
  }

  public Option<TreeNode<T>> find(T value) {
    Queue<TreeNode<T>> queue = new ArrayDeque<>();
    queue.add(this);
    while (!queue.isEmpty()) {
      var node = queue.remove();
      if (node.value().equals(value)) {
        return Option.Some(node);
      }
      queue.addAll(node.children());
    }
    return Option.None();
  }

  public int index() {
    if (parent == null) {
      return -1;
    }
    return AnStream.fromIndexed(parent.children())
      .filter(Pair.filteringLeft(n -> n == this))
      .map(Pair::right)
      .findFirstAsOption()
      .getOrElse(-1);
  }

  public boolean isRoot() {
    return parent == null;
  }

  @Override
  public @NotNull Iterator<TreeNode<T>> iterator(TreeOrder order) {
    return switch (order) {
      case null -> new PreorderIterator();
      case PREORDER -> new PreorderIterator();
      case POSTORDER -> new PostorderIterator();
    };
  }

  @Override
  public @NotNull AnStream<TreeNode<T>> stream(TreeOrder order) {
    return AnStream.from(Spliterators.spliteratorUnknownSize(iterator(order), Spliterator.ORDERED));
  }

  /**
   * Returns the root of a new tree consisting of this tree with all values mapped using the given mapping function.
   *
   * @param mapper the mapping function
   * @param <U>    the mapping target type for tree node values
   * @return a new tree with the mapped values
   */
  public <U> TreeNode<U> mapValues(@NotNull Function<? super T, U> mapper) {
    var root = new TreeNode<>(mapper.apply(value));
    root.addAll(children().stream().map(c -> c.mapValues(mapper)).toList());
    return root;
  }

  public <U> TreeNode<U> mapNodes(@NotNull Function<? super TreeNode<T>, U> mapper) {
    var root = new TreeNode<>(mapper.apply(this));
    root.addAll(children().stream().map(c -> c.mapNodes(mapper)).toList());
    return root;
  }

  public TreeNode<T> copy() {
    return mapValues(Function.identity());
  }

  public Option<TreeNode<T>> parent() {
    return Options.fromNullable(parent);
  }

  @CanIgnoreReturnValue
  public boolean remove(TreeNode<T> child) {
    if (child.parent != this) {
      return false;
    }
    child.parent = null;
    children().remove(child);
    return true;
  }

  @CanIgnoreReturnValue
  public boolean removeByValue(T childValue) {
    var match = children().stream()
      .filter(n -> n.value().equals(childValue))
      .findFirst();
    return match.map(this::remove).orElse(false);
  }

  @CanIgnoreReturnValue
  public boolean removeAll(Collection<TreeNode<T>> children) {
    Set<TreeNode<T>> childrenToRemove = new HashSet<>(children);
    childrenToRemove.retainAll(children());
    childrenToRemove.forEach(childToRemove -> {
      childToRemove.parent = null;
      children().remove(childToRemove);
    });
    return !childrenToRemove.isEmpty();
  }

  @CanIgnoreReturnValue
  public boolean removeAllByValue(Collection<? extends T> childValues) {
    Set<T> childValuesToRemove = new HashSet<>(childValues);
    var removed = false;
    for (TreeNode<T> child : children()) {
      if (childValuesToRemove.contains(child.value())) {
        remove(child);
        removed = true;
      }
    }
    return removed;
  }

  @CanIgnoreReturnValue
  public boolean removeFromParent() {
    if (parent == null) {
      return false;
    }
    parent.remove(this);
    return true;
  }

  public TreeNode<T> root() {
    // Iterative implementation to save some memory
    var current = this;
    while (current.parent != null) {
      current = current.parent;
    }
    return current;
  }

  @Contract(mutates = "this")
  public TreeNode<T> toRoot() {
    removeFromParent();
    return this;
  }

  public T value() {
    return value;
  }

  private final class PreorderIterator implements Iterator<TreeNode<T>> {

    private final Stack<TreeNode<T>> stack = new Stack<>();

    {
      stack.push(TreeNode.this);
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }

    @Override
    public @NotNull TreeNode<T> next() {
      if (stack.isEmpty()) {
        throw new NoSuchElementException();
      }
      var next = stack.pop();
      stack.pushAll(next.children().reversed());
      return next;
    }

  }

  private final class PostorderIterator implements Iterator<TreeNode<T>> {

    private final Stack<TreeNode<T>> stack = new Stack<>();

    {
      pushLeftDescent(TreeNode.this);
    }

    @Override
    public boolean hasNext() {
      return !stack.isEmpty();
    }

    @Override
    public @NotNull TreeNode<T> next() {
      if (stack.isEmpty()) {
        throw new NoSuchElementException();
      }
      var next = stack.pop();
      if (!stack.isEmpty()) {
        int index = next.index();
        var siblingsAndThis = parent.children();
        if (index < siblingsAndThis.size() - 1) {
          pushLeftDescent(siblingsAndThis.get(index + 1));
        }
      }
      return next;
    }

    private void pushLeftDescent(TreeNode<T> node) {
      while (node != null) {
        stack.push(node);
        node = Options.getFirst(node.children()).getOrNull();
      }
    }

  }

}
