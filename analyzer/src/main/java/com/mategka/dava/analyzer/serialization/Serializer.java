package com.mategka.dava.analyzer.serialization;

import com.mategka.dava.analyzer.collections.CountingMap;
import com.mategka.dava.analyzer.collections.Stack;
import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.IterablesX;
import com.mategka.dava.analyzer.extension.ListsX;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.option.Options;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.extension.time.ZonedDateTimes;
import com.mategka.dava.analyzer.git.AuthorInfo;
import com.mategka.dava.analyzer.git.CommitInfo;
import com.mategka.dava.analyzer.git.Hash;
import com.mategka.dava.analyzer.serialization.model.*;
import com.mategka.dava.analyzer.struct.CommitDiff;
import com.mategka.dava.analyzer.struct.History;
import com.mategka.dava.analyzer.struct.Strand;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyMap;
import com.mategka.dava.analyzer.struct.property.value.Kind;
import com.mategka.dava.analyzer.struct.property.value.Visibility;
import com.mategka.dava.analyzer.struct.property.value.type.UnknownType;
import com.mategka.dava.analyzer.struct.symbol.Symbol;
import com.mategka.dava.analyzer.struct.symbol.SymbolKey;
import com.mategka.dava.analyzer.util.Benchmark;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import me.tongfei.progressbar.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

@UtilityClass
public class Serializer {

  private final int PROGRESS_STEPS = 5;

  public void writeJson(@NotNull History history, List<CommitInfo> commits, @NotNull String path) throws IOException {
    try (
      ProgressBar progressBar = new ProgressBarBuilder()
        .setInitialMax(PROGRESS_STEPS)
        .setStyle(ProgressBarStyle.ASCII)
        .setTaskName("  Computing JSON")
        .setConsumer(new ConsoleProgressBarConsumer(System.out, 100))
        .build()
    ) {
      var benchmark = Benchmark.start();
      progressBar.setExtraMessage("Compiling authors");
      var strands = history.getStrandDag().nodes();
      List<AuthorInfo> authors = commits.stream()
        .map(CommitInfo::author)
        .distinct()
        .toList();
      Map<AuthorInfo, @NotNull Integer> authorIndex = AnStream.fromIndexed(authors).collect(CollectorsX.pairsToMap());
      progressBar.step();

      progressBar.setExtraMessage("Compiling commits");
      Map<Hash, CommitEntry> commitIndex = AnStream.fromIndexed(commits)
        .map(p -> CommitEntry.fromPair(p, authorIndex))
        .collect(CollectorsX.mapToKey(e -> e.commit().hash()));
      var dateBounds = IterablesX.minmax(commits.stream().map(CommitInfo::date).iterator());
      ZonedDateTime createdAt = dateBounds.map(Pair::left).getOrCompute(ZonedDateTimes::nowWithSecondPrecision);
      ZonedDateTime updatedAt = dateBounds.map(Pair::right).getOrElse(ZonedDateTimes.EPOCH);
      commits.clear();

      List<AuthorDto> authorDtos = getAuthorDtos(authors);
      List<CommitDto> commitDtos = getCommitDtos(commitIndex, history.getStrandMapping());
      progressBar.step();

      progressBar.setExtraMessage("Compiling symbols");
      AbstractIdAssignment assignment = getAbstractIdAssignment(strands);
      Map<@NotNull SymbolKey, @NotNull Long> keysToIds = assignment.keysToIds();

      SymbolStatesResult symbolStatesResult = getSymbolStatesResult(strands, commitIndex, keysToIds);
      ArrayListMultimap<@NotNull Long, @NotNull StateDto> symbolStates = symbolStatesResult.symbolStates();
      Map<@NotNull Long, @NotNull Deletion> deletions = symbolStatesResult.deletions();
      List<SymbolDto> symbolDtos = getSymbolDtos(symbolStates, commitDtos, commitIndex, keysToIds, deletions);
      progressBar.step();

      progressBar.setExtraMessage("Creating indices");
      MetaDto metaDto = MetaDto.builder()
        .name(history.getName())
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .indexedAt(ZonedDateTime.now())
        .commitCount(commitDtos.size())
        .strandCount(strands.size())
        .strandSymbolCount(keysToIds.size())
        .symbolCount(assignment.idCount())
        .build();

      IndexRootDto indexDto = getIndexRootDto(symbolDtos);

      var rootDto = RootDto.builder()
        .meta(metaDto)
        .authors(authorDtos)
        .commits(commitDtos)
        .symbols(symbolDtos)
        .indices(indexDto)
        .build();
      progressBar.step();

      progressBar.setExtraMessage("Writing JSON");
      try (FileOutputStream fos = new FileOutputStream(path)) {
        getObjectMapper().writeValue(fos, rootDto);
        fos.write('\n');
      }
      progressBar.step();

      var time = benchmark.end();
      progressBar.setExtraMessage("Done in %.1f seconds".formatted(time.toMillis() / 1000d));
    }
    System.out.println("Output written to " + Path.of(path));
  }

