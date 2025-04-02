package com.mategka.dava.analyzer.wip;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a computational task with progress tracking.
 */
public class Task {

  @Getter
  private final UUID id;
  @Getter
  private final String name;
  @Getter
  private final double size;
  @Getter
  private final int subtaskCount;
  @Getter
  private final Task parent;
  private final List<Task> subtasks;
  private final Instant startTime;
  private Instant endTime;
  @Getter
  private double progress;
  @Getter
  private Duration estimatedTimeRemaining;
  private double completedSize;
  private double completedTime; // in seconds

  /**
   * Creates a new task with the specified parameters.
   *
   * @param name         The name of the task
   * @param size         The size of the task (in arbitrary units)
   * @param subtaskCount The number of subtasks this task will have
   * @param parent       The parent task, or null if this is a root task
   */
  public Task(String name, double size, int subtaskCount, Task parent) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.size = size;
    this.subtaskCount = subtaskCount;
    this.parent = parent;
    this.subtasks = new ArrayList<>();
    this.startTime = Instant.now();
    this.progress = 0.0;
    this.completedSize = 0;
    this.completedTime = 0;

    if (parent != null) {
      parent.addSubtask(this);
    }
  }

  /**
   * Completes this task and updates progress and time estimates for all ancestor tasks.
   * This should only be called on leaf tasks (tasks with no subtasks).
   */
  public void complete() {
    if (subtaskCount > 0 && !subtasks.isEmpty()) {
      throw new IllegalStateException("Cannot complete a task with subtasks");
    }

    endTime = Instant.now();
    double elapsedSeconds = Duration.between(startTime, endTime).toMillis() / 1000.0;

    // Update this task
    this.progress = 1.0;
    this.estimatedTimeRemaining = Duration.ZERO;
    this.completedSize = size;
    this.completedTime = elapsedSeconds;

    // Update all ancestor tasks
    Task current = this.parent;
    while (current != null) {
      current.updateProgressAndEstimates();
      current = current.parent;
    }
  }

  /**
   * Gets the elapsed time since this task was started.
   *
   * @return The elapsed time
   */
  public Duration getElapsedTime() {
    Instant end = endTime != null ? endTime : Instant.now();
    return Duration.between(startTime, end);
  }

  /**
   * Gets the list of subtasks.
   *
   * @return The subtasks
   */
  public List<Task> getSubtasks() {
    return new ArrayList<>(subtasks);
  }

  /**
   * Checks if this task is completed.
   *
   * @return True if the task is completed, false otherwise
   */
  public boolean isCompleted() {
    return progress >= 0.999; // Allow for floating-point imprecision
  }

  /**
   * Adds a subtask to this task.
   *
   * @param subtask The subtask to add
   */
  private void addSubtask(Task subtask) {
    if (subtasks.size() < subtaskCount) {
      subtasks.add(subtask);
    } else {
      throw new IllegalStateException("Cannot add more subtasks than specified by subtaskCount");
    }
  }

  /**
   * Formats a duration for display.
   */
  private String formatDuration(Duration duration) {
    long hours = duration.toHours();
    long minutes = duration.toMinutesPart();
    long seconds = duration.toSecondsPart();

    if (hours > 0) {
      return String.format("%dh %dm %ds", hours, minutes, seconds);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, seconds);
    } else {
      return String.format("%ds", seconds);
    }
  }

  private double getExpectedTotalSize(boolean allSubtasksKnown, double totalCompletedSize) {
    if (allSubtasksKnown) {
      return subtasks.stream().mapToDouble(Task::getSize).sum();
    } else {
      // Estimate based on completed subtasks
      double completedSubtaskCount = subtasks.stream().mapToDouble(Task::getProgress).sum();
      if (completedSubtaskCount > 0) {
        double averageSizePerSubtask = totalCompletedSize / completedSubtaskCount;
        return averageSizePerSubtask * subtaskCount;
      } else {
        return size; // Fall back to the task's declared size
      }
    }
  }

  private double getNewProgress(double expectedTotalSize, double totalCompletedSize) {
    double newProgress = expectedTotalSize > 0 ? totalCompletedSize / expectedTotalSize : 0;
    // Ensure monotonically increasing progress (for root task)
    if (parent == null && newProgress < progress) {
      // If estimate is off, still increase progress but with a penalty
      newProgress = progress + (newProgress - progress) * 0.1; // Small increase
    } else if (newProgress < progress) {
      // For non-root tasks, we still ensure progress doesn't decrease
      newProgress = progress;
    }
    return newProgress;
  }

  /**
   * Updates the progress percentage and time remaining estimate for this task
   * based on completed subtasks.
   */
  private void updateProgressAndEstimates() {
    double totalCompletedSize = 0;
    double totalCompletedTime = 0;
    boolean allSubtasksKnown = subtasks.size() == subtaskCount;

    for (Task subtask : subtasks) {
      totalCompletedSize += subtask.completedSize * subtask.progress;
      if (subtask.progress == 1.0) {
        totalCompletedTime += subtask.completedTime;
      }
    }

    // Calculate expected total size based on what we know so far
    double expectedTotalSize = getExpectedTotalSize(allSubtasksKnown, totalCompletedSize);

    // Calculate new progress
    double newProgress = getNewProgress(expectedTotalSize, totalCompletedSize);

    this.progress = Math.min(newProgress, 1.0); // Ensure progress doesn't exceed 100%
    this.completedSize = totalCompletedSize;

    // Calculate estimated time remaining
    if (totalCompletedSize > 0 && totalCompletedTime > 0) {
      double rateOfWork = totalCompletedSize / totalCompletedTime; // size units per second
      double remainingSize = expectedTotalSize - totalCompletedSize;
      double remainingTimeSeconds = remainingSize / rateOfWork;
      this.estimatedTimeRemaining = Duration.ofSeconds(Math.round(remainingTimeSeconds));
    }
  }

  @Override
  public String toString() {
    return String.format(
      "%s (%.1f%% complete, %s elapsed, %s remaining)",
      name,
      progress * 100,
      formatDuration(getElapsedTime()),
      estimatedTimeRemaining != null ? formatDuration(estimatedTimeRemaining) : "unknown"
    );
  }

}
