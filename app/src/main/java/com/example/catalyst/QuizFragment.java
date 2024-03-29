package com.example.catalyst;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.catalyst.QuizFragmentArgs;
import com.example.catalyst.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class QuizFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "QUIZ_FRAGMENT_LOG";

    private NavController navController;

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private String currentUserId;

    private String quizName;
    private String quizId;

    //UI Elements
    private TextView quizTitle;
    private Button optionOneBtn;
    private Button optionTwoBtn;
    private Button optionThreeBtn;
    private Button nextBtn;
    private ImageButton closeBtn;
    private TextView questionFeedback;
    private TextView questionText;
    private TextView questionTime;
    private ProgressBar questionProgress;
    private TextView questionNumber;

    private List<QuestionModel> allQuestionsList = new ArrayList<>();
    private List<QuestionModel> questionsToAnswer = new ArrayList<>();
    private Long totalQuestionsToAnswer = 0L;
    private CountDownTimer countDownTimer;

    private boolean canAnswer = false;
    private int currentQuestion = 0;

    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private int notAnswered = 0;

//    ......................
    Long timeToAnswer;

    // for average time;
    private String currentTime;
    private int totalTimeeSpent = 0;
    private float averageTime = 0;

    public QuizFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        firebaseAuth = firebaseAuth.getInstance();
        // Get User ID
        if(firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }
        else {
            // Go back to home page
        }

        firebaseFirestore = FirebaseFirestore.getInstance();

        //UI Initialize

        closeBtn = view.findViewById(R.id.quiz_close_btn);

        quizTitle = view.findViewById(R.id.quiz_title);
        optionOneBtn = view.findViewById(R.id.quiz_option_one);
        optionTwoBtn = view.findViewById(R.id.quiz_option_two);
        optionThreeBtn = view.findViewById(R.id.quiz_option_three);
        nextBtn = view.findViewById(R.id.quiz_next_btn);
        questionFeedback = view.findViewById(R.id.quiz_question_feedback);
        questionText = view.findViewById(R.id.quiz_question);
        questionTime = view.findViewById(R.id.quiz_question_time);
        questionProgress = view.findViewById(R.id.quiz_question_progress);
        questionNumber = view.findViewById(R.id.quiz_question_number);

        quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizId();
        quizName = QuizFragmentArgs.fromBundle(getArguments()).getQuizName();
        totalQuestionsToAnswer = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();

        //Query Firestore Data
        firebaseFirestore.collection("QuizList")
                .document(quizId).collection("Questions")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allQuestionsList = task.getResult().toObjects(QuestionModel.class);
                    pickQuestions();
                    loadUI();
                } else {
                    quizTitle.setText("Error : " + task.getException().getMessage());
                }
            }
        });

        //Set Button Click Listeners

        closeBtn.setOnClickListener(this);

        optionOneBtn.setOnClickListener(this);
        optionTwoBtn.setOnClickListener(this);
        optionThreeBtn.setOnClickListener(this);

        nextBtn.setOnClickListener(this);
    }

    private void loadUI() {
        //Quiz Data Loaded, Load the UI
        quizTitle.setText(quizName);
        questionText.setText("Load First Question");

        //Enable Options
        enableOptions();

        //Load First Question
        loadQuestion(1);
    }

    private void loadQuestion(int questNum) {
        //Set Question Number
        questionNumber.setText(questNum + "");

        //Load Question Text
        questionText.setText(questionsToAnswer.get(questNum - 1).getQuestion());

        //Load Options
        optionOneBtn.setText(questionsToAnswer.get(questNum - 1).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questNum - 1).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questNum - 1).getOption_c());

        //Question Loaded, Set an Answer
        canAnswer = true;
        currentQuestion = questNum;

        //Start Question Timer
        startTimer(questNum);
    }

    private void startTimer(int questionNumber) {

        //Set Timer Text
        timeToAnswer = questionsToAnswer.get(questionNumber - 1).getTimer();
        questionTime.setText(timeToAnswer.toString());

        //Show Timer ProgressBar
        questionProgress.setVisibility(View.VISIBLE);

        //Start CountDown
        countDownTimer = new CountDownTimer(timeToAnswer * 1000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                //Update Time
                questionTime.setText(millisUntilFinished / 1000 + "");

                //Progress in percent
                Long percent = millisUntilFinished / (timeToAnswer * 10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                //Time Up, Cannot Answer Question Anymore
                canAnswer = false;

                questionFeedback.setText("Time Up!! Press Next to go the next question");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary, null));
                notAnswered++;
                showNextBtn();
            }
        };

        countDownTimer.start();
    }

    private void enableOptions() {
        //Show All Option Buttons
        optionOneBtn.setVisibility(View.VISIBLE);
        optionTwoBtn.setVisibility(View.VISIBLE);
        optionThreeBtn.setVisibility(View.VISIBLE);

        //Enable Option Buttons
        optionOneBtn.setEnabled(true);
        optionTwoBtn.setEnabled(true);
        optionThreeBtn.setEnabled(true);

        //Hide Feedback and next Button
        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void pickQuestions() {
        for (int i = 0; i < totalQuestionsToAnswer; i++) {
            int randomNumber = getRandomInt(0, allQuestionsList.size());
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.remove(randomNumber);
            Log.d("LOG", "Question " + i + " : " + questionsToAnswer.get(i).getQuestion());
        }
    }

    private int getRandomInt(int min, int max) {
        return ((int) (Math.random() * (max - min))) + min;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.quiz_close_btn:
                navController.navigate(R.id.action_quizFragment_to_listFragment2);
            case R.id.quiz_option_one:
                verifyAnswer(optionOneBtn);
                break;
            case R.id.quiz_option_two:
                verifyAnswer(optionTwoBtn);
                break;
            case R.id.quiz_option_three:
                verifyAnswer(optionThreeBtn);
                break;
            case R.id.quiz_next_btn:
                if (currentQuestion == totalQuestionsToAnswer) {
                    // no more questions to load submit results
                    submitResults();
                }
                else {
                    currentQuestion++;
                    loadQuestion(currentQuestion);
                    resetOptions();
                    break;
                }
        }
    }

    private void submitResults() {

        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put("Correct", correctAnswers);
        resultMap.put("Wrong", wrongAnswers);
        resultMap.put("Missed", notAnswered);

        averageTime = (float) totalTimeeSpent / (float) totalQuestionsToAnswer;

        firebaseFirestore.collection("QuizList").document(quizId)
                .collection("Results").document(currentUserId).set(resultMap, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Go to result page
                            QuizFragmentDirections.ActionQuizFragmentToResultFragment action = QuizFragmentDirections.actionQuizFragmentToResultFragment();
                            action.setQuizId(quizId);
                            action.setAverageTime(averageTime);
                            navController.navigate(action);
                        }
                        else {
                            // show an error
                            quizTitle.setText(task.getException().getMessage());
                        }
                    }
                });
    }

    private void resetOptions() {
        optionOneBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionTwoBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));
        optionThreeBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg, null));

        optionOneBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));
        optionTwoBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));
        optionThreeBtn.setTextColor(getResources().getColor(R.color.colorLightText, null));

        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void verifyAnswer(Button selectedAnswerBtn) {

        //Check Answer
        if (canAnswer) {

            // Set answer button text to black
            selectedAnswerBtn.setTextColor(getResources().getColor(R.color.colorDark, null));

            if (questionsToAnswer.get(currentQuestion - 1).getAnswer().equals(selectedAnswerBtn.getText())) {
                //Correct Answer
                correctAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.correct_answer_btn_bg, null));

                // Set feedback text
                questionFeedback.setText("Correct Answer");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            } else {
                //Wrong Answer
                wrongAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.wrong_answer_btn_bg, null));

                // Set feedback text
                questionFeedback.setText("Wrong Answer \n Correct Answer: " + questionsToAnswer.get(currentQuestion - 1).getAnswer());
                questionFeedback.setTextColor(getResources().getColor(R.color.colorAccent, null));
            }
            //Set Can answer to false, so that user cannot submit again
            canAnswer = false;

            // stop the timer
            currentTime = questionTime.getText().toString();

            totalTimeeSpent += (int) (timeToAnswer - Integer.parseInt(currentTime));
            Log.v("Logger", "total time" + totalTimeeSpent);
            countDownTimer.cancel();


            // show next button
            showNextBtn();
        }
    }

    private void showNextBtn() {
        if (currentQuestion == totalQuestionsToAnswer) {
            nextBtn.setText("Submit Result");
        }
        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);
        nextBtn.setEnabled(true);
    }
}
