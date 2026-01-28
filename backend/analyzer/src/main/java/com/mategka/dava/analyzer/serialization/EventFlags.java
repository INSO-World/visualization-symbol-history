package com.mategka.dava.analyzer.serialization;

import com.mategka.dava.analyzer.extension.CollectorsX;
import com.mategka.dava.analyzer.extension.option.Option;
import com.mategka.dava.analyzer.extension.stream.AnStream;
import com.mategka.dava.analyzer.extension.struct.Pair;
import com.mategka.dava.analyzer.serialization.model.ChangeCause;
import com.mategka.dava.analyzer.serialization.model.EventCategory;
import com.mategka.dava.analyzer.serialization.model.EventFlag;
import com.mategka.dava.analyzer.struct.property.*;
import com.mategka.dava.analyzer.struct.property.index.PropertyKeys;
import com.mategka.dava.analyzer.struct.symbol.UpdateFlag;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@UtilityClass
public class EventFlags {

  private final Map<@NotNull String, EventFlag> PROPERTY_FLAGS = AnStream.sequence(
    Pair.of(InitialValueProperty.class, EventFlag.VALUE),
    Pair.of(EnumArgumentsProperty.class, EventFlag.VALUE),
    Pair.of(AnnotationsProperty.class, EventFlag.ANNOTATIONS),
    Pair.of(KindProperty.class, EventFlag.KIND),
    Pair.of(ModifiersProperty.class, EventFlag.MODIFIERS),
    Pair.of(RealizationsProperty.class, EventFlag.REALIZATIONS),
    Pair.of(SupertypesProperty.class, EventFlag.SUPERTYPES),
    Pair.of(TypeParametersProperty.class, EventFlag.TYPE_PARAMETERS),
    Pair.of(TypeProperty.class, EventFlag.TYPE),
    Pair.of(VisibilityProperty.class, EventFlag.VISIBILITY)
  )
    .map(Pair.mappingLeft(PropertyKeys::get))
    .collect(CollectorsX.pairsToMap());

  public Set<EventFlag> forState(ChangeCause cause, @Nullable Set<UpdateFlag> flags, @Nullable Set<@NotNull String> updated) {
    Set<EventFlag> result = new HashSet<>(forCause(cause));
    if (flags != null) {
      result.addAll(forUpdateFlags(flags));
    }
    if (updated != null) {
      result.addAll(forProperties(updated));
    }
    return result;
  }

  public EventFlag getMainEvent(Collection<? extends EventFlag> events) {
    if (events.isEmpty()) {
      return EventFlag.NONE;
    }
    EventCategory mainCategory = EventCategory.max(events);
    var candidates = events.stream()
      .filter(e -> e.getCategory() == mainCategory)
      .toList();
    return candidates.getLast();
  }

  private Set<EventFlag> forCause(ChangeCause cause) {
    if (cause == ChangeCause.ADDED) {
      return Set.of(EventFlag.ADDED);
    } else if (cause == ChangeCause.DELETED) {
      return Set.of(EventFlag.DELETED);
    } else if (cause == ChangeCause.CHANGED) {
      return Collections.emptySet();
    } else {
      return Set.of(EventFlag.BRANCHED);
    }
  }

  private Set<EventFlag> forUpdateFlags(Set<UpdateFlag> flags) {
    Set<EventFlag> result = AnStream.from(flags)
      .mapOption(EventFlags::forUpdateFlag)
      .toSet();
    if (flags.contains(UpdateFlag.MOVED_WITH_PARENT)) {
      result.remove(EventFlag.MOVED);
    }
    return result;
  }

  private Set<EventFlag> forProperties(Set<@NotNull String> updated) {
    return AnStream.from(updated)
      .map(PROPERTY_FLAGS::get)
      .filter(Objects::nonNull)
      .toSet();
  }

  private Option<EventFlag> forUpdateFlag(UpdateFlag flag) {
    return switch (flag) {
      case REPLACED -> Option.Some(EventFlag.REPLACED);
      case RENAMED -> Option.Some(EventFlag.RENAMED);
      case MOVED -> Option.Some(EventFlag.MOVED);
      case REORDERED -> Option.Some(EventFlag.REORDERED);
      case BODY_UPDATED -> Option.Some(EventFlag.BODY);
      default -> Option.None();
    };
  }

}
