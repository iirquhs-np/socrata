package sg.edu.np.mad.socrata;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TimerActivity extends AppCompatActivity implements View.OnClickListener {

    ImageButton buttonStart, buttonRestart, buttonStop, buttonBack;
    Chronometer chronometer;
    LottieAnimationView lottieAnimation;
    TextView textViewModuleName;

    String moduleName;
    boolean isRunning = false;

    private long pauseOffset;

    User user;

    FirebaseUtils firebaseUtils;

    LocalStorage localStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        Intent intent = getIntent();

        moduleName = intent.getStringExtra("module_name");

        textViewModuleName = findViewById(R.id.textViewModuleName);
        textViewModuleName.setText("Studying " + moduleName);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lottieAnimation = findViewById(R.id.lottieAnimation);

        buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(view -> finish());

        buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(this);

        buttonRestart = findViewById(R.id.buttonRestart);
        buttonRestart.setOnClickListener(this);

        buttonStop = findViewById(R.id.buttonStop);
        buttonStop.setOnClickListener(this);

        chronometer = findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime());

        // Set the font here cuz its not working in the xml
        chronometer.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_bold));

        localStorage = new LocalStorage(this);
        user = localStorage.getUser();

        firebaseUtils = new FirebaseUtils();

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.buttonStart) {
            if (!isRunning) {
                chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                chronometer.start();

                buttonStart.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.red_color)));
                buttonStart.setImageResource(R.drawable.ic_round_pause_24);

                lottieAnimation.resumeAnimation();

            } else {
                pauseTimer();
            }
            isRunning = !isRunning;
        } else if (id == R.id.buttonStop) {
            pauseTimer();
            isRunning = false;

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setMessage("Are you sure you want to stop studying?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            insertStudySession();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
            builder.create().show();
        } else if (id == R.id.buttonRestart) {
            pauseTimer();
            isRunning = false;

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setMessage("Are you sure you want to reset the timer?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            chronometer.setBase(SystemClock.elapsedRealtime());
                            pauseOffset = 0;

                            chronometer.stop();

                            lottieAnimation.setProgress(0);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Insert the study session into firebase for that specific module
     */
    private void insertStudySession() {
        // Convert millisecond to second
        double studyTime = ((double) (SystemClock.elapsedRealtime() - chronometer.getBase())) / 1000.0;
        Log.d("TAG", Double.toString(studyTime));
        if (studyTime <= 60) {
            Toast.makeText(this, "You must study for more than 1 minute to save", Toast.LENGTH_SHORT).show();
            return;
        }

        StudySession studySession = new StudySession(studyTime);

        ArrayList<Module> moduleArrayList = user.getModuleArrayList();

        int moduleIndex = ModuleUtils.findModule(moduleArrayList, moduleName);
        moduleArrayList.get(moduleIndex).addStudySession(studySession);

        localStorage.setModuleArrayList(moduleArrayList);
        firebaseUtils.updateModuleArrayList(moduleArrayList);

        finish();

    }

    private void pauseTimer() {
        pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();

        setStart();
    }

    private void setStart() {
        buttonStart.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_color)));
        buttonStart.setImageResource(R.drawable.ic_round_play_arrow_24);

        lottieAnimation.pauseAnimation();
    }
}