package com.example.catalyst;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.catalyst.DetailsFragmentArgs;
import com.example.catalyst.DetailsFragmentDirections;
import com.example.catalyst.QuizListModel;
import com.example.catalyst.QuizListViewModel;
import com.example.catalyst.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.util.List;


public class DetailsFragment extends Fragment implements View.OnClickListener {

    private FirebaseFirestore firebaseFirestore;

    private NavController navController;
    private com.example.catalyst.QuizListViewModel quizListViewModel;
    private int position;

    private FirebaseAuth firebaseAuth;

    private ImageView detailsImage;
    private TextView detailsTitle;
    private TextView detailsDesc;
    private TextView detailsDiff;
    private TextView detailsQuestions;
    private TextView detailsScore;
    // for best and worst
    private TextView bestQuizName;
    private TextView bestQuizScore;
    private TextView worstQuizName;
    private TextView worstQuizScore;

    private Button detailsStartBtn;

    private String quizTitle;
    private String quizId;
    private long totalQuestions = 0;
    private String bestQuizNameVal;
    private String worstQuizNameVal;
    private float bestQuizScoreVal;
    private float worstQuizScoreVal;

    // got from list




    public DetailsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        position = DetailsFragmentArgs.fromBundle(getArguments()).getPosition();

        // initialize args
        bestQuizNameVal = DetailsFragmentArgs.fromBundle(getArguments()).getBestQuizName();
        worstQuizNameVal = DetailsFragmentArgs.fromBundle(getArguments()).getWorstQuizName();
        bestQuizScoreVal =  DetailsFragmentArgs.fromBundle(getArguments()).getBestQuizScore();
        worstQuizScoreVal =  DetailsFragmentArgs.fromBundle(getArguments()).getWorstQuizScore();


        //Initialize UI Elements
        detailsImage = view.findViewById(R.id.details_image);
        detailsTitle = view.findViewById(R.id.details_title);
        detailsDesc = view.findViewById(R.id.details_desc);
        detailsDiff = view.findViewById(R.id.details_difficulty_text);
        detailsQuestions = view.findViewById(R.id.details_questions_text);
        detailsScore = view.findViewById(R.id.details_score_text);

        // best and worst
        bestQuizName = view.findViewById(R.id.best_quiz_name_text);
        worstQuizName = view.findViewById(R.id.worst_quiz_name_text);
        bestQuizScore = view.findViewById(R.id.best_quiz_score_text);
        worstQuizScore = view.findViewById(R.id.worst_quiz_score_text);

        detailsStartBtn = view.findViewById(R.id.details_start_btn);
        detailsStartBtn.setOnClickListener(this);


        // Load Previous Results
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        quizListViewModel = new ViewModelProvider(getActivity()).get(QuizListViewModel.class);
        quizListViewModel.getQuizListModelData().observe(getViewLifecycleOwner(), new Observer<List<QuizListModel>>() {
            @Override
            public void onChanged(List<QuizListModel> quizListModels) {

                Glide.with(getContext())
                        .load(quizListModels.get(position).getImage())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .into(detailsImage);

                detailsTitle.setText(quizListModels.get(position).getName());
                detailsDesc.setText(quizListModels.get(position).getDesc());
                detailsDiff.setText(quizListModels.get(position).getLevel());
                detailsQuestions.setText(quizListModels.get(position).getQuestions() + "");

                quizTitle = quizListModels.get(position).getName();
                quizId = quizListModels.get(position).getQuiz_id();
                totalQuestions = quizListModels.get(position).getQuestions();

                // Load Results Data
                loadResultsData();

                // loadBestWorstData
                loadStatsData();

            }
        });


    }

    private void loadResultsData() {
        firebaseFirestore.collection("QuizList")
                .document(quizId).collection("Results")
                .document(firebaseAuth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        //Get Result
                        Long correct = document.getLong("Correct");
                        Long wrong = document.getLong("Wrong");
                        Long missed = document.getLong("Missed");

                        //Calculate Progress
                        Long total = correct + wrong + missed;
                        Long percent = (correct * 100) / total;

                        detailsScore.setText(percent + "%");
                    } else {
                        //Document Doesn't Exist, and result should say N/A
                    }
                }
            }
        });

    }

    public void loadStatsData() {

        if(bestQuizScoreVal != -1) {
            bestQuizName.setText(bestQuizNameVal);
            bestQuizScore.setText(bestQuizScoreVal + "%");
        }
        else {
            bestQuizName.setText("NA");
            bestQuizScore.setText("NA");
        }

        if(worstQuizScoreVal != 101) {
            worstQuizName.setText(worstQuizNameVal);
            worstQuizScore.setText(worstQuizScoreVal + "%");
        }
        else {
            worstQuizName.setText("NA");
            worstQuizScore.setText("NA");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.details_start_btn:
                DetailsFragmentDirections.ActionDetailsFragmentToQuizFragment action = DetailsFragmentDirections.actionDetailsFragmentToQuizFragment();
                action.setQuizId(quizId);
                action.setQuizName(quizTitle);
                action.setTotalQuestions(totalQuestions);
                navController.navigate(action);
                break;
        }
    }
}
