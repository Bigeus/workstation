package com.example.workstation;

public class Task {
    private String id;
    private String name;
    private int priority; // 1=azul, 2=amarelo, 3=vermelho
    private boolean isFinished;

    public Task() {
        // Construtor vazio necessário para Firebase
    }

    public Task(String name, int priority) {
        this.name = name;
        this.priority = priority;
        this.isFinished = false;
    }

    public Task(String id, String name, int priority, boolean isFinished) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.isFinished = isFinished;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}