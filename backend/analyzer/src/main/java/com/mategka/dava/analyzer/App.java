package com.mategka.dava.analyzer;

import com.mategka.dava.analyzer.git.Commits;
import com.mategka.dava.analyzer.git.Repositories;
import com.mategka.dava.analyzer.util.Optionals;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.NullOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class App {

  public static void main(String[] args) {
    System.out.println("Hello World!");
    try (Repository repository = Repositories.open("?REPO")) {
      Ref mainBranch = repository.findRef("main");
      //var history = History.emptyOfBranch(repository, mainBranch);
      var testChangeTypes = List.of(DiffEntry.ChangeType.ADD, DiffEntry.ChangeType.MODIFY);
      var timeBefore = System.currentTimeMillis();
      int offset = 0;
      try (RevWalk walk = Commits.topologicalReverse(repository, mainBranch)) {
        for (RevCommit commit : walk) {
          System.out.print(commit.getId().getName().substring(0, 6) + " ");
          if (++offset >= 12) {
            offset = 0;
            System.out.println();
          }
          var parent = Optionals.getFirst(commit.getParents());
          try (DiffFormatter formatter = new DiffFormatter(NullOutputStream.INSTANCE)) {
            formatter.setRepository(repository);
            formatter.setContext(0);
            formatter.setDetectRenames(true);
            formatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS));
            var renameDetector = formatter.getRenameDetector();
            renameDetector.setRenameScore(50);
            var diffs = formatter.scan(parent.map(RevCommit::getTree).orElse(null), commit.getTree());
            for (var diff : diffs) {
              var path = diff.getNewPath();
              if (testChangeTypes.contains(diff.getChangeType())) {
                var objectId = diff.getNewId().toObjectId();
                try (ObjectReader reader = repository.newObjectReader(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                  reader.open(objectId).copyTo(output);
                  var content = output.toString(StandardCharsets.UTF_8);
                }
              }
            }
          }
        }
      }
      System.out.println("Done!");
      var time = System.currentTimeMillis() - timeBefore;
      System.out.println("Time (ms): " + time);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // Goal 1: Get all changes for one commit
    // Goal 2: Get all changes for history (starting from specified commit)
    // Goal 3: Get all changes and refactorings for history
    // Goal 4: Get all symbol changes for history
    // Goal 5: Get all structured symbol changes for history (symbol parentage, ...)
    // Side Goal: Make sure each commit-file combo is only read and parsed ONCE
  }

}
