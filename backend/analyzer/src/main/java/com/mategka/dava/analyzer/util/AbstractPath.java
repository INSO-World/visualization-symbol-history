package com.mategka.dava.analyzer.util;

import lombok.NonNull;

import java.io.File;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AbstractPath implements Comparable<AbstractPath>, Iterable<AbstractPath> {

  public static final AbstractPath EMPTY = new AbstractPath(Collections.emptyList());
  private static final String ROOT_SYMBOL = "";
  public static final AbstractPath ROOT = new AbstractPath(List.of(ROOT_SYMBOL));
  private static final String DEFAULT_SEPARATOR = "/";
  private static final String DEFAULT_CURRENT = ".";
  private static final String DEFAULT_PARENT = "..";
  private final List<String> parts;

  private AbstractPath(List<String> parts) {
    this.parts = parts;
  }

  public static AbstractPath of(String path, String separator) {
    requireValidPath(path, separator);
    if (path.isEmpty()) {
      return EMPTY;
    }
    return path.startsWith(separator) ? absolute(path, separator) : relative(path, separator);
  }

  public static AbstractPath of(List<String> components) {
    if (components.isEmpty()) {
      return EMPTY;
    }
    return new AbstractPath(components);
  }

  public static AbstractPath relative(String path, String separator) {
    requireValidPath(path, separator);
    if (path.isEmpty()) {
      return EMPTY;
    }
    var parts = List.of(path.split(Pattern.quote(separator)));
    if (parts.getFirst().isEmpty()) {
      parts.removeFirst();
    }
    return new AbstractPath(parts);
  }

  public static AbstractPath relative(String path) {
    return relative(path, DEFAULT_SEPARATOR);
  }

  private static AbstractPath relativeSegment(String segment) {
    if (segment.isEmpty()) {
      return EMPTY;
    }
    return new AbstractPath(List.of(segment));
  }

  public static AbstractPath absolute(String path, String separator) {
    requireValidPath(path, separator);
    if (path.isEmpty()) {
      return EMPTY;
    }
    var relativePath = path.startsWith(separator) ? path.substring(separator.length()) : path;
    return ROOT.resolve(relativePath, separator);
  }

  public static AbstractPath absolute(String path) {
    return absolute(path, DEFAULT_SEPARATOR);
  }

  private static void requireValidPath(String path, String separator) {
    if (path.contains(separator + separator)) {
      throw new IllegalArgumentException("Path contains empty segment based on separator \"%s\"".formatted(separator));
    }
  }

  public boolean isEmpty() {
    return parts.isEmpty();
  }

  public boolean isRoot() {
    return parts.size() == 1 && Objects.equals(parts.getFirst(), ROOT_SYMBOL);
  }

  /**
   * Tells whether or not this path is absolute.
   *
   * <p> An absolute path is complete in that it doesn't need to be combined
   * with other path information in order to locate a file.
   *
   * @return {@code true} if, and only if, this path is absolute
   */
  public boolean isAbsolute() {
    return !isEmpty() && Objects.equals(parts.getFirst(), ROOT_SYMBOL);
  }

  public boolean isRelative() {
    return !isAbsolute();
  }

  /**
   * Returns the root component of this path as a {@code Path} object,
   * or {@code null} if this path does not have a root component.
   *
   * @return a path representing the root component of this path,
   * or {@code null}
   */
  public AbstractPath getRoot() {
    return isAbsolute() ? ROOT : null;
  }

  /**
   * Returns the name of the file or directory denoted by this path as a
   * {@code Path} object. The file name is the <em>farthest</em> element from
   * the root in the directory hierarchy.
   *
   * @return a path representing the name of the file or directory, or
   * {@code null} if this path has zero elements
   */
  public AbstractPath getFileName() {
    if (isEmpty() || isRoot()) {
      return null;
    }
    return AbstractPath.relativeSegment(parts.getLast());
  }

  /**
   * Returns the <em>parent path</em>, or {@code null} if this path does not
   * have a parent.
   *
   * <p> The parent of this path object consists of this path's root
   * component, if any, and each element in the path except for the
   * <em>farthest</em> from the root in the directory hierarchy. This method
   * does not access the file system; the path or its parent may not exist.
   * Furthermore, this method does not eliminate special names such as "."
   * and ".." that may be used in some implementations. On UNIX for example,
   * the parent of "{@code /a/b/c}" is "{@code /a/b}", and the parent of
   * {@code "x/y/.}" is "{@code x/y}". This method may be used with the {@link
   * #normalize normalize} method, to eliminate redundant names, for cases where
   * <em>shell-like</em> navigation is required.
   *
   * <p> If this path has more than one element, and no root component, then
   * this method is equivalent to evaluating the expression:
   * {@snippet lang = java:
   *     subpath(0, getNameCount()-1);
   *}
   *
   * @return a path representing the path's parent
   */
  public AbstractPath getParent() {
    if (isRoot() || isEmpty()) {
      return null;
    }
    return new AbstractPath(parts.subList(0, parts.size() - 1));
  }

  /**
   * Returns the number of name elements in the path.
   *
   * @return the number of elements in the path, or {@code 0} if this path
   * only represents a root component
   */
  public int getNameCount() {
    return isAbsolute() ? parts.size() - 1 : parts.size();
  }

  /**
   * Returns a name element of this path as a {@code Path} object.
   *
   * <p> The {@code index} parameter is the index of the name element to return.
   * The element that is <em>closest</em> to the root in the directory hierarchy
   * has index {@code 0}. The element that is <em>farthest</em> from the root
   * has index {@link #getNameCount count}{@code -1}.
   *
   * @param index the index of the element
   * @return the name element
   * @throws IllegalArgumentException if {@code index} is negative, {@code index} is greater than or
   *                                  equal to the number of elements, or this path has zero name
   *                                  elements
   */
  public AbstractPath getName(int index) {
    return AbstractPath.relativeSegment(isAbsolute() ? parts.get(index + 1) : parts.get(index));
  }

  /**
   * Returns a relative {@code Path} that is a subsequence of the name
   * elements of this path.
   *
   * <p> The {@code beginIndex} and {@code endIndex} parameters specify the
   * subsequence of name elements. The name that is <em>closest</em> to the root
   * in the directory hierarchy has index {@code 0}. The name that is
   * <em>farthest</em> from the root has index {@link #getNameCount
   * count}{@code -1}. The returned {@code Path} object has the name elements
   * that begin at {@code beginIndex} and extend to the element at index {@code
   * endIndex-1}.
   *
   * @param beginIndex the index of the first element, inclusive
   * @param endIndex   the index of the last element, exclusive
   * @return a new {@code Path} object that is a subsequence of the name
   * elements in this {@code Path}
   * @throws IllegalArgumentException if {@code beginIndex} is negative, or greater than or equal to
   *                                  the number of elements. If {@code endIndex} is less than or
   *                                  equal to {@code beginIndex}, or larger than the number of elements.
   */
  public AbstractPath subpath(int beginIndex, int endIndex) {
    int offset = isAbsolute() ? 1 : 0;
    return new AbstractPath(parts.subList(beginIndex + offset, endIndex + offset));
  }

  /**
   * Tests if this path starts with the given path.
   *
   * <p> This path <em>starts</em> with the given path if this path's root
   * component <em>starts</em> with the root component of the given path,
   * and this path starts with the same name elements as the given path.
   * If the given path has more name elements than this path then {@code false}
   * is returned.
   *
   * @param other the given path
   * @return {@code true} if this path starts with the given path; otherwise
   * {@code false}
   */
  public boolean startsWith(AbstractPath other) {
    int compareCount = other.parts.size();
    if (compareCount > parts.size()) {
      return false;
    }
    return parts.subList(0, compareCount).equals(other.parts);
  }

  /**
   * Tests if this path starts with an {@code AbstractPath}, constructed by converting
   * the given path string, in exactly the manner specified by the {@link
   * #startsWith(AbstractPath) startsWith(AbstractPath)} method.
   *
   * @param other the given path string
   * @return {@code true} if this path starts with the given path; otherwise
   * {@code false}
   * @throws InvalidPathException If the path string cannot be converted to a Path.
   * @implSpec The default implementation is equivalent for this path to:
   * {@snippet lang = java:
   *     startsWith(getFileSystem().getPath(other));
   *}
   */
  public boolean startsWith(String other) {
    return startsWith(other, DEFAULT_SEPARATOR);
  }

  public boolean startsWith(String other, String separator) {
    return startsWith(AbstractPath.of(other, separator));
  }

  /**
   * Tests if this path ends with the given path.
   *
   * <p> If the given path has <em>N</em> elements, and no root component,
   * and this path has <em>N</em> or more elements, then this path ends with
   * the given path if the last <em>N</em> elements of each path, starting at
   * the element farthest from the root, are equal.
   *
   * <p> If the given path has a root component then this path ends with the
   * given path if the root component of this path <em>ends with</em> the root
   * component of the given path, and the corresponding elements of both paths
   * are equal. If this path does not have a root component and the given path
   * has a root component then this path does not end with the given path.
   *
   * @param other the given path
   * @return {@code true} if this path ends with the given path; otherwise
   * {@code false}
   */
  public boolean endsWith(AbstractPath other) {
    if (isRelative() && other.isAbsolute()) {
      return false;
    }
    int compareCount = other.parts.size();
    int end = parts.size();
    if (compareCount > end) {
      return false;
    }
    return parts.subList(end - compareCount, end).equals(other.parts);
  }

  /**
   * Tests if this path ends with a {@code Path}, constructed by converting
   * the given path string, in exactly the manner specified by the {@link
   * #endsWith(AbstractPath) endsWith(AbstractPath)} method. On UNIX for example, the path
   * "{@code foo/bar}" ends with "{@code foo/bar}" and "{@code bar}". It does
   * not end with "{@code r}" or "{@code /bar}". Note that trailing separators
   * are not taken into account, and so invoking this method on the {@code
   * Path} "{@code foo/bar}" with the {@code String} "{@code bar/}" returns
   * {@code true}.
   *
   * @param other the given path string
   * @return {@code true} if this path ends with the given path; otherwise
   * {@code false}
   * @throws InvalidPathException If the path string cannot be converted to a Path.
   * @implSpec The default implementation is equivalent for this path to:
   * {@snippet lang = java:
   *     endsWith(getFileSystem().getPath(other));
   *}
   */
  public boolean endsWith(String other) {
    return endsWith(other, DEFAULT_SEPARATOR);
  }

  public boolean endsWith(String other, String separator) {
    return endsWith(AbstractPath.of(other, separator));
  }

  /**
   * Returns a path that is this path with redundant name elements eliminated.
   *
   * <p> The precise definition of this method is implementation dependent but
   * in general it derives from this path, a path that does not contain
   * <em>redundant</em> name elements. In many file systems, the "{@code .}"
   * and "{@code ..}" are special names used to indicate the current directory
   * and parent directory. In such file systems all occurrences of "{@code .}"
   * are considered redundant. If a "{@code ..}" is preceded by a
   * non-"{@code ..}" name then both names are considered redundant (the
   * process to identify such names is repeated until it is no longer
   * applicable).
   *
   * <p> This method does not access the file system; the path may not locate
   * a file that exists. Eliminating "{@code ..}" and a preceding name from a
   * path may result in the path that locates a different file than the original
   * path. This can arise when the preceding name is a symbolic link.
   *
   * @return the resulting path or this path if it does not contain
   * redundant name elements; an empty path is returned if this path
   * does not have a root component and all name elements are redundant
   * @see #getParent
   */
  public AbstractPath normalize() {
    return normalize(DEFAULT_PARENT, DEFAULT_CURRENT);
  }

  public AbstractPath normalize(String parentPattern, String currentPattern) {
    List<String> newParts = new ArrayList<>(parts.size());
    int skipCount = 0;
    for (int i = parts.size() - 1; i >= 0; i--) {
      String part = parts.get(i);
      if (Objects.equals(part, parentPattern)) {
        skipCount++;
        continue;
      }
      if (skipCount > 0) {
        skipCount--;
        continue;
      }
      if (Objects.equals(part, currentPattern)) {
        continue;
      }
      newParts.add(part);
    }
    if (skipCount > 0) {
      throw new IllegalStateException(
        "Path normalization through parent pattern \"%s\" resulted in attempted sub-root navigation".formatted(
          parentPattern));
    }
    Collections.reverse(newParts);
    return new AbstractPath(newParts);
  }

  /**
   * Resolve the given path against this path.
   *
   * <p> If the {@code other} parameter is an {@link #isAbsolute() absolute}
   * path then this method trivially returns {@code other}. If {@code other}
   * is an <i>empty path</i> then this method trivially returns this path.
   * Otherwise, this method considers this path to be a directory and resolves
   * the given path against this path. In the simplest case, the given path
   * does not have a {@link #getRoot root} component, in which case this method
   * <em>joins</em> the given path to this path and returns a resulting path
   * that {@link #endsWith ends} with the given path. Where the given path has
   * a root component then resolution is highly implementation dependent and
   * therefore unspecified.
   *
   * @param other the path to resolve against this path
   * @return the resulting path
   * @see #relativize
   */
  public AbstractPath resolve(AbstractPath other) {
    if (other.isAbsolute() || isEmpty()) {
      return other;
    }
    if (other.isEmpty()) {
      return this;
    }
    return new AbstractPath(Stream.concat(parts.stream(), other.parts.stream()).toList());
  }

  /**
   * Converts a given path string to a {@code Path} and resolves it against
   * this {@code Path} in exactly the manner specified by the {@link
   * #resolve(AbstractPath) resolve} method. For example, suppose that the name
   * separator is "{@code /}" and a path represents "{@code foo/bar}", then
   * invoking this method with the path string "{@code gus}" will result in
   * the {@code Path} "{@code foo/bar/gus}".
   *
   * @param other the path string to resolve against this path
   * @return the resulting path
   * @throws InvalidPathException if the path string cannot be converted to a Path.
   * @implSpec The default implementation is equivalent for this path to:
   * {@snippet lang = java:
   *     resolve(getFileSystem().getPath(other));
   *}
   * @see FileSystem#getPath
   */
  public AbstractPath resolve(String other) {
    return resolve(other, DEFAULT_SEPARATOR);
  }

  public AbstractPath resolve(String other, String separator) {
    return resolve(AbstractPath.of(other, separator));
  }

  /**
   * Resolves the given path against this path's {@link #getParent parent}
   * path. This is useful where a file name needs to be <i>replaced</i> with
   * another file name. For example, suppose that the name separator is
   * "{@code /}" and a path represents "{@code dir1/dir2/foo}", then invoking
   * this method with the {@code Path} "{@code bar}" will result in the {@code
   * Path} "{@code dir1/dir2/bar}". If this path does not have a parent path,
   * or {@code other} is {@link #isAbsolute() absolute}, then this method
   * returns {@code other}. If {@code other} is an empty path then this method
   * returns this path's parent, or where this path doesn't have a parent, the
   * empty path.
   *
   * @param other the path to resolve against this path's parent
   * @return the resulting path
   * @implSpec The default implementation is equivalent for this path to:
   * {@snippet lang = java:
   *     (getParent() == null) ? other : getParent().resolve(other);
   *}
   * unless {@code other == null}, in which case a
   * {@code NullPointerException} is thrown.
   * @see #resolve(AbstractPath)
   */
  public AbstractPath resolveSibling(AbstractPath other) {
    AbstractPath parent = getParent();
    return (parent == null) ? other : parent.resolve(other);
  }

  /**
   * Converts a given path string to a {@code Path} and resolves it against
   * this path's {@link #getParent parent} path in exactly the manner
   * specified by the {@link #resolveSibling(AbstractPath) resolveSibling} method.
   *
   * @param other the path string to resolve against this path's parent
   * @return the resulting path
   * @throws InvalidPathException if the path string cannot be converted to a Path.
   * @implSpec The default implementation is equivalent for this path to:
   * {@snippet lang = java:
   *     resolveSibling(getFileSystem().getPath(other));
   *}
   * @see FileSystem#getPath
   */
  public AbstractPath resolveSibling(String other) {
    return resolveSibling(other, DEFAULT_SEPARATOR);
  }

  public AbstractPath resolveSibling(String other, String separator) {
    return resolveSibling(AbstractPath.of(other, separator));
  }

  /**
   * Constructs a relative path between this path and a given path.
   *
   * <p> Relativization is the inverse of {@link #resolve(AbstractPath) resolution}.
   * This method attempts to construct a {@link #isRelative relative} path
   * that when {@link #resolve(AbstractPath) resolved} against this path, yields a
   * path that locates the same file as the given path. For example, on UNIX,
   * if this path is {@code "/a/b"} and the given path is {@code "/a/b/c/d"}
   * then the resulting relative path would be {@code "c/d"}. Where this
   * path and the given path do not have a {@link #getRoot root} component,
   * then a relative path can be constructed. A relative path cannot be
   * constructed if only one of the paths have a root component. Where both
   * paths have a root component then it is implementation dependent if a
   * relative path can be constructed. If this path and the given path are
   * {@link #equals equal} then an <i>empty path</i> is returned.
   *
   * <p> For any two {@link #normalize normalized} paths <i>p</i> and
   * <i>q</i>, where <i>q</i> does not have a root component,
   * <blockquote>
   * <i>p</i>{@code .relativize(}<i>p</i>
   * {@code .resolve(}<i>q</i>{@code )).equals(}<i>q</i>{@code )}
   * </blockquote>
   *
   * <p> When symbolic links are supported, then whether the resulting path,
   * when resolved against this path, yields a path that can be used to locate
   * the {@link Files#isSameFile same} file as {@code other} is implementation
   * dependent. For example, if this path is  {@code "/a/b"} and the given
   * path is {@code "/a/x"} then the resulting relative path may be {@code
   * "../x"}. If {@code "b"} is a symbolic link then is implementation
   * dependent if {@code "a/b/../x"} would locate the same file as {@code "/a/x"}.
   *
   * @param other the path to relativize against this path
   * @return the resulting relative path, or an empty path if both paths are
   * equal
   * @throws IllegalArgumentException if {@code other} is not a {@code Path} that can be relativized
   *                                  against this path
   */
  public AbstractPath relativize(AbstractPath other) {
    if (!other.startsWith(this)) {
      throw new IllegalArgumentException(
        "Given path is not a subpath of this path and can therefore not be relativized");
    }
    return new AbstractPath(other.parts.subList(parts.size(), other.parts.size()));
  }

  /**
   * Returns a URI to represent this path.
   *
   * <p> This method constructs an absolute {@link URI} with a {@link
   * URI#getScheme() scheme} equal to the URI scheme that identifies the
   * provider. The exact form of the scheme specific part is highly provider
   * dependent.
   *
   * <p> In the case of the default provider, the URI is hierarchical with
   * a {@link URI#getPath() path} component that is absolute. The query and
   * fragment components are undefined. Whether the authority component is
   * defined or not is implementation dependent. There is no guarantee that
   * the {@code URI} may be used to construct a {@link File java.io.File}.
   * In particular, if this path represents a Universal Naming Convention (UNC)
   * path, then the UNC server name may be encoded in the authority component
   * of the resulting URI. In the case of the default provider, and the file
   * exists, and it can be determined that the file is a directory, then the
   * resulting {@code URI} will end with a slash.
   *
   * <p> The default provider provides a similar <em>round-trip</em> guarantee
   * to the {@link File} class. For a given {@code Path} <i>p</i> it
   * is guaranteed that
   * <blockquote>
   * {@link Path#of(URI) Path.of}{@code (}<i>p</i>{@code .toUri()).equals(}<i>p</i>
   * {@code .}{@link #toAbsolutePath() toAbsolutePath}{@code ())}
   * </blockquote>
   * so long as the original {@code Path}, the {@code URI}, and the new {@code
   * Path} are all created in (possibly different invocations of) the same
   * Java virtual machine. Whether other providers make any guarantees is
   * provider specific and therefore unspecified.
   *
   * <p> When a file system is constructed to access the contents of a file
   * as a file system then it is highly implementation specific if the returned
   * URI represents the given path in the file system, or it represents a
   * <em>compound</em> URI that encodes the URI of the enclosing file system.
   * A format for compound URIs is not defined in this release; such a scheme
   * may be added in a future release.
   *
   * @return the URI representing this path
   */
  public URI toUri() {
    return toPath().toUri();
  }

  /**
   * Returns a {@code Path} object representing the absolute path of this
   * path.
   *
   * @return a {@code Path} object representing the absolute path
   */
  public AbstractPath toAbsolutePath() {
    if (isAbsolute()) {
      return this;
    }
    return ROOT.resolve(this);
  }

  public Path toPath() {
    return null;
  }

  /**
   * Returns an iterator over the name elements of this path.
   *
   * <p> The first element returned by the iterator represents the name
   * element that is closest to the root in the directory hierarchy, the
   * second element is the next closest, and so on. The last element returned
   * is the name of the file or directory denoted by this path. The {@link
   * #getRoot root} component, if present, is not returned by the iterator.
   *
   * @return an iterator over the name elements of this path
   * @implSpec The default implementation returns an {@code Iterator<Path>} which, for
   * this path, traverses the {@code Path}s returned by
   * {@code getName(index)}, where {@code index} ranges from zero to
   * {@code getNameCount() - 1}, inclusive.
   */
  public @NonNull Iterator<AbstractPath> iterator() {
    final int offset = isAbsolute() ? 1 : 0;
    return IntStream.range(0, parts.size() - offset)
      .mapToObj(this::getName)
      .iterator();
  }

  /**
   * Creates a {@link Spliterator} over the elements described by this
   * {@code Iterable}.
   *
   * @return a {@code Spliterator} over the elements described by this
   * {@code Iterable}.
   * @implSpec The default implementation creates an
   * <em><a href="../util/Spliterator.html#binding">early-binding</a></em>
   * spliterator from the iterable's {@code Iterator}.  The spliterator
   * inherits the <em>fail-fast</em> properties of the iterable's iterator.
   * @implNote The default implementation should usually be overridden.  The
   * spliterator returned by the default implementation has poor splitting
   * capabilities, is unsized, and does not report any spliterator
   * characteristics. Implementing classes can nearly always provide a
   * better implementation.
   * @since 1.8
   */
  public Spliterator<AbstractPath> spliterator() {
    final int offset = isAbsolute() ? 1 : 0;
    return IntStream.range(0, parts.size() - offset)
      .mapToObj(this::getName)
      .spliterator();
  }

  /**
   * Compares two abstract paths lexicographically. The ordering defined by
   * this method is provider specific, and in the case of the default
   * provider, platform specific. This method does not access the file system
   * and neither file is required to exist.
   *
   * @param other the path compared to this path.
   * @return zero if the argument is {@link #equals equal} to this path, a
   * value less than zero if this path is lexicographically less than
   * the argument, or a value greater than zero if this path is
   * lexicographically greater than the argument
   */
  @Override
  public int compareTo(@NonNull AbstractPath other) {
    final List<String> s1 = parts;
    final List<String> s2 = other.parts;
    int n1 = s1.size();
    int n2 = s2.size();
    int min = Math.min(n1, n2);
    return IntStream.range(0, min)
      .map(i -> s1.get(i).compareTo(s2.get(i)))
      .filter(c -> c != 0)
      .findFirst()
      .orElse(n1 - n2);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    AbstractPath that = (AbstractPath) o;
    return Objects.equals(parts, that.parts);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(parts);
  }

}
