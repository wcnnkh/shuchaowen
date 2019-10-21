package scw.timer.support;

import scw.timer.CrontabConfig;
import scw.timer.Task;
import scw.timer.TaskListener;
import scw.timer.annotation.Crontab;

public class SimpleCrontabConfig extends SimpleTaskConfig implements CrontabConfig {
	private String dayOfWeek;
	private String month;
	private String dayOfMonth;
	private String hour;
	private String minute;

	public SimpleCrontabConfig(Crontab crontab, Task task, TaskListener taskListener) {
		super(crontab.name(), task, taskListener);
		this.dayOfWeek = crontab.dayOfWeek();
		this.month = crontab.month();
		this.dayOfMonth = crontab.dayOfMonth();
		this.hour = crontab.hour();
		this.minute = crontab.minute();
	}

	public SimpleCrontabConfig(String taskId, Task task, TaskListener taskListener, String dayOfWeek, String month,
			String dayOfMonth, String hour, String minute) {
		super(taskId, task, taskListener);
		this.dayOfWeek = dayOfWeek;
		this.month = month;
		this.dayOfMonth = dayOfMonth;
		this.hour = hour;
		this.minute = minute;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public String getMonth() {
		return month;
	}

	public String getDayOfMonth() {
		return dayOfMonth;
	}

	public String getHour() {
		return hour;
	}

	public String getMinute() {
		return minute;
	}

}
