package com.mategka.dava.analyzer.struct.symbol;

import java.util.concurrent.atomic.AtomicLong;

public record SymbolCreationContext(AtomicLong symbolIdCounter, long strandId, String commitSha) {

}
