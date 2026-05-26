package david.dokholyan.aquatime.ui;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSession {
    private String title;
    private int totalDistance;
    private List<String> warmUpTasks = new ArrayList<>();
    private List<String> mainSetTasks = new ArrayList<>();
    private List<String> coolDownTasks = new ArrayList<>();

    public WorkoutSession(String title) {
        this.title = title;
    }

    public void addWarmUp(String task, int meters) {
        warmUpTasks.add(task);
        totalDistance += meters;
    }

    public void addMainSet(String task, int meters) {
        mainSetTasks.add(task);
        totalDistance += meters;
    }

    public void addCoolDown(String task, int meters) {
        coolDownTasks.add(task);
        totalDistance += meters;
    }

    public String getTitle() { return title; }
    public int getTotalDistance() { return totalDistance; }
    public List<String> getWarmUpTasks() { return warmUpTasks; }
    public List<String> getMainSetTasks() { return mainSetTasks; }
    public List<String> getCoolDownTasks() { return coolDownTasks; }
}