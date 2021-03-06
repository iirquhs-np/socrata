package sg.edu.np.mad.socrata;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AlarmManagerHelper {

    AlarmManager alarmManager;

    Context context;

    public AlarmManagerHelper(Context context) {
        this.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public void setMotivationalSettingAlarm(MotivationalQuoteSetting motivationalQuoteSetting) {
        Intent serviceIntent = new Intent(context.getApplicationContext(), MotivationBackgroundReceiver.class);

        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(context, 1, serviceIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, 1, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        ZonedDateTime currentLocalDateTime = ZonedDateTime.now();

        LocalTime localTime = motivationalQuoteSetting.getTimeOfDay();

        ZonedDateTime nextZonedDateTime = ZonedDateTime.of(currentLocalDateTime.getYear(),
                currentLocalDateTime.getMonth().getValue(),
                currentLocalDateTime.getDayOfMonth(), localTime.getHour(), localTime.getMinute(), 0, 0, ZoneId.systemDefault());

        int multiplier = motivationalQuoteSetting.getMultiplier();

        long interval = multiplier;

        switch (motivationalQuoteSetting.getFrequency()) {
            case "Hour":
                nextZonedDateTime = nextZonedDateTime.withHour(currentLocalDateTime.getHour()+multiplier);
                interval *= AlarmManager.INTERVAL_HOUR;
                break;
            case "Minute":
                nextZonedDateTime = nextZonedDateTime.withHour(currentLocalDateTime.getHour());
                nextZonedDateTime = nextZonedDateTime.withMinute(currentLocalDateTime.getMinute()+multiplier);
                interval *= AlarmManager.INTERVAL_FIFTEEN_MINUTES/15;
                break;
            case "Week":
                nextZonedDateTime = nextZonedDateTime.plusWeeks(multiplier);
                nextZonedDateTime = nextZonedDateTime.with(DayOfWeek.MONDAY);
                interval *= AlarmManager.INTERVAL_DAY*7;
                break;
            case "Month":
                nextZonedDateTime = nextZonedDateTime.plusMonths(multiplier);
                nextZonedDateTime = nextZonedDateTime.withDayOfMonth(1);
                interval *= AlarmManager.INTERVAL_DAY*30;
                break;
            case "Year":
                nextZonedDateTime = nextZonedDateTime.plusYears(multiplier);
                nextZonedDateTime = nextZonedDateTime.withDayOfYear(1);
                interval *= AlarmManager.INTERVAL_DAY*365;
                break;
            default:
                nextZonedDateTime = nextZonedDateTime.plusDays(multiplier);
                interval *= AlarmManager.INTERVAL_DAY;
                break;
        }

        if (motivationalQuoteSetting.isNotificationOn()) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, nextZonedDateTime.toInstant().toEpochMilli()
                    , interval, pendingIntent);
            Log.d("TAG", nextZonedDateTime.toString());
            return;
        }

        alarmManager.cancel(pendingIntent);
    }

    public void setHomeworkReminderAlarm(Homework homework, long minutesBeforeDueDate, String dateFrequency) {
        Intent reminderIntent = new Intent(context.getApplicationContext(), ReminderReceiver.class);

        reminderIntent.putExtra("homeworkName", homework.getHomeworkName());

        reminderIntent.putExtra("reminder", minutesBeforeDueDate + " " + dateFrequency);

        switch (dateFrequency) {
            case "Minute":
                break;
            case "Hour":
                minutesBeforeDueDate *= 60;
                break;
            case "Day":
                minutesBeforeDueDate *= 60 * 24;
                break;
            case "Week":
                minutesBeforeDueDate *= 60 * 24 * 7;
                break;
            case "Month":
                minutesBeforeDueDate *= 60 * 24 * 30;
                break;
            case "Year":
                minutesBeforeDueDate *= 60 * 24 * 365;
                break;
        }

        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                    homework.getHomeworkId(), reminderIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                    homework.getHomeworkId(), reminderIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (homework.CalculateSecondsLeftBeforeDueDate() - Duration.ofMinutes(1).getSeconds() * minutesBeforeDueDate) * 1000,
                pendingIntent);

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis() + (homework.CalculateSecondsLeftBeforeDueDate() - Duration.ofMinutes(1).getSeconds() * minutesBeforeDueDate) * 1000),
                ZoneId.systemDefault());

        Log.d("TAG", zonedDateTime.toString());
    }
}