  private static @NotNull List<AuthorDto> getAuthorDtos(List<AuthorInfo> authors) {
    return AnStream.fromIndexed(authors)
      .map(Pair.folding((author, id) -> AuthorDto.builder()
        .id(id)
        .name(author.name())
        .email(author.email())
        .build()
      ))
      .toList();
  }

  private static void closeOpenKeyDtos(Map<Hash, CommitEntry> commitIndex, Hash commitHash,
                                       Map<Hash, KeyDto> openKeyDtos, ZonedDateTime date) {
    Set<Hash> visited2 = new HashSet<>();
    Queue<Hash> queue = new ArrayDeque<>(commitIndex.get(commitHash).commit().parents());
    while (!queue.isEmpty()) {
      var hash = queue.poll();
      if (visited2.contains(hash)) {
        continue;
      }
      visited2.add(hash);
      if (openKeyDtos.containsKey(hash)) {
        openKeyDtos.remove(hash).setTo(date);
      }
      queue.addAll(commitIndex.get(hash).commit().parents());
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private static @NotNull AbstractIdAssignment getAbstractIdAssignment(Set<Strand> strands) {
    /*
      NOTE: On each strand, a connected symbol (Symbol instances belonging to the same abstract symbol)
            can occur at most twice:
            - A. Once for the entirety of the strand, as an addition or succession that gets succeeded or deleted
            - B. Once at the start, as an addition or succession that then gets deleted mid-strand
            - C. Once at the end, as an addition that then gets succeeded or deleted
            - D. Twice, when both B. and C. apply

            This is because, if a string of Symbol instances belonged to an abstract symbol it is not connected to
            mid-strand, it would have to connect via a different "branch", which would contradict the fact that
            the given string of commits belongs to the same strand (since strands can, by definition, not be
            interrupted).
     */
    long idCounter = 0;
    Map<@NotNull SymbolKey, @NotNull Long> keysToIds = new HashMap<>();
    var expectedNodeCount = AnStream.from(strands)
      .flatMapCollection(Strand::getCommitDiffs)
      .mapToInt(d -> d.getAdditions().size())
      .sum();
    // NOTE: Needs to be a graph structure to support both forward and backward linking in one go
    MutableGraph<SymbolKey> symbolKeyGraph = GraphBuilder.undirected().expectedNodeCount(expectedNodeCount).build();
    for (var strand : strands) {
      for (var diff : strand.getCommitDiffs()) {
        for (var startSymbol : diff.getStartSymbols()) {
          var key = startSymbol.getKey();
          if (startSymbol.getPredecessors().isEmpty()) {
            symbolKeyGraph.addNode(key);
            continue;
          }
          for (var predecessor : startSymbol.getPredecessors()) {
            symbolKeyGraph.putEdge(predecessor.right(), key);
          }
        }
      }
    }
    Set<SymbolKey> visited = new HashSet<>();
    for (var firstKey : symbolKeyGraph.nodes()) {
      if (!visited.contains(firstKey)) {
        long id = idCounter++;
        Stack<SymbolKey> stack = new Stack<>();
        stack.push(firstKey);
        while (!stack.isEmpty()) {
          var nodeKey = stack.pop();
          visited.add(nodeKey);
          keysToIds.put(nodeKey, id);
          for (var neighbor : symbolKeyGraph.adjacentNodes(nodeKey)) {
            if (!visited.contains(neighbor)) {
              stack.push(neighbor);
            }
          }
        }
      }
    }
    return new AbstractIdAssignment(idCounter, keysToIds);
  }

  private static @NotNull List<CommitDto> getCommitDtos(Map<Hash, CommitEntry> commitIndex,
                                                        Map<Hash, Strand> strandMapping) {
    List<CommitDto> commitDtos = new ArrayList<>();
    for (var commitEntry : commitIndex.values()) {
      var commit = commitEntry.commit();
      var commitDto = CommitDto.builder()
        .id(commitEntry.index())
        .hash(commit.hash())
        .date(commit.date())
        .author(commitEntry.authorIndex())
        .summary(commit.summary())
        .desc(commit.description())
        .parents(ListsX.map(commit.parents(), h -> commitIndex.get(h).index()))
        .strand(strandMapping.get(commit.hash()).getId())
        .build();
      commitDtos.add(commitDto);
    }
    commitDtos.sort(Comparator.comparing(CommitDto::getId));
    return commitDtos;
  }

  private static IndexRootDto getIndexRootDto(List<SymbolDto> symbolDtos) {
    Multimap<@NotNull Visibility, @NotNull Long> byVisibility = symbolDtos.stream()
      .flatMap(s -> s.getStates().values().stream()
        .flatMap(Collection::stream)
        .map(t -> t.getProperties().getPropertyValue(VisibilityProperty.class).getOrNull())
        .filter(Objects::nonNull)
        .distinct()
        .map(v -> Pair.of(v, s.getId()))
      )
      .collect(CollectorsX.toMultimap(Pair::left, Pair::right, ArrayListMultimap::create));
    Multimap<@NotNull Kind, @NotNull Long> byKind = symbolDtos.stream()
      .flatMap(s -> s.getKeys().stream()
        .map(KeyDto::getKind)
        .distinct()
        .map(k -> Pair.of(k, s.getId()))
      )
      .collect(CollectorsX.toMultimap(Pair::left, Pair::right, ArrayListMultimap::create));
    Multimap<@NotNull String, @NotNull Long> byType = symbolDtos.stream()
      .flatMap(s -> AnStream.from(s.getStates().values())
        .flatMap(Collection::stream)
        .mapOption(t -> t.getProperties().getPropertyValue(TypeProperty.class))
        .allow(UnknownType.class)
        .map(UnknownType::getQualifiedName)
        .distinct()
        .map(n -> Pair.of(n, s.getId()))
      )
      .collect(CollectorsX.toMultimap(Pair::left, Pair::right, ArrayListMultimap::create));
    Multimap<@NotNull YearMonth, @NotNull Long> byExistence = symbolDtos.stream()
      .flatMap(s -> {
        var start = IterablesX.getFirst(s.getStates().sequencedKeySet());
        YearMonth end;
        if (s.getDeletedAt() != null) {
          end = IterablesX.getFirst(s.getStates().sequencedKeySet().reversed());
        } else {
          end = YearMonth.now();
        }
        return Stream.iterate(start, ym -> ym.compareTo(end) <= 0, ym -> ym.plusMonths(1))
          .map(ym -> Pair.of(ym, s.getId()));
      })
      .collect(CollectorsX.toMultimap(Pair::left, Pair::right, ArrayListMultimap::create));
    Multimap<@NotNull YearMonth, @NotNull Long> byChanged = symbolDtos.stream()
      .flatMap(s -> s.getStates().sequencedKeySet().stream().map(ym -> Pair.of(ym, s.getId())))
      .collect(CollectorsX.toMultimap(Pair::left, Pair::right, ArrayListMultimap::create));
    return IndexRootDto.builder()
      .byVisibility(byVisibility)
      .byKind(byKind)
      .byType(byType)
      .byExistence(byExistence)
      .byChanged(byChanged)
      .build();
  }

  private @NotNull ObjectMapper getObjectMapper() {
    var jsonFactory = new JsonFactory();
    jsonFactory.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    var mapper = new ObjectMapper(jsonFactory);
    mapper.registerModule(new JavaTimeModule());
    var multimapSerializerModule = new SimpleModule();
    multimapSerializerModule.addSerializer(Multimap.class, new MultimapSerializer());
    mapper.registerModule(multimapSerializerModule);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  private static @NotNull List<SymbolDto> getSymbolDtos(
    ArrayListMultimap<@NotNull Long, @NotNull StateDto> symbolStates, List<CommitDto> commitDtos,
    Map<Hash, CommitEntry> commitIndex, Map<@NotNull SymbolKey, @NotNull Long> keysToIds,
    Map<@NotNull Long, @NotNull Deletion> deletions) {
    List<SymbolDto> symbolDtos = new ArrayList<>();
    for (var stateEntry : symbolStates.asMap().entrySet()) {
      var id = stateEntry.getKey();
      var states = stateEntry.getValue();
      var sortedStates = states.stream().sorted(
        Comparator.comparing(s -> commitDtos.get(s.getCommit()).getDate())
      ).toList();
      SortedMap<@NotNull YearMonth, List<StateDto>> groupedStates = new TreeMap<>();
      CountingMap<@NotNull Integer> authorContributions = new CountingMap<>();
      int totalContributions = 0;
      for (var state : sortedStates) {
        var commitDto = commitDtos.get(state.getCommit());
        var yearMonth = YearMonth.from(commitDto.getDate());
        groupedStates.computeIfAbsent(yearMonth, k -> new ArrayList<>()).add(state);
        if (state.getMainEvent().getCategory().getValue() > EventCategory.MINISCULE.getValue()) {
          var authorIndex = commitIndex.get(commitDto.getHash()).authorIndex();
          authorContributions.increment(authorIndex);
          totalContributions++;
        }
      }
      var contributions = PercentageSums.getPercentages(authorContributions, totalContributions)
        .entrySet().stream()
        .map(e -> ContributionDto.builder()
          .author(e.getKey())
          .percent(e.getValue())
          .build()
        )
        .sorted((a, b) -> b.getPercent() - a.getPercent())
        .toList();
      List<KeyDto> keyDtos = new ArrayList<>();
      Map<Hash, KeyDto> openKeyDtos = new HashMap<>();
      for (var state : sortedStates) {
        final var commitDto = commitDtos.get(state.getCommit());
        var date = commitDto.getDate();
        var commitHash = commitDto.getHash();
        closeOpenKeyDtos(commitIndex, commitHash, openKeyDtos, date);
        var properties = state.getProperties();
        Option<@NotNull Long> parentId = properties.getPropertyValue(ParentProperty.class)
          .map(symbolId -> new SymbolKey(symbolId, commitDto.getStrand()))
          .map(keysToIds::get);
        ParentDto parentDto = new ParentDto(parentId.getOrNull(), -1);
        var keyDto = KeyDto.builder()
          .from(date)
          .kind(properties.getPropertyValue(KindProperty.class).getOrThrow())
          .name(properties.getPropertyValue(SimpleNameProperty.class).getOrThrow())
          .parent(parentDto)
          .build();
        keyDtos.add(keyDto);
        openKeyDtos.put(commitHash, keyDto);
      }
      var deletedAt = Options.fromNullable(deletions.get(id)).map(Deletion::deletedAtOrNull);
      var symbolDto = SymbolDto.builder()
        .id(id)
        .deletedAt(deletedAt.getOrNull())
        .states(groupedStates)
        .keys(keyDtos)
        .contributions(contributions)
        .build();
      symbolDtos.add(symbolDto);
    }
    return symbolDtos;
  }

  private static @NotNull SymbolStatesResult getSymbolStatesResult(Set<Strand> strands,
                                                                   Map<Hash, CommitEntry> commitIndex,
                                                                   Map<@NotNull SymbolKey, @NotNull Long> keysToIds) {
    SymbolStatesWipResult wipResult = SymbolStatesWipResult.create(commitIndex, keysToIds);
    for (var strand : strands) {
      for (var diff : IterablesX.consuming(strand.getCommitDiffs())) {
        int commitId = commitIndex.get(diff.getCommit()).index();
        ZonedDateTime commitDate = commitIndex.get(diff.getCommit()).commit().date();
        SymbolStatesDiffContext diffContext = SymbolStatesDiffContext.create(diff, commitId, commitDate);
        putAdditionStates(wipResult, diffContext);
        for (var succession : diff.getSuccessions()) {
          var key = succession.getKey();
          var id = keysToIds.get(key);
          wipResult.lastSeenProperties().put(key, succession.getProperties());
          diffContext.successions().put(id, succession);
          Options.fromNullable(wipResult.deletions().get(id)).ifSome(Deletion::deactivate);
        }
        putDeletionStates(wipResult, diffContext);
        putChangedStates(wipResult, diffContext);
        putPureSuccessionStates(wipResult, diffContext);
      }
    }
    return wipResult.toFinalResult();
  }

  private static void putAdditionStates(SymbolStatesWipResult wipResult, SymbolStatesDiffContext diffContext) {
    for (var addition : diffContext.diff().getAdditions()) {
      var key = addition.getKey();
      var id = wipResult.keysToIds().get(key);
      wipResult.lastSeenProperties().put(key, addition.getProperties());
      var stateProperties = addition.getProperties().clone();
      var cause = ChangeCause.ADDED;
      var events = EventFlags.forState(cause, null, null);
      var mainEvent = EventFlags.getMainEvent(events);
      var stateDto = StateDto.builder()
        .cause(cause)
        .commit(diffContext.commitId())
        .origins(Collections.emptyList())
        .symbolId(addition.getKey().symbolId())
        .properties(stateProperties)
        .events(events)
        .mainEvent(mainEvent)
        //.updated(stateProperties.keySet())
        .build();
      wipResult.symbolStates().put(id, stateDto);
      Options.fromNullable(wipResult.deletions().get(id)).ifSome(Deletion::deactivate);
    }
  }

  private static void putChangedStates(SymbolStatesWipResult wipResult, SymbolStatesDiffContext diffContext) {
    for (var update : diffContext.diff().getUpdates()) {
      var key = update.getTargetContext().key();
      var id = wipResult.keysToIds().get(key);
      var hasSuccession = diffContext.successions().containsKey(id);
      if (hasSuccession) {
        diffContext.successionHandledParents().put(id, update.getParentIndex());
      }

      var properties = wipResult.lastSeenProperties().get(key);
      if (!hasSuccession) {
        properties.applyUpdate(update.getProperties());
      }
      var stateProperties = properties.clone();
      var origins = OriginDto.listOf(
        update.getParentIndex(), wipResult.commitIndex().get(update.getSourceContext().commit()).index());
      var cause = hasSuccession ? ChangeCause.SUCCEEDED_CHANGED : ChangeCause.CHANGED;
      var flags = update.getFlags();
      var updated = update.getProperties().keySet();
      var events = EventFlags.forState(cause, flags, updated);
      var mainEvent = EventFlags.getMainEvent(events);
      var stateDto = StateDto.builder()
        .cause(cause)
        .commit(diffContext.commitId())
        .origins(origins)
        .symbolId(key.symbolId())
        .properties(stateProperties)
        .updated(updated)
        .flags(flags)
        .events(events)
        .mainEvent(mainEvent)
        .build();
      wipResult.symbolStates().put(id, stateDto);
    }
  }

  private static void putDeletionStates(SymbolStatesWipResult wipResult, SymbolStatesDiffContext diffContext) {
    for (var deletion : diffContext.diff().getDeletions()) {
      var key = deletion.getKey();
      var id = wipResult.keysToIds().get(key);
      wipResult.lastSeenProperties().remove(key);
      var stateProperties = deletion.getProperties();
      var sourceCommitHash = deletion.getContext().getOrThrow().commit();
      var parentIndex = diffContext.diff().getParentCommits().indexOf(sourceCommitHash);
      var cause = ChangeCause.DELETED;
      var events = EventFlags.forState(cause, null, null);
      var mainEvent = EventFlags.getMainEvent(events);
      var stateDto = StateDto.builder()
        .cause(cause)
        .commit(diffContext.commitId())
        .origins(OriginDto.listOf(parentIndex, wipResult.commitIndex().get(sourceCommitHash).index()))
        .symbolId(deletion.getKey().symbolId())
        .properties(stateProperties)
        .events(events)
        .mainEvent(mainEvent)
        .build();
      wipResult.symbolStates().put(id, stateDto);
      var deletionInMap = wipResult.deletions().get(id);
      if (deletionInMap != null) {
        deletionInMap.updateDeletedAt(diffContext.commitDate());
      } else {
        wipResult.deletions().put(id, new Deletion(diffContext.commitDate()));
      }
    }
  }

  private static void putPureSuccessionStates(SymbolStatesWipResult wipResult, SymbolStatesDiffContext diffContext) {
    var diff = diffContext.diff();
    for (var successionEntry : diffContext.successions().entrySet()) {
      var id = successionEntry.getKey();
      var succession = successionEntry.getValue();
      var stateProperties = succession.getProperties().clone();
      List<OriginDto> origins = new ArrayList<>();
      for (int parentIndex = 0; parentIndex < diff.getParentCommits().size(); parentIndex++) {
        if (!diffContext.successionHandledParents().containsEntry(id, parentIndex)) {
          var sourceCommitHash = diff.getParentCommits().get(parentIndex);
          origins.add(OriginDto.of(parentIndex, wipResult.commitIndex().get(sourceCommitHash).index()));
        }
      }
      if (origins.isEmpty()) {
        // If symbol changed relative to ALL parents, do not create a pure succession entry
        continue;
      }
      var cause = ChangeCause.SUCCEEDED_PURE;
      var events = EventFlags.forState(cause, null, null);
      var mainEvent = EventFlags.getMainEvent(events);
      var stateDto = StateDto.builder()
        .cause(cause)
        .commit(diffContext.commitId())
        .origins(origins)
        .symbolId(succession.getKey().symbolId())
        .properties(stateProperties)
        .events(events)
        .mainEvent(mainEvent)
        .build();
      wipResult.symbolStates().put(id, stateDto);
    }
  }

  private record CommitEntry(CommitInfo commit, int index, int authorIndex) {

    public static CommitEntry fromPair(Pair<CommitInfo, @NotNull Integer> p, Map<AuthorInfo, @NotNull Integer> authorIndex) {
      return new CommitEntry(p.left(), p.right(), authorIndex.get(p.left().author()));
    }

  }

  private record SymbolStatesDiffContext(
    CommitDiff diff,
    int commitId,
    ZonedDateTime commitDate,
    Multimap<@NotNull Long, @NotNull Integer> successionHandledParents,
    Map<@NotNull Long, @NotNull Symbol> successions
  ) {

    public static SymbolStatesDiffContext create(CommitDiff diff, int commitId, ZonedDateTime commitDate) {
      return new SymbolStatesDiffContext(diff, commitId, commitDate, HashMultimap.create(), new HashMap<>());
    }

  }

  private record SymbolStatesWipResult(
    Map<@NotNull Long, @NotNull Deletion> deletions,
    ArrayListMultimap<@NotNull Long, @NotNull StateDto> symbolStates,
    Map<@NotNull SymbolKey, @NotNull PropertyMap> lastSeenProperties,
    Map<Hash, CommitEntry> commitIndex,
    Map<@NotNull SymbolKey, @NotNull Long> keysToIds
  ) {

    public static SymbolStatesWipResult create(Map<Hash, CommitEntry> commitIndex,
                                               Map<@NotNull SymbolKey, @NotNull Long> keysToIds) {
      return new SymbolStatesWipResult(
        new HashMap<@NotNull Long, @NotNull Deletion>(),
        ArrayListMultimap.<@NotNull Long, @NotNull StateDto>create(),
        new HashMap<@NotNull SymbolKey, @NotNull PropertyMap>(),
        commitIndex,
        keysToIds
      );
    }

    public SymbolStatesResult toFinalResult() {
      return new SymbolStatesResult(deletions, symbolStates);
    }

  }

  private record SymbolStatesResult(
    Map<@NotNull Long, @NotNull Deletion> deletions,
    ArrayListMultimap<@NotNull Long, @NotNull StateDto> symbolStates
  ) {

  }

  private record AbstractIdAssignment(long idCount, Map<@NotNull SymbolKey, @NotNull Long> keysToIds) {

  }

  @Getter
  @FieldDefaults(level = AccessLevel.PRIVATE)
  @RequiredArgsConstructor
  @EqualsAndHashCode
  private class Deletion {

    boolean active = true;
    @NonNull ZonedDateTime deletedAt;

    public void deactivate() {
      this.active = false;
    }

    public void updateDeletedAt(@NotNull ZonedDateTime deletedAt) {
      active = true;
      this.deletedAt = ZonedDateTimes.max(this.deletedAt, deletedAt);
    }

    @Contract(pure = true)
    public @Nullable ZonedDateTime deletedAtOrNull() {
      return active ? this.deletedAt : null;
    }

  }

}
