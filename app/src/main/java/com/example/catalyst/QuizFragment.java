package com.example.catalyst;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QuizFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QuizFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView quizTitle;

    private FirebaseFirestore firebaseFirestore;
    private String quizId;

    // firebase data
    // to hold all the questions
    private List<QuestionsModel> allQuestionsList = new ArrayList<>();
    private long totalQuestionsAnswers = 10;
    // to hold picked questions
    private List<QuestionsModel> questionsToAnswer = new ArrayList<>();

    public QuizFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment QuizFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static QuizFragment newInstance(String param1, String param2) {
        QuizFragment fragment = new QuizFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
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

        // initialize
        firebaseFirestore = FirebaseFirestore.getInstance();

        quizTitle = view.findViewById(R.id.quiz_title);

        // get quizId
        quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizid();
        totalQuestionsAnswers = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();

        // get all questions from the quiz
        firebaseFirestore.collection("QuizList").document(quizId).collection("Questions")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allQuestionsList = task.getResult().toObjects(QuestionsModel.class);
//                    Log.d("list", "###########################################################################################################################################" + allQuestionsList.get(0).getQuestion());

                    // pick given no of questions from list of questions
                    pickQuestions();
                } else {
                    // Error getting Questions
                    quizTitle.setText("Error Loading Data");
                }
            }
        });

    }

    public void pickQuestions() {
        for (int i = 0; i < totalQuestionsAnswers; i++){
            int randomNumber = getRandomInteger(allQuestionsList.size(), 0);
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.remove(randomNumber);

            Log.d("picked questions : ", "################################################" + questionsToAnswer.get(i).getQuestion());

        }
    }

    public static int getRandomInteger(int maximum, int minimum) {
        return ((int) (Math.random() * (maximum - minimum))) + minimum;
    }
}