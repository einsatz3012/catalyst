package com.example.catalyst;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.catalyst.ListFragmentDirections;
import com.example.catalyst.QuizListAdapter;
import com.example.catalyst.QuizListModel;
import com.example.catalyst.QuizListViewModel;
import com.example.catalyst.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class ListFragment extends Fragment implements com.example.catalyst.QuizListAdapter.OnQuizListItemClicked {

    private FirebaseFirestore firebaseFirestore;

    private FirebaseAuth firebaseAuth;

    private NavController navController;

    private RecyclerView listView;
    private com.example.catalyst.QuizListViewModel quizListViewModel;

    private com.example.catalyst.QuizListAdapter adapter;
    private ProgressBar listProgress;

    private Animation fadeInAnim;
    private Animation fadeOutAnim;

    // for worst and best
    private boolean isConnectedToInternet;
    private float fetchedTotal;
    private float fetchedAttempts;
    private String bestQuizName = "NA";
    private double bestQuizScore = -1;
    private String worstQuizName = "NA";
    private double worstQuizScore = 101;


    public ListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        listView = view.findViewById(R.id.list_view);
        listProgress = view.findViewById(R.id.list_progress);
        adapter = new QuizListAdapter(this);

        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setHasFixedSize(true);
        listView.setAdapter(adapter);

        fadeInAnim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        quizListViewModel = new ViewModelProvider(getActivity()).get(QuizListViewModel.class);
        quizListViewModel.getQuizListModelData().observe(getViewLifecycleOwner(), new Observer<List<QuizListModel>>() {
            @Override
            public void onChanged(List<QuizListModel> quizListModels) {
                //Load RecyclerView
                listView.startAnimation(fadeInAnim);
                listProgress.startAnimation(fadeOutAnim);

                adapter.setQuizListModels(quizListModels);
                adapter.notifyDataSetChanged();
            }
        });


        //checking for leaderboard

        // check if device is connected to internet and it is working
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    isConnectedToInternet = isInternetWorking();
                    Log.v("internet_log", ": " + isConnectedToInternet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        if (isConnectedToInternet) {

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firebaseAuth = firebaseAuth.getInstance();

            firestore.collection("QuizList").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot documentSnapshots : task.getResult()) {

//                        Log.v("tag", ":" + documentSnapshots.getId()); // gives the id of quizes

                            firestore.collection("QuizList")
                                    .document(documentSnapshots.getId()).collection("Results").get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                boolean isResultsExist = task.getResult().isEmpty();

                                                if (!isResultsExist) {

                                                    Log.v("leader", "Result collection exists: " + documentSnapshots.get("name"));

                                                    firestore.collection("QuizList")
                                                            .document(documentSnapshots.getId()).collection("Results")
                                                            .document(firebaseAuth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                                                            DocumentSnapshot result = task.getResult();

                                                            if (task.isSuccessful()) {
                                                                boolean isUidExits = task.getResult().exists();

                                                                if (isUidExits) {
                                                                    Log.v("leader", "uid document exists: " + documentSnapshots.get("name"));

                                                                    fetchedTotal = (float) result.getLong("totalScore");
                                                                    fetchedAttempts = (float) result.getLong("attempts");

                                                                    float average = fetchedTotal / fetchedAttempts;
                                                                    double roundOff = Math.round(average * 100.0) / 100.0;
                                                                    Log.v("leader", "fetched avg" + roundOff + "%");

                                                                    if (roundOff > bestQuizScore) {
                                                                        bestQuizScore = roundOff;
                                                                        bestQuizName = documentSnapshots.get("name").toString();

                                                                        Log.v("results", "best score: " + bestQuizScore + "s........name" + bestQuizName);
                                                                        Log.v("results", "worst score: " + worstQuizScore + "s........name" + worstQuizName);
                                                                    }
                                                                    if (roundOff < worstQuizScore) {
                                                                        worstQuizScore = roundOff;
                                                                        worstQuizName = documentSnapshots.get("name").toString();

                                                                        Log.v("results", "best score: " + bestQuizScore + "s........name" + bestQuizName);
                                                                        Log.v("results", "worst score: " + worstQuizScore + "s........name" + worstQuizName);
                                                                    }


                                                                } else {
                                                                    Log.v("leader", "uid doc doesn't exists: " + documentSnapshots.get("name"));
                                                                }

                                                            } else {
                                                                Log.v("leader", "Task UID Unsuccessful ");
                                                            }

                                                        }
                                                    });
                                                } else {
                                                    Log.v("leader", "Result collection doesn't exists: " + documentSnapshots.get("name"));
                                                }
                                            } else {
                                                Log.v("leader", "Task Unsuccessful");
                                            }
                                        }
                                    });

                        }
                        Log.v("results", "best outer score: " + bestQuizScore + "s........name" + bestQuizName);
                        Log.v("results", "worst outer score: " + worstQuizScore + "s........name" + worstQuizName);
                    } else {
                        Log.v("Leader", "Querying unsuccessful");
                    }
                }

            });
            
        }
    }

    private boolean isInternetWorking() {
        boolean success = false;
        try {
            URL url = new URL("https://google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.connect();
            success = connection.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;

    }

    @Override
    public void onItemClicked(int position) {
        ListFragmentDirections.ActionListFragmentToDetailsFragment action = ListFragmentDirections.actionListFragmentToDetailsFragment();
        action.setPosition(position);
        action.setBestQuizName(bestQuizName);
        action.setBestQuizScore((float) bestQuizScore);
        action.setWorstQuizName(worstQuizName);
        action.setWorstQuizScore((float) worstQuizScore);
        navController.navigate(action);
    }
}
