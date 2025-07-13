package ru.practicum.kanban.model;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
	private final List<Integer> subtaskIds;

	public Epic(String name, String description) {
		super(name, description);
		this.subtaskIds = new ArrayList<>();
	}

	public Epic(int id, String name, String description, TaskStatus status) {
		super(id, name, description, status);
		this.subtaskIds = new ArrayList<>();
	}

	public List<Integer> getSubtaskIds() {
		return new ArrayList<>(subtaskIds);
	}

	public void addSubtaskId(int subtaskId) {
		subtaskIds.add(subtaskId);
	}

	public void removeSubtaskId(int subtaskId) {
		subtaskIds.remove(Integer.valueOf(subtaskId));
	}

	@Override
	public String toString() {
		return "Epic{" +
				"id=" + id +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", status=" + status +
				", subtaskIds=" + subtaskIds +
				'}';
	}
}