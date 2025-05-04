package com.mategka.dava.analyzer.struct.symbol;

import com.mategka.dava.analyzer.git.Hash;

import lombok.NonNull;

public record Context(@NonNull SymbolKey key, @NonNull Hash commit) {

}
