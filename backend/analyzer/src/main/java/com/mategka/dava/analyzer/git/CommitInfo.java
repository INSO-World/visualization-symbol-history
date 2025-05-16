package com.mategka.dava.analyzer.git;

import java.time.ZonedDateTime;
import java.util.List;

public record CommitInfo(Hash hash, String summary, String description, ZonedDateTime date, List<Hash> parents) {

}
