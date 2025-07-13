package ru.practicum.kanban.model;

import java.util.Objects;

public class Task {
	protected int id;
	protected String name;
	protected String description;
	protected TaskStatus status;

	public Task(String name, String description) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Название задачи не может быть пустым");
		}
		if (description == null) {
			throw new IllegalArgumentException("Описание задачи не может быть пустым");
		}
		this.name = name;
		this.description = description;
		this.status = TaskStatus.NEW;
	}

	public Task(int id, String name, String description, TaskStatus status) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Название задачи не может быть пустым");
		}
		if (description == null) {
			throw new IllegalArgumentException("Описание задачи не может быть пустым");
		}
		if (status == null) {
			throw new IllegalArgumentException("Статус задачи не может быть пустым");
		}
		this.id = id;
		this.name = name;
		this.description = description;
		this.status = status;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Название задачи не может быть пустым");
		}
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if (description == null) {
			throw new IllegalArgumentException("Описание задачи не может быть пустым");
		}
		this.description = description;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("Статус задачи не может быть пустым");
		}
		this.status = status;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Task task = (Task) o;
		return id == task.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Task{" +
				"id=" + id +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				", status=" + status +
				'}';
	}
}