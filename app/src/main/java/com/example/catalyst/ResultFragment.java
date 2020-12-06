package com.example.catalyst;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.catalyst.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.w3c.dom.Text;

import java.util.HashMap;


public class ResultFragment extends Fragment {

    private NavController navController;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String currentUserId;


    private String quizId;
    private float averageTimeValue;

    private TextView resultCorrect;
    private TextView resultWrong;
    private TextView resultMissed;
    private TextView averageScore;
    private TextView averageTime;

    private TextView resultPercent;
    private ProgressBar resultProgress;

    private Button resultHomeBtn;

    public ResultFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        firebaseAuth = firebaseAuth.getInstance();

        // Get User ID
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        } else {
            // Go back to home page
        }

        firebaseFirestore = FirebaseFirestore.getInstance();

        quizId = ResultFragmentArgs.fromBundle(getArguments()).getQuizId();
        averageTimeValue = ResultFragmentArgs.fromBundle(getArguments()).getAverageTime();
        Log.v("Logger", "value got from args: " + averageTimeValue);

        // Initialize UI elements
        resultCorrect = view.findViewById(R.id.results_correct_text);
        resultWrong = view.findViewById(R.id.results_wrong_text);
        resultMissed = view.findViewById(R.id.results_missed_text);
        averageScore = view.findViewById(R.id.average_score_text);
        averageTime = view.findViewById(R.id.average_time_text);
        double averageTimeRoundedValue = Math.round(averageTimeValue * 100.0) / 100.0;
        averageTime.setText(String.valueOf(averageTimeRoundedValue)  + 's');

        resultPercent = view.findViewById(R.id.results_percent);

        resultHomeBtn = view.findViewById(R.id.results_home_btn);
        resultHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.action_resultFragment_to_listFragment);
            }
        });

        resultProgress = view.findViewById(R.id.results_progress);

        // Get Results
        firebaseFirestore.collection("QuizList")
                .document(quizId).collection("Results")
                .document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot result = task.getResult();

                    Long correct = result.getLong("Correct");
                    Long wrong = result.getLong("Wrong");
                    Long missed = result.getLong("Missed");

                    resultCorrect.setText(correct.toString());
                    resultWrong.setText(wrong.toString());
                    resultMissed.setText(missed.toString());

                    // Calculte Progress
                    Long total = correct + wrong + missed;
                    Long percent = (correct * 100) / total;

                    resultPercent.setText(percent + "%");
                    resultProgress.setProgress(percent.intValue());


                    // for Average Score

                    DocumentReference resultRef = firebaseFirestore.collection("QuizList").document(quizId)
                            .collection("Results").document(currentUserId);

                    resultRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {

//                                HashMap<String, Object> result = new HashMap<>();
//                                result.put("totalScore", (correct * 100) / total);

                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    HashMap<String, Object> result = new HashMap<>();
                                    result.put("totalScore", percent);
                                    result.put("attempts", 1);

                                    final float[] totalScore = new float[1];
                                    final float[] attempts = new float[1];

                                    // for totalScore
                                    if (document.getLong("totalScore") != null) {
                                        Log.d("logger", "your field exist");
                                        // field exists, update its value
                                        // get the val from db
                                        totalScore[0] = (float) document.getLong("totalScore");
                                        // add the db val to current value and update it in db
                                        resultRef.update("totalScore", totalScore[0] + percent);
                                        totalScore[0] = totalScore[0] + percent;

                                    } else {
                                        Log.d("logger", "your field does not exist");
                                        // Field doesn't exist, Create the field
                                        resultRef.update("totalScore", percent);
                                        totalScore[0] = (float) percent;
                                    }

                                    // for attempts
                                    if (document.getLong("attempts") != null) {
                                        Log.d("logger", "your attempt field exist");
                                        // field exists, update its value
                                        // get the val from db
                                        attempts[0] = (float) document.getLong("attempts");
                                        // add the db val to current value and update it in db
                                        resultRef.update("attempts", attempts[0] + 1);
                                        attempts[0]++;

                                    } else {
                                        Log.d("logger", "your attempts field does not exist");
                                        // Field doesn't exist, Create the field
                                        resultRef.update("attempts", 1);
                                        attempts[0] = 1;

                                    }
                                    setAverageScoreValue(totalScore[0], attempts[0]);
                                }
                            }
                        }
                    });


                } else {
                    // Document doesn't exist and result should say N/A
                }
            }

            private void setAverageScoreValue(float totalScore, float attempt) {
                float average = totalScore / attempt;
                double roundOff = Math.round(average * 100.0) / 100.0;
                averageScore.setText(roundOff + "%");
            }
        });
    }
}
