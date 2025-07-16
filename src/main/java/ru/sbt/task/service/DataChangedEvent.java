package ru.sbt.task.service;

public class DataChangedEvent {
    private final Class<?> entityType;
    private final Object entity;
    private final ChangeType changeType;

    public enum ChangeType {
        CREATE, UPDATE, DELETE
    }

    public DataChangedEvent(Class<?> entityType) {
        this(entityType, null, null);
    }

    public DataChangedEvent(Class<?> entityType, Object entity, ChangeType changeType) {
        this.entityType = entityType;
        this.entity = entity;
        this.changeType = changeType;
    }

    // Геттеры
    public Class<?> getEntityType() { return entityType; }
    public Object getEntity() { return entity; }
    public ChangeType getChangeType() { return changeType; }

}
